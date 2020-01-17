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
import static org.junit.Assert.fail;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;

import org.junit.Before;
import org.junit.Test;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import net.bluemind.backend.postfix.Activator;
import net.bluemind.backend.postfix.internal.maps.DomainInfo;
import net.bluemind.backend.postfix.internal.maps.MapRow;
import net.bluemind.backend.postfix.internal.maps.ServerMaps;
import net.bluemind.config.InstallationId;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.domain.api.Domain;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.node.api.ExitList;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NCUtils;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class ServerMapsTests {
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

	private IServer getServerService() {
		return ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IServer.class,
				InstallationId.getIdentifier());
	}

	@Test
	public void init_noSmtpOrEdgeTagged() throws Exception {
		Server server = new Server();
		server.ip = "10.0.0.1";
		server.name = "test-" + System.nanoTime();
		server.tags = Arrays.asList("mail/imap");
		PopulateHelper.createServers(server);

		ItemValue<Domain> domain = PopulateHelper.createTestDomain("domain.tld", server);

		Map<String, DomainInfo> domainInfoByUid = new HashMap<>();
		domainInfoByUid.put(domain.uid, DomainInfo.build(domain, Collections.emptyMap()));

		Optional<ServerMaps> serverMaps = ServerMaps.init(Collections.emptyList(),
				getServerService().getComplete(server.ip), domainInfoByUid, Collections.emptyList());

		assertFalse(serverMaps.isPresent());
	}

	@Test
	public void init_smtpTagged_assignedUnknowDomain() throws Exception {
		Server server = new Server();
		server.ip = "10.0.0.1";
		server.name = "test-" + System.nanoTime();
		server.tags = Arrays.asList("mail/smtp");
		PopulateHelper.createServers(server);

		PopulateHelper.createTestDomain("domain.tld", server);

		Optional<ServerMaps> optionalServerMaps = ServerMaps.init(Collections.emptyList(),
				getServerService().getComplete(server.ip), Collections.emptyMap(), Collections.emptyList());

		assertTrue(optionalServerMaps.isPresent());

		ServerMaps serverMaps = optionalServerMaps.get();
		assertEquals(server.ip, serverMaps.getServer().uid);
		assertTrue(serverMaps.getDomainInfoByUid().isEmpty());
		assertTrue(serverMaps.getEdgeNextHopByDomainUid().isEmpty());
		assertTrue(serverMaps.getMapRows().isEmpty());
	}

	@Test
	public void init_edgeTagged_assignedUnknowDomain() throws Exception {
		Server server = new Server();
		server.ip = "10.0.0.1";
		server.name = "test-" + System.nanoTime();
		server.tags = Arrays.asList("mail/smtp-edge");
		PopulateHelper.createServers(server);

		PopulateHelper.createTestDomain("domain.tld", server);

		Optional<ServerMaps> optionalServerMaps = ServerMaps.init(Collections.emptyList(),
				getServerService().getComplete(server.ip), Collections.emptyMap(), Collections.emptyList());
		assertTrue(optionalServerMaps.isPresent());

		ServerMaps serverMaps = optionalServerMaps.get();
		assertEquals(server.ip, serverMaps.getServer().uid);
		assertTrue(serverMaps.getDomainInfoByUid().isEmpty());
		assertTrue(serverMaps.getEdgeNextHopByDomainUid().isEmpty());
		assertTrue(serverMaps.getMapRows().isEmpty());
	}

	@Test
	public void init_smtpAndEdgeTagged_sameDomain() throws Exception {
		Server server = new Server();
		server.ip = "10.0.0.1";
		server.name = "test-" + System.nanoTime();
		server.tags = Arrays.asList("mail/smtp", "mail/smtp-edge");
		PopulateHelper.createServers(server);

		ItemValue<Domain> domain = PopulateHelper.createTestDomain("domain.tld", server);

		Map<String, DomainInfo> domainInfoByUid = new HashMap<>();
		domainInfoByUid.put(domain.uid, DomainInfo.build(domain, Collections.emptyMap()));

		Optional<ServerMaps> optionalServerMaps = ServerMaps.init(getServerService().allComplete(),
				getServerService().getComplete(server.ip), domainInfoByUid, Collections.emptyList());

		assertTrue(optionalServerMaps.isPresent());

		ServerMaps serverMaps = optionalServerMaps.get();

		assertNotNull(serverMaps.getServer());
		assertEquals(server.ip, serverMaps.getServer().uid);

		assertNotNull(serverMaps.getDomainInfoByUid());
		assertEquals(1, serverMaps.getDomainInfoByUid().size());
		assertTrue(serverMaps.getDomainInfoByUid().containsKey("domain.tld"));
		assertTrue(serverMaps.getDomainInfoByUid().get("domain.tld").domain.uid.equals("domain.tld"));

		assertEquals(0, serverMaps.getEdgeNextHopByDomainUid().size());
	}

	@Test
	public void init_noSmtpTag_edgeAssigned() throws Exception {
		Server server = new Server();
		server.ip = "10.0.0.1";
		server.name = "test-" + System.nanoTime();
		server.tags = Arrays.asList("mail/smtp-edge");
		PopulateHelper.createServers(server);

		ItemValue<Domain> domain = PopulateHelper.createTestDomain("domain.tld", server);

		Map<String, DomainInfo> domainInfoByUid = new HashMap<>();
		domainInfoByUid.put(domain.uid, DomainInfo.build(domain, Collections.emptyMap()));

		try {
			ServerMaps.init(getServerService().allComplete(), getServerService().getComplete(server.ip),
					domainInfoByUid, Collections.emptyList());
			fail("Test must thrown an excpetion");
		} catch (InvalidParameterException ipe) {
			assertEquals("Unable to find host tagued as mail/smtp for domain uid: domain.tld", ipe.getMessage());
		}
	}

	@Test
	public void init_noSmtpAssigned_edgeAssigned() throws Exception {
		Server edge = new Server();
		edge.ip = "10.0.0.1";
		edge.name = "test-" + System.nanoTime();
		edge.tags = Arrays.asList("mail/smtp-edge");

		Server smtp = new Server();
		smtp.ip = "10.0.0.2";
		smtp.name = "test-" + System.nanoTime();
		smtp.tags = Arrays.asList("mail/smtp");
		PopulateHelper.createServers(smtp, edge);

		ItemValue<Domain> domain = PopulateHelper.createTestDomain("domain.tld", edge);

		Map<String, DomainInfo> domainInfoByUid = new HashMap<>();
		domainInfoByUid.put(domain.uid, DomainInfo.build(domain, Collections.emptyMap()));

		try {
			ServerMaps.init(getServerService().allComplete(), getServerService().getComplete(edge.ip), domainInfoByUid,
					Collections.emptyList());
			fail("Test must thrown an excpetion");
		} catch (InvalidParameterException ipe) {
			assertEquals("Unable to find host tagued as mail/smtp for domain uid: domain.tld", ipe.getMessage());
		}
	}

	@Test
	public void init_smtpAndEdgeAssigned() throws Exception {
		Server edge = new Server();
		edge.ip = "10.0.0.1";
		edge.name = "test-" + System.nanoTime();
		edge.tags = Arrays.asList("mail/smtp-edge");

		Server smtp = new Server();
		smtp.ip = "10.0.0.2";
		smtp.name = "test-" + System.nanoTime();
		smtp.tags = Arrays.asList("mail/smtp");
		PopulateHelper.createServers(smtp, edge);

		ItemValue<Domain> domain = PopulateHelper.createTestDomain("domain.tld", smtp, edge);

		Map<String, DomainInfo> domainInfoByUid = new HashMap<>();
		domainInfoByUid.put(domain.uid, DomainInfo.build(domain, Collections.emptyMap()));
		// SMTP
		Optional<ServerMaps> optionalServerMaps = ServerMaps.init(getServerService().allComplete(),
				getServerService().getComplete(smtp.ip), domainInfoByUid, Collections.emptyList());

		assertTrue(optionalServerMaps.isPresent());

		ServerMaps serverMaps = optionalServerMaps.get();

		assertNotNull(serverMaps.getServer());
		assertEquals(smtp.ip, serverMaps.getServer().uid);

		assertNotNull(serverMaps.getDomainInfoByUid());
		assertEquals(1, serverMaps.getDomainInfoByUid().size());
		assertTrue(serverMaps.getDomainInfoByUid().containsKey("domain.tld"));
		assertTrue(serverMaps.getDomainInfoByUid().get("domain.tld").domain.uid.equals("domain.tld"));

		assertEquals(0, serverMaps.getEdgeNextHopByDomainUid().size());

		// Edge
		optionalServerMaps = ServerMaps.init(getServerService().allComplete(), getServerService().getComplete(edge.ip),
				domainInfoByUid, Collections.emptyList());

		assertTrue(optionalServerMaps.isPresent());

		serverMaps = optionalServerMaps.get();

		assertNotNull(serverMaps.getServer());
		assertEquals(edge.ip, serverMaps.getServer().uid);

		assertNotNull(serverMaps.getDomainInfoByUid());
		assertEquals(1, serverMaps.getDomainInfoByUid().size());
		assertTrue(serverMaps.getDomainInfoByUid().containsKey("domain.tld"));
		assertTrue(serverMaps.getDomainInfoByUid().get("domain.tld").domain.uid.equals("domain.tld"));

		assertEquals(1, serverMaps.getEdgeNextHopByDomainUid().size());
		assertEquals("domain.tld", serverMaps.getEdgeNextHopByDomainUid().keySet().iterator().next());
		assertNotNull(serverMaps.getEdgeNextHopByDomainUid().get("domain.tld"));
		assertEquals("10.0.0.2", serverMaps.getEdgeNextHopByDomainUid().get("domain.tld").uid);
	}

	@Test
	public void init_edgeAssigned_smtpNotFound() throws Exception {
		Server edge = new Server();
		edge.ip = "10.0.0.1";
		edge.name = "test-" + System.nanoTime();
		edge.tags = Arrays.asList("mail/smtp-edge");

		Server smtp = new Server();
		smtp.ip = "10.0.0.2";
		smtp.name = "test-" + System.nanoTime();
		smtp.tags = Arrays.asList("mail/smtp");
		PopulateHelper.createServers(smtp, edge);

		ItemValue<Domain> domain = PopulateHelper.createTestDomain("domain.tld", smtp, edge);

		Map<String, DomainInfo> domainInfoByUid = new HashMap<>();
		domainInfoByUid.put(domain.uid, DomainInfo.build(domain, Collections.emptyMap()));

		try {
			ServerMaps.init(Arrays.asList(getServerService().getComplete(edge.ip)),
					getServerService().getComplete(edge.ip), domainInfoByUid, Collections.emptyList());
			fail("Test must thrown an exception");
		} catch (InvalidParameterException ipe) {
			assertEquals("Unable to find host uid: 10.0.0.2", ipe.getMessage());
		}
	}

	@Test
	public void init_multipleSmtpAssigned_OneEdgeAssigned() throws Exception {
		Server edge = new Server();
		edge.ip = "10.0.0.1";
		edge.name = "test-" + System.nanoTime();
		edge.tags = Arrays.asList("mail/smtp-edge");

		Server smtp1 = new Server();
		smtp1.ip = "10.0.0.2";
		smtp1.name = "test-" + System.nanoTime();
		smtp1.tags = Arrays.asList("mail/smtp");

		Server smtp2 = new Server();
		smtp2.ip = "10.0.0.3";
		smtp2.name = "test-" + System.nanoTime();
		smtp2.tags = Arrays.asList("mail/smtp");
		PopulateHelper.createServers(smtp1, smtp2, edge);

		ItemValue<Domain> domain = PopulateHelper.createTestDomain("domain.tld", smtp1, smtp2, edge);

		Map<String, DomainInfo> domainInfoByUid = new HashMap<>();
		domainInfoByUid.put(domain.uid, DomainInfo.build(domain, Collections.emptyMap()));

		// SMTP 1
		Optional<ServerMaps> optionalServerMaps = ServerMaps.init(getServerService().allComplete(),
				getServerService().getComplete(smtp1.ip), domainInfoByUid, Collections.emptyList());

		assertTrue(optionalServerMaps.isPresent());

		ServerMaps serverMaps = optionalServerMaps.get();

		assertNotNull(serverMaps.getServer());
		assertEquals(smtp1.ip, serverMaps.getServer().uid);

		assertNotNull(serverMaps.getDomainInfoByUid());
		assertEquals(1, serverMaps.getDomainInfoByUid().size());
		assertTrue(serverMaps.getDomainInfoByUid().containsKey("domain.tld"));
		assertTrue(serverMaps.getDomainInfoByUid().get("domain.tld").domain.uid.equals("domain.tld"));

		assertEquals(0, serverMaps.getEdgeNextHopByDomainUid().size());

		// SMTP 2
		optionalServerMaps = ServerMaps.init(getServerService().allComplete(), getServerService().getComplete(smtp2.ip),
				domainInfoByUid, Collections.emptyList());

		assertTrue(optionalServerMaps.isPresent());

		serverMaps = optionalServerMaps.get();

		assertNotNull(serverMaps.getServer());
		assertEquals(smtp2.ip, serverMaps.getServer().uid);

		assertNotNull(serverMaps.getDomainInfoByUid());
		assertEquals(1, serverMaps.getDomainInfoByUid().size());
		assertTrue(serverMaps.getDomainInfoByUid().containsKey("domain.tld"));
		assertTrue(serverMaps.getDomainInfoByUid().get("domain.tld").domain.uid.equals("domain.tld"));

		assertEquals(0, serverMaps.getEdgeNextHopByDomainUid().size());

		// Edge
		optionalServerMaps = ServerMaps.init(getServerService().allComplete(), getServerService().getComplete(edge.ip),
				domainInfoByUid, Collections.emptyList());

		assertTrue(optionalServerMaps.isPresent());

		serverMaps = optionalServerMaps.get();

		assertNotNull(serverMaps.getServer());
		assertEquals(edge.ip, serverMaps.getServer().uid);

		assertNotNull(serverMaps.getDomainInfoByUid());
		assertEquals(1, serverMaps.getDomainInfoByUid().size());
		assertTrue(serverMaps.getDomainInfoByUid().containsKey("domain.tld"));
		assertTrue(serverMaps.getDomainInfoByUid().get("domain.tld").domain.uid.equals("domain.tld"));

		assertEquals(1, serverMaps.getEdgeNextHopByDomainUid().size());
		assertEquals("domain.tld", serverMaps.getEdgeNextHopByDomainUid().keySet().iterator().next());
		assertNotNull(serverMaps.getEdgeNextHopByDomainUid().get("domain.tld"));
		assertTrue("10.0.0.2".equals(serverMaps.getEdgeNextHopByDomainUid().get("domain.tld").uid)
				|| "10.0.0.3".equals(serverMaps.getEdgeNextHopByDomainUid().get("domain.tld").uid));
	}

	@Test
	public void init_smtp_keepMapRowsFromAssignedDomainsOnly() throws Exception {
		Server edge = new Server();
		edge.ip = "10.0.0.1";
		edge.name = "test-" + System.nanoTime();
		edge.tags = Arrays.asList("mail/smtp-edge");

		Server smtp = new Server();
		smtp.ip = "10.0.0.2";
		smtp.name = "test-" + System.nanoTime();
		smtp.tags = Arrays.asList("mail/smtp");
		PopulateHelper.createServers(smtp, edge);

		ItemValue<Domain> domain1 = PopulateHelper.createTestDomain("domain1.tld", smtp);
		ItemValue<Domain> domain2 = PopulateHelper.createTestDomain("domain2.tld", edge);

		Map<String, DomainInfo> domainInfoByUid = new HashMap<>();
		domainInfoByUid.put(domain1.uid, DomainInfo.build(domain1, Collections.emptyMap()));
		domainInfoByUid.put(domain2.uid, DomainInfo.build(domain2, Collections.emptyMap()));

		List<MapRow> mapRows = new ArrayList<>();
		mapRows.add(new MapRow(domain1, 1, null, null, null, null, null, null));
		mapRows.add(new MapRow(domain2, 2, null, null, null, null, null, null));
		mapRows.add(new MapRow(PopulateHelper.createTestDomain("domain3.tld"), 2, null, null, null, null, null, null));

		// SMTP
		Optional<ServerMaps> optionalServerMaps = ServerMaps.init(getServerService().allComplete(),
				getServerService().getComplete(smtp.ip), domainInfoByUid, mapRows);

		assertTrue(optionalServerMaps.isPresent());

		ServerMaps serverMaps = optionalServerMaps.get();

		assertNotNull(serverMaps.getServer());
		assertEquals(smtp.ip, serverMaps.getServer().uid);

		assertEquals(1, serverMaps.getMapRows().size());
		assertEquals(1, serverMaps.getMapRows().iterator().next().itemId);
	}

	@Test
	public void init_edge_keepMapRowsFromAssignedDomainsOnly() throws Exception {
		Server edge = new Server();
		edge.ip = "10.0.0.1";
		edge.name = "test-" + System.nanoTime();
		edge.tags = Arrays.asList("mail/smtp-edge");

		Server smtp = new Server();
		smtp.ip = "10.0.0.2";
		smtp.name = "test-" + System.nanoTime();
		smtp.tags = Arrays.asList("mail/smtp");
		PopulateHelper.createServers(smtp, edge);

		ItemValue<Domain> domain1 = PopulateHelper.createTestDomain("domain1.tld", smtp);
		ItemValue<Domain> domain2 = PopulateHelper.createTestDomain("domain2.tld", smtp, edge);

		Map<String, DomainInfo> domainInfoByUid = new HashMap<>();
		domainInfoByUid.put(domain1.uid, DomainInfo.build(domain1, Collections.emptyMap()));
		domainInfoByUid.put(domain2.uid, DomainInfo.build(domain2, Collections.emptyMap()));

		List<MapRow> mapRows = new ArrayList<>();
		mapRows.add(new MapRow(domain1, 1, null, null, null, null, null, null));
		mapRows.add(new MapRow(domain2, 2, null, null, null, null, null, null));
		mapRows.add(new MapRow(PopulateHelper.createTestDomain("domain3.tld"), 2, null, null, null, null, null, null));

		// Edge
		Optional<ServerMaps> optionalServerMaps = ServerMaps.init(getServerService().allComplete(),
				getServerService().getComplete(edge.ip), domainInfoByUid, mapRows);

		assertTrue(optionalServerMaps.isPresent());

		ServerMaps serverMaps = optionalServerMaps.get();

		assertNotNull(serverMaps.getServer());
		assertEquals(edge.ip, serverMaps.getServer().uid);

		assertEquals(1, serverMaps.getMapRows().size());
		assertEquals(2, serverMaps.getMapRows().iterator().next().itemId);
	}

	@Test
	public void writeFlatMaps_smtpTaggedAndAssigned() throws Exception {
		Server server = new Server();
		server.ip = new BmConfIni().get("bluemind/smtp-role");
		server.name = "test-" + System.nanoTime();
		server.tags = Arrays.asList("mail/smtp");
		PopulateHelper.createServers(server);

		PopulateHelper.createTestDomain("domain.tld", server);

		Optional<ServerMaps> optionalServerMaps = ServerMaps.init(Collections.emptyList(),
				getServerService().getComplete(server.ip), Collections.emptyMap(), Collections.emptyList());

		assertTrue(optionalServerMaps.isPresent());

		ServerMaps serverMaps = optionalServerMaps.get();

		INodeClient nodeClient = NodeActivator.get(server.address());
		for (String mapFileName : mapsFileNames) {
			NCUtils.waitFor(nodeClient, nodeClient.executeCommand("rm -f " + mapFileName + "-flat"));

			ExitList status = NCUtils.waitFor(nodeClient,
					nodeClient.executeCommand("test -e " + mapFileName + "-flat"));
			assertEquals(1, status.getExitCode());
		}

		serverMaps.writeFlatMaps();

		for (String mapFileName : mapsFileNames) {
			ExitList status = NCUtils.waitFor(nodeClient,
					nodeClient.executeCommand("test -e " + mapFileName + "-flat"));
			assertEquals(0, status.getExitCode());
		}
	}

	@Test
	public void writeFlatMaps_activateMaps_smtpTaggedAndAssigned() throws Exception {
		Server server = new Server();
		server.ip = new BmConfIni().get("bluemind/smtp-role");
		server.name = "test-" + System.nanoTime();
		server.tags = Arrays.asList("mail/smtp");
		PopulateHelper.createServers(server);

		PopulateHelper.createTestDomain("domain.tld", server);

		Optional<ServerMaps> optionalServerMaps = ServerMaps.init(Collections.emptyList(),
				getServerService().getComplete(server.ip), Collections.emptyMap(), Collections.emptyList());

		assertTrue(optionalServerMaps.isPresent());

		ServerMaps serverMaps = optionalServerMaps.get();

		INodeClient nodeClient = NodeActivator.get(server.address());
		for (String mapFileName : mapsFileNames) {
			NCUtils.waitFor(nodeClient, nodeClient.executeCommand(
					"rm -f " + mapFileName + "-flat.db " + mapFileName + "-flat " + mapFileName + ".db"));

			ExitList status = NCUtils.waitFor(nodeClient, nodeClient.executeCommand("test -e " + mapFileName));
			assertEquals(1, status.getExitCode());
		}

		serverMaps.writeFlatMaps();
		serverMaps.enableMaps();

		for (String mapFileName : mapsFileNames) {
			ExitList status = NCUtils.waitFor(nodeClient, nodeClient.executeCommand("test -e " + mapFileName + ".db"));
			assertEquals(0, status.getExitCode());
		}
	}

	@Test
	public void writeFlatMaps_edgeTaggedNotAssigned() throws Exception {
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

		Optional<ServerMaps> optionalServerMaps = ServerMaps.init(Collections.emptyList(),
				getServerService().getComplete(edge.ip), Collections.emptyMap(), Collections.emptyList());
		assertTrue(optionalServerMaps.isPresent());

		ServerMaps serverMaps = optionalServerMaps.get();

		INodeClient nodeClient = NodeActivator.get(edge.address());
		for (String mapFileName : mapsFileNames) {
			NCUtils.waitFor(nodeClient, nodeClient.executeCommand("rm -f " + mapFileName + "-flat"));

			ExitList status = NCUtils.waitFor(nodeClient,
					nodeClient.executeCommand("test -e " + mapFileName + "-flat"));
			assertEquals(1, status.getExitCode());
		}

		serverMaps.writeFlatMaps();

		for (String mapFileName : mapsFileNames) {
			ExitList status = NCUtils.waitFor(nodeClient,
					nodeClient.executeCommand("test -e " + mapFileName + "-flat"));
			assertEquals(0, status.getExitCode());
		}
	}

	@Test
	public void init_notAssigned_noSmtpTag() {
		Server server = new Server();
		server.ip = "10.0.0.1";
		server.name = "test-" + System.nanoTime();
		server.tags = Arrays.asList("mail/imap");

		PopulateHelper.createServers(server);

		assertFalse(ServerMaps.init(getServerService().getComplete(server.ip)).isPresent());

		server = new Server();
		server.ip = "10.0.0.2";
		server.name = "test-" + System.nanoTime();
		server.tags = Collections.emptyList();

		PopulateHelper.createServers(server);

		assertFalse(ServerMaps.init(getServerService().getComplete(server.ip)).isPresent());
	}

	@Test
	public void init_notAssigned_smtpTag() {
		Server smtp = new Server();
		smtp.ip = "10.0.0.1";
		smtp.name = "test-" + System.nanoTime();
		smtp.tags = Arrays.asList("mail/smtp");

		PopulateHelper.createServers(smtp);

		Optional<ServerMaps> optionalServerMaps = ServerMaps.init(getServerService().getComplete(smtp.ip));
		assertTrue(optionalServerMaps.isPresent());

		ServerMaps serverMaps = optionalServerMaps.get();

		assertTrue(serverMaps.getDomainInfoByUid().isEmpty());
		assertTrue(serverMaps.getEdgeNextHopByDomainUid().isEmpty());
		assertTrue(serverMaps.getMapRows().isEmpty());
	}

	@Test
	public void init_notAssigned_edgeTag() {
		Server edge = new Server();
		edge.ip = "10.0.0.1";
		edge.name = "test-" + System.nanoTime();
		edge.tags = Arrays.asList("mail/smtp-edge");

		PopulateHelper.createServers(edge);

		Optional<ServerMaps> optionalServerMaps = ServerMaps.init(getServerService().getComplete(edge.ip));
		assertTrue(optionalServerMaps.isPresent());

		ServerMaps serverMaps = optionalServerMaps.get();

		assertTrue(serverMaps.getDomainInfoByUid().isEmpty());
		assertTrue(serverMaps.getEdgeNextHopByDomainUid().isEmpty());
		assertTrue(serverMaps.getMapRows().isEmpty());
	}
}
