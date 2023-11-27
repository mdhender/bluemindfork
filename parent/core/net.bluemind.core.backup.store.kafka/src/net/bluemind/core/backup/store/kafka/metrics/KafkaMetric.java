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

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.vertx.core.json.JsonObject;

@JsonSerialize
public class KafkaMetric {

	public final String id;
	public final String key;
	public long value;
	public final String client;
	public int increment = 1;

	public KafkaMetric(String id, String key, long value, String client) {
		this.id = id;
		this.key = key;
		this.value = value;
		this.client = client;
	}

	public JsonObject toJsonObj() {
		JsonObject dashObj = new JsonObject();
		dashObj.put("id", id);
		dashObj.put("key", key);
		dashObj.put("value", value);
		dashObj.put("client", client);
		return dashObj;
	}

	public String toJson() {
		return toJsonObj().encode();
	}

	public KafkaMetric addValue(long newValue) {
		value += newValue;
		return this;
	}

	public KafkaMetric avgValue(long newValue) {
		value = (value * increment + newValue) / (increment + 1);
		increment += 1;
		return this;
	}

}
