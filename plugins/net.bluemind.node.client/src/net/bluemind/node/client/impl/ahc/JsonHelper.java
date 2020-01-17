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

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import net.bluemind.node.shared.ExecRequest;
import net.bluemind.node.shared.ExecRequest.Options;

public class JsonHelper {

	public static JsonObject toJson(ExecRequest execReq) {
		JsonObject jso = new JsonObject();
		jso.put("command", execReq.command);
		jso.put("group", execReq.group).put("name", execReq.name);
		JsonArray options = new JsonArray();
		for (ExecRequest.Options opt : execReq.options) {
			options.add(opt.name());
		}
		jso.put("options", options);
		// for compat with older server versions
		jso.put("withOutput", !options.contains(Options.DISCARD_OUTPUT));
		return jso;
	}

}
