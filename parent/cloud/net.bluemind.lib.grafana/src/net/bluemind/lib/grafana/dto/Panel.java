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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class Panel {

	private static final String DATASOURCE_NODE = "datasource";
	private static final String TARGETS_NODE = "targets";
	private static final String CONTENT_URL_OPT = "contentUrl";

	Datasource datasource;
	String json;
	Map<String, Target> targets = new HashMap<>();

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
		panelObj.put(DATASOURCE_NODE, datasource.toLightJson());

		JsonObject optionObj = panelObj.getJsonObject("options");
		optionObj.put(CONTENT_URL_OPT, datasourceUrl + "/monitoring/topology");

		this.json = panelObj.encode();
	}

	public void setTargetsFromJson() {
		JsonObject jsonObj = new JsonObject(json);
		JsonArray targetsArray = jsonObj.getJsonArray("targets");
		if (targetsArray != null && !targetsArray.isEmpty()) {
			for (Object t : targetsArray) {
				Target target = Target.fromJson(((JsonObject) t));
				targets.put(target.expression, target);
			}
		}
	}

	public void addMetricsTargets(Target template, List<String> metrics) {
		for (int i = 0; i < metrics.size(); i++) {
			Target target = template.copy();
			target.applyMetric(metrics.get(i));
			targets.put(metrics.get(i), target);
		}

		AtomicInteger a = new AtomicInteger(65);
		targets.values().forEach(t -> t.setRefId(String.valueOf(Character.toChars(a.getAndIncrement()))));

		updateTargets();
	}

	private void updateTargets() {
		JsonObject jsonObj = new JsonObject(json);
		JsonArray targetsArray = new JsonArray();
		targets.values().stream().map(Target::toJson).forEach(targetsArray::add);
		jsonObj.put(TARGETS_NODE, targetsArray);
		json = jsonObj.encode();
	}

}
