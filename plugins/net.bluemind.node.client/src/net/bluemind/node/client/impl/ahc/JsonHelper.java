/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.node.client.impl.ahc;

import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import net.bluemind.node.shared.ExecRequest;
import net.bluemind.node.shared.ExecRequest.Options;

public class JsonHelper {

	public static JsonObject toJson(ExecRequest execReq) {
		JsonObject jso = new JsonObject();
		jso.putString("command", execReq.command);
		jso.putString("group", execReq.group).putString("name", execReq.name);
		JsonArray options = new JsonArray();
		for (ExecRequest.Options opt : execReq.options) {
			options.addString(opt.name());
		}
		jso.putArray("options", options);
		// for compat with older server versions
		jso.putBoolean("withOutput", !options.contains(Options.DISCARD_OUTPUT));
		return jso;
	}

}
