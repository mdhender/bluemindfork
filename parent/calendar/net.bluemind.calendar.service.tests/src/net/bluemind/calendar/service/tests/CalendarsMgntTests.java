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
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;

import net.bluemind.calendar.api.ICalendarsMgmt;
import net.bluemind.calendar.api.VEventQuery;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.calendar.persistence.VEventIndexStore;
import net.bluemind.calendar.service.AbstractCalendarTests;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.api.ITask;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus;

public class CalendarsMgntTests extends AbstractCalendarTests {

	@Before
	public void beforeBefore() throws Exception {
		super.beforeBefore();
	}

	@Test
	public void testReIndex() throws ServerFault, InterruptedException {

		VEventSeries event = defaultVEvent();

		getCalendarService(userSecurityContext, userCalendarContainer).create("testUid", event, sendNotifications);
		refreshIndexes();
		assertEquals(1, getCalendarService(userSecurityContext, userCalendarContainer)
				.search(VEventQuery.create("testUid")).total);

		new VEventIndexStore(ElasticsearchTestHelper.getInstance().getClient(), userCalendarContainer, null)
				.deleteAll();

		refreshIndexes();
		assertEquals(0, getCalendarService(userSecurityContext, userCalendarContainer)
				.search(VEventQuery.create("testUid")).total);

		TaskRef taskRef = getCalsMgmt().reindex(userCalendarContainer.uid);

		ITask task = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(ITask.class,
				"" + taskRef.id);
		TaskStatus status = null;
		while (true) {
			status = task.status();
			if (status.state != TaskStatus.State.InProgress && status.state != TaskStatus.State.NotStarted) {
				break;
			}
			Thread.sleep(100);
		}

		assertNotNull(status);
		assertEquals(TaskStatus.State.Success, status.state);
		refreshIndexes();
		assertEquals(1, getCalendarService(userSecurityContext, userCalendarContainer)
				.search(VEventQuery.create("testUid")).total);
	}

	private ICalendarsMgmt getCalsMgmt() throws ServerFault {
		return ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(ICalendarsMgmt.class);

	}

	protected void refreshIndexes() {
		ElasticsearchTestHelper.getInstance().getClient().admin().indices()
				.prepareRefresh(VEventIndexStore.VEVENT_INDEX).execute().actionGet();
	}

}
