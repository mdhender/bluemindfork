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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.configfile.elastic;

import java.time.Duration;

import com.typesafe.config.Config;

import net.bluemind.configfile.elastic.ElasticsearchConfig.Client.Pool;
import net.bluemind.configfile.elastic.ElasticsearchConfig.Client.Retry;
import net.bluemind.configfile.elastic.ElasticsearchConfig.Client.Timeout;

public class ElasticsearchConfig {

	public static final String OVERRIDE_PATH = "/etc/bm/elasticsearch.conf";

	public static class Maintenance {
		public static final String DELETED_DOC_THRESHOLD = "elasticsearch.maintenance.deleted-doc-threshold";

		public static final class Rebalance {
			public static final String ENABLED = "elasticsearch.maintenance.rebalance.enabled";
			public static final String STRATEGY = "elasticsearch.maintenance.rebalance.strategy";

			public static final class RefreshDurationRatio {
				public static final String HIGH = "elasticsearch.maintenance.rebalance.refresh-duration-ratio.high";
				public static final String LOW = "elasticsearch.maintenance.rebalance.refresh-duration-ratio.low";
			}

			public static final class RefreshDurationThreshold {
				public static final String HIGH = "elasticsearch.maintenance.rebalance.refresh-duration-threshold.high";
				public static final String LOW = "elasticsearch.maintenance.rebalance.refresh-duration-threshold.low";
			}
		}

		public static final class AddShard {
			public static final String ENABLED = "elasticsearch.maintenance.add-shard.enabled";
			public static final String REFRESH_DURATION_THRESHOLD = "elasticsearch.maintenance.add-shard.refresh-duration-threshold";
		}
	}

	public static record Client(Timeout timeout, Pool pool, Retry retry) {

		public static Client of(Config config) {
			return new Client(Timeout.of(config), Pool.of(config), Retry.of(config));
		}

		public static record Timeout(Duration connect, Duration socket, Duration request) {

			public static final String CONNECT_TIMEOUT = "elasticsearch.client.timeout.connect-timeout";
			public static final String SOCKET_TIMEOUT = "elasticsearch.client.timeout.socket-timeout";
			public static final String CONNECTION_REQUEST_TIMEOUT = "elasticsearch.client.timeout.connection-request-timeout";

			public static Timeout of(Config config) {
				Duration connectTimeout = config.getDuration(Timeout.CONNECT_TIMEOUT);
				Duration socketTimeout = config.getDuration(Timeout.SOCKET_TIMEOUT);
				Duration connectionRequestTimeout = config.getDuration(Timeout.CONNECTION_REQUEST_TIMEOUT);
				return new Timeout(connectTimeout, socketTimeout, connectionRequestTimeout);

			}
		}

		public static record Pool(int maxConnTotal, int maxConnPerRoute) {
			public static final String MAX_CONN_TOTAL = "elasticsearch.client.pool.max-conn-total";
			public static final String MAX_CONN_PER_ROUTE = "elasticsearch.client.pool.max-conn-per-route";

			public static Pool of(Config config) {
				int maxConnTotal = config.getInt(Pool.MAX_CONN_TOTAL);
				int maxConnPerRoute = config.getInt(Pool.MAX_CONN_PER_ROUTE);
				return new Pool(maxConnTotal, maxConnPerRoute);

			}
		}

		public static record Retry(boolean enabled, int count, Duration delay, double multiplier) {

			public static final String ENABLED = "elasticsearch.client.retry.enabled";
			public static final String COUNT = "elasticsearch.client.retry.count";
			public static final String DELAY = "elasticsearch.client.retry.delay";
			public static final String MULTIPLIER = "elasticsearch.client.retry.multiplier";

			public static Retry of(Config config) {
				boolean enabled = config.getBoolean(Retry.ENABLED);
				int count = config.getInt(Retry.COUNT);
				Duration delay = config.getDuration(Retry.DELAY);
				double multiplier = config.getDouble(Retry.MULTIPLIER);
				return new Retry(enabled, count, delay, multiplier);
			}
		}

	}
}