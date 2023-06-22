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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.lib.grafana.client;

import io.netty.handler.codec.http.HttpMethod;
import net.bluemind.lib.grafana.config.GrafanaConnection;
import net.bluemind.lib.grafana.dto.Dashboard;
import net.bluemind.lib.grafana.dto.Datasource;
import net.bluemind.lib.grafana.exception.GrafanaException;
import net.bluemind.lib.grafana.exception.GrafanaNotFoundException;
import net.bluemind.lib.grafana.utils.ApiHttpHelper;

public class GrafanaClient {
	private final ApiHttpHelper http;

	public GrafanaClient(GrafanaConnection config) {
		this.http = new ApiHttpHelper(config.host, config.apiKey);
	}

	public Dashboard getOrCreateDashboard(String uid, String title) throws GrafanaException, InterruptedException {
		Dashboard dashboard = null;
		try {
			dashboard = getDashboard(uid);
		} catch (GrafanaNotFoundException e) {
			createDashboard(uid, title);
			dashboard = getDashboard(uid);
		}
		return dashboard;
	}

	private void createDashboard(String uid, String title) throws GrafanaException, InterruptedException {
		http.execute(GrafanaService.dashboardsDbPath(), HttpMethod.POST, Dashboard.toJsonPostRequest(null, title, uid));
	}

	public void updateDashboard(Dashboard dashboard) throws GrafanaException, InterruptedException {
		http.execute(GrafanaService.dashboardsDbPath(), HttpMethod.POST, dashboard.toJsonPutRequest());
	}

	private Dashboard getDashboard(String uid) throws GrafanaException, InterruptedException {
		String url = GrafanaService.dashboardsPath(uid);
		String response = http.execute(url, HttpMethod.GET, null);
		return Dashboard.fromJson(response);
	}

	public Datasource getOrCreateDatasource(Datasource datasource) throws GrafanaException, InterruptedException {
		Datasource ds = null;
		try {
			ds = getDatasource(datasource.getName());
		} catch (GrafanaNotFoundException e) {
			createDatasource(datasource);
			ds = getDatasource(datasource.getName());
		}
		return ds;
	}

	private void createDatasource(Datasource datasource) throws GrafanaException, InterruptedException {
		http.execute(GrafanaService.datasourcesPath(), HttpMethod.POST, datasource.toJson());
	}

	private Datasource getDatasource(String name) throws GrafanaException, InterruptedException {
		String url = GrafanaService.datasourcesPath(name);
		String response = http.execute(url, HttpMethod.GET, null);
		return Datasource.fromJson(response);
	}

}
