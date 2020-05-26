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
package net.bluemind.calendar.service.tests;

import static org.junit.Assert.assertEquals;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import net.bluemind.calendar.api.IFreebusyUids;
import net.bluemind.calendar.persistence.FreebusyStore;
import net.bluemind.calendar.service.internal.repair.FreebusyRepairSupport;
import net.bluemind.core.api.report.DiagnosticReport;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.task.service.NullTaskMonitor;
import net.bluemind.core.tests.BmTestContext;

public class FreebusyRepairTests {

	private SecurityContext securityContext;
	private ContainerStore cs;
	private Container container;
	private FreebusyStore store;
	private Container cal;

	@BeforeClass
	public static void oneShotBefore() {
		System.setProperty("es.mailspool.count", "1");
	}

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		JdbcTestHelper.getInstance().getDbSchemaService().initialize();

		securityContext = new SecurityContext("testUser", "test", Arrays.<String>asList(), Arrays.<String>asList(),
				"bm.lan");

		cs = new ContainerStore(null, JdbcTestHelper.getInstance().getDataSource(), securityContext);

		container = Container.create(UUID.randomUUID().toString(), IFreebusyUids.TYPE, "fb container",
				securityContext.getSubject(), "bm.lan", true);
		container = cs.create(container);

		store = new FreebusyStore(JdbcTestHelper.getInstance().getDataSource(), container);

		cal = Container.create(UUID.randomUUID().toString(), "calendar", "this is calendar",
				securityContext.getSubject(), "bm.lan", true);
		cal = cs.create(cal);

	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void checkEmptyFreebusyOk() throws SQLException {

		BmTestContext context = new BmTestContext(securityContext);
		FreebusyRepairSupport rs = new FreebusyRepairSupport(context, "reportId");
		DiagnosticReport report = DiagnosticReport.create();
		rs.check(container.uid, report, new NullTaskMonitor());
		assertEquals(1, report.entries.size());
		assertEquals("reportId", report.entries.get(0).id);

		assertEquals(DiagnosticReport.State.OK, report.entries.get(0).state);
	}

	@Test
	public void checkOk() throws SQLException {

		store.set(Arrays.asList(cal.uid));

		BmTestContext context = new BmTestContext(securityContext);
		FreebusyRepairSupport rs = new FreebusyRepairSupport(context, "reportId");
		DiagnosticReport report = DiagnosticReport.create();
		rs.check(container.uid, report, new NullTaskMonitor());
		assertEquals(1, report.entries.size());
		assertEquals("reportId", report.entries.get(0).id);

		assertEquals(DiagnosticReport.State.OK, report.entries.get(0).state);
	}

	@Test
	public void unknownCalendarCheckNeedRepair() throws SQLException {

		store.set(Arrays.asList("unknownCalendarUid"));

		BmTestContext context = new BmTestContext(securityContext);
		FreebusyRepairSupport rs = new FreebusyRepairSupport(context, "reportId");
		DiagnosticReport report = DiagnosticReport.create();
		rs.check(container.uid, report, new NullTaskMonitor());
		assertEquals(1, report.entries.size());
		assertEquals("reportId", report.entries.get(0).id);

		assertEquals(DiagnosticReport.State.KO, report.entries.get(0).state);
	}

	@Test
	public void emptyFreebusyNothingToRepairOk() throws SQLException {
		BmTestContext context = new BmTestContext(securityContext);
		FreebusyRepairSupport rs = new FreebusyRepairSupport(context, "reportId");
		DiagnosticReport report = DiagnosticReport.create();
		rs.repair(container.uid, report, new NullTaskMonitor());
		assertEquals(1, report.entries.size());
		assertEquals("reportId", report.entries.get(0).id);

		assertEquals(DiagnosticReport.State.OK, report.entries.get(0).state);
	}

	@Test
	public void nothingToRepairOk() throws SQLException {

		store.set(Arrays.asList(cal.uid));

		BmTestContext context = new BmTestContext(securityContext);
		FreebusyRepairSupport rs = new FreebusyRepairSupport(context, "reportId");
		DiagnosticReport report = DiagnosticReport.create();
		rs.repair(container.uid, report, new NullTaskMonitor());
		assertEquals(1, report.entries.size());
		assertEquals("reportId", report.entries.get(0).id);

		assertEquals(DiagnosticReport.State.OK, report.entries.get(0).state);

		List<String> calendars = store.get();
		assertEquals(1, calendars.size());
		assertEquals(cal.uid, calendars.get(0));
	}

	@Test
	public void repairOk() throws SQLException {

		store.set(Arrays.asList(cal.uid, "fake"));

		BmTestContext context = new BmTestContext(securityContext);
		FreebusyRepairSupport rs = new FreebusyRepairSupport(context, "reportId");
		DiagnosticReport report = DiagnosticReport.create();
		rs.repair(container.uid, report, new NullTaskMonitor());
		assertEquals(1, report.entries.size());
		assertEquals("reportId", report.entries.get(0).id);

		assertEquals(DiagnosticReport.State.OK, report.entries.get(0).state);

		List<String> calendars = store.get();
		assertEquals(1, calendars.size());
		assertEquals(cal.uid, calendars.get(0));
	}
}
