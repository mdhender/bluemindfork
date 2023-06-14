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
import net.bluemind.lib.grafana.dto.Panel;
import net.bluemind.lib.grafana.utils.ApiHttpHelper;

public class GrafanaClient {

	private static final String dashboardsPath = "/api/dashboards";
	private static final String dashboardsDbPath = "/api/dashboards/db";
	private static final String datasourcesPath = "/api/datasources";

	private final ApiHttpHelper http;

	public GrafanaClient(GrafanaConnection config) {
		this.http = new ApiHttpHelper(config.host, config.apiKey);
	}

	public Dashboard getOrCreateDashboard(String uid, String title, Panel panel) throws Exception {
		Dashboard dashboard = getDashboard(uid);
		if (dashboard == null) {
			createDashboard(uid, title);
			dashboard = getDashboard(uid);
		}
		dashboard.panel = panel;
		updateDashboardPanel(dashboard);
		return dashboard;
	}

	private void createDashboard(String uid, String title) throws Exception {
		http.execute(dashboardsDbPath, HttpMethod.POST, Dashboard.toJsonPostRequest(null, title, uid));
	}

	private void updateDashboardPanel(Dashboard dashboard) throws Exception {
		http.execute(dashboardsDbPath, HttpMethod.POST, dashboard.toJsonPutRequest());
	}

	private Dashboard getDashboard(String uid) throws Exception {
		String url = dashboardsPath + "/uid/" + uid;
		String response = http.execute(url, HttpMethod.GET, null);
		return Dashboard.fromJson(response);
	}

	public Datasource getOrCreateDatasource(String name, String url) throws Exception {
		Datasource datasource = getDatasource(name);
		if (datasource == null) {
			datasource = createDatasource(name, url);
		}
		return datasource;
	}

	private Datasource createDatasource(String name, String url) throws Exception {
		String response = http.execute(datasourcesPath, HttpMethod.POST, Datasource.toJson(name, url));
		return Datasource.fromJson(response);
	}

	private Datasource getDatasource(String name) throws Exception {
		String url = datasourcesPath + "/name/" + name;
		String response = http.execute(url, HttpMethod.GET, null);
		return Datasource.fromJson(response);
	}

}
