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
package net.bluemind.calendar.sync.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.Lists;

import net.bluemind.calendar.api.CalendarDescriptor;
import net.bluemind.calendar.api.ICalendarsMgmt;
import net.bluemind.common.task.Tasks;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.IContainerManagement;
import net.bluemind.core.container.api.IContainerSync;
import net.bluemind.core.container.api.internal.IInternalContainerSync;
import net.bluemind.core.container.model.ContainerSyncResult;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus;
import net.bluemind.core.task.api.TaskStatus.State;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.server.api.Server;
import net.bluemind.server.api.TagDescriptor;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class CalendarSyncTests {

	private String domain = "bm.lan";
	private String userUid = "admin";
	protected SecurityContext context;
	protected Supplier<ServerSideServiceProvider> provider = Suppliers
			.memoize(() -> ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM));

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		ElasticsearchTestHelper.getInstance().beforeTest();
		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		esServer.tags = Lists.newArrayList(TagDescriptor.bm_es.getTag());

		PopulateHelper.initGlobalVirt(esServer);

		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);

		context = new SecurityContext("sid", userUid, Arrays.<String>asList(), Arrays.<String>asList(), domain);

		Sessions.get().put(context.getSessionId(), context);

		PopulateHelper.createTestDomain(domain);
		PopulateHelper.addDomainAdmin("admin", domain);
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testSync() throws Exception {
		sync("https://www.mozilla.org/media/caldata/FrenchHolidays.ics");
	}

	@Test
	public void testInternalSync() throws Exception {
		internalSync("https://www.mozilla.org/media/caldata/FrenchHolidays.ics");
	}

	@Test
	public void testSyncWebcal() throws Exception {
		sync("webcal://www.mozilla.org/media/caldata/FrenchHolidays.ics");
	}

	@Test
	public void testInvalidUrl() throws Exception {
		String uid = UUID.randomUUID().toString();
		CalendarDescriptor cd = CalendarDescriptor.create("invalid url", userUid, domain);

		HashMap<String, String> settings = new HashMap<>();
		settings.put("type", "externalIcs");
		settings.put("icsUrl", "url");
		cd.settings = settings;

		getCalendarMgmtService().create(uid, cd);

		getContainerManagementService(uid)
				.setAccessControlList(Arrays.asList(AccessControlEntry.create(userUid, Verb.Write)));

		// sync
		ContainerSyncResult res = this.syncCal(uid);
		assertNotNull(res);
		Long ns = res.status.nextSync;
		assertTrue(ns > 0);

		// resync
		res = this.syncCal(uid);
		assertTrue(res.status.nextSync > ns);
	}

	@Test
	public void testNot200() throws Exception {
		String uid = UUID.randomUUID().toString();
		CalendarDescriptor cd = CalendarDescriptor.create("not 200", userUid, domain);
		CompletableFuture<Void> comp = new CompletableFuture<>();
		VertxPlatform.getVertx().createHttpServer().requestHandler(req -> {
			req.endHandler(v -> {
				req.response().setStatusMessage("fail").setStatusCode(400).end();
			});
		}).listen(9898, ar -> {
			if (ar.failed()) {
				comp.completeExceptionally(ar.cause());
			} else {
				System.err.println("listening !");
				comp.complete(null);
			}
		});
		comp.join();

		HashMap<String, String> settings = new HashMap<>();
		settings.put("type", "externalIcs");
		settings.put("icsUrl", "http://localhost:9898/fail");
		cd.settings = settings;

		getCalendarMgmtService().create(uid, cd);

		getContainerManagementService(uid)
				.setAccessControlList(Arrays.asList(AccessControlEntry.create(userUid, Verb.Write)));

		// sync
		ContainerSyncResult res = this.syncCal(uid);
		assertNotNull(res);
		Long ns = res.status.nextSync;
		assertTrue(ns > 0);

		// ensure we don't too many open files here
		for (int i = 0; i < 1024; i++) {
			res = this.syncCal(uid);
			if (i % 100 == 0) {
				System.err.println(i + " / 1024");
			}
			assertTrue(res.status.nextSync > ns);
		}
	}

	private void internalSync(String icsUrl) throws ServerFault {
		String uid = UUID.randomUUID().toString();
		CalendarDescriptor cd = CalendarDescriptor.create(icsUrl, userUid, domain);

		HashMap<String, String> settings = new HashMap<>();
		settings.put("type", "externalIcs");
		settings.put("icsUrl", icsUrl);
		cd.settings = settings;

		getCalendarMgmtService().create(uid, cd);

		getContainerManagementService(uid)
				.setAccessControlList(Arrays.asList(AccessControlEntry.create(userUid, Verb.Write)));

		// sync
		IInternalContainerSync service = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IInternalContainerSync.class, uid);
		ContainerSyncResult res = service.sync();

		assertNotNull(res);
		assertTrue(res.added > 0);
		assertEquals(0, res.updated);
		assertEquals(0, res.removed);
		Long ns = res.status.nextSync;
		assertTrue(ns > 0);

		// resync
		res = service.sync();
		assertNotNull(res);
		assertEquals(0, res.added);
		assertEquals(0, res.updated);
		assertEquals(0, res.removed);
		assertTrue(res.status.nextSync > ns);
	}

	private ContainerSyncResult syncCal(final String calUid) {
		IContainerSync service = provider.get().instance(IContainerSync.class, calUid);
		TaskRef taskRef = service.sync();
		assertNotNull(taskRef);
		return waitTaskRef(taskRef);
	}

	private ContainerSyncResult waitTaskRef(TaskRef taskRef) {
		Logger logger = LoggerFactory.getLogger(CalendarSyncTests.class);
		CompletableFuture<TaskStatus> futstatus = Tasks.followStream(provider.get(), logger, null, taskRef);

		TaskStatus status;
		try {
			status = futstatus.get(30, TimeUnit.SECONDS);
			if (status.state == State.InError) {
				throw new ServerFault("import error");
			}
			return JsonUtils.read(status.result, ContainerSyncResult.class);
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
			throw new ServerFault("follow stream failed: " + ie.getMessage());
		} catch (ExecutionException | TimeoutException e) {
			throw new ServerFault("follow stream failed: " + e.getMessage(), e);
		}
	}

	private void sync(String icsUrl) throws ServerFault {
		String uid = UUID.randomUUID().toString();
		CalendarDescriptor cd = CalendarDescriptor.create(icsUrl, userUid, domain);

		HashMap<String, String> settings = new HashMap<>();
		settings.put("type", "externalIcs");
		settings.put("icsUrl", icsUrl);
		cd.settings = settings;

		getCalendarMgmtService().create(uid, cd);

		getContainerManagementService(uid)
				.setAccessControlList(Arrays.asList(AccessControlEntry.create(userUid, Verb.Write)));

		// sync
		ContainerSyncResult res = this.syncCal(uid);
		assertNotNull(res);
		assertTrue(res.added > 0);
		assertEquals(0, res.updated);
		assertEquals(0, res.removed);
		Long ns = res.status.nextSync;
		assertTrue(ns > 0);

		// resync
		res = this.syncCal(uid);
		assertNotNull(res);
		assertEquals(0, res.added);
		assertEquals(0, res.updated);
		assertEquals(0, res.removed);
		assertTrue(res.status.nextSync > ns);
	}

	private ICalendarsMgmt getCalendarMgmtService() throws ServerFault {
		return ServerSideServiceProvider.getProvider(context).instance(ICalendarsMgmt.class);
	}

	private IContainerManagement getContainerManagementService(String container) throws ServerFault {
		return ServerSideServiceProvider.getProvider(context).instance(IContainerManagement.class, container);
	}
}
