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
package net.bluemind.lib.grafana.dto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class Dashboard {
	public static final Logger logger = LoggerFactory.getLogger(Dashboard.class);

	public String uid;
	public String id;
	public String title;
	public Integer version;
	public Panel panel;

	public static Dashboard fromJson(String response) throws Exception {
		if (Strings.isNullOrEmpty(response)) {
			return null;
		}

		JsonObject jsonObj = new JsonObject(response);
		JsonObject dashObject = jsonObj.getJsonObject("dashboard");
		Dashboard d = new Dashboard();
		d.id = dashObject.getString("id");
		d.uid = dashObject.getString("uid");
		d.title = dashObject.getString("title");
		d.version = dashObject.getInteger("version");

		d.loadPanelWithDatasource(jsonObj);

		return d;
	}

	private void loadPanelWithDatasource(JsonObject jsonObj) throws Exception {
		JsonArray panelsArray = jsonObj.getJsonArray("panels");
		if (panelsArray == null || panelsArray.isEmpty()) {
			return;
		}
		if (panelsArray.size() > 1) {
			throw new Exception("Too many panels");
		} else if (panelsArray.size() == 1) {
			JsonObject panelObj = panelsArray.getJsonObject(0);
			this.panel = new Panel(panelObj);
		}
	}

	public String toJsonPutRequest() {
		JsonObject obj = new JsonObject(toJsonPostRequest(id, title, uid));
		JsonObject dashObj = obj.getJsonObject("dashboard");
		dashObj.put("version", version);
		if (!Strings.isNullOrEmpty(panel.json)) {
			JsonArray panels = new JsonArray();
			panels.add(panel.toJsonObject());
			dashObj.put("panels", panels);
		}
		return obj.encode();
	}

	public static String toJsonPostRequest(String id, String title, String uid) {
		JsonObject obj = new JsonObject();
		obj.put("overwrite", false);
		JsonObject dashObj = new JsonObject();
		dashObj.put("id", id);
		dashObj.put("title", title);
		dashObj.put("uid", uid);
		obj.put("dashboard", dashObj);
		return obj.encode();
	}

}
