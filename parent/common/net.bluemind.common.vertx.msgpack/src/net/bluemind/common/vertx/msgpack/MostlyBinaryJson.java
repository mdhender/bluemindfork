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
package net.bluemind.common.vertx.msgpack;

import java.util.Map;

import io.vertx.core.json.JsonObject;

public class MostlyBinaryJson implements IMsgpackCompliant {

	private final Map<String, Object> map;

	public MostlyBinaryJson(JsonObject orig) {
		this.map = orig.getMap();
	}

	@Override
	public Map<String, Object> getMap() {
		return map;
	}

}
