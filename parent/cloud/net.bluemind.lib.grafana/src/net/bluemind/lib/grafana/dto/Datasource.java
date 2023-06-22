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

import io.vertx.core.json.JsonObject;

public class Datasource {
	public static final Logger logger = LoggerFactory.getLogger(Datasource.class);

	String uid;
	String type;
	String name;
	String url;

	public static Datasource lightFromJson(JsonObject datasourceObj) {
		if (datasourceObj != null) {
			Datasource datasource = new Datasource();
			datasource.uid = datasourceObj.getString("uid");
			datasource.type = datasourceObj.getString("type");
			return datasource;
		}
		return null;
	}

	public static Datasource fromJson(String response) {
		if (Strings.isNullOrEmpty(response)) {
			return null;
		}
		Datasource datasource = new Datasource();
		JsonObject jsonObj = new JsonObject(response);
		datasource.uid = jsonObj.getString("uid");
		datasource.type = jsonObj.getString("type");
		datasource.name = jsonObj.getString("name");
		datasource.url = jsonObj.getString("url");
		return datasource;
	}

	public String toJson() {
		JsonObject obj = new JsonObject();
		obj.put("name", name);
		obj.put("type", type);
		obj.put("url", url);
		obj.put("access", "proxy");
		obj.put("basicAuth", false);
		return obj.encode();
	}

	public JsonObject toLightJson() {
		JsonObject obj = new JsonObject();
		obj.put("uid", uid);
		obj.put("type", type);
		return obj;
	}

	public Datasource copy() {
		Datasource datasource = new Datasource();
		datasource.uid = this.uid;
		datasource.type = this.type;
		datasource.name = this.name;
		datasource.url = this.url;
		return datasource;
	}

	public static Datasource createFromTemplate(String name, String url, String template) {
		Datasource datasource = Datasource.fromJson(template);
		if (datasource == null) {
			return null;
		}
		datasource.name = name;
		datasource.url = url;
		return datasource;
	}

	public String getName() {
		return name;
	}
}
