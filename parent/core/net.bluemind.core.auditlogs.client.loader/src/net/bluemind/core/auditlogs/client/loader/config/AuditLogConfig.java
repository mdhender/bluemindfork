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

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class AuditLogConfig {
	private static final Logger logger = LoggerFactory.getLogger(AuditLogConfig.class);
	public static final String AUDITLOG_DATASTREAM_NAME = "audit_log_%s";
	private static Config INSTANCE = loadConfig();

	protected AuditLogConfig() {

	}

	public static class AuditLogStore {
		private AuditLogStore() {

		}

		public static final String ACTIVATED = "activate";
		public static final String MULTIDOMAIN_DATASTREAMS = "domain_datastream";
	}

	private static Config loadConfig() {
		Config conf = ConfigFactory.load(AuditLogConfig.class.getClassLoader(), "resources/auditlog-store.conf");
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

	protected static boolean getOrDefaultBool(String key) {
		try {
			return AuditLogConfig.get().getBoolean(key);
		} catch (Exception e) {
			return false;
		}
	}

	protected static String getOrDefaultStr(String key) {
		try {
			return AuditLogConfig.get().getString(key);
		} catch (Exception e) {
			return null;
		}
	}

	protected static int getOrDefaultInt(String key) {
		try {
			return AuditLogConfig.get().getInt(key);
		} catch (Exception e) {
			return 0;
		}
	}

	public static boolean isActivated() {
		return AuditLogConfig.getOrDefaultBool(AuditLogStore.ACTIVATED);
	}

	public static String getDataStreamName() {
		String dataStreamPattern = AuditLogConfig.getOrDefaultStr(AuditLogStore.MULTIDOMAIN_DATASTREAMS);
		if (dataStreamPattern == null) {
			return AUDITLOG_DATASTREAM_NAME;
		}
		return dataStreamPattern;
	}

	public static String resolveDataStreamName(String domainUid) {

		String dataStreamPattern = AuditLogConfig.getOrDefaultStr(AuditLogStore.MULTIDOMAIN_DATASTREAMS);
		if (dataStreamPattern == null) {
			return String.format(AUDITLOG_DATASTREAM_NAME, domainUid);
		}

		if (dataStreamPattern.contains("%s")) {
			return String.format(dataStreamPattern, domainUid);
		}
		return dataStreamPattern;
	}

	public record ExternalESConfig(String ip, int port) {

	}

	@VisibleForTesting
	public static void clear() {
		INSTANCE = null;
	}
}
