/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2019
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
package net.bluemind.backend.cyrus.integrity.check.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

import net.bluemind.backend.cyrus.CyrusService;
import net.bluemind.backend.cyrus.integrity.check.CyrusFilesystemCheck;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.locator.LocatorVerticle;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.network.topology.Topology;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.vertx.testhelper.Deploy;

public class CheckCyrusContainerFilesystemTests {

	private String cyrusIp;
	private String domainUid;
	private String userUid;
	private BmContext ctx;

	@Before
	public void before() throws Exception {

		JdbcTestHelper.getInstance().beforeTest();
		Deploy.verticles(false, LocatorVerticle::new).get(5, TimeUnit.SECONDS);

		BmConfIni ini = new BmConfIni();

		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		System.out.println("ES is " + esServer.ip);
		assertNotNull(esServer.ip);
		esServer.tags = Lists.newArrayList("bm/es");

		this.cyrusIp = ini.get("imap-role");
		Server imapServer = new Server();
		imapServer.ip = cyrusIp;
		imapServer.tags = Lists.newArrayList("mail/imap");

		ItemValue<Server> cyrusServer = ItemValue.create("localhost", imapServer);
		CyrusService cyrusService = new CyrusService(cyrusServer);
		cyrusService.reset();

		PopulateHelper.initGlobalVirt(esServer, imapServer);
		ElasticsearchTestHelper.getInstance().beforeTest();
		PopulateHelper.addDomainAdmin("admin0", "global.virt", Routing.none);

		String unique = "" + System.currentTimeMillis();
		domainUid = "test" + unique + ".lab";
		userUid = "user" + unique;

		// ensure the partition is created correctly before restarting cyrus
		PopulateHelper.addDomain(domainUid, Routing.none);

		CountDownLatch cdl = new CountDownLatch(1);
		VertxPlatform.spawnVerticles(ar -> {
			cdl.countDown();
		});
		boolean beforeTimeout = cdl.await(30, TimeUnit.SECONDS);
		assertTrue(beforeTimeout);

		MQ.init().get(30, TimeUnit.SECONDS);
		Topology.get();

		System.err.println("Start populate user " + userUid);
		PopulateHelper.addUser(userUid, domainUid, Routing.internal);

		this.ctx = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).getContext();
		assertNotNull(ctx);
		Thread.sleep(1000);
		System.err.println("********** BEFORE ENDS ************");
	}

	@After
	public void after() throws Exception {
		System.err.println("********** AFTER STARTS ************");
		Thread.sleep(1000);
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testCheckFS() throws InterruptedException, ExecutionException, TimeoutException {
		List<ItemValue<Domain>> domains = ctx.provider().instance(IDomains.class).all().stream()
				.filter(d -> !d.uid.equals("global.virt")).collect(Collectors.toList());

		CyrusFilesystemCheck cs = new CyrusFilesystemCheck(ctx, domains);
		ItemValue<Server> cyrusBackend = Topology.get().any("mail/imap");
		CompletableFuture<List<String>> prom = cs.check(cyrusBackend);
		assertNotNull(prom);
		List<String> strangeDirs = prom.get(10, TimeUnit.SECONDS);
		System.err.println("stangeDirs: " + strangeDirs);
		assertTrue(strangeDirs.isEmpty());
	}

	@Test
	public void testBreakAndCheckFS() throws InterruptedException, ExecutionException, TimeoutException {
		List<ItemValue<Domain>> domains = ctx.provider().instance(IDomains.class).all().stream()
				.filter(d -> !d.uid.equals("global.virt")).collect(Collectors.toList());
		ItemValue<Server> cyrusBackend = Topology.get().any("mail/imap");
		String extraSuffix = "mail/domain/t/" + domainUid + "/u/user/unknown";
		String extra = "/var/spool/cyrus/meta/" + extraSuffix;
		NodeActivator.get(cyrusBackend.value.address()).executeCommand("mkdir -p " + extra);

		CyrusFilesystemCheck cs = new CyrusFilesystemCheck(ctx, domains);
		CompletableFuture<List<String>> prom = cs.check(cyrusBackend);
		assertNotNull(prom);
		List<String> strangeDirs = prom.get(10, TimeUnit.SECONDS);
		System.err.println("stangeDirs: " + strangeDirs);
		NodeActivator.get(cyrusBackend.value.address()).executeCommand("rmdir " + extra);
		assertFalse(strangeDirs.isEmpty());
		assertEquals(extraSuffix, strangeDirs.get(0));
	}

}
