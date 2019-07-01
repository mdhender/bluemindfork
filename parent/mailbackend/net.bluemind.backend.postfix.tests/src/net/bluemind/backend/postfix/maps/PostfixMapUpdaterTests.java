/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.backend.postfix.maps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.junit.Before;
import org.junit.Test;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;

import net.bluemind.backend.postfix.Activator;
import net.bluemind.backend.postfix.internal.maps.PostfixMapUpdater;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.node.api.ExitList;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NCUtils;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class PostfixMapUpdaterTests {
	private static final List<String> mapsFileNames = Arrays.asList("/etc/postfix/virtual_domains",
			"/etc/postfix/virtual_mailbox", "/etc/postfix/virtual_alias", "/etc/postfix/transport",
			"/etc/postfix/master_relay_transport");

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		JdbcTestHelper.getInstance().getDbSchemaService().initialize();
		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());

		PopulateHelper.initGlobalVirt();

		final CountDownLatch launched = new CountDownLatch(1);
		VertxPlatform.spawnVerticles(new Handler<AsyncResult<Void>>() {
			@Override
			public void handle(AsyncResult<Void> event) {
				launched.countDown();
			}
		});
		launched.await();

		Activator.DISABLE_EVENT = true;
	}

	@Test
	public void refreshMaps_noServer_NoDomain() throws SQLException {
		PostfixMapUpdater postfixMapUpdater = new PostfixMapUpdater(new BmTestContext(SecurityContext.SYSTEM));
		postfixMapUpdater.refreshMaps();
	}

	@Test
	public void refreshMaps_noSmtpOrEdgeServerAssignedToDomain() throws Exception {
		Server needlessServer = new Server();
		needlessServer.ip = "10.0.0.1";
		needlessServer.name = "test-" + System.nanoTime();
		needlessServer.tags = Arrays.asList("mail/imap");
		PopulateHelper.createServers(needlessServer);

		PopulateHelper.createTestDomain("domain.tld", needlessServer);

		PostfixMapUpdater postfixMapUpdater = new PostfixMapUpdater(new BmTestContext(SecurityContext.SYSTEM));
		postfixMapUpdater.refreshMaps();
	}

	@Test
	public void refreshMaps_smtpServerAssignedToDomain() throws Exception {
		Server needlessServer = new Server();
		needlessServer.ip = "10.0.0.1";
		needlessServer.name = "test-" + System.nanoTime();
		needlessServer.tags = Arrays.asList("mail/imap");

		Server smtp = new Server();
		smtp.ip = new BmConfIni().get("bluemind/smtp-role");
		smtp.name = "test-" + System.nanoTime();
		smtp.tags = Arrays.asList("mail/smtp");
		PopulateHelper.createServers(needlessServer, smtp);

		PopulateHelper.createTestDomain("domain.tld", needlessServer, smtp);

		INodeClient nodeClient = NodeActivator.get(smtp.address());
		for (String mapFileName : mapsFileNames) {
			NCUtils.waitFor(nodeClient, nodeClient.executeCommand(
					"rm -f " + mapFileName + "-flat.db " + mapFileName + "-flat " + mapFileName + ".db"));

			ExitList status = NCUtils.waitFor(nodeClient, nodeClient.executeCommand("test -e " + mapFileName));
			assertEquals(1, status.getExitCode());
		}

		PostfixMapUpdater postfixMapUpdater = new PostfixMapUpdater(new BmTestContext(SecurityContext.SYSTEM));
		postfixMapUpdater.refreshMaps();

		nodeClient = NodeActivator.get(smtp.address());
		for (String mapFileName : mapsFileNames) {
			ExitList status = NCUtils.waitFor(nodeClient, nodeClient.executeCommand("test -e " + mapFileName + ".db"));
			assertEquals(0, status.getExitCode());

			status = NCUtils.waitFor(nodeClient, nodeClient.executeCommand("test -e " + mapFileName + "-flat"));
			assertEquals(0, status.getExitCode());

			String map = new String(nodeClient.read(mapFileName + "-flat"));
			if (!map.isEmpty()) {
				assertTrue(map.contains("domain.tld"));
			}
		}
	}

	@Test
	public void refreshMaps_smtpAndEdgeServerAssignedToDomain() throws Exception {
		Server needlessServer = new Server();
		needlessServer.ip = "10.0.0.1";
		needlessServer.name = "test-" + System.nanoTime();
		needlessServer.tags = Arrays.asList("mail/imap");

		Server smtp = new Server();
		smtp.ip = new BmConfIni().get("bluemind/smtp-role");
		smtp.name = "test-" + System.nanoTime();
		smtp.tags = Arrays.asList("mail/smtp");

		Server edge = new Server();
		edge.ip = new BmConfIni().get("bluemind/smtp-edge-role");
		edge.name = "test-" + System.nanoTime();
		edge.tags = Arrays.asList("mail/smtp-edge");
		PopulateHelper.createServers(needlessServer, smtp, edge);

		PopulateHelper.createTestDomain("domain.tld", needlessServer, smtp, edge);

		for (Server server : Arrays.asList(smtp, edge)) {
			INodeClient nodeClient = NodeActivator.get(server.address());
			for (String mapFileName : mapsFileNames) {
				NCUtils.waitFor(nodeClient, nodeClient.executeCommand(
						"rm -f " + mapFileName + "-flat.db " + mapFileName + "-flat " + mapFileName + ".db"));

				ExitList status = NCUtils.waitFor(nodeClient, nodeClient.executeCommand("test -e " + mapFileName));
				assertEquals(1, status.getExitCode());
			}
		}

		PostfixMapUpdater postfixMapUpdater = new PostfixMapUpdater(new BmTestContext(SecurityContext.SYSTEM));
		postfixMapUpdater.refreshMaps();

		for (Server server : Arrays.asList(smtp, edge)) {
			INodeClient nodeClient = NodeActivator.get(server.address());
			for (String mapFileName : mapsFileNames) {
				ExitList status = NCUtils.waitFor(nodeClient,
						nodeClient.executeCommand("test -e " + mapFileName + ".db"));
				assertEquals(0, status.getExitCode());

				status = NCUtils.waitFor(nodeClient, nodeClient.executeCommand("test -e " + mapFileName + "-flat"));
				assertEquals(0, status.getExitCode());

				String map = new String(nodeClient.read(mapFileName + "-flat"));
				if (!map.isEmpty()) {
					assertTrue(map.contains("domain.tld"));
				}
			}
		}
	}

	@Test
	public void refreshMaps_domainWithSmtpAndEdgeServerAssigned_domainWithSmtpServerAssigned() throws Exception {
		Server needlessServer = new Server();
		needlessServer.ip = "10.0.0.1";
		needlessServer.name = "test-" + System.nanoTime();
		needlessServer.tags = Arrays.asList("mail/imap");

		Server smtp = new Server();
		smtp.ip = new BmConfIni().get("bluemind/smtp-role");
		smtp.name = "test-" + System.nanoTime();
		smtp.tags = Arrays.asList("mail/smtp");

		Server edge = new Server();
		edge.ip = new BmConfIni().get("bluemind/smtp-edge-role");
		edge.name = "test-" + System.nanoTime();
		edge.tags = Arrays.asList("mail/smtp-edge");
		PopulateHelper.createServers(needlessServer, smtp, edge);

		PopulateHelper.createTestDomain("domain1.tld", needlessServer, smtp, edge);
		PopulateHelper.createTestDomain("domain2.tld", needlessServer, smtp);

		for (Server server : Arrays.asList(smtp, edge)) {
			INodeClient nodeClient = NodeActivator.get(server.address());
			for (String mapFileName : mapsFileNames) {
				NCUtils.waitFor(nodeClient, nodeClient.executeCommand(
						"rm -f " + mapFileName + "-flat.db " + mapFileName + "-flat " + mapFileName + ".db"));

				ExitList status = NCUtils.waitFor(nodeClient, nodeClient.executeCommand("test -e " + mapFileName));
				assertEquals(1, status.getExitCode());
			}
		}

		PostfixMapUpdater postfixMapUpdater = new PostfixMapUpdater(new BmTestContext(SecurityContext.SYSTEM));
		postfixMapUpdater.refreshMaps();
		INodeClient nodeClient = NodeActivator.get(smtp.address());
		for (String mapFileName : mapsFileNames) {
			ExitList status = NCUtils.waitFor(nodeClient, nodeClient.executeCommand("test -e " + mapFileName + ".db"));
			assertEquals(0, status.getExitCode());

			status = NCUtils.waitFor(nodeClient, nodeClient.executeCommand("test -e " + mapFileName + "-flat"));
			assertEquals(0, status.getExitCode());

			String map = new String(nodeClient.read(mapFileName + "-flat"));
			if (!map.isEmpty()) {
				assertTrue(map.contains("domain1.tld"));
				assertTrue(map.contains("domain2.tld"));
			}
		}

		nodeClient = NodeActivator.get(edge.address());
		for (String mapFileName : mapsFileNames) {
			ExitList status = NCUtils.waitFor(nodeClient, nodeClient.executeCommand("test -e " + mapFileName + ".db"));
			assertEquals(0, status.getExitCode());

			status = NCUtils.waitFor(nodeClient, nodeClient.executeCommand("test -e " + mapFileName + "-flat"));
			assertEquals(0, status.getExitCode());

			String map = new String(nodeClient.read(mapFileName + "-flat"));
			if (!map.isEmpty()) {
				assertTrue(map.contains("domain1.tld"));
				assertFalse(map.contains("domain2.tld"));
			}
		}
	}

	@Test
	public void refreshMaps_smtpAndEdgeTagged_noDomainAssigned_notSmtpHost() throws Exception {
		Server needlessServer = new Server();
		needlessServer.ip = "10.0.0.1";
		needlessServer.name = "test-" + System.nanoTime();
		needlessServer.tags = Arrays.asList("mail/imap");

		Server smtp = new Server();
		smtp.ip = new BmConfIni().get("bluemind/smtp-role");
		smtp.name = "test-" + System.nanoTime();
		smtp.tags = Arrays.asList("mail/smtp");

		Server edge = new Server();
		edge.ip = new BmConfIni().get("bluemind/smtp-edge-role");
		edge.name = "test-" + System.nanoTime();
		edge.tags = Arrays.asList("mail/smtp-edge");
		PopulateHelper.createServers(needlessServer, smtp, edge);

		PopulateHelper.createTestDomain("domain1.tld", needlessServer);

		for (Server server : Arrays.asList(smtp, edge)) {
			INodeClient nodeClient = NodeActivator.get(server.address());
			for (String mapFileName : mapsFileNames) {
				NCUtils.waitFor(nodeClient, nodeClient.executeCommand(
						"rm -f " + mapFileName + "-flat.db " + mapFileName + "-flat " + mapFileName + ".db"));

				ExitList status = NCUtils.waitFor(nodeClient, nodeClient.executeCommand("test -e " + mapFileName));
				assertEquals(1, status.getExitCode());
			}
		}

		PostfixMapUpdater postfixMapUpdater = new PostfixMapUpdater(new BmTestContext(SecurityContext.SYSTEM));
		postfixMapUpdater.refreshMaps();

		for (Server server : Arrays.asList(smtp, edge)) {
			INodeClient nodeClient = NodeActivator.get(server.address());
			for (String mapFileName : mapsFileNames) {
				ExitList status = NCUtils.waitFor(nodeClient,
						nodeClient.executeCommand("test -e " + mapFileName + ".db"));
				assertEquals(0, status.getExitCode());

				status = NCUtils.waitFor(nodeClient, nodeClient.executeCommand("test -e " + mapFileName + "-flat"));
				assertEquals(0, status.getExitCode());

				assertEquals(0, nodeClient.read(mapFileName + "-flat").length);
			}
		}
	}

	@Test
	public void refreshMaps_smtpAndEdgeTaggedAndAssigned() throws Exception {
		Server smtp = new Server();
		smtp.ip = new BmConfIni().get("bluemind/smtp-role");
		smtp.name = "test-" + System.nanoTime();
		smtp.tags = Arrays.asList("mail/smtp");

		Server edge = new Server();
		edge.ip = new BmConfIni().get("bluemind/smtp-edge-role");
		edge.name = "test-" + System.nanoTime();
		edge.tags = Arrays.asList("mail/smtp-edge");
		PopulateHelper.createServers(smtp, edge);

		PopulateHelper.createTestDomain("domain.tld", smtp, edge);

		PostfixMapUpdater postfixMapUpdater = new PostfixMapUpdater(new BmTestContext(SecurityContext.SYSTEM));
		assertNotNull(postfixMapUpdater);

		for (Server server : Arrays.asList(smtp, edge)) {
			INodeClient nodeClient = NodeActivator.get(server.address());
			for (String mapFileName : mapsFileNames) {
				NCUtils.waitFor(nodeClient, nodeClient.executeCommand(
						"rm -f " + mapFileName + "-flat.db " + mapFileName + "-flat " + mapFileName + ".db"));

				ExitList status = NCUtils.waitFor(nodeClient, nodeClient.executeCommand("test -e " + mapFileName));
				assertEquals(1, status.getExitCode());
			}
		}

		postfixMapUpdater.refreshMaps();

		for (Server server : Arrays.asList(smtp, edge)) {
			INodeClient nodeClient = NodeActivator.get(server.address());
			for (String mapFileName : mapsFileNames) {
				ExitList status = NCUtils.waitFor(nodeClient,
						nodeClient.executeCommand("test -e " + mapFileName + ".db"));
				assertEquals(0, status.getExitCode());
			}
		}
	}

	@Test
	public void refreshMaps_smtpTaggedAndAssigned_edgeTaggedNotAssigned() throws Exception {
		Server smtp = new Server();
		smtp.ip = new BmConfIni().get("bluemind/smtp-role");
		smtp.name = "test-" + System.nanoTime();
		smtp.tags = Arrays.asList("mail/smtp");

		Server edge = new Server();
		edge.ip = new BmConfIni().get("bluemind/smtp-edge-role");
		edge.name = "test-" + System.nanoTime();
		edge.tags = Arrays.asList("mail/smtp-edge");
		PopulateHelper.createServers(smtp, edge);

		PopulateHelper.createTestDomain("domain.tld", smtp);

		PostfixMapUpdater postfixMapUpdater = new PostfixMapUpdater(new BmTestContext(SecurityContext.SYSTEM));
		assertNotNull(postfixMapUpdater);

		for (Server server : Arrays.asList(smtp, edge)) {
			INodeClient nodeClient = NodeActivator.get(server.address());
			for (String mapFileName : mapsFileNames) {
				NCUtils.waitFor(nodeClient, nodeClient.executeCommand(
						"rm -f " + mapFileName + "-flat.db " + mapFileName + "-flat " + mapFileName + ".db"));

				ExitList status = NCUtils.waitFor(nodeClient, nodeClient.executeCommand("test -e " + mapFileName));
				assertEquals(1, status.getExitCode());
			}
		}

		postfixMapUpdater.refreshMaps();

		for (Server server : Arrays.asList(smtp, edge)) {
			INodeClient nodeClient = NodeActivator.get(server.address());
			for (String mapFileName : mapsFileNames) {
				ExitList status = NCUtils.waitFor(nodeClient,
						nodeClient.executeCommand("test -e " + mapFileName + ".db"));
				assertEquals(0, status.getExitCode());
			}
		}
	}
}
