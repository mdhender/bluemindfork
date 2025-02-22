/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.hornetq.client;

import java.util.Collection;

import io.vertx.core.json.JsonObject;

public class OOPMessage {

	private final JsonObject payload;

	public OOPMessage(JsonObject payload) {
		this.payload = payload;
	}

	public OOPMessage putStringProperty(String key, String value) {
		payload.put(key, value);
		return this;
	}

	public Collection<String> getPropertyNames() {
		return payload.fieldNames();
	}

	public String getStringProperty(String prop) {
		return payload.getString(prop);
	}

	public long getLongProperty(String key) {
		return payload.getLong(key);
	}

	public OOPMessage putLongProperty(String key, long value) {
		payload.put(key, value);
		return this;
	}

	public boolean containsProperty(String key) {
		return payload.containsKey(key);
	}

	public OOPMessage putIntProperty(String key, int value) {
		payload.put(key, value);
		return this;
	}

	public int getIntProperty(String key) {
		return payload.getInteger(key);
	}

	public JsonObject toJson() {
		return payload.copy();
	}

	public String toString() {
		return payload.encode();
	}

}
