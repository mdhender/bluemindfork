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
package net.bluemind.system.iptables.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

import net.bluemind.config.InstallationId;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
import net.bluemind.domain.service.DomainsContainerIdentifier;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.node.api.ExitList;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NCUtils;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.server.api.Server;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.system.iptables.IptablesPath;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class RulesUpdaterTests {
	private static final String NODE_IP = new BmConfIni().get("bluemind/node-tests");

	private static final String CONTAINER_UID = "global.virt";

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());

		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);

		DataSource dataSource = JdbcActivator.getInstance().getDataSource();
		ContainerStore cs = new ContainerStore(null, dataSource, SecurityContext.SYSTEM);
		cs.create(Container.create(InstallationId.getIdentifier(), "installation", "installation",
				SecurityContext.SYSTEM.getSubject(), true));

		if (cs.get(DomainsContainerIdentifier.getIdentifier()) == null) {
			cs.create(Container.create(DomainsContainerIdentifier.getIdentifier(), "domains", "domain",
					SecurityContext.SYSTEM.getSubject(), true));
		}
		IDomains domains = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IDomains.class);

		domains.create(CONTAINER_UID,
				Domain.create(CONTAINER_UID, CONTAINER_UID, CONTAINER_UID, Collections.<String>emptySet()));

		Server nodeServer = new Server();
		nodeServer.ip = NODE_IP;
		nodeServer.tags = Lists.newArrayList("mail/smtp", "mail/imap");

		PopulateHelper.createServers(nodeServer);
	}

	@After
	public void after() throws ServerFault {
		INodeClient nc = NodeActivator.get(NODE_IP);

		String removeScript = "#!/bin/bash\nrm -f " + IptablesPath.IPTABLES_SCRIPT_PATH + " /etc/rc?.d/*"
				+ IptablesPath.IPTABLES_SCRIPT_NAME + " " + IptablesPath.CHKCONFIG_IPTABLES_PATH;
		nc.writeFile("/tmp/remove.sh", new ByteArrayInputStream(removeScript.getBytes()));
		NCUtils.execNoOut(nc, "chmod +x /tmp/remove.sh");
		NCUtils.execNoOut(nc, "/tmp/remove.sh");
		NCUtils.execNoOut(nc, "rm -f /tmp/remove.sh");
	}

	@Test
	public void updateIptablesScriptTask() throws ServerFault {
		RulesUpdater.updateIptablesScript();

		INodeClient nc = NodeActivator.get(NODE_IP);
		String iptablesScript = new String(nc.read(IptablesPath.IPTABLES_SCRIPT_PATH));

		Pattern pattern = Pattern.compile("^bmHosts=\"" + NODE_IP + "\"$", Pattern.MULTILINE);
		assertTrue(pattern.matcher(iptablesScript).find());
	}

	@Test
	public void updateIptablesScriptTaskCheckFiles() throws ServerFault {
		RulesUpdater.updateIptablesScript();

		INodeClient nc = NodeActivator.get(NODE_IP);

		assertTrue(nc.listFiles(IptablesPath.IPTABLES_SCRIPT_PATH).size() == 1);

		String script = "#!/bin/sh\nls /etc/rc[016].d/K??" + IptablesPath.IPTABLES_SCRIPT_NAME;
		nc.writeFile("/tmp/list.sh", new ByteArrayInputStream(script.getBytes()));
		NCUtils.execNoOut(nc, "chmod +x /tmp/list.sh");
		ExitList list = NCUtils.exec(nc, "/tmp/list.sh");
		NCUtils.execNoOut(nc, "rm -f /tmp/list.sh");

		assertEquals(0, list.getExitCode());

		int nbParts = 0;
		for (String line : list) {
			for (String part : line.split("\n")) {
				nbParts++;
				assertTrue(part.matches("^/etc/rc[016].d/K.." + IptablesPath.IPTABLES_SCRIPT_NAME + "$"));
			}
		}
		assertEquals(3, nbParts);

		assertTrue(nc.listFiles(IptablesPath.CHKCONFIG_IPTABLES_PATH).isEmpty());
	}

	@Test
	public void updateIptablesScriptTaskAdditionnalAddresses() throws ServerFault {
		String otherIp = "10.0.0.10";
		ISystemConfiguration systemConfiguration = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(ISystemConfiguration.class, CONTAINER_UID);

		Map<String, String> sc = new HashMap<>();
		sc.put("fwAdditionalIPs", otherIp);
		systemConfiguration.updateMutableValues(sc);
		RulesUpdater.updateIptablesScript();

		INodeClient nc = NodeActivator.get(NODE_IP);
		String iptablesScript = new String(nc.read(IptablesPath.IPTABLES_SCRIPT_PATH));

		Pattern pattern = Pattern.compile(
				"^bmHosts=\"(" + NODE_IP + " " + otherIp + "|" + otherIp + " " + NODE_IP + ")\"$", Pattern.MULTILINE);
		assertTrue(pattern.matcher(iptablesScript).find());

		sc.put("fwAdditionalIPs", null);
		systemConfiguration.updateMutableValues(sc);
		RulesUpdater.updateIptablesScript();

		nc = NodeActivator.get(NODE_IP);
		iptablesScript = new String(nc.read(IptablesPath.IPTABLES_SCRIPT_PATH));

		pattern = Pattern.compile("^bmHosts=\"" + NODE_IP + "\"$", Pattern.MULTILINE);
		assertTrue(pattern.matcher(iptablesScript).find());

		sc.put("fwAdditionalIPs", otherIp);
		systemConfiguration.updateMutableValues(sc);
		RulesUpdater.updateIptablesScript();

		sc.put("fwAdditionalIPs", "");
		systemConfiguration.updateMutableValues(sc);
		RulesUpdater.updateIptablesScript();

		nc = NodeActivator.get(NODE_IP);
		iptablesScript = new String(nc.read(IptablesPath.IPTABLES_SCRIPT_PATH));

		assertTrue(pattern.matcher(iptablesScript).find());
	}

	@Test
	public void updateIptablesScript() throws ServerFault {
		RulesUpdater.updateIptablesScript(new BmTestContext(SecurityContext.SYSTEM), null, null);

		INodeClient nc = NodeActivator.get(NODE_IP);
		String iptablesScript = new String(nc.read(IptablesPath.IPTABLES_SCRIPT_PATH));

		Pattern pattern = Pattern.compile("^bmHosts=\"" + NODE_IP + "\"$", Pattern.MULTILINE);
		assertTrue(pattern.matcher(iptablesScript).find());
	}

	@Test
	public void updateIptablesScriptAddRemoveHost() throws ServerFault {
		Server testServer = new Server();
		testServer.ip = "10.0.0.10";

		RulesUpdater.updateIptablesScript(new BmTestContext(SecurityContext.SYSTEM), null, testServer);

		INodeClient nc = NodeActivator.get(NODE_IP);
		String iptablesScript = new String(nc.read(IptablesPath.IPTABLES_SCRIPT_PATH));

		Pattern pattern = Pattern.compile(
				"^bmHosts=\"(" + NODE_IP + " " + testServer.ip + "|" + testServer.ip + " " + NODE_IP + ")\"$",
				Pattern.MULTILINE);
		assertTrue(pattern.matcher(iptablesScript).find());

		RulesUpdater.updateIptablesScript(new BmTestContext(SecurityContext.SYSTEM), testServer, null);

		iptablesScript = new String(nc.read(IptablesPath.IPTABLES_SCRIPT_PATH));

		pattern = Pattern.compile("^bmHosts=\"" + NODE_IP + "\"$", Pattern.MULTILINE);
		assertTrue(pattern.matcher(iptablesScript).find());
	}
}
