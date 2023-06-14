/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.core.backup.store.kafka;

import java.util.List;
import java.util.Map;

import org.apache.kafka.common.metrics.KafkaMetric;
import org.apache.kafka.common.metrics.MetricsReporter;

import io.vertx.core.json.JsonObject;
import net.bluemind.lib.vertx.VertxPlatform;

public class BluemindMetricsReporter implements MetricsReporter {

	public BluemindMetricsReporter() {
	}

	@Override
	public void configure(Map<String, ?> configs) {
	}

	@Override
	public void init(List<KafkaMetric> metrics) {
	}

	@Override
	public void metricChange(KafkaMetric metric) {
		String metricName = metric.metricName().name();
		if ("outgoing-byte-rate".equals(metricName) || "record-send-rate".equals(metricName)) {
			VertxPlatform.getVertx().eventBus().publish("MASTER_METRICS", metricToJsonObject(metric));
		} else if ("records-lag-max".equals(metricName)) {
			VertxPlatform.getVertx().eventBus().publish("TAIL_METRICS", metricToJsonObject(metric));
		}
	}

	private static JsonObject metricToJsonObject(KafkaMetric metric) {
		Object metricValue = metric.metricValue();
		if (metricValue instanceof Double dvalue && dvalue.isNaN()) {
			metricValue = 0.0;
		}
		return new JsonObject().put("key", metric.metricName().name()).put("value", metricValue.toString());
	}

	@Override
	public void metricRemoval(KafkaMetric metric) {
	}

	@Override
	public void close() {
	}

}
