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
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.sds.store.scalityring;

import io.vertx.core.json.JsonObject;

public class ScalityConfiguration {
	public String endpoint;
	public boolean trustAll = true;

	public JsonObject asJson() {
		return new JsonObject()//
				.put("storeType", "scalityring")//
				.put("endpoint", endpoint)//
				.put("trustAll", trustAll);
	}

	public static ScalityConfiguration from(JsonObject configuration) {
		ScalityConfiguration conf = new ScalityConfiguration(configuration.getString("endpoint"));
		conf.trustAll = configuration.getBoolean("trustAll", true);
		return conf;
	}

	public ScalityConfiguration(String endpoint) {
		this.endpoint = endpoint;
	}

	public String getEndpoint() {
		return endpoint;
	}
}
