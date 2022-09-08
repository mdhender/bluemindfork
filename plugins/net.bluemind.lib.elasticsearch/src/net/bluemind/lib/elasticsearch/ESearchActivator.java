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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest.AliasActions;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesAction;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequestBuilder;
import org.elasticsearch.action.admin.indices.get.GetIndexResponse;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsResponse;
import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsRequest;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.Requests;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryAction;
import org.elasticsearch.index.reindex.DeleteByQueryRequestBuilder;
import org.elasticsearch.search.Scroll;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.elasticsearch.xcontent.XContentType;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;

import io.vertx.core.json.JsonObject;
import net.bluemind.network.topology.Topology;
import net.bluemind.network.utils.NetworkHelper;

public final class ESearchActivator implements BundleActivator {

	private static final String ES_TAG = "bm/es";
	private static final Map<String, Client> clients = new ConcurrentHashMap<>();
	private static final Map<String, Lock> refreshLocks = new ConcurrentHashMap<>();
	private static final Map<String, IndexDefinition> indexes = new HashMap<>();
	private static Logger logger = LoggerFactory.getLogger(ESearchActivator.class);

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
		logger.info("ES activator started , schemas : {}", indexes.keySet());

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
	public static final void initClient(Client client) {
		clients.put(ES_TAG, client);
	}

	public static final void initClasspath() {
		Client client = initClient(ES_TAG);
		if (client != null) {
			clients.put(ES_TAG, client);
		} else {
			logger.warn("elasticsearch node not found");
		}
	}

	public static final void index(String index, String kind, String id, Map<String, Object> obj) {
		IndexResponse result = asyncIndexImpl(index, kind, id, obj).actionGet();
		logger.debug("[{}] +'{}', v:{}", index, id, result.getVersion());
	}

	public static final void asyncIndex(String index, String kind, String id, Map<String, Object> obj) {
		asyncIndexImpl(index, kind, id, obj);
	}

	public static final void delete(String index, String kind, String id) {
		Client cli = getClient();
		cli.prepareDelete(index, kind, id).execute().actionGet();
	}

	public static final void deleteByQuery(String index, String query) {

		QueryStringQueryBuilder qb = QueryBuilders.queryStringQuery(query);
		deleteByQuery(index, qb);
	}

	public static final void deleteByQuery(String index, QueryBuilder query) {
		Client cli = getClient();
		DeleteByQueryRequestBuilder req = new DeleteByQueryRequestBuilder(cli, DeleteByQueryAction.INSTANCE)
				.abortOnVersionConflict(false);
		req.source().setIndices(index).setQuery(query);
		BulkByScrollResponse ret = req.get();
		logger.info("deleteByQuery on index {} took {}", index, ret.getTook());
	}

	public static final SearchHits search(String index, String query) {
		return search(index, query, 0, 256, "*");
	}

	public static final SearchHits search(String index, String query, int from, int size) {
		return search(index, query, from, size, "*");
	}

	public static final SearchHits search(String index, String query, int from, int size, String field) {
		Client cli = getClient();
		QueryStringQueryBuilder q = QueryBuilders.queryStringQuery(query);

		SearchResponse sr = cli.prepareSearch(index).setSearchType(SearchType.QUERY_THEN_FETCH) // type
				.setQuery(q) // query
				.addStoredField(field) // tweak API...
				.setFrom(from).setSize(size) // pagination
				.execute().actionGet();
		SearchHits hits = sr.getHits();
		logger.info("{} hit(s) {}ms.", hits.getTotalHits().value, sr.getTook().millis());

		return hits;
	}

	public static SearchRequestBuilder prepareSearch(String index, String query) {
		Client cli = getClient();
		QueryStringQueryBuilder q = QueryBuilders.queryStringQuery(query).analyzeWildcard(true)
				.defaultOperator(Operator.AND);

		return cli.prepareSearch(index).setScroll((Scroll) null).setSearchType(SearchType.QUERY_THEN_FETCH).setQuery(q);
	}

	public static SearchRequestBuilder prepareSearch(String index, QueryBuilder query) {
		Client cli = getClient();
		return cli.prepareSearch(index).setScroll((Scroll) null).setSearchType(SearchType.QUERY_THEN_FETCH)
				.setQuery(query);
	}

	public static final void update(String index, String kind, String id, String field, Object value) {
		long time = System.currentTimeMillis();
		Client cli = getClient();
		UpdateRequestBuilder urb = cli.prepareUpdate().setIndex(index).setType(kind).setId(id);
		urb.setDoc(field, value);
		urb.execute().actionGet();
		time = System.currentTimeMillis() - time;
		logger.info("Update response for {}/{} in {}ms.", kind, id, time);
	}

	private static ActionFuture<IndexResponse> asyncIndexImpl(String index, String kind, String id,
			Map<String, Object> obj) {
		Client cli = getClient();
		IndexRequestBuilder req = cli.prepareIndex(index, kind, id);
		return req.setSource(obj).execute();
	}

	public static Client getClient(String tag) {
		Client client = clients.computeIfAbsent(tag, ESearchActivator::initClient);
		if (client == null) {
			logger.error("no elasticsearch instance found for tag {}", tag);
		}
		return client;
	}

	public static Client getClient() {
		return getClient(ES_TAG);
	}

	public static void putMeta(String index, String k, String v) {
		JsonObject js = new JsonObject();
		js.put("_meta", new JsonObject().put(k, v));
		BytesReference br = BytesReference.fromByteBuffer(js.toBuffer().getByteBuf().nioBuffer());
		AcknowledgedResponse resp = getClient().admin().indices()
				.putMapping(Requests.putMappingRequest(index).type("_doc").source(br, XContentType.JSON)).actionGet();
		logger.info("putMeta({}, {}, {}) => {}", index, k, v, resp.isAcknowledged());
	}

	public static String getMeta(String index, String key) {
		GetMappingsResponse res = getClient().admin().indices().prepareGetMappings(index).get();
		JsonObject mappings = new JsonObject(res.toString());
		JsonObject meta = mappings.getJsonObject(index).getJsonObject("mappings").getJsonObject("_meta");
		return Optional.ofNullable(meta).map(js -> js.getString(key)).orElse(null);
	}

	public static Client createClient(Collection<String> hosts) {
		try {
			org.elasticsearch.common.settings.Settings.Builder settingsBuilder = Settings.builder();

			File mcastIdFile = new File("/etc/bm/mcast.id");
			if (mcastIdFile.exists()) {
				settingsBuilder.put("cluster.name",
						"bluemind-" + Files.asCharSource(mcastIdFile, StandardCharsets.US_ASCII).readFirstLine());
			} else {
				logger.warn("/etc/bm/mcast.id not found");
				settingsBuilder.put("cluster.name", "bluemind");
			}

			settingsBuilder.put("node.name", "client-" + UUID.randomUUID());
			settingsBuilder.put("client.transport.ping_timeout", "20s");
			Settings settings = settingsBuilder.build();
			TransportClient cli = new PreBuiltTransportClient(settings);
			StringBuilder hlist = new StringBuilder();
			for (String host : hosts) {
				cli.addTransportAddress(new TransportAddress(InetAddress.getByName(host), 9300));
				hlist.append(' ').append(host);
			}
			logger.info("Created client with {} nodes:{}", hosts.size(), hlist);
			return cli;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static Client initClient(String tag) {
		Collection<String> hosts = hosts(tag);
		if (hosts == null || hosts.isEmpty()) {
			logger.warn("Es host missing for tag {}", tag);
			return null;
		}
		return createClient(hosts);
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
		Client cli = getClient();
		long time = System.currentTimeMillis();
		cli.admin().indices().prepareRefresh(index).execute().actionGet();
		long ms = (System.currentTimeMillis() - time);
		if (ms > 5) {
			logger.info("time to refresh {} : {}ms", index, ms);
		}
	}

	public static void disableFlush(String... indexes) {
		Client cli = getClient();
		logger.info("Disabling flush for {} indexes", indexes.length);
		manageFlush(cli, true, indexes);
	}

	public static void enableFlush(String... indexes) {
		Client cli = getClient();
		logger.info("Enabling flush for {} indexes", indexes.length);
		manageFlush(cli, false, indexes);
	}

	private static void manageFlush(Client cli, boolean disable, String... indexes) {
		for (String index : indexes) {
			UpdateSettingsRequest request = new UpdateSettingsRequest(index);
			request.settings(Settings.builder().put("index.translog.disable_flush", disable).build());
			try {
				cli.admin().indices().updateSettings(request).actionGet();
			} catch (Exception e) {
				logger.warn("Cannot change flush settings of index {}:{}", index, e.getMessage());
			}
		}
	}

	public static void flush(String index) {
		Client cli = getClient();
		long time = System.currentTimeMillis();
		cli.admin().indices().prepareFlush(index).execute().actionGet();
		long ms = (System.currentTimeMillis() - time);
		if (ms > 5) {
			logger.info("time to flush {} : {}ms", index, ms);
		}
	}

	public static void resetAll() {
		Client client = ESearchActivator.getClient();
		GetIndexResponse resp = client.admin().indices().prepareGetIndex().addIndices("*").get();
		List<String> indices = Arrays.asList(resp.indices()).stream().filter(indexName -> !indexName.startsWith(".ds-"))
				.collect(Collectors.toList());

		if (!indices.isEmpty()) {
			logger.warn("Full ES reset of {} ", indices);
			client.admin().indices().prepareDelete(indices.toArray(new String[0])).get();
		}
	}

	private static Collection<String> hosts(String tag) {
		return Topology.get().nodes().stream().filter(iv -> iv.value.tags.contains(tag)).map(iv -> iv.value.address())
				.collect(Collectors.toList());
	}

	public static void resetIndex(String index) {
		waitForElasticsearchHosts();
		Client client = ESearchActivator.getClient();
		resetIndex(client, index);
	}

	private static void resetIndex(Client client, String index) {
		logger.info("Resetting index {}", index);
		deleteIndex(client, index);
		initIndex(client, index);
	}

	private static void deleteIndex(Client client, String index) {
		deleteIfExists(client, index);
		IndexDefinition indexDefinition = indexes.get(index);
		if (indexDefinition != null) {
			int count = indexDefinition.count();
			boolean isRewritable = indexDefinition.isRewritable();
			if (count > 1 || isRewritable) {
				long realCount = new GetAliasesRequestBuilder(client, GetAliasesAction.INSTANCE).get().getAliases()
						.keySet().stream()//
						.filter(indexDefinition::supportsIndex) //
						.map(indexName -> deleteIfExists(client, indexName)) //
						.count();
				if (count != realCount) {
					logger.warn("Found {} {} indexes which differs from the expected count of {}", realCount, index,
							count);
				}
			}
		}
		logger.info("All matching indices deleted for {}", index);
	}

	private static boolean deleteIfExists(Client client, String index) {
		try {
			client.admin().indices().prepareDelete(index).execute().actionGet();
			logger.info("index '{}' deleted.", index);
			return true;
		} catch (Exception e) {
			logger.warn("index '{}' can't be delete: {}", index, e.getMessage());
			return false;
		}
	}

	private static Optional<IndexDefinition> indexDefinitionOf(String index) {
		return indexes.values().stream().filter(item -> item.supportsIndex(index)).findFirst();
	}

	public static void addAliasTo(String aliasName, String indexName, boolean isWriteIndex) {
		waitForElasticsearchHosts();
		Client client = ESearchActivator.getClient();
		logger.info("add alias {} to {} (write:{})", aliasName, indexName, isWriteIndex);
		AliasActions addAliasActions = AliasActions.add().index(indexName).alias(aliasName).writeIndex(isWriteIndex);
		IndicesAliasesRequest addAliasRequest = Requests.indexAliasesRequest().addAliasAction(addAliasActions);
		client.admin().indices().aliases(addAliasRequest).actionGet();
	}

	private static void waitForElasticsearchHosts() {
		Collection<String> hosts = hosts(ES_TAG);
		if (hosts != null) {
			for (String host : hosts) {
				new NetworkHelper(host).waitForListeningPort(9300, 30, TimeUnit.SECONDS);
			}
		}
	}

	public static Optional<String> initIndexIfNotExists(Client client, String index) {
		return indexDefinitionOf(index).map(indexDefinition -> {
			return new GetAliasesRequestBuilder(client, GetAliasesAction.INSTANCE).get().getAliases().keySet().stream() //
					.filter(indexDefinition::supportsIndex) //
					.findFirst() //
					.orElseGet(() -> {
						initIndex(client, indexDefinition.index);
						return indexDefinition.index;
					});
		});
	}

	public static void initIndex(Client client, String index) {
		Optional<IndexDefinition> indexDefinition = indexDefinitionOf(index);
		if (!indexDefinition.isPresent()) {
			logger.warn("no SCHEMA for {}", index);
			try {
				client.admin().indices().prepareCreate(index).execute().actionGet();
			} catch (Exception e) {
				logger.warn("failed to create indice {} : {}", index, e.getMessage());
				throw e;
			}
		} else {
			IndexDefinition definition = indexDefinition.get();
			int count = definition.index.equals(index) ? definition.count() : 1;
			byte[] schema = definition.schema;
			try {
				for (int i = 1; i <= count; i++) {
					String indexName = (count == 1) ? index : index + "_" + i;
					logger.info("init index '{}' with settings & schema", indexName);
					client.admin().indices().prepareCreate(indexName).setSource(schema, XContentType.JSON).get();
					logger.info("index '{}' created, waiting for green...", indexName);
					ClusterHealthResponse resp = client.admin().cluster().prepareHealth(indexName)
							.setWaitForGreenStatus().get();
					definition.rewritableIndex().ifPresent(
							rewritableIndex -> addRewritableIndexAliases(client, indexName, rewritableIndex));
					logger.info("add index '{}' aliases", indexName);
					logger.debug("index health {}", resp);
				}
			} catch (Exception e) {
				logger.warn("failed to create indice '{}' : {}", index, e.getMessage(), e);
				throw e;
			}
		}
	}

	private static void addRewritableIndexAliases(Client client, String name, RewritableIndex index) {
		AliasActions readAlias = AliasActions.add().index(name).alias(index.readAlias()).writeIndex(false);
		AliasActions writeAlias = AliasActions.add().index(name).alias(index.writeAlias()).writeIndex(true);
		IndicesAliasesRequest addAliasRequest = Requests.indexAliasesRequest() //
				.addAliasAction(readAlias) //
				.addAliasAction(writeAlias);
		client.admin().indices().aliases(addAliasRequest).actionGet();
	}

	public static byte[] getIndexSchema(String indexName) {
		return indexes.get(indexName).schema;
	}

	public static RewritableIndex getRewritableIndex(String indexName) {
		return indexes.get(indexName).rewritableIndex;
	}

	public static void resetIndexes() {
		indexes.keySet().forEach(ESearchActivator::resetIndex);
	}

	public static void clearClientCache() {
		clients.clear();
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
}
