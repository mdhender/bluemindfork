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
package net.bluemind.cloud.monitoring.cli;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "configure-monitoring", description = "Configure Grafana cloud monitoring")
public class ConfigureMonitoringCommand implements ICmdLet, Runnable {

	private static final String CONF_FILE = "/etc/bm-crp/grafana.conf";
	private static final String CONF_AGENT_FILE = "/etc/bm-crp/grafana-agent.yaml";
	private static final String AGENT_FILE = "/etc/default/grafana-agent";
	private static final String RESOURCE_CONF_AGENT_FILE = "/templates/grafana-agent.yaml";
	private static final String RESOURCE_AGENT_FILE = "/templates/grafana-agent";
	private static final String RESOURCE_CONF_GRAFANA = "/templates/grafana.conf";

	private static final String PARAM_ACTIVE = "${active}";
	private static final String PARAM_HOST = "${host}";
	private static final String PARAM_PORT = "${port}";
	private static final String PARAM_USERINFO = "${auth}";
	private static final String PARAM_DATASOURCE_URL = "${datasource_url}";
	private static final String PARAM_DIAGRAM_URL = "${panel_url}";

	private static final String PARAM_WAL_DIR = "${interval}";
	private static final String PARAM_SCRAPE_INTERVAL = "${wal_directory}";
	private static final String PARAM_AGENT_URL = "${remote_url}";

	private static final String PARAM_CONFIG_FILE = "${config_file}";

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("forest");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return ConfigureMonitoringCommand.class;
		}

	}

	private CliContext ctx;

	@Option(names = "--active", description = "activate", defaultValue = "false")
	public Boolean active = false;
	@Option(required = true, names = "--grafana-host", description = "Grafana host or IP adresse")
	public String host;
	@Option(required = true, names = "--grafana-port", description = "Grafana port")
	public int port;
	@Option(required = true, names = "--grafana-user", description = "Grafana admin user")
	public String user;
	@Option(required = true, names = "--grafana-password", description = "Grafana admin password")
	public String password;
	@Option(required = true, names = "--grafana-datasource", description = "URL to the prometheus/Mimir datasource")
	public String datasource;
	@Option(required = true, names = "--grafana-diagram-url", description = "Base URL (http(s)://host) to the Monitoring server exposing the diagram (usually, this server)")
	public String diagramUrl;

	@Override
	public void run() {
		try {
			createGrafanaConfFile();
			ctx.info("/etc/bm-crp/grafana.conf created");
		} catch (IOException e) {
			ctx.error("Grafana conf cannot be write to {}", CONF_FILE);
			ctx.error(e.getMessage(), e);
			e.printStackTrace();
		}

		try {
			createGrafanaAgentConfFile();
			ctx.info("/etc/bm-crp/grafana-agent.yaml created");
		} catch (IOException e) {
			ctx.error("Grafana agent config cannot be write to {}", CONF_AGENT_FILE);
			ctx.error(e.getMessage());
			e.printStackTrace();
		}

		try {
			updateGrafanaAgent();
			ctx.info("/etc/default/grafana-agent updated");
		} catch (IOException e) {
			ctx.error("Grafana agent cannot be write to {}", AGENT_FILE);
			ctx.error(e.getMessage());
			e.printStackTrace();
		}
	}

	private void createGrafanaConfFile() throws IOException {
		try (InputStream in = getClass().getClassLoader().getResourceAsStream(RESOURCE_CONF_GRAFANA)) {
			String fileContent = new String(in.readAllBytes(), StandardCharsets.UTF_8);
			fileContent = fileContent.replace(PARAM_HOST, host) //
					.replace(PARAM_PORT, "" + port) //
					.replace(PARAM_USERINFO, user + ":" + password) //
					.replace(PARAM_ACTIVE, "" + active) //
					.replace(PARAM_DATASOURCE_URL, datasource) //
					.replace(PARAM_DIAGRAM_URL, diagramUrl);
			Files.write(Paths.get(CONF_FILE), fileContent.getBytes());
		}
	}

	private void createGrafanaAgentConfFile() throws IOException {
		try (InputStream in = getClass().getClassLoader().getResourceAsStream(RESOURCE_CONF_AGENT_FILE)) {
			String fileContent = new String(in.readAllBytes(), StandardCharsets.UTF_8);
			fileContent = fileContent.replace(PARAM_WAL_DIR, "/tmp/wal") //
					.replace(PARAM_SCRAPE_INTERVAL, "15s") //
					.replace(PARAM_AGENT_URL, "http://" + host + ":9009/api/v1/push");
			Files.write(Paths.get(CONF_AGENT_FILE), fileContent.getBytes());
		}
	}

	private void updateGrafanaAgent() throws IOException {
		try (InputStream in = getClass().getClassLoader().getResourceAsStream(RESOURCE_AGENT_FILE)) {
			String fileContent = new String(in.readAllBytes(), StandardCharsets.UTF_8);
			fileContent = fileContent.replace(PARAM_CONFIG_FILE, CONF_AGENT_FILE);
			Files.write(Paths.get(AGENT_FILE), fileContent.getBytes());
		}
	}

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		return this;
	}

}