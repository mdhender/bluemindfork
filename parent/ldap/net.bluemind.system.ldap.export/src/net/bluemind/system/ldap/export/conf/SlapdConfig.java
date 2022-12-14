/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.system.ldap.export.conf;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.template.Configuration;
import freemarker.template.Template;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.network.utils.NetworkHelper;
import net.bluemind.node.api.FileDescription;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NCUtils;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.server.api.Server;

public class SlapdConfig {
	private Logger logger = LoggerFactory.getLogger(SlapdConfig.class);

	private ItemValue<Server> server;

	private static final String APPARMOR_INIT_SCRIPT = "apparmor";
	private static final String APPARMOR_DISABLE_PATH = "/etc/apparmor.d/disable";
	private static final String APPARMOR_SLAPD_CONF = "/etc/apparmor.d/usr.sbin.slapd";
	private static final String APPARMOR_DISABLE_SLAPD = APPARMOR_DISABLE_PATH + "/usr.sbin.slapd";

	protected final String confPath;
	protected final String schemaPath;
	protected final String varRunPath;
	protected final String usrLibPath;
	private final String varLibPath = "/var/lib/ldap";

	protected final String sasl2Path;
	private final String sasl2ConfFile = "slapd.conf";
	private final String sasl2ConfTemplate;

	protected final String slapdDefaultPath;
	protected final String slapdDefaultTemplate;

	protected final String owner;
	protected final String group;

	public static SlapdConfig build(ItemValue<Server> server) {
		INodeClient nodeClient = NodeActivator.get(server.value.address());

		// JUnit
		String sasl2ConfTemplate = nodeClient.listFiles("/var/run/saslauthd/mux.accept").isEmpty() ? "slapd.sasl2.conf"
				: "slapd.sasl2.conf.docker";

		if (!nodeClient.listFiles("/etc/redhat-release").isEmpty()) {
			// RedHat
			String confPath = "/etc/openldap/slapd.d";
			String schemaPath = "/etc/openldap/schema";
			String varRunPath = "/var/run/openldap";
			String usrLibPath = "/usr/lib64/openldap";

			String sasl2Path = "/usr/lib64/sasl2";

			String slapdDefaultPath = "/etc/sysconfig/slapd";
			String slapdDefaultTemplate = "slapd.default.redhat";

			String owner = "ldap";
			String group = "ldap";
			return new SlapdConfig(server, confPath, schemaPath, varRunPath, usrLibPath, sasl2Path, sasl2ConfTemplate,
					slapdDefaultPath, slapdDefaultTemplate, owner, group);
		}

		String confPath = "/etc/ldap/slapd.d";
		String schemaPath = "/etc/ldap/schema";
		String varRunPath = "/var/run/slapd";
		String usrLibPath = "/usr/lib/ldap";

		String sasl2Path = "/etc/ldap/sasl2";

		String slapdDefaultPath = "/etc/default/slapd";
		String slapdDefaultTemplate = "slapd.default.debian";

		String owner = "openldap";
		String group = "openldap";
		return new SlapdConfig(server, confPath, schemaPath, varRunPath, usrLibPath, sasl2Path, sasl2ConfTemplate,
				slapdDefaultPath, slapdDefaultTemplate, owner, group);
	}

	private SlapdConfig(ItemValue<Server> server, String confPath, String schemaPath, String varRunPath,
			String usrLibPath, String sasl2Path, String sasl2ConfTemplate, String slapdDefaultPath,
			String slapdDefaultTemplate, String owner, String group) {
		this.server = server;

		this.confPath = confPath;
		this.schemaPath = schemaPath;
		this.varRunPath = varRunPath;
		this.usrLibPath = usrLibPath;

		this.sasl2Path = sasl2Path;
		this.sasl2ConfTemplate = sasl2ConfTemplate;

		this.slapdDefaultPath = slapdDefaultPath;
		this.slapdDefaultTemplate = slapdDefaultTemplate;

		this.owner = owner;
		this.group = group;
	}

	public void init() {
		INodeClient nodeClient = NodeActivator.get(server.value.address());

		disableApparmor(nodeClient);
		stopSlapd(nodeClient);

		configureSlapd(nodeClient);
		startSlapd(nodeClient);
	}

	public void updateSasl() {
		INodeClient nodeClient = NodeActivator.get(server.value.address());

		initSasl(nodeClient);
		stopSlapd(nodeClient);
		startSlapd(nodeClient);
	}

	private void initSasl(INodeClient nodeClient) {
		NCUtils.exec(nodeClient, "/bin/mkdir -p " + sasl2Path);
		nodeClient.writeFile(sasl2Path + "/" + sasl2ConfFile,
				getContentFromTemplate(sasl2ConfTemplate, Collections.emptyMap()));
		NCUtils.exec(nodeClient, "/bin/chown -R " + owner + ":" + group + " " + sasl2Path);
	}

	private void stopSlapd(INodeClient nodeClient) {
		logger.info("Stoping LDAP service");
		NCUtils.exec(nodeClient, "service slapd stop");
	}

	private void startSlapd(INodeClient nodeClient) {
		logger.info("Starting LDAP service");
		NCUtils.exec(nodeClient, "service slapd start");

		new NetworkHelper(server.value.address()).waitForListeningPort(389, 10, TimeUnit.SECONDS);
	}

	private void configureSlapd(INodeClient nodeClient) {
		logger.info("Configuring slapd");

		// Re-create database folder
		List<FileDescription> files = nodeClient.listFiles(varLibPath);
		if (files.size() != 0) {
			NCUtils.exec(nodeClient, String.format("/bin/rm -rf %s",
					files.stream().map(fd -> fd.getPath()).collect(Collectors.joining(" "))));
		}
		NCUtils.exec(nodeClient, "/bin/mkdir -p " + varLibPath);
		NCUtils.exec(nodeClient, "/bin/chown " + owner + ":" + group + " " + varLibPath);

		Map<String, Object> confData = new HashMap<>();
		confData.put("varRunPath", varRunPath);
		confData.put("confPath", confPath);
		confData.put("schemaPath", schemaPath);
		confData.put("usrLibPath", usrLibPath);
		confData.put("varLibPath", varLibPath);

		if (!initSlapd(nodeClient, "slapd.init.bdb.ldif", confData)
				&& !initSlapd(nodeClient, "slapd.init.mdb.ldif", confData)) {
			throw new ServerFault("Neither bdb nor mdb are present - no valid configuration available !");
		}

		nodeClient.writeFile(slapdDefaultPath, getContentFromTemplate(slapdDefaultTemplate, Collections.emptyMap()));

		initSasl(nodeClient);
	}

	private boolean initSlapd(INodeClient nodeClient, String template, Map<String, Object> data) {
		String ldifPath = "/tmp/slapd.init.ldif";

		NCUtils.exec(nodeClient, "/bin/rm -rf " + confPath);
		NCUtils.exec(nodeClient, "/bin/mkdir -p " + confPath);
		NCUtils.exec(nodeClient, "/bin/chown " + owner + ":" + group + " " + confPath);

		nodeClient.writeFile(ldifPath, getContentFromTemplate(template, data));
		int code = NCUtils.exec(nodeClient, "/usr/sbin/slapadd -F " + confPath + " -b cn=config -l " + ldifPath)
				.getExitCode();
		if (code != 0) {
			return false;
		}

		NCUtils.exec(nodeClient, "/bin/chown -R " + owner + ":" + group + " " + confPath);
		logger.info("{} generated from {} template", confPath, template);
		return true;
	}

	private ByteArrayInputStream getContentFromTemplate(String name, Map<String, Object> data) {
		Configuration cfg = new Configuration();
		cfg.setClassForTemplateLoading(getClass(), "/templates");

		Template t = null;
		try {
			t = cfg.getTemplate(name);
		} catch (IOException e) {
			throw new ServerFault(e);
		}

		StringWriter sw = new StringWriter();
		try {
			t.process(data, sw);
		} catch (Exception e) {
			throw new ServerFault(e);
		}

		return new ByteArrayInputStream(sw.toString().getBytes());
	}

	private void disableApparmor(INodeClient nodeClient) {
		try {
			if (NCUtils.exec(nodeClient, "apparmor_status --enabled").getExitCode() != 0) {
				return;
			}
		} catch (ServerFault sf) {
			logger.warn("Unable to get apparmor status, assume disabled");
			return;
		}

		logger.info("Disable apparmor for LDAP service on: " + server.value.address());

		NCUtils.exec(nodeClient, "ln -s " + APPARMOR_SLAPD_CONF + " " + APPARMOR_DISABLE_SLAPD);
		NCUtils.exec(nodeClient, "service " + APPARMOR_INIT_SCRIPT + " teardown");
		NCUtils.exec(nodeClient, "service " + APPARMOR_INIT_SCRIPT + " restart");
		NCUtils.exec(nodeClient, "apparmor_parser -R " + APPARMOR_DISABLE_SLAPD);
	}
}
