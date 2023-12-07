/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.lib.elasticsearch;

import static java.util.stream.Collectors.joining;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthSchemeProvider;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.InvalidRegistryObjectException;
import org.eclipse.core.runtime.Platform;
import org.elasticsearch.client.RestClient;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.ByteStreams;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;

import co.elastic.clients.ApiClient;
import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.HealthStatus;
import co.elastic.clients.elasticsearch._types.analysis.Analyzer;
import co.elastic.clients.elasticsearch.indices.PutMappingResponse;
import co.elastic.clients.elasticsearch.indices.resolve_index.ResolveIndexItem;
import co.elastic.clients.json.DelegatingDeserializer;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.json.ObjectDeserializer;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import net.bluemind.configfile.elastic.ElasticsearchConfig;
import net.bluemind.lib.elasticsearch.exception.ElasticIndexException;
import net.bluemind.network.topology.IServiceTopology;
import net.bluemind.network.topology.Topology;
import net.bluemind.network.utils.NetworkHelper;

public final class ESearchActivator implements BundleActivator {
	private static Logger logger = LoggerFactory.getLogger(ESearchActivator.class);

	private static Config config;
	private static final String ES_TAG = "bm/es";
	private static final Map<String, ElasticsearchTransport> transports = new ConcurrentHashMap<>();
	private static final Map<String, Lock> refreshLocks = new ConcurrentHashMap<>();
	private static final Map<String, IndexDefinition> indexes = new HashMap<>();

	/**
	 * key for {@link #putMeta(String, String, String)}. Indices with this prop will
	 * be ignored when allocating new aliases
	 */
	public static final String BM_MAINTENANCE_STATE_META_KEY = "bmMaintenanceState";

	static {
		System.setProperty("es.set.netty.runtime.available.processors", "false");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext )
	 */
	@Override
	public void start(BundleContext bundleContext) throws Exception {
		fixupElasticsearchClientSerde();
		loadIndexSchema();
		setupConfig();
		logger.info("ES activator started , schemas : {}", indexes.keySet());
	}

	private static void fixupElasticsearchClientSerde() {
		@SuppressWarnings("unchecked")
		ObjectDeserializer<Analyzer> unwrapped = (ObjectDeserializer<Analyzer>) DelegatingDeserializer
				.unwrap(Analyzer._DESERIALIZER);
		unwrapped.setTypeProperty("type", "custom");
	}

	private static void loadIndexSchema() throws IOException, InvalidRegistryObjectException, CoreException {
		IExtensionPoint ep = Platform.getExtensionRegistry().getExtensionPoint("net.bluemind.elasticsearch.schema");
		for (IExtension ext : ep.getExtensions()) {
			for (IConfigurationElement ce : ext.getConfigurationElements()) {
				String index = ce.getAttribute("index");
				String schema = ce.getAttribute("schema");
				// to override the count for faster testing
				int count = Integer.parseInt(System.getProperty("es." + index + ".count", ce.getAttribute("count")));
				ISchemaMatcher matcher = (ce.getAttribute("schemamatcher") != null)
						? (ISchemaMatcher) ce.createExecutableExtension("schemamatcher")
						: null;
				boolean rewritable = Boolean.parseBoolean(ce.getAttribute("rewritable"));
				Bundle bundle = Platform.getBundle(ext.getContributor().getName());
				URL url = bundle.getResource(schema);
				try (InputStream in = url.openStream()) {
					indexes.put(index,
							new IndexDefinition(index, ByteStreams.toByteArray(in), matcher, count, rewritable));
					refreshLocks.put(index, new ReentrantLock());
				}

				if (logger.isDebugEnabled()) {
					logger.debug("schema for index {}: \n {} ", index, new String(indexes.get(index).schema));
				}
			}
		}
	}

	private static synchronized void setupConfig() {
		config = ElasticsearchClientConfig.get();
		ElasticsearchClientConfig.addListener(newConfig -> {
			config = newConfig;
			transports.clear();
		});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(BundleContext bundleContext) throws Exception {
		// Nothing to do
	}

	@VisibleForTesting
	public static final void initClient(ElasticsearchTransport transport) {
		transports.put(ES_TAG, transport);
	}

	public static final void initClasspath() {
		ElasticsearchTransport client = initTransport(ES_TAG);
		if (client != null) {
			transports.put(ES_TAG, client);
		} else {
			logger.warn("elasticsearch node not found");
		}
	}

	public static ElasticsearchClient getClient(List<String> hosts) {
		ElasticsearchTransport transport = ESearchActivator.createTansport(hosts);
		return new ElasticsearchClient(transport);
	}

	public static ElasticsearchClient getClient() {
		ElasticsearchTransport transport = transports.computeIfAbsent(ES_TAG, ESearchActivator::initTransport);
		return buildClient(ES_TAG, transport, ElasticsearchClient::new);
	}

	public static ElasticsearchAsyncClient getAysncClient() {
		ElasticsearchTransport transport = transports.computeIfAbsent(ES_TAG, ESearchActivator::initTransport);
		return buildClient(ES_TAG, transport, ElasticsearchAsyncClient::new);
	}

	public static <T extends ApiClient<?, ?>> T buildClient(String tag, ElasticsearchTransport transport,
			Function<ElasticsearchTransport, T> builder) {
		if (transport == null) {
			logger.error("no elasticsearch instance found for tag {}", tag);
			return null;
		} else {
			return builder.apply(transport);
		}
	}

	private static ElasticsearchTransport initTransport(String tag) {
		List<String> hosts = hosts(tag);
		if (hosts == null || hosts.isEmpty()) {
			logger.warn("Es host missing for tag {}", tag);
			return null;
		}
		return createTansport(hosts);
	}

	private static List<String> hosts(String tag) {
		return Topology.getIfAvailable().map(t -> topoHots(t, tag)).orElse(Collections.emptyList());
	}

	private static List<String> topoHots(IServiceTopology topo, String tag) {
		return topo.nodes().stream().filter(iv -> iv.value.tags.contains(tag)).map(iv -> iv.value.address()).toList();
	}

	public static ElasticsearchTransport createTansport(List<String> hosts) {
		HttpHost[] httpHosts = hosts.stream().map(host -> new HttpHost(host, 9200)).toArray(l -> new HttpHost[l]);
		RestClient restClient;
		try {
			ElasticsearchConfig.Client clientConfig = ElasticsearchConfig.Client.of(config);
			restClient = RestClient.builder(httpHosts) //
					.setRequestConfigCallback(builder -> builder //
							.setConnectTimeout((int) clientConfig.timeout().connect().toMillis()) //
							.setSocketTimeout((int) clientConfig.timeout().socket().toMillis()) //
							.setConnectionRequestTimeout((int) clientConfig.timeout().request().toMillis()))
					.setHttpClientConfigCallback(builder -> builder //
							.setDefaultAuthSchemeRegistry(RegistryBuilder.<AuthSchemeProvider>create().build())//
							.setDefaultCredentialsProvider(new BasicCredentialsProvider())//
							.disableAuthCaching()//
							.setMaxConnTotal(clientConfig.pool().maxConnTotal()) //
							.setMaxConnPerRoute(clientConfig.pool().maxConnPerRoute())) //
					.build();
		} catch (ConfigException e) {
			restClient = RestClient.builder(httpHosts).build();
			logger.error("[es] Elasticsearch client configuration is invalid, using defaults: {}", e.getMessage());
		}
		ElasticsearchTransport transport = new RetryingRestClientTransport(restClient, new JacksonJsonpMapper(),
				config);
		if (logger.isInfoEnabled()) {
			logger.info("[es] Created client with {} nodes:{}", hosts.size(), hosts.stream().collect(joining(" ")));
		}

		return transport;
	}

	public static void putMeta(String index, String k, String v) throws ElasticIndexException {
		PutMappingResponse response;
		try {
			response = getClient().indices().putMapping(m -> m.index(index).meta(k, JsonData.of(v)));
			logger.info("[es] putMeta({}, {}, {}) => {}", index, k, v, response.acknowledged());
		} catch (ElasticsearchException | IOException e) {
			throw new ElasticIndexException(index, e);
		}
	}

	public static void refreshIndex(String index) {
		if (refreshLocks.computeIfAbsent(index, k -> new ReentrantLock()).tryLock()) {
			try {
				refresh(index);
			} finally {
				refreshLocks.get(index).unlock();
			}
		} else {
			try {
				boolean acquiredLock = refreshLocks.get(index).tryLock(10, TimeUnit.SECONDS);
				if (acquiredLock) {
					refreshLocks.get(index).unlock();
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}

	private static void refresh(String index) {
		long time = System.currentTimeMillis();
		try {
			getClient().indices().refresh(r -> r.index(index));
		} catch (ElasticsearchException | IOException e) {
			throw new ElasticIndexException(index, e);
		}
		long ms = (System.currentTimeMillis() - time);
		if (ms > 5) {
			logger.info("time to refresh {}: {}ms", index, ms);
		}
	}

	public static void resetIndexes() {
		indexes.keySet().forEach(ESearchActivator::resetIndex);
	}

	public static void resetIndex(String index) {
		waitForElasticsearchHosts();
		ElasticsearchClient esClient = ESearchActivator.getClient();
		logger.info("Resetting index {}", index);
		deleteIndex(esClient, index);
		initIndex(esClient, index);
	}

	private static void deleteIndex(ElasticsearchClient esClient, String index) {
		deleteIfExists(esClient, index);
		IndexDefinition indexDefinition = indexes.get(index);
		if (indexDefinition != null) {
			int count = indexDefinition.count();
			boolean isRewritable = indexDefinition.isRewritable();
			if (count > 1 || isRewritable) {
				long realCount = indexNames(esClient).stream() //
						.filter(indexDefinition::supportsIndex) //
						.map(indexName -> deleteIfExists(esClient, indexName)) //
						.count();
				if (count != realCount) {
					logger.warn("Found {} {} indexes which differs from the expected count of {}", realCount, index,
							count);
				}
			}
		}
		logger.info("All matching indices deleted for {}", index);
	}

	private static boolean deleteIfExists(ElasticsearchClient esClient, String index) {
		try {
			esClient.indices().delete(d -> d.index(index));
			logger.info("index '{}' deleted.", index);
			return true;
		} catch (ElasticsearchException e) {
			if (e.error() != null && "index_not_found_exception".equals(e.error().type())) {
				logger.warn("index '{}' not found, can't be delete", index);
				return false;
			}
			throw new ElasticIndexException(index, e);
		} catch (IOException e) {
			throw new ElasticIndexException(index, e);
		}
	}

	public static Optional<String> initIndexIfNotExists(String index) {
		ElasticsearchClient esClient = getClient();
		return indexDefinitionOf(index).map(indexDefinition -> indexNames(esClient).stream() //
				.filter(indexDefinition::supportsIndex) //
				.findFirst() //
				.orElseGet(() -> {
					initIndex(esClient, indexDefinition.index);
					return indexDefinition.index;
				}));
	}

	public static void initIndex(ElasticsearchClient esClient, String index) {
		IndexAliasCreator mailspoolCreator = IndexAliasCreator.get();
		logger.info("Initialising indices using mode {}", IndexAliasMode.getMode());
		indexDefinitionOf(index).ifPresentOrElse(definition -> {
			int count = definition.index.equals(index) ? definition.count() : 1;
			byte[] schema = definition.schema;
			try {
				for (int i = 1; i <= count; i++) {
					String indexName = mailspoolCreator.getIndexName(index, count, i);
					logger.info("init index '{}' with settings & schema", indexName);
					esClient.indices().create(c -> c.index(indexName).withJson(new ByteArrayInputStream(schema)));
					logger.info("index '{}' created, waiting for green...", indexName);
					esClient.cluster().health(h -> h.index(indexName).waitForStatus(HealthStatus.Green));
					definition.rewritableIndex().ifPresent(
							rewritableIndex -> addRewritableIndexAliases(esClient, indexName, rewritableIndex));
					mailspoolCreator.addAliases(index, indexName, count, i);
					logger.info("added index '{}' aliases", indexName);
				}
			} catch (Exception e) {
				throw new ElasticIndexException(index, e);
			}
		}, () -> {
			logger.warn("no SCHEMA for {}", index);
			try {
				esClient.indices().create(c -> c.index(index));
			} catch (Exception e) {
				throw new ElasticIndexException(index, e);
			}
		});
	}

	public static List<String> indexNames(ElasticsearchClient esClient) {
		try {
			return esClient.indices().resolveIndex(r -> r.name("*")).indices() //
					.stream().map(ResolveIndexItem::name).toList();
		} catch (ElasticsearchException | IOException e) {
			logger.error("[es][indices] Failed to list indices", e);
			return Collections.emptyList();
		}
	}

	private static void addRewritableIndexAliases(ElasticsearchClient esClient, String name, RewritableIndex index) {
		try {
			esClient.indices().updateAliases(u -> u //
					.actions(a -> a.add(add -> add.index(name).alias(index.readAlias()).isWriteIndex(false)))
					.actions(a -> a.add(add -> add.index(name).alias(index.writeAlias()).isWriteIndex(true))));
		} catch (Exception e) {
			throw new ElasticIndexException(name, e);
		}
	}

	private static Optional<IndexDefinition> indexDefinitionOf(String index) {
		return indexes.values().stream().filter(item -> item.supportsIndex(index)).findFirst();
	}

	public static byte[] getIndexSchema(String indexName) {
		return indexes.get(indexName).schema;
	}

	public static int getIndexCount(String index) {
		return indexDefinitionOf(index).map(IndexDefinition::count).orElse(0);
	}

	public static RewritableIndex getRewritableIndex(String indexName) {
		return indexes.get(indexName).rewritableIndex;
	}

	public static void clearClientCache() {
		transports.clear();
	}

	public static MailspoolStats mailspoolStats() {
		return new MailspoolStats(getClient());
	}

	private static class IndexDefinition {
		private final String index;
		private final byte[] schema;
		private final ISchemaMatcher matcher;
		private final int cnt;
		private final RewritableIndex rewritableIndex;

		IndexDefinition(String index, byte[] schema, ISchemaMatcher matcher, int count, boolean rewritable) {
			this.index = index;
			this.schema = schema;
			this.matcher = matcher;
			this.cnt = count;
			this.rewritableIndex = rewritable ? RewritableIndex.fromPrefix(index) : null;
		}

		public int count() {
			return Integer.parseInt(System.getProperty("es." + index + ".count", "" + cnt));
		}

		public boolean isRewritable() {
			return rewritableIndex != null;
		}

		public Optional<RewritableIndex> rewritableIndex() {
			return Optional.ofNullable(rewritableIndex);
		}

		boolean supportsIndex(String name) {
			if (name.equals(index)) {
				return true;
			}
			if (matcher != null) {
				return matcher.supportsIndexName(name);
			}
			if (isRewritable()) {
				return name.startsWith(index + "_");
			}
			return false;
		}
	}

	public static void waitForElasticsearchHosts() {
		Collection<String> hosts = hosts(ES_TAG);
		if (hosts != null) {
			for (String host : hosts) {
				new NetworkHelper(host).waitForListeningPort(9200, 30, TimeUnit.SECONDS);
			}
		}
	}

}
