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

import com.google.common.base.Strings;

import io.vertx.core.json.JsonObject;

public class Target {

	Datasource datasource;
	String editorMode;
	boolean exemplar;
	String expression;
	boolean hide;
	String legendFormat;
	boolean range;
	String refId;

	public static Target fromJson(String jsonStr) {
		if (Strings.isNullOrEmpty(jsonStr)) {
			return null;
		}

		JsonObject jsonObj = new JsonObject(jsonStr);
		return fromJson(jsonObj);
	}

	public static Target fromJson(JsonObject jsonObj) {
		if (jsonObj == null) {
			return null;
		}

		Target target = new Target();
		JsonObject dataSourceObj = jsonObj.getJsonObject("datasource");
		if (dataSourceObj != null) {
			target.datasource = Datasource.lightFromJson(dataSourceObj);
		}
		target.editorMode = jsonObj.getString("editorMode");
		target.exemplar = jsonObj.getBoolean("exemplar");
		target.expression = jsonObj.getString("expr");
		target.hide = jsonObj.getBoolean("hide");
		target.legendFormat = jsonObj.getString("legendFormat");
		target.range = jsonObj.getBoolean("range");
		target.refId = jsonObj.getString("refId");

		return target;
	}

	public Target copy() {
		Target target = new Target();
		target.datasource = this.datasource.copy();
		target.editorMode = this.editorMode;
		target.exemplar = this.exemplar;
		target.expression = this.expression;
		target.hide = this.hide;
		target.legendFormat = this.legendFormat;
		target.range = this.range;
		target.refId = this.refId;
		return target;
	}

	public void applyMetric(String m) {
		expression = expression.replace("INSTANCE", m);
		legendFormat = legendFormat.replace("INSTANCE", m);
	}

	public void setRefId(String refid) {
		refId = refid;
	}

	public JsonObject toJson() {
		JsonObject jsonObj = new JsonObject();
		jsonObj.put("datasource", datasource.toLightJson());
		jsonObj.put("editorMode", editorMode);
		jsonObj.put("exemplar", exemplar);
		jsonObj.put("expr", expression);
		jsonObj.put("hide", hide);
		jsonObj.put("legendFormat", legendFormat);
		jsonObj.put("range", range);
		jsonObj.put("refId", refId);
		return jsonObj;
	}

	public void setDatasource(Datasource datasource) {
		this.datasource = datasource;
	}
}
