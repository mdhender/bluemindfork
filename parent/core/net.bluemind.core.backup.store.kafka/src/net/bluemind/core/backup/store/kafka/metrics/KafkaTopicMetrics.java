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
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class KafkaTopicMetrics {
	private static final Logger logger = LoggerFactory.getLogger(KafkaTopicMetrics.class);

	public static final String SEND_RATE = "record-send-rate";
	public static final String LAG = "lag";

	final Map<String, KafkaMetric> rateList = new ConcurrentHashMap<>();
	final Map<String, KafkaMetric> lagList = new ConcurrentHashMap<>();

	private static final KafkaTopicMetrics INSTANCE = new KafkaTopicMetrics();

	public static KafkaTopicMetrics get() {
		return INSTANCE;
	}

	public enum ClientEnum {
		CONSUMER, PRODUCER;
	}

	public void sumOnLag(String id, long value) {
		sumUp(id, LAG, value, ClientEnum.CONSUMER.name());
	}

	public void avgOnSendRate(String id, long value) {
		avgUp(id, SEND_RATE, value, ClientEnum.PRODUCER.name());
	}

	private void sumUp(String id, String key, long value, String client) {
		lagList.compute(id,
				(keyy, metric) -> metric == null ? new KafkaMetric(id, key, value, client) : metric.addValue(value));
	}

	private void avgUp(String id, String key, long value, String client) {
		rateList.compute(id,
				(keyy, metric) -> metric == null ? new KafkaMetric(id, key, value, client) : metric.avgValue(value));
	}

	@SuppressWarnings("serial")
	public static class PublishedMetrics extends ArrayList<KafkaMetric> {

		public JsonObject toJson() {
			JsonObject jObj = new JsonObject();
			JsonArray jArr = new JsonArray();
			for (KafkaMetric m : this) {
				jArr.add(m.toJsonObj());
			}
			jObj.put("metrics", jArr);
			return jObj;
		}
	}

	public PublishedMetrics publish() {
		PublishedMetrics target = new PublishedMetrics();
		target.addAll(publishSendRateMetrics());
		target.addAll(publishLagMetrics());
		if (logger.isDebugEnabled()) {
			logger.debug("publishing {} metric(s)", target.size());
		}
		return target;
	}

	private List<KafkaMetric> publishSendRateMetrics() {
		List<KafkaMetric> list = rateList.entrySet().stream().map(Entry::getValue).toList();
		rateList.clear();
		return list;
	}

	private List<KafkaMetric> publishLagMetrics() {
		List<KafkaMetric> list = lagList.entrySet().stream().map(Entry::getValue).toList();
		lagList.clear();
		return list;
	}

}
