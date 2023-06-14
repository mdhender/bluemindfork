/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
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
package net.bluemind.central.reverse.proxy.launcher;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.central.reverse.proxy.ReverseProxyServer;
import net.bluemind.central.reverse.proxy.launcher.GrafanaConfig.GrafanaConfigApi;
import net.bluemind.central.reverse.proxy.launcher.GrafanaConfig.GrafanaConfigDashboard;
import net.bluemind.central.reverse.proxy.launcher.GrafanaConfig.GrafanaConfigDatasource;
import net.bluemind.central.reverse.proxy.launcher.GrafanaConfig.GrafanaConfigServer;
import net.bluemind.central.reverse.proxy.stream.DirEntriesStreamVerticleFactory;
import net.bluemind.lib.grafana.client.GrafanaClient;
import net.bluemind.lib.grafana.config.GrafanaConnection;
import net.bluemind.lib.grafana.dto.Datasource;
import net.bluemind.lib.grafana.dto.Panel;
import net.bluemind.system.application.registration.ApplicationInfo;
import net.bluemind.systemd.notify.Startup;

public class CRPLauncher implements IApplication {

	private static final Logger logger = LoggerFactory.getLogger(CRPLauncher.class);
	private static final int BUFF_SIZE = 100000;

	@Override
	public Object start(IApplicationContext context) throws Exception {
		ReverseProxyServer crp = new ReverseProxyServer();
		crp.run();
		logger.info("CRP started");
		registerApplication();
		metricsVizualization();
		Startup.notifyReady();
		return IApplication.EXIT_OK;
	}

	private void registerApplication() {
		String machineId;
		try {
			machineId = Files.readString(new File("/etc/machine-id").toPath()).replaceAll("\\r|\\n", "");
		} catch (Exception e) {
			machineId = "unknown";
		}
		ApplicationInfo.register(
				new ApplicationInfo("bm-crp", getIpAddresses(), machineId,
						DirEntriesStreamVerticleFactory.config.getString("bm.crp.stream.forest-id")),
				() -> "running", () -> "");
	}

	private String getIpAddresses() {
		List<String> addrList = new ArrayList<>();
		try {
			for (Enumeration<NetworkInterface> eni = NetworkInterface.getNetworkInterfaces(); eni.hasMoreElements();) {
				final NetworkInterface ifc = eni.nextElement();
				if (ifc.isUp()) {
					for (Enumeration<InetAddress> ena = ifc.getInetAddresses(); ena.hasMoreElements();) {
						addrList.add(ena.nextElement().getHostAddress());
					}
				}
			}
		} catch (Exception e) {
			logger.warn("Cannot detect IP", e);
			try {
				return Inet4Address.getLocalHost().getHostAddress();
			} catch (UnknownHostException e1) {
				return "unknown";
			}
		}
		return addrList.stream().filter(ip -> !ip.startsWith("127.")).collect(Collectors.joining(",", "", ""));
	}

	private void metricsVizualization() {
		GrafanaConnection gConnection = null;
		logger.info("============ CRP GRAFANA =========");
		try {
			gConnection = new GrafanaConnection(GrafanaConfig.getOrDefaultStr(GrafanaConfigServer.HOST), //
					GrafanaConfig.getOrDefaultStr(GrafanaConfigApi.TOKEN), //
					GrafanaConfig.getOrDefaultInt(GrafanaConfigApi.SERVICE_ACCOUNT_ID), //
					GrafanaConfig.getOrDefaultStr(GrafanaConfigApi.SERVICE_ACCOUNT_NAME));
			GrafanaConfig.updateToken(gConnection);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return;
		}
		String datasourceUrl = GrafanaConfig.get().getString(GrafanaConfigDatasource.URL);

		GrafanaClient gClient = new GrafanaClient(gConnection);
		Datasource datasource = null;
		try {
			String datasourceName = GrafanaConfig.get().getString(GrafanaConfigDatasource.NAME);
			datasource = gClient.getOrCreateDatasource(datasourceName, datasourceUrl + "/monitoring/metrics");
		} catch (Exception e) {
			logger.error(e.getMessage());
			return;
		}

		if (datasource == null) {
			logger.error("Cannot continue without datasource");
			return;
		}

		Panel panel = null;
		try (InputStream in = getClass().getClassLoader().getResourceAsStream("/data/panel.json")) {
			panel = new Panel(datasource, readStream(in));
			panel.updateJsonDatasource(datasourceUrl);
		} catch (Exception e) {
			logger.error(e.getMessage());
		}

		try {
			String dashboardUid = GrafanaConfig.get().getString(GrafanaConfigDashboard.UID);
			String dashboardTitle = GrafanaConfig.get().getString(GrafanaConfigDashboard.TITLE);
			gClient.getOrCreateDashboard(dashboardUid, dashboardTitle, panel);
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
	}

	private static String readStream(InputStream in) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		final byte[] buffer = new byte[BUFF_SIZE];

		try {
			while (true) {
				int amountRead = in.read(buffer);
				if (amountRead == -1) {
					break;
				}
				out.write(buffer, 0, amountRead);
			}
		} finally {
			out.flush();
			out.close();
		}

		return new String(out.toByteArray(), StandardCharsets.UTF_8.name());
	}

	@Override
	public void stop() {
		logger.info("Application stopped.");
	}

}
