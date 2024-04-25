/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
  *
  * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License)
  * or the CeCILL as published by CeCILL.info (version 2 of the License).
  *
  * There are special exceptions to the terms and conditions of the
  * licenses as they are applied to this program. See LICENSE.txt in
  * the directory of this program distribution.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.configfile.core;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;

public class CoreConfig {

	public static final String OVERRIDE_PATH = "/etc/bm/core.conf";

	public static class Pool {
		private Pool() {
		}

		public static final String TASKS_SIZE = "core.pool.tasks.size";
		public static final String TASKS_COMPLETED_TIMEOUT = "core.pool.tasks.completed-timeout";
		public static final String WORKER_SIZE = "core.pool.worker.size";
		public static final String EXECUTOR_SIZE = "core.pool.executor.size";
		public static final String EXECUTOR_COMPLETION_TIMEOUT = "core.pool.executor.completion-timeout";
	}

	public static class Io {
		private Io() {

		}

		public static final String WRITE_BUFFER = "core.io.write-buffer";
	}

	public static class Sessions {
		private Sessions() {
		}

		public static final String STORAGE_PATH = "core.sessions.storage-path";
		public static final String IDLE_TIMEOUT = "core.sessions.idle-timeout";
	}

	public static class PostgreSQL {
		private PostgreSQL() {
		}

		public static final String STARTUP_TIMEOUT = "core.database.postgresql.startup-timeout";
		public static final String MAX_LIFETIME = "core.database.postgresql.max-lifetime";
		public static final String MAX_POOLSIZE = "core.database.postgresql.max-poolsize";
		public static final String SSL_MODE = "core.database.postgresql.sslmode";
		public static final String PREPARED_STATEMENT_CACHE_QUERIES = "core.database.postgresql.prepared-statement-cache-queries";
		public static final String PREPARED_STATEMENT_CACHE_SIZE_MIB = "core.database.postgresql.prepared-statement-cache-size-mib";
		public static final String PREFER_QUERY_MODE = "core.database.postgresql.prefer-query-mode";
		public static final String DEFAULT_ROW_FETCHSIZE = "core.database.postgresql.default-row-fetch-size";
		public static final String APPLICATION_NAME = "core.database.postgresql.application-name";
		public static final String REWRITE_BATCHED_INSERTS = "core.database.postgresql.rewrite-batched-inserts";
		public static final String PREPARE_THRESHOLD = "core.database.postgresql.prepare-threshold";
		public static final String LEAK_DETECTION_THRESHOLD = "core.database.postgresql.leak-detection-threshold";

	}

	public static class AclChangedNotification {
		private AclChangedNotification() {
		}

		public static final String QUEUE_SIZE = "core.aclChangedNotification.queue-size";
		public static final String DELAY = "core.aclChangedNotification.delay";
	}

	private static final Logger logger = LoggerFactory.getLogger(CoreConfig.class);
	private static Config instance = load();

	private CoreConfig() {
	}

	private static Config load() {
		Config conf = ConfigFactory.load(CoreConfig.class.getClassLoader(), "resources/application.conf");
		String configPath = System.getProperty("core.config.path");
		File local = configPath != null ? new File(configPath) : new File(OVERRIDE_PATH); // NOSONAR
		if (local.exists()) {
			try {
				Config parsed = ConfigFactory.parseFile(local);
				conf = parsed.withFallback(conf);
			} catch (ConfigException e) {
				logger.error("Invalid Core config file in '{}', ignored: {}", local, e.getMessage());
			}
		}
		return conf;
	}

	public static Config get() {
		return instance;
	}

}
