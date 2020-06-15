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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.indices.get.GetIndexResponse;
import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsRequest;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.common.xcontent.XContentType;
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
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.ByteStreams;
import com.google.common.io.Files;

import net.bluemind.network.topology.Topology;
import net.bluemind.network.utils.NetworkHelper;

public final class ESearchActivator implements BundleActivator {

	private static BundleContext context;
	private static final Map<String, Client> clients = new ConcurrentHashMap<>();
	private static final Map<String, Lock> refreshLocks = new ConcurrentHashMap<>();
	private static final Map<String, IndexDefinition> indexes = new HashMap<>();
	private static Logger logger = LoggerFactory.getLogger(ESearchActivator.class);

	static {
		System.setProperty("es.set.netty.runtime.available.processors", "false");
	}

	static BundleContext getContext() {
		return context;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext )
	 */
	public void start(BundleContext bundleContext) throws Exception {

		ESearchActivator.context = bundleContext;
		IExtensionPoint ep = Platform.getExtensionRegistry().getExtensionPoint("net.bluemind.elasticsearch.schema");

		for (IExtension ext : ep.getExtensions()) {
			for (IConfigurationElement ce : ext.getConfigurationElements()) {
				String index = ce.getAttribute("index");
				String schema = ce.getAttribute("schema");
				// to override the count for faster testing
				int count = Integer.parseInt(System.getProperty("es." + index + ".count", ce.getAttribute("count")));

				ISchemaMatcher matcher = null;
				if (ce.getAttribute("schemamatcher") != null) {
					matcher = (ISchemaMatcher) ce.createExecutableExtension("schemamatcher");
				}
				Bundle bundle = Platform.getBundle(ext.getContributor().getName());
				URL url = bundle.getResource(schema);
				try (InputStream in = url.openStream()) {
					indexes.put(index, new IndexDefinition(index, ByteStreams.toByteArray(in),
							Optional.ofNullable(matcher), count));
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
	public void stop(BundleContext bundleContext) throws Exception {
		ESearchActivator.context = null;
	}

	public static final void initClient(Client client) {
		clients.put("bm/es", client);
	}

	public static final void initClasspath() {
		Client client = initClient("bm/es");
		if (client != null) {
			clients.put("bm/es", client);
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
		logger.debug("sByQuery: " + q);

		SearchResponse sr = cli.prepareSearch(index).setSearchType(SearchType.QUERY_THEN_FETCH) // type
				.setQuery(q) // query
				.addStoredField(field) // tweak API...
				.setFrom(from).setSize(size) // pagination
				.execute().actionGet();
		SearchHits hits = sr.getHits();
		logger.info("{} hit(s) {}ms.", hits.getTotalHits(), sr.getTook().millis());

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
		Client client = clients.get(tag);
		if (null == client) {
			client = initClient(tag);
			if (client != null) {
				clients.put(tag, client);
			} else {
				logger.warn("no elasticsearch instance found for tag {}", tag);
			}
		}
		return client;
	}

	public static Client getClient() {
		return getClient("bm/es");
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
			Settings settings = settingsBuilder.build();
			TransportClient cli = new PreBuiltTransportClient(settings);
			StringBuilder hlist = new StringBuilder();
			for (String host : hosts) {
				cli.addTransportAddress(new TransportAddress(InetAddress.getByName(host), 9300));
				hlist.append(' ').append(host);
			}
			logger.info("Created client with {} nodes:{}", hosts.size(), hlist.toString());
			return cli;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static Client initClient(String tag) {
		Collection<String> hosts = hosts(tag);
		if (hosts == null || hosts.isEmpty()) {
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

		if (resp.indices().length > 0) {
			logger.warn("Full ES reset of {} ", (Object) resp.indices());
			client.admin().indices().prepareDelete(resp.indices()).get();
		}
	}

	private static Collection<String> hosts(String tag) {
		return Topology.get().nodes().stream().filter(iv -> iv.value.tags.contains(tag)).map(iv -> iv.value.address())
				.collect(Collectors.toList());
	}

	public static void resetIndex(String index) {
		Collection<String> hosts = hosts("bm/es");
		if (hosts != null) {
			for (String host : hosts) {
				new NetworkHelper(host).waitForListeningPort(9300, 30, TimeUnit.SECONDS);
			}
		}
		Client client = ESearchActivator.getClient();
		resetIndex(client, index);
	}

	private static void resetIndex(Client client, String index) {
		logger.info("reset index {}", index);
		try {
			client.admin().indices().prepareDelete(index).execute().actionGet();
			if (isPrimary(index)) {
				int count = indexes.values().stream().filter(item -> item.supportsIndex(index)).findFirst().get()
						.count();
				if (count > 1) {
					for (int i = 1; i <= count; i++) {
						String indexName = index + "_" + i;
						logger.info("reset index {}", indexName);
						client.admin().indices().prepareDelete(indexName).execute().actionGet();
					}
				}
			}
			logger.info("index {} reset.", index);
		} catch (Exception e) {

		}

		initIndex(client, index, isPrimary(index));
	}

	private static boolean isPrimary(String index) {
		return !Pattern.compile(".*_\\d+").matcher(index).matches();
	}

	private static void initIndex(Client client, String index, boolean primary) {
		Optional<IndexDefinition> indexDefinition = indexes.values().stream().filter(item -> item.supportsIndex(index))
				.findFirst();

		if (!indexDefinition.isPresent()) {
			logger.warn("no SCHEMA for {}", index);
			try {
				client.admin().indices().prepareCreate(index).execute().actionGet();
				return;
			} catch (Exception e) {
				logger.warn("failed to create indice {} : {}", index, e.getMessage());
			}
		} else {
			IndexDefinition definition = indexDefinition.get();
			int count = primary ? definition.count() : 1;
			byte[] schema = definition.schema;
			try {
				if (count > 1) {
					for (int i = 1; i <= count; i++) {
						String indexName = index + "_" + i;
						logger.info("init index {} with settings & schema", indexName);
						client.admin().indices().prepareCreate(indexName).setSource(schema, XContentType.JSON).execute()
								.actionGet();
						logger.info("Index {} created, waiting for green...", indexName);
						ClusterHealthResponse resp = client.admin().cluster().prepareHealth(indexName)
								.setWaitForGreenStatus().execute().actionGet();
						logger.debug("index health {}", resp);
					}
				} else {
					logger.info("init index {} with settings & schema", index);
					client.admin().indices().prepareCreate(index).setSource(schema, XContentType.JSON).execute()
							.actionGet();
					logger.info("Index {} created, waiting for green...", index);
					ClusterHealthResponse resp = client.admin().cluster().prepareHealth(index).setWaitForGreenStatus()
							.execute().actionGet();
					logger.debug("index health {}", resp);
				}
			} catch (Exception e) {
				logger.warn("failed to create indice {} : {}", index, e.getMessage(), e);
			}
		}
	}

	public static byte[] getIndexSchema(String indexName) {
		return indexes.get(indexName).schema;
	}

	public static void resetIndexes() {
		indexes.keySet().forEach(index -> resetIndex(index));
	}

	public static void clearClientCache() {
		clients.clear();
	}

	private static class IndexDefinition {
		private final String index;
		private final byte[] schema;
		private final Optional<ISchemaMatcher> matcher;
		private final int cnt;

		IndexDefinition(String index, byte[] schema, Optional<ISchemaMatcher> matcher, int count) {
			this.index = index;
			this.schema = schema;
			this.matcher = matcher;
			this.cnt = count;
		}

		public int count() {
			return Integer.parseInt(System.getProperty("es." + index + ".count", "" + cnt));
		}

		boolean supportsIndex(String name) {
			if (name.equals(index)) {
				return true;
			}
			if (matcher.isPresent()) {
				return matcher.get().supportsIndexName(name);
			}
			return false;
		}
	}
}
