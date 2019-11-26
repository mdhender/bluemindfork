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
package net.bluemind.calendar.service.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoField;
import java.util.Arrays;
import java.util.Collections;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;

import com.google.common.util.concurrent.SettableFuture;

import net.bluemind.calendar.api.CalendarSettingsData;
import net.bluemind.calendar.api.CalendarSettingsData.Day;
import net.bluemind.calendar.api.ICalendarSettings;
import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.persistence.AclStore;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class CalendarSettingsTests {

	private SecurityContext userSecurityContext;
	private SecurityContext admSecurityContext;
	private String calendarUid;

	@Before
	public void beforeBefore() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		final SettableFuture<Void> future = SettableFuture.<Void>create();
		Handler<AsyncResult<Void>> done = new Handler<AsyncResult<Void>>() {

			@Override
			public void handle(AsyncResult<Void> event) {
				future.set(null);
			}
		};
		VertxPlatform.spawnVerticles(done);
		future.get();

		PopulateHelper.initGlobalVirt();

		String domainUid = "bm.lan";
		PopulateHelper.createTestDomain(domainUid);

		ContainerStore containerStore = new ContainerStore(JdbcTestHelper.getInstance().getDataSource(),
				SecurityContext.SYSTEM);

		// calendar container
		Container calendar = Container.create("cal" + System.currentTimeMillis(), ICalendarUids.TYPE, "calendar test",
				"test", "bm.lan", true);
		containerStore.create(calendar);
		calendar = containerStore.get(calendar.uid);

		calendarUid = calendar.uid;
		userSecurityContext = new SecurityContext("s1", "simpleUser", Collections.<String>emptyList(),
				Collections.<String>emptyList(), domainUid);
		Sessions.get().put(userSecurityContext.getSessionId(), userSecurityContext);

		admSecurityContext = new SecurityContext("s2", "adminUser", Collections.<String>emptyList(),
				Collections.<String>emptyList(), domainUid);
		Sessions.get().put(admSecurityContext.getSessionId(), admSecurityContext);

		// Acls
		AclStore aclStore = new AclStore(JdbcTestHelper.getInstance().getDataSource());
		aclStore.store(calendar, Arrays.asList(
				//
				AccessControlEntry.create(admSecurityContext.getSubject(), Verb.All), //
				AccessControlEntry.create(userSecurityContext.getSubject(), Verb.Read)));

	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testGet() throws ServerFault {
		ICalendarSettings calendarSettings = service(admSecurityContext);
		assertNotNull(calendarSettings.get());

		calendarSettings = service(userSecurityContext);
		assertNotNull(calendarSettings.get());
	}

	@Test
	public void testSet() throws ServerFault {
		ICalendarSettings calendarSettings = service(admSecurityContext);
		calendarSettings.set(defaultSettings());

		calendarSettings = service(userSecurityContext);
		try {
			calendarSettings.set(defaultSettings());
			fail("should fail");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		calendarSettings = service(userSecurityContext);
		assertEquals(defaultSettings().dayStart, calendarSettings.get().dayStart);
		assertEquals(defaultSettings().dayEnd, calendarSettings.get().dayEnd);
		assertEquals(defaultSettings().timezoneId, calendarSettings.get().timezoneId);
		assertEquals(defaultSettings().minDuration, calendarSettings.get().minDuration);
		assertEquals(defaultSettings().workingDays, calendarSettings.get().workingDays);

	}

	private ICalendarSettings service(SecurityContext sc) throws ServerFault {
		return ServerSideServiceProvider.getProvider(sc).instance(ICalendarSettings.class, calendarUid);
	}

	private CalendarSettingsData defaultSettings() {
		CalendarSettingsData s = new CalendarSettingsData();

		s.dayStart = LocalTime.of(8, 0).get(ChronoField.MILLI_OF_DAY);
		s.dayEnd = LocalTime.of(18, 0).get(ChronoField.MILLI_OF_DAY);
		s.timezoneId = ZoneId.of("UTC").getId();
		s.minDuration = 5;
		s.workingDays = Arrays.asList(Day.MO, Day.FR);
		return s;
	}
}
