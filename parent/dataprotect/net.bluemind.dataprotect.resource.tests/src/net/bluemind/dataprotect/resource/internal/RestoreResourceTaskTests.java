/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.dataprotect.resource.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import net.bluemind.calendar.api.CalendarDescriptor;
import net.bluemind.calendar.api.CalendarSettingsData;
import net.bluemind.calendar.api.CalendarSettingsData.Day;
import net.bluemind.calendar.api.ICalendar;
import net.bluemind.calendar.api.ICalendarSettings;
import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.calendar.api.ICalendarsMgmt;
import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.core.api.Email;
import net.bluemind.core.api.date.BmDateTime.Precision;
import net.bluemind.core.api.date.BmDateTimeWrapper;
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
import net.bluemind.dataprotect.resource.impl.RestoreResourceTask;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.resource.api.IResources;
import net.bluemind.resource.api.ResourceDescriptor;
import net.bluemind.resource.api.ResourceReservationMode;
import net.bluemind.server.api.Server;
import net.bluemind.system.api.DomainTemplate;
import net.bluemind.system.api.IDomainTemplate;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class RestoreResourceTaskTests {
	private static final boolean RUN_AS_ROOT = System.getProperty("user.name").equals("root");

	private DataProtectGeneration latestGen;
	private BmTestContext testContext;

	static final String login = "chang";
	static final String domain = "junit.lan";
	static final String latd = login + "@" + domain;

	@AfterClass
	public static void afterClass() {
		VertxPlatform.getVertx().close();
	}

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

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	private List<String> getTagsExcept(String... except) {
		// tag & assign host for everything
		List<String> tags = new LinkedList<>();

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

		if (!RUN_AS_ROOT) {
			Process p = Runtime.getRuntime()
					.exec("sudo chown -R " + System.getProperty("user.name") + " /var/spool/bm-hollowed");
			p.waitFor(10, TimeUnit.SECONDS);
		}
	}

	private ResourceDescriptor createResource(final String label) {
		final ResourceDescriptor resourceDescriptor = new ResourceDescriptor();
		resourceDescriptor.typeIdentifier = "default";
		resourceDescriptor.label = label;
		resourceDescriptor.description = "What a mighty description!";
		resourceDescriptor.reservationMode = ResourceReservationMode.OWNER_MANAGED;
		resourceDescriptor.emails = Collections.singletonList(Email.create(label.toLowerCase() + "@test.lan", true));
		return resourceDescriptor;
	}

	private IResources getResourceService() {
		return testContext.provider().instance(IResources.class, domain);
	}

	@Test
	public void testRestoreDeletedResource() throws Exception {
		IResources resourceService = getResourceService();
		ResourceDescriptor rec = createResource("res1.1");
		resourceService.create("testRes", rec);
		DirEntry entry = testContext.provider().instance(IDirectory.class, domain).findByEntryUid("testRes");

		ICalendarSettings backCalendarSettings = testContext.provider().instance(ICalendarSettings.class,
				ICalendarUids.resourceCalendar(entry.entryUid));
		CalendarSettingsData oldCalendarSettings = backCalendarSettings.get();
		assertNotNull(oldCalendarSettings);
		assertTrue(oldCalendarSettings.present());

		doBackup();
		resourceService.delete(entry.entryUid);

		Restorable restorable = new Restorable();
		restorable.domainUid = domain;
		restorable.entryUid = entry.entryUid;
		restorable.kind = RestorableKind.RESOURCE;

		new RestoreResourceTask(latestGen, restorable).run(new TestMonitor());

		ItemValue<ResourceDescriptor> restoredResource = resourceService.getComplete(entry.entryUid);
		assertNotNull(restoredResource);
		assertEquals("res1.1", restoredResource.value.label);
		assertTrue(oldCalendarSettings.present());

	}

	static VEvent defaultEvent() {
		VEvent event = new VEvent();
		ZonedDateTime temp = ZonedDateTime.of(2022, 2, 13, 1, 0, 0, 0, ZoneId.of("Asia/Ho_Chi_Minh"));
		event.dtstart = BmDateTimeWrapper.create(temp, Precision.DateTime);
		event.summary = "event " + System.currentTimeMillis();
		event.location = "Toulouse";
		event.description = "Lorem ipsum";
		event.transparency = VEvent.Transparency.Opaque;
		event.classification = VEvent.Classification.Private;
		event.status = VEvent.Status.Confirmed;
		event.priority = 3;

		return event;
	}

	@Test
	public void testRestoreCalendarWithVEventChanges() throws Exception {
		IResources resourceService = getResourceService();
		ResourceDescriptor rec = createResource("res1.1");
		resourceService.create("testRes", rec);
		DirEntry entry = testContext.provider().instance(IDirectory.class, domain).findByEntryUid("testRes");
		String resourceCal = ICalendarUids.resourceCalendar(entry.entryUid);

		ICalendarSettings settingsService = testContext.provider().instance(ICalendarSettings.class, resourceCal);
		CalendarSettingsData calSettings = new CalendarSettingsData();
		calSettings.dayStart = 9;
		calSettings.dayEnd = 18;
		calSettings.minDuration = 20;
		calSettings.workingDays = Arrays.asList(Day.MO);
		calSettings.timezoneId = "UTC";
		settingsService.set(calSettings);

		ICalendar calApi = testContext.provider().instance(ICalendar.class, resourceCal);
		calApi.create("event1", VEventSeries.create(defaultEvent()), false);
		calApi.create("event2", VEventSeries.create(defaultEvent()), false);

		doBackup();

		// update event 1
		VEventSeries vcard1 = calApi.get("event1");
		vcard1.main.conference = "conference update";
		calApi.update("event1", vcard1, false);
		// remove event 2
		calApi.delete("event2");
		// create event 3
		calApi.create("event3", VEventSeries.create(defaultEvent()), false);

		doBackup();

		Restorable restorable = new Restorable();
		restorable.domainUid = domain;
		restorable.entryUid = entry.entryUid;
		restorable.kind = RestorableKind.RESOURCE;

		new RestoreResourceTask(latestGen, restorable).run(new TestMonitor());

		CalendarDescriptor calendarDescriptor = testContext.provider().instance(ICalendarsMgmt.class).get(resourceCal);
		assertNotNull(calendarDescriptor);

		List<String> allUids = calApi.all();
		assertEquals(2, allUids.size());
		List<ItemValue<VEventSeries>> veventsRestored = calApi.multipleGet(allUids);
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

}
