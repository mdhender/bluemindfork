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
package net.bluemind.core.elasticsearch;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.indices.ResolveIndexResponse;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.transport.ElasticsearchTransport;
import net.bluemind.core.auditlogs.client.loader.AuditLogLoader;
import net.bluemind.lib.elasticsearch.ESearchActivator;
import net.bluemind.lib.elasticsearch.ESearchActivator.Authentication;
import net.bluemind.lib.elasticsearch.ESearchActivator.AuthenticationCredential;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.pool.impl.BmConfIni;

public class ElasticsearchTestHelper implements BundleActivator {

	private static final Logger logger = LoggerFactory.getLogger(ElasticsearchTestHelper.class);
	private static ElasticsearchTestHelper instance;

	static {
		System.setProperty("es.set.netty.runtime.available.processors", "false");
	}

	private ElasticsearchTransport transport;

	@Override
	public void start(BundleContext context) throws Exception {
		instance = this;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		instance = null;
		if (transport != null) {
			transport.close();
		}
	}

	public String getHost() {
		BmConfIni conf = new BmConfIni();
		String host = conf.get("es-host");

		if (host == null) {
			host = conf.get("host");
		}

		return host;
	}

	public ElasticsearchClient getClient() {
		return new ElasticsearchClient(getTransport());
	}

	private ElasticsearchTransport getTransport() {
		if (transport != null) {
			return transport;
		}
		transport = ESearchActivator.createTansport(Arrays.asList(getHost()),
				new AuthenticationCredential(Authentication.NONE));
		return transport;
	}

	public static ElasticsearchTestHelper getInstance() {
		return instance;
	}

	public void afterTest() {
		try {
			getClient().indices().delete(d -> d.index("contact"));
		} catch (Exception e) {
		}
		try {
			getClient().indices().delete(d -> d.index("event"));
		} catch (Exception e) {
		}
		try {
			getClient().indices().delete(d -> d.index("todo"));
		} catch (Exception e) {
		}
		try {
			getClient().indices().delete(d -> d.index("note"));
		} catch (Exception e) {
		}
		try {
			AuditLogLoader auditLogProvider = new AuditLogLoader();
			auditLogProvider.getManager().removeAuditLogBackingStores();
		} catch (Exception e) {
		}
	}

	public void beforeTest(int count) {
		try {
			System.setProperty("es.mailspool.count", count + "");
			ESearchActivator.initClient(getTransport());
			deleteAll();
			ESearchActivator.resetIndexes();
			AuditLogLoader auditLogProvider = new AuditLogLoader();
			auditLogProvider.getManager().removeAuditLogBackingStores();
			// Force elasticsearch to go on the edge
			ESearchActivator.getClient().cluster()
					.putSettings(sb -> sb
							.persistent("cluster.routing.allocation.disk.watermark.low", JsonData.of("98%"))
							.persistent("cluster.routing.allocation.disk.watermark.high", JsonData.of("99%"))
							.persistent("cluster.routing.allocation.disk.watermark.flood_stage", JsonData.of("99%"))
							.persistent("cluster.routing.allocation.disk.watermark.flood_stage.frozen.max_headroom",
									JsonData.of("1GB")));
		} catch (Exception n) {
			String host = getHost();
			System.err.println("Starting checks on " + host + " after " + n.getMessage() + " klass: " + n.getClass());
			INodeClient node = NodeActivator.get(host);
			byte[] fetched = node.read("/var/log/bm-elasticsearch/bluemind.log");
			System.err.println("ES log in docker:'\n" + new String(fetched) + "'\n");
			throw new RuntimeException(n);
		}

	}

	public void beforeTest() {
		beforeTest(1);
	}

	public void deleteAll() throws ElasticsearchException, IOException {
		ElasticsearchClient esClient = ESearchActivator.getClient();
		ResolveIndexResponse resp = esClient.indices().resolveIndex(r -> r.name("*"));
		List<String> indices = resp.indices().stream().map(i -> i.name()).filter(n -> !n.startsWith(".ds-")).toList();
		if (!indices.isEmpty()) {
			logger.warn("Full ES reset of {} ", indices);
			esClient.indices().delete(d -> d.index(indices));
		}
	}

	public void refresh(String index) throws ElasticsearchException, IOException {
		getClient().indices().refresh(r -> r.index(index));
	}

}
