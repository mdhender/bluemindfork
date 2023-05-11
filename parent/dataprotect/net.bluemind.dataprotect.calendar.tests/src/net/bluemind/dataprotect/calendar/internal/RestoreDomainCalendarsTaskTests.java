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

package net.bluemind.dataprotect.calendar.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import net.bluemind.calendar.api.CalendarDescriptor;
import net.bluemind.calendar.api.ICalendar;
import net.bluemind.calendar.api.ICalendarsMgmt;
import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.api.ITask;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus;
import net.bluemind.core.task.api.TaskStatus.State;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.dataprotect.api.DataProtectGeneration;
import net.bluemind.dataprotect.api.IDataProtect;
import net.bluemind.dataprotect.api.PartGeneration;
import net.bluemind.dataprotect.api.Restorable;
import net.bluemind.dataprotect.api.RestorableKind;
import net.bluemind.dataprotect.calendar.impl.RestoreDomainCalendarsTask;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.server.api.Server;
import net.bluemind.system.api.DomainTemplate;
import net.bluemind.system.api.IDomainTemplate;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class RestoreDomainCalendarsTaskTests {
	private static final boolean RUN_AS_ROOT = System.getProperty("user.name").equals("root");

	static final String login = "chang";
	static final String domain = "junit.lan";
	static final String latd = login + "@" + domain;

	private DataProtectGeneration latestGen;
	private BmTestContext testContext;

	@BeforeClass
	public static void beforeClass() {
		System.setProperty("ahcnode.fail.https.ok", "true");
		System.setProperty("node.local.ipaddr", PopulateHelper.FAKE_CYRUS_IP);
		System.setProperty("imap.local.ipaddr", PopulateHelper.FAKE_CYRUS_IP);
		System.setProperty("imap.port", "1143");
	}

	@Before
	public void before() throws Exception {
		prepareLocalFilesystem();

		JdbcTestHelper.getInstance().beforeTest();
		ElasticsearchTestHelper.getInstance().beforeTest();

		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);

		Server core = new Server();
		core.ip = new BmConfIni().get("node-host");
		core.tags = getTagsExcept("bm/es", "mail/imap", "bm/pgsql", "bm/pgsql-data");

		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		esServer.tags = Lists.newArrayList("bm/es");

		Server imapServer = new Server();
		imapServer.ip = PopulateHelper.FAKE_CYRUS_IP;
		imapServer.tags = Lists.newArrayList("mail/imap", "mail/archive");

		Server dbServer = new Server();
		dbServer.ip = new BmConfIni().get("host");
		dbServer.tags = Lists.newArrayList("bm/pgsql", "bm/pgsql-data");

		PopulateHelper.initGlobalVirt(false, core, esServer, dbServer, imapServer);
		PopulateHelper.addDomainAdmin("admin0", "global.virt");

		PopulateHelper.addDomain(domain, Routing.none);

		testContext = new BmTestContext(SecurityContext.SYSTEM);

		PopulateHelper.addUser(login, domain, Routing.internal);
		testContext.provider().instance(ISystemConfiguration.class)
				.updateMutableValues(ImmutableMap.of("db_version", "3.1.0"));
	}

	private List<String> getTagsExcept(String... except) {
		// tag & assign host for everything
		List<String> tags = new LinkedList<String>();

		IDomainTemplate dt = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDomainTemplate.class);
		DomainTemplate template = dt.getTemplate();
		for (DomainTemplate.Kind kind : template.kinds) {
			for (DomainTemplate.Tag tag : kind.tags) {
				if (tag.autoAssign) {
					tags.add(tag.value);
				}
			}
		}

		tags.removeAll(Arrays.asList(except));

		return tags;
	}

	private void prepareLocalFilesystem() throws IOException, InterruptedException {
		if (RUN_AS_ROOT) {
			Process p = Runtime.getRuntime().exec(new String[] { "bash", "-c", "rm -rf /var/spool/bm-hollowed/*" });
			p.waitFor(10, TimeUnit.SECONDS);
			p = Runtime.getRuntime().exec(new String[] { "bash", "-c", "rm -rf /var/backups/bluemind/*" });
			p.waitFor(10, TimeUnit.SECONDS);
		} else {
			Process p = Runtime.getRuntime()
					.exec(new String[] { "sudo", "bash", "-c", "rm -rf /var/spool/bm-hollowed/*" });
			p.waitFor(10, TimeUnit.SECONDS);
			p = Runtime.getRuntime().exec(new String[] { "sudo", "bash", "-c", "rm -rf /var/backups/bluemind/*" });
			p.waitFor(10, TimeUnit.SECONDS);
		}

		Process p = Runtime.getRuntime()
				.exec("sudo chown -R " + System.getProperty("user.name") + " /var/spool/bm-hollowed");
		p.waitFor(10, TimeUnit.SECONDS);
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	private void doBackup() throws Exception {

		TaskRef task = testContext.provider().instance(IDataProtect.class).saveAll();
		track(task);
		List<DataProtectGeneration> generations = testContext.provider().instance(IDataProtect.class)
				.getAvailableGenerations();
		assertTrue(generations.size() > 0);
		latestGen = null;
		for (DataProtectGeneration dpg : generations) {
			if (latestGen == null || dpg.id > latestGen.id) {
				latestGen = dpg;
			}
			System.out.println("On generation " + dpg.id + ", time: " + dpg.protectionTime);
			for (PartGeneration pg : dpg.parts) {
				System.out.println("   * " + pg.server + " " + pg.tag + " saved @ " + pg.end);
			}
		}
	}

	private void track(TaskRef task) throws ServerFault, InterruptedException {
		ITask tracker = testContext.provider().instance(ITask.class, "" + task.id);
		TaskStatus status = tracker.status();
		while (!status.state.ended) {
			status = tracker.status();
			Thread.sleep(500);
		}
		for (String s : tracker.getCurrentLogs(0)) {
			System.out.println(s);
		}

		assertEquals(State.Success, status.state);
	}

	@Test
	public void testRestoreDeletedCalendar() throws Exception {
		ICalendarsMgmt calMgmt = testContext.provider().instance(ICalendarsMgmt.class, domain);
		calMgmt.create("testCal", CalendarDescriptor.create("testCal", domain, domain));
		DirEntry entry = testContext.provider().instance(IDirectory.class, domain).findByEntryUid("testCal");

		doBackup();
		calMgmt.delete(entry.entryUid);

		Restorable restorable = new Restorable();
		restorable.domainUid = domain;
		restorable.entryUid = entry.entryUid;
		restorable.kind = RestorableKind.CALENDAR;

		new RestoreDomainCalendarsTask(latestGen, restorable).run(new TestMonitor());

		CalendarDescriptor calendarDescriptor = calMgmt.get(entry.entryUid);
		assertNotNull(calendarDescriptor);
		assertEquals("testCal", calendarDescriptor.name);
	}

	@Test
	public void testRestoreCalendarWithVEventChanges() throws Exception {
		ICalendarsMgmt abMgmt = testContext.provider().instance(ICalendarsMgmt.class, domain);
		abMgmt.create("testCal", CalendarDescriptor.create("testCal", domain, domain));
		DirEntry entry = testContext.provider().instance(IDirectory.class, domain).findByEntryUid("testCal");

		ICalendar addbookapi = testContext.provider().instance(ICalendar.class, entry.entryUid);
		addbookapi.create("event1", VEventSeries.create(defaultEvent()), false);
		addbookapi.create("event2", VEventSeries.create(defaultEvent()), false);

		doBackup();

		// update event 1
		VEventSeries vcard1 = addbookapi.get("event1");
		vcard1.main.conference = "conference update";
		addbookapi.update("event1", vcard1, false);
		// remove event 2
		addbookapi.delete("event2");
		// create event 3
		addbookapi.create("event3", VEventSeries.create(defaultEvent()), false);

		doBackup();

		Restorable restorable = new Restorable();
		restorable.domainUid = domain;
		restorable.entryUid = entry.entryUid;
		restorable.kind = RestorableKind.CALENDAR;

		new RestoreDomainCalendarsTask(latestGen, restorable).run(new TestMonitor());

		CalendarDescriptor addressBookDescriptor = abMgmt.get(entry.entryUid);
		assertNotNull(addressBookDescriptor);
		assertEquals("testCal", addressBookDescriptor.name);

		List<String> allUids = addbookapi.all();
		assertEquals(2, allUids.size());
		List<ItemValue<VEventSeries>> veventsRestored = addbookapi.multipleGet(allUids);
		veventsRestored.forEach(e -> {
			if ("event1".equals(e.uid)) {
				assertEquals("conference update", e.value.main.conference);
			} else if ("event3".equals(e.uid)) {
				assertTrue(Strings.isNullOrEmpty(e.value.main.conference));
			} else {
				fail();
			}
		});
	}

	private VEvent defaultEvent() {
		return RestoreUserCalendarsTaskTests.defaultEvent();
	}

}
