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
package net.bluemind.system.ldap.export.internal.conf;

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

public abstract class SlapdConfig {
	private Logger logger = LoggerFactory.getLogger(SlapdConfig.class);

	private ItemValue<Server> server;

	private static final String APPARMOR_INIT_SCRIPT = "apparmor";
	private static final String APPARMOR_DISABLE_PATH = "/etc/apparmor.d/disable";
	private static final String APPARMOR_SLAPD_CONF = "/etc/apparmor.d/usr.sbin.slapd";
	private static final String APPARMOR_DISABLE_SLAPD = APPARMOR_DISABLE_PATH + "/usr.sbin.slapd";

	protected String confPath = null;
	protected String schemaPath = null;
	protected String varRunPath = null;
	protected String usrLibPath = null;
	private String varLibPath = "/var/lib/ldap";

	protected String sasl2Path = null;
	private String sasl2ConfFile = "slapd.conf";
	private String sasl2ConfTemplate = "slapd.sasl2.conf";

	protected String slapdDefaultPath = null;
	protected String slapdDefaultTemplate = null;

	protected String owner = null;
	protected String group = null;

	public SlapdConfig(ItemValue<Server> server) {
		this.server = server;
	}

	public void init() {
		INodeClient nodeClient = NodeActivator.get(server.value.address());

		disableApparmor(nodeClient);
		stopAndRemoveConf(nodeClient);

		configureSlapd(nodeClient);
		startSlapd(nodeClient);
	}

	private void startSlapd(INodeClient nodeClient) {
		logger.info("Starting LDAP service");
		NCUtils.exec(nodeClient, "service slapd start");

		new NetworkHelper(server.value.address()).waitForListeningPort(389, 10, TimeUnit.SECONDS);
	}

	private void configureSlapd(INodeClient nodeClient) {
		logger.info("Create LDAP configuration");
		String ldifPath = "/tmp/slapd.init.ldif";

		Map<String, Object> confData = new HashMap<>();
		confData.put("varRunPath", varRunPath);
		confData.put("confPath", confPath);
		confData.put("schemaPath", schemaPath);
		confData.put("usrLibPath", usrLibPath);
		confData.put("varLibPath", varLibPath);

		nodeClient.writeFile(ldifPath, getContentFromTemplate("slapd.init.ldif", confData));

		NCUtils.exec(nodeClient, "/usr/sbin/slapadd -F " + confPath + " -b cn=config -l " + ldifPath);
		NCUtils.exec(nodeClient, "/bin/chown -R " + owner + ":" + group + " " + confPath);

		nodeClient.writeFile(slapdDefaultPath, getContentFromTemplate(slapdDefaultTemplate, Collections.emptyMap()));

		NCUtils.exec(nodeClient, "/bin/mkdir -p " + sasl2Path);
		nodeClient.writeFile(sasl2Path + "/" + sasl2ConfFile,
				getContentFromTemplate(sasl2ConfTemplate, Collections.emptyMap()));
		NCUtils.exec(nodeClient, "/bin/chown -R " + owner + ":" + group + " " + sasl2Path);

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

	private void stopAndRemoveConf(INodeClient nodeClient) {
		logger.info("Initializing slapd configuration");

		logger.info("Stopping LDAP service");
		NCUtils.exec(nodeClient, "service slapd stop");

		logger.info("Reset LDAP configuration");
		NCUtils.exec(nodeClient, "/bin/rm -rf " + confPath);
		NCUtils.exec(nodeClient, "/bin/mkdir -p " + confPath);
		NCUtils.exec(nodeClient, "/bin/chown " + owner + ":" + group + " " + confPath);

		List<FileDescription> files = nodeClient.listFiles(varLibPath);
		if (files.size() != 0) {
			NCUtils.exec(nodeClient, String.format("/bin/rm -rf %s",
					files.stream().map(fd -> fd.getPath()).collect(Collectors.joining(" "))));
		}
		NCUtils.exec(nodeClient, "/bin/mkdir -p " + varLibPath);
		NCUtils.exec(nodeClient, "/bin/chown " + owner + ":" + group + " " + varLibPath);
	}

	private void disableApparmor(INodeClient nodeClient) {
		List<FileDescription> files = nodeClient.listFiles("/etc/init.d/" + APPARMOR_INIT_SCRIPT);
		if (!files.isEmpty()) {
			logger.info("Disable apparmor for LDAP service on: " + server.value.address());

			NCUtils.exec(nodeClient, "ln -s " + APPARMOR_SLAPD_CONF + " " + APPARMOR_DISABLE_SLAPD);
			NCUtils.exec(nodeClient, "service " + APPARMOR_INIT_SCRIPT + " teardown");
			NCUtils.exec(nodeClient, "service " + APPARMOR_INIT_SCRIPT + " restart");
		}
	}
}
