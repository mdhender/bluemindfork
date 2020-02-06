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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.calendar.service.internal.repair;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.SettableFuture;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import net.bluemind.backend.cyrus.CyrusAdmins;
import net.bluemind.backend.cyrus.CyrusService;
import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.api.VEventOccurrence;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.calendar.service.internal.VEventContainerStoreService;
import net.bluemind.core.api.report.DiagnosticReport;
import net.bluemind.core.container.persistence.ContainerSettingsStore;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.DataSourceRouter;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.service.NullTaskMonitor;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.tag.api.ITags;
import net.bluemind.tag.api.Tag;
import net.bluemind.tag.api.TagRef;
import net.bluemind.tests.defaultdata.BmDateTimeHelper;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class CalendarRepairSupportTests {

	private String domainUid;

	private BmTestContext testContext;

	private String calUid;

	private String user2;

	private String user1;

	@Before
	public void setup() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		JdbcTestHelper.getInstance().getDbSchemaService().initialize();
		ElasticsearchTestHelper.getInstance().beforeTest();

		final SettableFuture<Void> future = SettableFuture.<Void>create();
		Handler<AsyncResult<Void>> done = new Handler<AsyncResult<Void>>() {

			@Override
			public void handle(AsyncResult<Void> event) {
				future.set(null);
			}
		};
		VertxPlatform.spawnVerticles(done);
		future.get();

		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		esServer.tags = Lists.newArrayList("bm/es");

		Server imapServer = new Server();
		imapServer.ip = new BmConfIni().get("imap-role");
		imapServer.tags = Lists.newArrayList("mail/imap");

		PopulateHelper.initGlobalVirt(esServer, imapServer);

		domainUid = "test.lan";
		PopulateHelper.createTestDomain(domainUid, esServer, imapServer);

		this.createCyrusPartition(imapServer, this.domainUid);

		user1 = PopulateHelper.addUser("test1", domainUid);
		user2 = PopulateHelper.addUser("test2", domainUid);

		testContext = new BmTestContext(SecurityContext.SYSTEM);
		testContext.provider().instance(ITags.class, "tags_" + user1).create("user1Tag", Tag.create("t1", "c1"));
		testContext.provider().instance(ITags.class, "tags_" + user2).create("user2Tag", Tag.create("t2", "c2"));

		calUid = ICalendarUids.defaultUserCalendar(user1);

		Map<String, String> settings = new HashMap<String, String>();
		settings.put("calendar.workingDays", "MO,TU,WE,TH,FR");
		settings.put("calendar.minDuration", "60");
		settings.put("calendar.dayStart", "08:00");
		settings.put("calendar.dayEnd", "18:00");
		settings.put("calendar.timezone", "Europe/Paris");

		ContainerSettingsStore css = containerSettings(calUid);
		css.setSettings(settings);
	}

	private void createCyrusPartition(final Server imapServer, final String domainUid) {
		final CyrusService cyrusService = new CyrusService(imapServer.ip);
		cyrusService.createPartition(domainUid);
		cyrusService.refreshPartitions(Arrays.asList(domainUid));
		new CyrusAdmins(
				ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IServer.class, "default"),
				imapServer.ip).write();
		cyrusService.reload();
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testCheck_Ok() throws Exception {
		calendarStore(calUid).create("e1", "e1", defaultVEvent());
		CalendarRepairSupport rs = new CalendarRepairSupport(testContext, "reportId");
		DiagnosticReport report = DiagnosticReport.create();
		rs.check(calUid, report, new NullTaskMonitor());
		assertEquals(1, report.entries.size());
		assertEquals("reportId", report.entries.get(0).id);
		assertEquals(DiagnosticReport.State.OK, report.entries.get(0).state);
	}

	@Test
	public void testCheck_NeedRepair() throws Exception {
		containerSettings(calUid).delete();

		VEventSeries series = defaultVEvent();
		series.main.categories = ImmutableList.of(TagRef.create("tags_" + user1, "baduser1Tag", "badc1", "badt1"));
		calendarStore(calUid).create("e1", "e1", series);
		CalendarRepairSupport rs = new CalendarRepairSupport(testContext, "reportId");
		DiagnosticReport report = DiagnosticReport.create();
		rs.check(calUid, report, new NullTaskMonitor());
		assertEquals(1, report.entries.size());
		assertEquals("reportId", report.entries.get(0).id);
		assertEquals(DiagnosticReport.State.KO, report.entries.get(0).state);
	}

	@Test
	public void testRepair_Ok() throws Exception {
		calendarStore(calUid).create("e1", "e1", defaultVEvent());
		CalendarRepairSupport rs = new CalendarRepairSupport(testContext, "reportId");
		DiagnosticReport report = DiagnosticReport.create();
		rs.repair(calUid, report, new NullTaskMonitor());
		assertEquals(2, report.entries.size());
		assertEquals("reportId", report.entries.get(0).id);
		assertEquals(DiagnosticReport.State.OK, report.entries.get(0).state);
	}

	@Test
	public void testRepair_RepairTags() throws Exception {
		containerSettings(calUid).delete();

		VEventSeries series = defaultVEvent();
		series.main.categories = ImmutableList.of(TagRef.create("tags_" + user1, "baduser1Tag", "badc1", "badt1"));
		calendarStore(calUid).create("e1", "e1", series);
		CalendarRepairSupport rs = new CalendarRepairSupport(testContext, "reportId");
		DiagnosticReport report = DiagnosticReport.create();
		rs.repair(calUid, report, new NullTaskMonitor());
		assertEquals(2, report.entries.size());

		assertEquals("reportId", report.entries.get(0).id);
		assertEquals(DiagnosticReport.State.OK, report.entries.get(0).state);

		assertEquals("reportId", report.entries.get(1).id);
		assertEquals(DiagnosticReport.State.OK, report.entries.get(1).state);
	}

	@Test
	public void testCheckSettings_Ok() {
		CalendarRepairSupport rs = new CalendarRepairSupport(testContext, "reportId");
		DiagnosticReport report = DiagnosticReport.create();
		rs.check(calUid, report, new NullTaskMonitor());
		assertEquals(1, report.entries.size());
		assertEquals("reportId", report.entries.get(0).id);
		assertEquals(DiagnosticReport.State.OK, report.entries.get(0).state);
	}

	@Test
	public void testCheckSettings_NeedRepair() throws Exception {

		ContainerSettingsStore css = containerSettings(calUid);
		css.delete();

		CalendarRepairSupport rs = new CalendarRepairSupport(testContext, "reportId");
		DiagnosticReport report = DiagnosticReport.create();
		rs.check(calUid, report, new NullTaskMonitor());
		assertEquals(1, report.entries.size());
		assertEquals("reportId", report.entries.get(0).id);
		assertEquals(DiagnosticReport.State.KO, report.entries.get(0).state);
	}

	@Test
	public void testCheckSettings_Repair() throws Exception {
		ContainerSettingsStore css = containerSettings(calUid);
		Map<String, String> settings = new HashMap<String, String>();
		// css.setSettings(settings);

		CalendarRepairSupport rs = new CalendarRepairSupport(testContext, "reportId");
		DiagnosticReport report = DiagnosticReport.create();
		rs.repair(calUid, report, new NullTaskMonitor());
		assertEquals(2, report.entries.size());
		assertEquals("reportId", report.entries.get(0).id);
		assertEquals(DiagnosticReport.State.OK, report.entries.get(0).state);

		settings = css.getSettings();
		System.err.println(settings);
		assertFalse(settings.isEmpty());

	}

	private VEventContainerStoreService calendarStore(String calUid) throws Exception {
		DataSource ds = DataSourceRouter.get(testContext, calUid);
		ContainerStore cs = new ContainerStore(testContext, ds, SecurityContext.SYSTEM);
		return new VEventContainerStoreService(testContext, ds, SecurityContext.SYSTEM, cs.get(calUid));
	}

	private ContainerSettingsStore containerSettings(String calUid) throws Exception {
		DataSource ds = DataSourceRouter.get(testContext, calUid);
		ContainerStore cs = new ContainerStore(testContext, ds, SecurityContext.SYSTEM);
		return new ContainerSettingsStore(ds, cs.get(calUid));
	}

	/**
	 * @return
	 */
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
		event.categories = new ArrayList<TagRef>(2);
		event.categories.add(TagRef.create("tags_" + user1, "user1Tag", "c1", "t1"));
		event.categories.add(TagRef.create("tags_" + user2, "user2Tag", "c2", "t2"));
		series.main = event;
		series.icsUid = "check";
		series.occurrences = ImmutableList.of(VEventOccurrence.fromEvent(event,
				BmDateTimeHelper.time(ZonedDateTime.of(2022, 2, 13, 1, 0, 0, 0, tz))));
		return series;
	}

}
