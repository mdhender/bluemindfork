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

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class KafkaTopicMetrics {
	private static final Logger logger = LoggerFactory.getLogger(KafkaTopicMetrics.class);

	public static final String SEND_RATE = "record-send-rate";
	public static final String LAG = "lag";

	List<KafkaMetric> metrics = new ArrayList<>();
	List<KafkaMetric> publishMetrics = new ArrayList<>();

	private static final KafkaTopicMetrics INSTANCE = new KafkaTopicMetrics();

	public static KafkaTopicMetrics get() {
		return INSTANCE;
	}

	public void addProducerMetric(String id, String key, long value) {
		addMetric(id, key, value, ClientEnum.PRODUCER);
	}

	public void addConsumerMetric(String id, String key, long value) {
		addMetric(id, key, value, ClientEnum.CONSUMER);
	}

	private void addMetric(String id, String key, long value, ClientEnum client) {
		KafkaMetric metric = new KafkaMetric(id, key, value, client.name());
		metrics.add(metric);
	}

	private void addPublishMetric(String id, String key, long value, ClientEnum client) {
		KafkaMetric metric = new KafkaMetric(id, key, value, client.name());
		publishMetrics.add(metric);
	}

	enum ClientEnum {
		CONSUMER, PRODUCER;
	}

	private ClientEnum checkMetric(String key, List<KafkaMetric> metricsFiltered) throws Exception {
		boolean producerMetric = metricsFiltered.stream().filter(m -> m.client().equals(ClientEnum.PRODUCER.name()))
				.count() > 0;
		boolean consumerMetric = metricsFiltered.stream().filter(m -> m.client().equals(ClientEnum.CONSUMER.name()))
				.count() > 0;
		if (!producerMetric && !consumerMetric) {
			logger.debug(String.format("Cannot find metric key '%s' for CONSUMER or PRODUCER", key));
			return null;
		}

		if (producerMetric && consumerMetric) {
			throw new Exception(String.format("Cannot find metric key '%s' for CONSUMER and PRODUCER", key));
		}

		return producerMetric ? ClientEnum.PRODUCER : ClientEnum.CONSUMER;
	}

	public void sumOf(String... keys) throws Exception {
		for (String key : keys) {
			List<KafkaMetric> metricsFiltered = metrics.stream().filter(m -> key.equals(m.key())).toList();
			ClientEnum clientMetric = checkMetric(key, metricsFiltered);
			if (clientMetric == null) {
				return;
			}
			List<String> ids = metricsFiltered.stream().map(m -> m.id()).distinct().toList();
			for (String id : ids) {
				List<KafkaMetric> metricsIdsFiltered = metricsFiltered.stream().filter(m -> id.equals(m.id())).toList();
				long sum = metricsIdsFiltered.stream().map(m -> m.value()).reduce(0l, Long::sum);
				addPublishMetric(id, key, sum, clientMetric);
				metrics.removeIf(m -> id.equals(m.id()) && key.equals(m.key()));
			}
		}
	}

	public void avgOf(String... keys) throws Exception {
		for (String key : keys) {
			List<KafkaMetric> metricsFiltered = metrics.stream().filter(m -> key.equals(m.key())).toList();
			ClientEnum clientMetric = checkMetric(key, metricsFiltered);
			if (clientMetric == null) {
				return;
			}
			List<String> ids = metricsFiltered.stream().map(m -> m.id()).distinct().toList();
			for (String id : ids) {
				List<KafkaMetric> metricsIdsFiltered = metricsFiltered.stream().filter(m -> id.equals(m.id())).toList();
				long sum = metricsIdsFiltered.stream().map(m -> m.value()).reduce(0l, Long::sum);
				long average = metricsIdsFiltered.isEmpty() ? 0l : sum / metricsIdsFiltered.size();
				addPublishMetric(id, key, average, clientMetric);
				metrics.removeIf(m -> id.equals(m.id()) && key.equals(m.key()));
			}
		}
	}

	public JsonObject toJson() {
		JsonObject jObj = new JsonObject();
		JsonArray jArr = new JsonArray();
		for (KafkaMetric m : publishMetrics) {
			jArr.add(new JsonObject(m.toJson()));
		}
		jObj.put("metrics", jArr);
		return jObj;
	}

	public void clearAllPublishMetrics() {
		publishMetrics.clear();
	}

}
