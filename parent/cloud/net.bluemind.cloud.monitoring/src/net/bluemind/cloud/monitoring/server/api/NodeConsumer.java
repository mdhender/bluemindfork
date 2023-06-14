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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.cloud.monitoring.server.api;

import java.util.Collections;
import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.streams.StreamsConfig;

import com.typesafe.config.Config;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import net.bluemind.central.reverse.proxy.model.common.kafka.KafkaConsumerClient;
import net.bluemind.cloud.monitoring.server.MonitoringConfig;

public abstract class NodeConsumer<T> extends ApiCall<T> {

	protected static final String NODE_TOPIC = "bluemind_cluster-__nodes__";

	protected void consume(Config config, Vertx vertx, String topic,
			Handler<ConsumerRecord<String, String>> recordHandler, Runnable done) {

		Properties props = new Properties();
		props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, config.getString(MonitoringConfig.Kafka.BOOTSTRAP_SERVERS));
		props.put(ConsumerConfig.GROUP_ID_CONFIG, "cloud.monitor.listall" + System.currentTimeMillis());
		props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
		props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
		props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
		props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");

		KafkaConsumerClient<String, String> consumer = KafkaConsumerClient.create(vertx, props);

		consumer.handler(recordHandler) //
				.subscribe(Collections.singletonList(topic)) //
				.onSuccess(v -> done.run());
	}

	protected boolean isNodeInfoTopic(String topic) {
		return NODE_TOPIC.equals(topic);
	}
}
