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
package net.bluemind.addressbook.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.elasticsearch.client.transport.TransportClient;
import org.junit.Before;
import org.junit.Test;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.SettableFuture;

import net.bluemind.addressbook.api.AddressBookDescriptor;
import net.bluemind.addressbook.api.IAddressBooksMgmt;
import net.bluemind.addressbook.persistance.VCardStore;
import net.bluemind.addressbook.service.internal.DomainBookRepairSupport;
import net.bluemind.addressbook.service.internal.DomainBookRepairSupport.RepairAB;
import net.bluemind.core.api.report.DiagnosticReport;
import net.bluemind.core.api.report.DiagnosticReport.Entry;
import net.bluemind.core.api.report.DiagnosticReport.State;
import net.bluemind.core.caches.registry.CacheRegistry;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.persistance.ContainerStore;
import net.bluemind.core.container.persistance.ContainerSyncStore;
import net.bluemind.core.container.persistance.ItemStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.task.service.NullTaskMonitor;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.directory.api.MaintenanceOperation;
import net.bluemind.directory.service.internal.DirEntryRepairSupports;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.server.api.Server;
import net.bluemind.tag.api.Tag;
import net.bluemind.tag.api.TagRef;
import net.bluemind.tag.persistance.TagRefStore;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class DomainBookRepairSupportTests {

	protected VCardStore vCardStore;
	protected TagRefStore tagRefStore;
	protected ItemStore itemStore;

	protected SecurityContext defaultSecurityContext;

	protected TransportClient esearchClient;

	protected Container tagContainer;

	protected Tag tag1;

	protected Tag tag2;

	protected TagRef tagRef1;

	protected TagRef tagRef2;
	protected BmContext context;

	private BmTestContext testContext;

	private String domainUid = "test.com";

	private DirEntry entry;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		JdbcTestHelper.getInstance().getDbSchemaService().initialize();
		ElasticsearchTestHelper.getInstance().beforeTest();
		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		esServer.tags = Lists.newArrayList("bm/es");
		PopulateHelper.initGlobalVirt(esServer);
		PopulateHelper.addDomain(domainUid);

		final SettableFuture<Void> future = SettableFuture.<Void>create();
		Handler<AsyncResult<Void>> done = new Handler<AsyncResult<Void>>() {

			@Override
			public void handle(AsyncResult<Void> event) {
				future.set(null);
			}
		};
		VertxPlatform.spawnVerticles(done);
		future.get();
		context = new BmTestContext(defaultSecurityContext);

		testContext = new BmTestContext(SecurityContext.SYSTEM);
		testContext.provider().instance(IAddressBooksMgmt.class, domainUid).create("testAB",
				AddressBookDescriptor.create("testAB", domainUid, domainUid), true);
		entry = testContext.provider().instance(IDirectory.class, domainUid).findByEntryUid("testAB");
	}

	@Test
	public void testExtensionRegistred() {
		DirEntryRepairSupports drs = new DirEntryRepairSupports(testContext);
		Set<MaintenanceOperation> ao = drs.availableOperations(Kind.ADDRESSBOOK);
		assertTrue(ao.stream().anyMatch(o -> o.identifier.equals(DomainBookRepairSupport.REPAIR_AB_CONTAINER)));
	}

	@Test
	public void testCheckNormal() {
		RepairAB rab = new RepairAB(testContext);
		DiagnosticReport report = DiagnosticReport.create();
		rab.check(domainUid, entry, report, new NullTaskMonitor());
		assertEquals(State.OK, report.globalState());

		report = DiagnosticReport.create();
		rab.repair(domainUid, entry, report, new NullTaskMonitor());
		assertEquals(State.OK, report.globalState());
	}

	@Test
	public void testAbsentContainer() throws SQLException, InterruptedException {
		ContainerStore cs = new ContainerStore(testContext, JdbcTestHelper.getInstance().getMailboxDataDataSource(),
				testContext.getSecurityContext());
		ContainerSyncStore syncStore = new ContainerSyncStore(JdbcTestHelper.getInstance().getMailboxDataDataSource(),
				cs.get(entry.entryUid));
		syncStore.suspendSync();
		cs.delete(entry.entryUid);
		testContext.provider().instance(CacheRegistry.class).invalidateAll();

		RepairAB rab = new RepairAB(testContext);
		DiagnosticReport report = DiagnosticReport.create();
		rab.check(domainUid, entry, report, new NullTaskMonitor());
		assertEquals(State.KO, report.globalState());

		report = DiagnosticReport.create();
		rab.repair(domainUid, entry, report, new NullTaskMonitor());
		List<Entry> res = report.entries.stream().filter(e -> e.id.equals(DomainBookRepairSupport.REPAIR_AB_CONTAINER))
				.collect(Collectors.toList());
		Entry last = res.get(res.size() - 1);
		assertEquals(State.OK, last.state);
		testContext.provider().instance(CacheRegistry.class).invalidateAll();
		assertNotNull(cs.get(entry.entryUid));
	}

}
