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

import io.vertx.core.json.JsonObject;

public class Panel {

	private static final String DATASOURCE_NODE = "datasource";
	private static final String CONTENT_URL_OPT = "contentUrl";

	Datasource datasource;
	String json;

	public Panel(Datasource datasource, String json) {
		this.datasource = datasource;
		this.json = json;
	}

	public Panel(JsonObject panelObj) {
		JsonObject datasourceObj = panelObj.getJsonObject(DATASOURCE_NODE);
		if (datasourceObj != null) {
			datasource = Datasource.lightFromJson(datasourceObj);
		}
		this.json = panelObj.encode();
	}

	public JsonObject toJsonObject() {
		return new JsonObject(json);
	}

	public void updateJsonDatasource(String datasourceUrl) {
		JsonObject panelObj = new JsonObject(json);
		panelObj.remove(DATASOURCE_NODE);
		panelObj.put(DATASOURCE_NODE, datasource.toLightJson());

		JsonObject optionObj = panelObj.getJsonObject("options");
		optionObj.remove(CONTENT_URL_OPT);
		optionObj.put(CONTENT_URL_OPT, datasourceUrl + "/monitoring/topology");

		this.json = panelObj.encode();
	}
}
