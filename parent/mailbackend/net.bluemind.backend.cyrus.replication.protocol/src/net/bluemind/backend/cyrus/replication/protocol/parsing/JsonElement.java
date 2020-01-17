/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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
package net.bluemind.backend.cyrus.replication.protocol.parsing;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class JsonElement {

	final JsonObject object;
	final JsonArray array;

	public JsonElement(JsonObject obj, JsonArray arr) {
		this.object = obj;
		this.array = arr;
	}

	public JsonObject asObject() {
		return object;
	}

	public JsonArray asArray() {
		return array;
	}

	public static JsonElement of(JsonObject jsonObject) {
		return new JsonElement(jsonObject, null);
	}

	public static JsonElement of(JsonArray arr) {
		return new JsonElement(null, arr);
	}

	public boolean isArray() {
		return array != null;
	}

	public boolean isObject() {
		return object != null;
	}
}
