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
package net.bluemind.cloud.monitoring.server.grafana.monitor;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import net.bluemind.cloud.monitoring.server.grafana.monitor.GrafanaConfig.GrafanaApiInfos;
import net.bluemind.cloud.monitoring.server.grafana.monitor.GrafanaConfig.GrafanaConfigDashboard;
import net.bluemind.cloud.monitoring.server.grafana.monitor.GrafanaConfig.GrafanaConfigDatasource;
import net.bluemind.cloud.monitoring.server.grafana.monitor.GrafanaConfig.GrafanaConfigPanel;
import net.bluemind.lib.grafana.client.GrafanaClient;
import net.bluemind.lib.grafana.config.GrafanaConnection;
import net.bluemind.lib.grafana.dto.Dashboard;
import net.bluemind.lib.grafana.dto.Datasource;
import net.bluemind.lib.grafana.dto.Panel;
import net.bluemind.lib.grafana.dto.Target;
import net.bluemind.lib.grafana.exception.GrafanaException;

public class GrafanaVisualization {

	private GrafanaClient gClient;
	private Datasource datasource;
	private List<String> metrics;

	private static final String PANEL_TEMPLATE = "/templates/panel.json";
	private static final String DATASOURCE_TEMPLATE = "/templates/datasource.json";
	private static final String TARGET_TEMPLATE = "/templates/target.json";

	static final String DATASOURCE_URL = GrafanaConfig.get().getString(GrafanaConfigDatasource.URL);
	static final String DATASOURCE_NAME = GrafanaConfig.get().getString(GrafanaConfigDatasource.NAME);
	static final String DASHBOARD_UID = GrafanaConfig.get().getString(GrafanaConfigDashboard.UID);
	static final String DASHBOARD_TITLE = GrafanaConfig.get().getString(GrafanaConfigDashboard.TITLE);
	static final String CONTENT_URL = GrafanaConfig.getOrDefaultStr(GrafanaConfigPanel.CONTENT_URL);

	public static void update(List<String> metrics) throws GrafanaException, InterruptedException {
		new GrafanaVisualization().updateDashboard(metrics);
	}

	private void updateDashboard(List<String> metrics) throws GrafanaException, InterruptedException {
		GrafanaConnection gConnection = new GrafanaConnection(GrafanaApiInfos.USERINFO, //
				GrafanaApiInfos.HOST, //
				GrafanaApiInfos.PORT, //
				GrafanaApiInfos.TOKEN, //
				GrafanaApiInfos.ACCOUNTID, //
				GrafanaApiInfos.ACCOUNTNAME);
		GrafanaConfig.updateToken(gConnection);

		this.gClient = new GrafanaClient(gConnection);
		this.metrics = metrics;

		createDatasource();
		createDashboard();
	}

	private void createDatasource() throws GrafanaException, InterruptedException {
		Datasource ds = Datasource.createFromTemplate(DATASOURCE_NAME, DATASOURCE_URL,
				readTemplate(DATASOURCE_TEMPLATE));
		datasource = gClient.getOrCreateDatasource(ds);
	}

	private Panel createDefaultPanel() throws GrafanaException {
		Panel defaultPanel = new Panel(datasource, readTemplate(PANEL_TEMPLATE));
		defaultPanel.updateJsonDatasource(CONTENT_URL);
		return defaultPanel;
	}

	private void createDashboard() throws GrafanaException, InterruptedException {
		Dashboard dashboard = gClient.getOrCreateDashboard(DASHBOARD_UID, DASHBOARD_TITLE);
		if (dashboard.panel == null) {
			dashboard.setPanel(createDefaultPanel());
		}
		Target targetTemplate = Target.fromJson(readTemplate(TARGET_TEMPLATE));
		targetTemplate.setDatasource(datasource);
		dashboard.panel.addMetricsTargets(targetTemplate, metrics);

		gClient.updateDashboard(dashboard);
	}

	private String readTemplate(String template) throws GrafanaException {
		try (InputStream in = getClass().getClassLoader().getResourceAsStream(template)) {
			return new String(in.readAllBytes());
		} catch (IOException e) {
			throw new GrafanaException(e);
		}
	}

}
