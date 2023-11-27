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
package net.bluemind.core.backup.store.kafka.metrics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;

public class KafkaAccuMetrics extends AbstractVerticle {
	private static final Logger logger = LoggerFactory.getLogger(KafkaAccuMetrics.class);

	@Override
	public void start() {
		vertx.eventBus().consumer("bm.monitoring.fw.kafka.metrics", metric -> {
			JsonObject body = (JsonObject) metric.body();
			if (KafkaTopicMetrics.SEND_RATE.equals(body.getString("key"))) {
				KafkaTopicMetrics.get().avgOnSendRate(body.getString("id"), body.getLong("value"));
			} else if (KafkaTopicMetrics.LAG.equals(body.getString("key"))) {
				KafkaTopicMetrics.get().sumOnLag(body.getString("id"), body.getLong("value"));
			}
		});
	}
}