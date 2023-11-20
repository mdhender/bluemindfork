/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
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

package net.bluemind.core.auditlogs.client.loader.config;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class AuditLogStoreConfig {
	private static final Logger logger = LoggerFactory.getLogger(AuditLogStoreConfig.class);
	private static Config INSTANCE = loadConfig();

	private AuditLogStoreConfig() {

	}

	public static class AuditLogStore {
		private AuditLogStore() {

		}

		public static final String ACTIVATED = "auditlog.activate";
		public static final String EXTERNAL_ES_HOST = "auditlog.store.server";
		public static final String EXTERNAL_ES_PORT = "auditlog.store.port";
	}

	private static Config loadConfig() {
		Config conf = ConfigFactory.load(AuditLogStoreConfig.class.getClassLoader(), "resources/auditlog-store.conf");
		try {
			File local = new File("/etc/bm/auditlog-store.conf"); // NOSONAR
			if (local.exists()) {
				Config parsed = ConfigFactory.parseFile(local);
				conf = parsed.withFallback(conf);
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

		Config systemPropertyConfig = ConfigFactory.defaultApplication();
		return systemPropertyConfig.withFallback(conf);
	}

	public static Config get() {
		if (INSTANCE == null) {
			INSTANCE = loadConfig();
		}

		return INSTANCE;
	}

	public static boolean getOrDefaultBool(String key) {
		try {
			return AuditLogStoreConfig.get().getBoolean(key);
		} catch (Exception e) {
			return false;
		}
	}

	public static String getOrDefaultStr(String key) {
		try {
			return AuditLogStoreConfig.get().getString(key);
		} catch (Exception e) {
			return null;
		}
	}

	public static int getOrDefaultInt(String key) {
		try {
			return AuditLogStoreConfig.get().getInt(key);
		} catch (Exception e) {
			return 0;
		}
	}

	public static boolean isActivated() {
		return AuditLogStoreConfig.getOrDefaultBool(AuditLogStore.ACTIVATED);
	}

	public static ExternalESConfig getExternalEsConfig() {
		String host = getOrDefaultStr(AuditLogStore.EXTERNAL_ES_HOST);
		int port = getOrDefaultInt(AuditLogStore.EXTERNAL_ES_PORT);
		if (host == null && port == 0) {
			return null;
		}
		return new ExternalESConfig(host, port);
	}

	public record ExternalESConfig(String ip, int port) {

	}

	@VisibleForTesting
	public static void clear() {
		INSTANCE = null;
	}
}
