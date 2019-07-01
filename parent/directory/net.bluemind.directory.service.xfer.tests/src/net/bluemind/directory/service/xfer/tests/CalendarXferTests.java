package net.bluemind.directory.service.xfer.tests;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;

import com.google.common.collect.Lists;

import net.bluemind.calendar.api.ICalendar;
import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.api.VEventChanges;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.core.api.date.BmDateTimeHelper;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus;
import net.bluemind.core.task.api.TaskStatus.State;
import net.bluemind.core.task.service.TaskUtils;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.mailbox.service.SplittedShardsMapping;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.vertx.testhelper.Deploy;

@RunWith(Parameterized.class)
public class CalendarXferTests {

	private String domainUid = "bm.lan";
	private String userUid = "test" + System.currentTimeMillis();
	private String shardIp;
	private SecurityContext context;

	@BeforeClass
	public static void oneShotBefore() {
		System.setProperty("es.mailspool.count", "1");
	}

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());
		Deploy.verticles(false, "net.bluemind.locator.LocatorVerticle").get(5, TimeUnit.SECONDS);

		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		System.out.println("ES is " + esServer.ip);
		assertNotNull(esServer.ip);
		esServer.tags = Lists.newArrayList("bm/es");

		Server imapServer = new Server();
		imapServer.ip = new BmConfIni().get("imap-role");
		imapServer.tags = Lists.newArrayList("mail/imap");

		Server imapServer2 = new Server();
		imapServer2.ip = new BmConfIni().get("imap2-role");
		imapServer2.tags = Lists.newArrayList("mail/imap");

		Server pg2 = new Server();
		shardIp = new BmConfIni().get("pg2");
		pg2.ip = shardIp;
		pg2.tags = Lists.newArrayList("mail/shard");

		PopulateHelper.initGlobalVirt(pg2, esServer, imapServer, imapServer2);
		ElasticsearchTestHelper.getInstance().beforeTest();

		PopulateHelper.addDomain(domainUid, Routing.none);
		PopulateHelper.addUser(userUid, domainUid, Routing.none);

		final CountDownLatch launched = new CountDownLatch(1);
		VertxPlatform.spawnVerticles(new Handler<AsyncResult<Void>>() {
			@Override
			public void handle(AsyncResult<Void> event) {
				launched.countDown();
			}
		});
		launched.await();

		System.err.println("PG2 " + pg2.ip + " IMAP1: " + imapServer.ip + " IMAP2: " + imapServer2.ip);
		JdbcTestHelper.getInstance().initNewServer(pg2.ip);
		SplittedShardsMapping.map(pg2.ip, imapServer2.ip);

		context = new SecurityContext("user", userUid, Arrays.<String>asList(), Arrays.<String>asList(), domainUid);

		Sessions.get().put(context.getSessionId(), context);

	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Parameterized.Parameters
	public static Object[][] data() {
		return new Object[10][0];
	}

	@Test
	public void testXferCalendar() {

		String container = ICalendarUids.defaultUserCalendar(userUid);

		ICalendar service = ServerSideServiceProvider.getProvider(context).instance(ICalendar.class, container);

		VEventSeries new1 = defaultVEvent();
		VEventSeries new2 = defaultVEvent();
		String new1Id = "test_1" + System.nanoTime();
		String new2Id = "test_2" + System.nanoTime();

		VEventSeries update = defaultVEvent();
		String updateUID = "test_" + System.nanoTime();
		service.create(updateUID, update, false); // v1
		update.main.summary = "update" + System.currentTimeMillis();

		VEventSeries delete = defaultVEvent();
		String deleteUID = "test_" + System.nanoTime();
		service.create(deleteUID, delete, false); // v2

		VEventChanges.ItemAdd add1 = VEventChanges.ItemAdd.create(new1Id, new1, false); // v2
		VEventChanges.ItemAdd add2 = VEventChanges.ItemAdd.create(new2Id, new2, false); // v4

		VEventChanges.ItemModify modify = VEventChanges.ItemModify.create(updateUID, update, false); // v5

		VEventChanges.ItemDelete itemDelete = VEventChanges.ItemDelete.create(deleteUID, false); // v6

		VEventChanges changes = VEventChanges.create(Arrays.asList(add1, add2), Arrays.asList(modify),
				Arrays.asList(itemDelete));

		service.updates(changes);

		// initial container state
		int nbItems = service.all().size();
		assertEquals(3, nbItems);
		long version = service.getVersion();
		assertEquals(6, version);

		System.err.println("Starting transfer....");
		TaskRef tr = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IDirectory.class, domainUid)
				.xfer(userUid, shardIp);
		waitTaskEnd(tr);

		// current service should return nothing
		assertTrue(service.all().isEmpty());

		// new ICalendar instance
		service = ServerSideServiceProvider.getProvider(context).instance(ICalendar.class, container);

		assertEquals(nbItems, service.all().size());
		assertEquals(version, service.getVersion());

		service.create("new-one", defaultVEvent(), false);

		ContainerChangeset<String> changeset = service.changeset(version);
		assertEquals(1, changeset.created.size());
		assertEquals("new-one", changeset.created.get(0));
		assertTrue(changeset.updated.isEmpty());
		assertTrue(changeset.deleted.isEmpty());

	}

	private void waitTaskEnd(TaskRef taskRef) throws ServerFault {
		TaskStatus status = TaskUtils.wait(ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM), taskRef);
		System.err.println("EndStatus: " + status);
		if (status.state == State.InError) {
			throw new ServerFault("xfer error");
		}
	}

	protected VEventSeries defaultVEvent() {
		VEventSeries series = new VEventSeries();
		VEvent event = new VEvent();
		ZoneId tz = ZoneId.of("Asia/Ho_Chi_Minh");
		event.dtstart = BmDateTimeHelper.time(ZonedDateTime.of(2022, 2, 13, 1, 0, 0, 0, tz));
		event.summary = "event " + System.currentTimeMillis();
		event.location = "Toulouse";
		event.description = "Lorem ipsum";
		event.transparency = VEvent.Transparency.Opaque;
		event.classification = VEvent.Classification.Private;
		event.status = VEvent.Status.Confirmed;
		event.priority = 3;

		event.organizer = new VEvent.Organizer(userUid + "@bm.lan");

		List<VEvent.Attendee> attendees = new ArrayList<>(1);
		VEvent.Attendee me = VEvent.Attendee.create(VEvent.CUType.Individual, "", VEvent.Role.Chair,
				VEvent.ParticipationStatus.Accepted, true, "", "", "", "osef", null, null, null,
				"external@attendee.lan");
		attendees.add(me);

		event.attendees = attendees;

		series.main = event;
		return series;
	}
}
