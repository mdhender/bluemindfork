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

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.UUID;

import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Files;

import net.bluemind.lib.elasticsearch.ESearchActivator;
import net.bluemind.pool.impl.BmConfIni;

public class ElasticsearchTestHelper implements BundleActivator {

	private static final Logger logger = LoggerFactory.getLogger(ElasticsearchTestHelper.class);
	private static ElasticsearchTestHelper instance;

	static {
		System.setProperty("es.set.netty.runtime.available.processors", "false");
	}

	private TransportClient cli;

	@Override
	public void start(BundleContext context) throws Exception {
		instance = this;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		instance = null;
		if (cli != null) {
			cli.close();
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

	public TransportClient getClient() {
		if (cli != null) {
			return cli;
		}

		BmConfIni conf = new BmConfIni();
		String host = conf.get("es-host");

		if (host == null) {
			host = conf.get("host");
		}

		String mcastId = null;
		File mcastIdFile = new File("/etc/bm/mcast.id");
		if (mcastIdFile.exists()) {
			try {
				mcastId = "bluemind-" + Files.asCharSource(mcastIdFile, Charset.defaultCharset()).readFirstLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		org.elasticsearch.common.settings.Settings.Builder settingsBuilder = Settings.builder();

		if (mcastId != null) {
			settingsBuilder.put("cluster.name", mcastId);
		} else {
			settingsBuilder.put("cluster.name", "bluemind");
		}

		logger.info("elasticsearch host : {}, mcastId : {}", host, mcastId);

		settingsBuilder.put("node.name", "client-" + UUID.randomUUID());
		Settings settings = settingsBuilder.put("transport.tcp.connect_timeout", "5s").build();

		cli = new PreBuiltTransportClient(settings);
		try {
			cli.addTransportAddress(new TransportAddress(InetAddress.getByName(host), 9300));
		} catch (UnknownHostException e) {
			throw new RuntimeException(e);
		}
		return cli;
	}

	public static ElasticsearchTestHelper getInstance() {
		return instance;
	}

	public void afterTest() {
		try {
			getClient().admin().indices().prepareDelete("contact").execute().actionGet();
		} catch (Exception e) {
		}
		try {

			getClient().admin().indices().prepareDelete("event").execute().actionGet();
		} catch (Exception e) {
		}

		try {

			getClient().admin().indices().prepareDelete("todo").execute().actionGet();
		} catch (Exception e) {
		}
	}

	public void beforeTest(int count) {
		System.setProperty("es.mailspool.count", count + "");

		ESearchActivator.initClient(getClient());
		ESearchActivator.resetAll();
		ESearchActivator.resetIndex("mailspool_pending");
		ESearchActivator.resetIndex("mailspool");
		ESearchActivator.resetIndex("contact");
		ESearchActivator.resetIndex("event");
		ESearchActivator.resetIndex("todo");
		ESearchActivator.resetIndex("im");

	}

	public void beforeTest() {
		beforeTest(1);
	}

	public void refresh(String index) {
		getClient().admin().indices().prepareRefresh(index).execute().actionGet();
	}

}
