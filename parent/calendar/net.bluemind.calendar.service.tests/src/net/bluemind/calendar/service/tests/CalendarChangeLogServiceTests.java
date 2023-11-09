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
package net.bluemind.calendar.service.tests;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.junit.After;
import org.junit.Test;

import net.bluemind.calendar.service.AbstractCalendarTests;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ChangeLogEntry;
import net.bluemind.core.container.model.ItemChangelog;
import net.bluemind.lib.elasticsearch.ESearchActivator;

public class CalendarChangeLogServiceTests extends AbstractCalendarTests {

	private final String DATATSTREAM_PATH = "audit_log_" + domainUid;

	@Test
	public void testChangeLog() throws ServerFault {

		getCalendarService(userSecurityContext, userCalendarContainer).create("test1", defaultVEvent(),
				sendNotifications);
		getCalendarService(userSecurityContext, userCalendarContainer).create("test2", defaultVEvent(),
				sendNotifications);
		getCalendarService(userSecurityContext, userCalendarContainer).delete("test1", sendNotifications);
		getCalendarService(userSecurityContext, userCalendarContainer).update("test2", defaultVEvent(),
				sendNotifications);
		getCalendarService(userSecurityContext, userCalendarContainer).delete("test2", sendNotifications);
		ESearchActivator.refreshIndex(DATATSTREAM_PATH);

		Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> {
			ItemChangelog itemChangeLog = getCalendarService(userSecurityContext, userCalendarContainer)
					.itemChangelog("test1", 0L);

			return 2 == itemChangeLog.entries.size();
		});

		ItemChangelog itemChangeLog = getCalendarService(userSecurityContext, userCalendarContainer)
				.itemChangelog("test1", 0L);

		assertEquals(2, itemChangeLog.entries.size());
		assertEquals(ChangeLogEntry.Type.Created, itemChangeLog.entries.get(0).type);
		assertEquals(ChangeLogEntry.Type.Deleted, itemChangeLog.entries.get(1).type);
		assertEquals("John Doe", itemChangeLog.entries.get(0).author);
		assertEquals("John Doe", itemChangeLog.entries.get(1).author);
		assertEquals("unknown-origin", itemChangeLog.entries.get(0).origin);
		assertEquals("unknown-origin", itemChangeLog.entries.get(1).origin);

		itemChangeLog = getCalendarService(userSecurityContext, userCalendarContainer).itemChangelog("test2", 0L);
		assertEquals(3, itemChangeLog.entries.size());
		assertEquals(ChangeLogEntry.Type.Created, itemChangeLog.entries.get(0).type);
		assertEquals(ChangeLogEntry.Type.Updated, itemChangeLog.entries.get(1).type);
		assertEquals(ChangeLogEntry.Type.Deleted, itemChangeLog.entries.get(2).type);
		assertEquals("John Doe", itemChangeLog.entries.get(0).author);
		assertEquals("John Doe", itemChangeLog.entries.get(1).author);
		assertEquals("unknown-origin", itemChangeLog.entries.get(0).origin);
		assertEquals("unknown-origin", itemChangeLog.entries.get(1).origin);
	}

	@After
	public void tearDown() {
		CalendarTestSyncHook.reset();
		CalendarTestAsyncHook.reset();
	}
}
