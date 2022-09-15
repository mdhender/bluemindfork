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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.metrics.core.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.BoundRequestBuilder;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.metrics.alerts.api.AlertInfo;
import net.bluemind.metrics.alerts.api.AlertLevel;
import net.bluemind.metrics.alerts.api.IMonitoring;
import net.bluemind.metrics.core.tick.TickTemplateHelper;
import net.bluemind.metrics.core.tick.TickTemplateHelper.AlertId;
import net.bluemind.network.topology.Topology;
import net.bluemind.server.api.Server;
import net.bluemind.server.api.TagDescriptor;

public class MonitoringService implements IMonitoring {

	private static final Logger logger = LoggerFactory.getLogger(MonitoringService.class);

	public MonitoringService(BmContext context) {
	}

	@Override
	public List<AlertInfo> getAlerts(int limit, boolean filterResolved, List<AlertLevel> level) {
		Optional<ItemValue<Server>> influxServer = Topology.getIfAvailable()
				.flatMap(t -> t.anyIfPresent(TagDescriptor.bm_metrics_influx.getTag()));
		String url = String.format("http://%s:8086/query?db=chronograf",
				influxServer.map(server -> server.value.address()).orElse("127.0.0.1"));
		try {
			return collectAlerts(url, limit, filterResolved, level);
		} catch (Exception e) {
			throw new ServerFault(e);
		}
	}

	public List<AlertInfo> collectAlerts(String url, int limit, boolean filterResolved, List<AlertLevel> level)
			throws Exception {
		List<AlertInfo> alerts = new ArrayList<>();
		String result = query(url, limit);
		JsonObject asJson = new JsonObject(result);
		JsonObject results = asJson.getJsonArray("results").getJsonObject(0);
		if (results.containsKey("series")) {
			JsonObject series = results.getJsonArray("series").getJsonObject(0);
			JsonArray values = series.getJsonArray("values");
			Map<String, Boolean> alertStatus = new HashMap<>();
			for (int i = 0; i < values.size(); i++) {
				JsonArray row = values.getJsonArray(i);
				String product = "";
				String id = row.getString(1);
				AlertInfo alert = new AlertInfo();
				try {
					alert.level = AlertLevel.valueOf(row.getString(5));
				} catch (IllegalArgumentException e) {
					logger.error("alert level {} is not known", row.getString(5));
					alert.level = AlertLevel.WARNING;
				}

				if (filterResolved) {
					Boolean knownStatus = alertStatus.get(id);
					if (knownStatus != null && knownStatus) {
						continue;
					}

					if (alert.level == AlertLevel.OK) {
						if (knownStatus == null) {
							alertStatus.put(id, Boolean.TRUE);
						}
						continue;
					} else {
						if (knownStatus != null) {
							continue;
						}
						alertStatus.put(id, Boolean.FALSE);
					}
				}

				if (level.contains(alert.level)) {
					Optional<AlertId> idFromString = TickTemplateHelper.idFromString(id);
					if (idFromString.isPresent()) {
						AlertId alertId = idFromString.get();
						product = alertId.product.name();
						id = alertId.alertSubId;
					}

					alert.time = BmDateTimeWrapper.create(row.getString(0));
					alert.product = product;
					alert.id = id;
					alert.name = row.getString(2);
					alert.datalocation = row.getString(3);
					alert.host = row.getString(4);
					alert.message = row.getString(6);
					alerts.add(alert);
				}
			}
		}

		return alerts;

	}

	public String query(String url, int limit) throws Exception {
		try (AsyncHttpClient ahc = new DefaultAsyncHttpClient()) {
			BoundRequestBuilder post = ahc.preparePost(url);
			post.addHeader("Content-Type", "application/x-www-form-urlencoded");
			String select = "q=SELECT \"time\",\"alertID\",\"alertName\",\"datalocation\",\"host\",\"level\",\"message\" FROM \"alerts\" where \"time\" > now() - "
					+ limit + "d ORDER by \"time\" DESC";

			Response r = post.setBody(select).setReadTimeout(5000).setRequestTimeout(5000).execute().get(5,
					TimeUnit.SECONDS);
			return r.getResponseBody();
		}
	}

}
