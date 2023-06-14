/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.central.reverse.proxy.launcher;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import net.bluemind.lib.grafana.config.GrafanaConnection;

public class GrafanaConfig {

	private static final Logger logger = LoggerFactory.getLogger(GrafanaConfig.class);
	private static final Config INSTANCE = loadConfig();

	private static final String GRAFANA_RESOURCES = "resources/grafana.conf";
	private static final String GRAFANA_CONF = "/etc/bm-crp/grafana.conf";
	private static final String GRAFANA_API_CONF = "/etc/bm-crp/grafana-api.conf";

	private GrafanaConfig() {

	}

	public static class GrafanaConfigApi {
		private GrafanaConfigApi() {

		}

		public static final String TOKEN = "grafana.api.token";
		public static final String TOKEN_ID = "grafana.api.tokenId";
		public static final String SERVICE_ACCOUNT_NAME = "grafana.api.accountName";
		public static final String SERVICE_ACCOUNT_ID = "grafana.api.accountId";
	}

	public static class GrafanaConfigServer {
		private GrafanaConfigServer() {

		}

		public static final String HOST = "grafana.server.host";
	}

	public static class GrafanaConfigDatasource {
		private GrafanaConfigDatasource() {

		}

		public static final String URL = "grafana.datasource.url";
		public static final String NAME = "grafana.datasource.name";
	}

	public static class GrafanaConfigDashboard {
		private GrafanaConfigDashboard() {

		}

		public static final String UID = "grafana.dashboard.uid";
		public static final String TITLE = "grafana.dashboard.title";
	}

	private static Config loadConfig() {
		Config conf = ConfigFactory.load(GrafanaConfig.class.getClassLoader(), GRAFANA_RESOURCES);
		conf = loadConfigFromFile(GRAFANA_CONF, conf);
		conf = loadConfigFromFile(GRAFANA_API_CONF, conf);

		Config systemPropertyConfig = ConfigFactory.defaultApplication();
		conf = systemPropertyConfig.withFallback(conf);
		logger.info("Grafana config: {}", conf.getConfig("grafana").root());
		return conf;
	}

	private static Config loadConfigFromFile(String confFile, Config conf) {
		try {
			File local = new File(confFile); // NOSONAR
			if (local.exists()) {
				Config parsed = ConfigFactory.parseFile(local);
				conf = parsed.withFallback(conf);
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

		return conf;
	}

	public static Config get() {
		return INSTANCE;
	}

	public static String getOrDefaultStr(String key) {
		try {
			return GrafanaConfig.get().getString(key);
		} catch (Exception e) {
			return null;
		}
	}

	public static Integer getOrDefaultInt(String key) {
		try {
			return GrafanaConfig.get().getInt(key);
		} catch (Exception e) {
			return null;
		}
	}

	public static void updateToken(GrafanaConnection gConnection) {
		String tokenConfigContentFile = "grafana {api {"//
				+ "token = \"" + gConnection.apiKey + "\"" //
				+ "}}";
		if (gConnection.withServiceAccount()) {
			tokenConfigContentFile = "grafana {api {"//
					+ "token = \"" + gConnection.apiKey + "\"," //
					+ "tokenId = \"" + gConnection.tokenId() + "\"," //
					+ "accountId = \"" + gConnection.accountId() + "\"," //
					+ "accountName = \"" + gConnection.accountName() + "\"" //
					+ "}}";
		}
		try (FileWriter myWriter = new FileWriter(GRAFANA_API_CONF)) {
			myWriter.write(tokenConfigContentFile);
		} catch (IOException e) {
			logger.error("New Grafana token api cannot be write to {}", GRAFANA_API_CONF);
			logger.error(e.getMessage(), e);
		}
	}

}
