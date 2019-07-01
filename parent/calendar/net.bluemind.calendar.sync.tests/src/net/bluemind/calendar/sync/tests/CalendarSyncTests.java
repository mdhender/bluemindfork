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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.SettableFuture;

import net.bluemind.calendar.api.CalendarDescriptor;
import net.bluemind.calendar.api.ICalendarsMgmt;
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
import net.bluemind.core.task.api.ITask;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus;
import net.bluemind.core.task.api.TaskStatus.State;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class CalendarSyncTests {

	private String domain = "bm.lan";
	private String userUid = "admin";
	protected SecurityContext context;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		
		ElasticsearchTestHelper.getInstance().beforeTest();
		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		esServer.tags = Lists.newArrayList("bm/es");

		PopulateHelper.initGlobalVirt(esServer);

		final SettableFuture<Void> future = SettableFuture.<Void>create();
		Handler<AsyncResult<Void>> done = new Handler<AsyncResult<Void>>() {

			@Override
			public void handle(AsyncResult<Void> event) {
				future.set(null);
			}
		};
		VertxPlatform.spawnVerticles(done);
		future.get();

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
		sync("webcal://www.matthias-kuech.de/kq/Playstation_4_Releases_United_States.ics");
	}

	@Test
	public void testInvalidUrl() throws Exception {
		String uid = UUID.randomUUID().toString();
		CalendarDescriptor cd = CalendarDescriptor.create("invalid url", userUid, domain);

		HashMap<String, String> settings = new HashMap<String, String>();
		settings.put("type", "externalIcs");
		settings.put("icsUrl", "url");
		cd.settings = settings;

		getCalendarMgmtService().create(uid, cd);

		getContainerManagementService(uid)
				.setAccessControlList(Arrays.asList(AccessControlEntry.create(userUid, Verb.Write)));

		// sync
		IContainerSync service = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IContainerSync.class, uid);
		TaskRef taskRef = service.sync();
		assertNotNull(taskRef);
		ContainerSyncResult res = waitTaskRef(taskRef);
		assertNotNull(res);
		Long ns = res.status.nextSync;
		assertTrue(ns > 0);

		// resync
		taskRef = service.sync();
		assertNotNull(taskRef);
		res = waitTaskRef(taskRef);
		assertTrue(res.status.nextSync > ns);
	}

	private void internalSync(String icsUrl) throws ServerFault {
		String uid = UUID.randomUUID().toString();
		CalendarDescriptor cd = CalendarDescriptor.create(icsUrl, userUid, domain);

		HashMap<String, String> settings = new HashMap<String, String>();
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

	private void sync(String icsUrl) throws ServerFault {
		String uid = UUID.randomUUID().toString();
		CalendarDescriptor cd = CalendarDescriptor.create(icsUrl, userUid, domain);

		HashMap<String, String> settings = new HashMap<String, String>();
		settings.put("type", "externalIcs");
		settings.put("icsUrl", icsUrl);
		cd.settings = settings;

		getCalendarMgmtService().create(uid, cd);

		getContainerManagementService(uid)
				.setAccessControlList(Arrays.asList(AccessControlEntry.create(userUid, Verb.Write)));

		// sync
		IContainerSync service = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IContainerSync.class, uid);
		TaskRef taskRef = service.sync();
		assertNotNull(taskRef);
		ContainerSyncResult res = waitTaskRef(taskRef);
		assertNotNull(res);
		assertTrue(res.added > 0);
		assertEquals(0, res.updated);
		assertEquals(0, res.removed);
		Long ns = res.status.nextSync;
		assertTrue(ns > 0);

		// resync
		taskRef = service.sync();
		assertNotNull(taskRef);
		res = waitTaskRef(taskRef);
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

	private ContainerSyncResult waitTaskRef(TaskRef taskRef) throws ServerFault {
		ITask task = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(ITask.class, taskRef.id);
		while (!task.status().state.ended) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
		}

		TaskStatus status = task.status();
		if (status.state == State.InError) {
			throw new ServerFault("import error");
		}

		return JsonUtils.read(status.result, ContainerSyncResult.class);
	}

}
