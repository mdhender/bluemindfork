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
package net.bluemind.core.container.subscriptions.hook.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import net.bluemind.core.api.report.DiagnosticReport;
import net.bluemind.core.container.api.ContainerSubscriptionModel;
import net.bluemind.core.container.api.IOwnerSubscriptionUids;
import net.bluemind.core.container.api.IOwnerSubscriptions;
import net.bluemind.core.container.api.internal.IInternalOwnerSubscriptions;
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.core.task.service.LoggingTaskMonitor;
import net.bluemind.core.task.service.NullTaskMonitor;
import net.bluemind.core.task.service.TaskUtils;
import net.bluemind.directory.api.IDirEntryMaintenance;
import net.bluemind.directory.api.MaintenanceOperation;
import net.bluemind.directory.service.IInternalDirEntryMaintenance;
import net.bluemind.lib.vertx.Constructor;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.locator.LocatorVerticle;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.user.api.IUser;
import net.bluemind.vertx.testhelper.Deploy;

public class UserShardedSubscriptionsTests {

	private String domainUid;
	private static final Logger logger = LoggerFactory.getLogger(UserShardedSubscriptionsTests.class);

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		JdbcTestHelper.getInstance().getDbSchemaService().initialize();

		Deploy.verticles(false, Constructor.of(LocatorVerticle::new, LocatorVerticle.class)).get(20, TimeUnit.SECONDS);

		BmConfIni ini = new BmConfIni();

		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		esServer.tags = Lists.newArrayList("bm/es");

		Server imapServer = new Server();
		imapServer.ip = ini.get("imap-role");
		imapServer.tags = Lists.newArrayList("mail/imap");

		PopulateHelper.initGlobalVirt(esServer, imapServer);
		System.err.println("Deploying with es: " + esServer.ip + ", imap: " + imapServer.ip);

		PopulateHelper.addDomainAdmin("admin0", "global.virt", Routing.none);
		ElasticsearchTestHelper.getInstance().beforeTest();
		domainUid = "pipo" + System.currentTimeMillis() + ".io";
		PopulateHelper.addDomain(domainUid, Routing.none);

		final CompletableFuture<Void> spawn = new CompletableFuture<Void>();
		VertxPlatform.spawnVerticles(ar -> {
			if (ar.succeeded()) {
				spawn.complete(ar.result());
			} else {
				spawn.completeExceptionally(ar.cause());
			}
		});
		spawn.thenAccept(v -> {
			System.err.println("All deployed, before is complete.");
		}).get(40, TimeUnit.SECONDS);
	}

	@After
	public void after() throws Exception {
		System.err.println("After test");
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testCreateUserPopulatesSubscriptionsContainerThenRecreateIsPossible() throws Exception {
		String userUid = PopulateHelper.addUser("test" + System.currentTimeMillis(), domainUid, Routing.internal);
		System.err.println("After populateUser ends.");
		Thread.sleep(1000);
		ServerSideServiceProvider prov = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);

		IOwnerSubscriptions hierarchyService = prov.instance(IOwnerSubscriptions.class, domainUid, userUid);
		assertNotNull(hierarchyService);
		List<ItemValue<ContainerSubscriptionModel>> nodes = hierarchyService.list();
		assertFalse(nodes.isEmpty());
		Set<Long> hierIds = new HashSet<>();
		for (ItemValue<ContainerSubscriptionModel> iv : nodes) {
			System.err.println(" * " + iv);
			hierIds.add(iv.internalId);
		}

		IUser userApi = prov.instance(IUser.class, domainUid);
		System.err.println("*** pre-delete");
		TaskRef tr = userApi.delete(userUid);
		TaskUtils.wait(ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM), tr);

		Thread.sleep(1000);
		System.err.println("*** post-delete");
		userUid = PopulateHelper.addUser(userUid, domainUid, Routing.internal);
		System.err.println("**** pre re-recreate sleep");
		Thread.sleep(2000);
		System.err.println("**** post re-recreate");
		hierarchyService = prov.instance(IOwnerSubscriptions.class, domainUid, userUid);
		nodes = hierarchyService.list();
		assertFalse(nodes.isEmpty());
		for (ItemValue<ContainerSubscriptionModel> iv : nodes) {
			System.err.println(" * v2: " + iv);
			assertFalse(hierIds.contains(iv.internalId));
		}

		IDirEntryMaintenance maintenance = prov.instance(IDirEntryMaintenance.class, domainUid, userUid);
		Set<MaintenanceOperation> ops = maintenance.getAvailableOperations();
		Set<String> opIds = ops.stream().map(mo -> mo.identifier).collect(Collectors.toSet());
		System.err.println("ops: " + opIds);
		assertTrue(opIds.contains(IOwnerSubscriptionUids.REPAIR_OP_ID));
	}

	@Test
	public void testUserSubscriptionsRepair() throws Exception {
		String userUid = PopulateHelper.addUser("test" + System.currentTimeMillis(), domainUid, Routing.internal);
		System.err.println("After populateUser ends.");
		Thread.sleep(1000);
		ServerSideServiceProvider prov = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);

		IOwnerSubscriptions hierarchyService = prov.instance(IOwnerSubscriptions.class, domainUid, userUid);
		assertNotNull(hierarchyService);
		List<ItemValue<ContainerSubscriptionModel>> nodes = hierarchyService.list();
		assertFalse(nodes.isEmpty());
		ContainerChangeset<String> changeset = hierarchyService.changeset(0L);
		Stream.concat(changeset.created.stream(), changeset.updated.stream()).forEach(System.out::println);
		String toRm = changeset.created.get(0);
		IInternalOwnerSubscriptions intOwnAPi = prov.instance(IInternalOwnerSubscriptions.class, domainUid, userUid);
		intOwnAPi.delete(toRm);
		long toSync = intOwnAPi.getVersion();

		IDirEntryMaintenance maintenance = prov.instance(IDirEntryMaintenance.class, domainUid, userUid);
		Set<MaintenanceOperation> ops = maintenance.getAvailableOperations();
		Set<String> opIds = ops.stream().map(mo -> mo.identifier).collect(Collectors.toSet());
		System.err.println("ops: " + opIds);
		assertTrue(opIds.contains(IOwnerSubscriptionUids.REPAIR_OP_ID));

		IInternalDirEntryMaintenance maintInternal = prov.instance(IInternalDirEntryMaintenance.class, domainUid,
				userUid);
		DiagnosticReport report = new DiagnosticReport();
		IServerTaskMonitor mon = new LoggingTaskMonitor(logger, new NullTaskMonitor(), 0);
		maintInternal.repair(Sets.newHashSet(IOwnerSubscriptionUids.REPAIR_OP_ID), report, mon);

		ContainerChangeset<String> newChangeSet = hierarchyService.changeset(toSync);
		newChangeSet.created.stream().forEach(iv -> System.out.println("Cv " + iv));
		newChangeSet.updated.stream().forEach(iv -> System.out.println("Uv " + iv));
		newChangeSet.deleted.stream().forEach(iv -> System.out.println("Dv " + iv));
		assertEquals(1, newChangeSet.created.size());
		assertEquals(changeset.created.size() - 1, newChangeSet.updated.size());

	}

}
