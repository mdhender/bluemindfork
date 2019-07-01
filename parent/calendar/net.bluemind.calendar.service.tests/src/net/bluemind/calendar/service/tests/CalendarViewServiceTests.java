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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;

import org.junit.Test;

import net.bluemind.calendar.api.CalendarView;
import net.bluemind.calendar.api.CalendarView.CalendarViewType;
import net.bluemind.calendar.api.ICalendarView;
import net.bluemind.calendar.service.AbstractCalendarTests;
import net.bluemind.calendar.service.internal.CalendarViewService;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.tests.BmTestContext;

public class CalendarViewServiceTests extends AbstractCalendarTests {

	@Test
	public void testCreate() throws ServerFault {

		CalendarView view = defaultCalendarView();
		String uid = "test_" + System.nanoTime();

		// test anonymous
		try {
			getCalendarViewService(SecurityContext.ANONYMOUS, userCalendarViewContainer).create(uid, view);
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		getCalendarViewService(userSecurityContext, userCalendarViewContainer).create(uid, view);

		// test anonymous
		try {
			getCalendarViewService(SecurityContext.ANONYMOUS, userCalendarViewContainer).list();
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		ListResult<ItemValue<CalendarView>> list = getCalendarViewService(userSecurityContext,
				userCalendarViewContainer).list();

		assertEquals(1, list.total);
		CalendarView created = list.values.get(0).value;
		assertEquals(view.label, created.label);
		assertEquals(view.type, created.type);
		assertEquals(2, view.calendars.size());
		assertTrue(created.calendars.contains(view.calendars.get(0)));
		assertTrue(created.calendars.contains(view.calendars.get(1)));
	}

	@Test
	public void testUpdate() throws ServerFault {
		CalendarView view = defaultCalendarView();
		String uid = "test_" + System.nanoTime();
		getCalendarViewService(userSecurityContext, userCalendarViewContainer).create(uid, view);

		ListResult<ItemValue<CalendarView>> list = getCalendarViewService(userSecurityContext,
				userCalendarViewContainer).list();

		assertEquals(1, list.total);
		CalendarView created = list.values.get(0).value;

		created.label = "updated";
		created.type = CalendarViewType.MONTH;
		created.calendars = new ArrayList<String>(3);
		created.calendars.add("calendar1");
		created.calendars.add("calendar3");
		created.calendars.add("calendar42");

		getCalendarViewService(userSecurityContext, userCalendarViewContainer).update(uid, created);

		list = getCalendarViewService(userSecurityContext, userCalendarViewContainer).list();

		assertEquals(1, list.total);
		CalendarView updated = list.values.get(0).value;

		assertEquals(created.label, updated.label);
		assertEquals(created.type, updated.type);
		assertEquals(created.calendars.size(), updated.calendars.size());
		assertTrue(updated.calendars.contains(created.calendars.get(0)));
		assertTrue(updated.calendars.contains(created.calendars.get(1)));
		assertTrue(updated.calendars.contains(created.calendars.get(2)));
	}

	@Test
	public void testDelete() throws ServerFault {
		CalendarView view = defaultCalendarView();
		String uid = "test_" + System.nanoTime();
		getCalendarViewService(userSecurityContext, userCalendarViewContainer).create(uid, view);

		ListResult<ItemValue<CalendarView>> list = getCalendarViewService(userSecurityContext,
				userCalendarViewContainer).list();
		assertEquals(1, list.total);

		// test anonymous
		try {
			getCalendarViewService(SecurityContext.ANONYMOUS, userCalendarViewContainer).delete(uid);
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		getCalendarViewService(userSecurityContext, userCalendarViewContainer).delete(uid);

		list = getCalendarViewService(userSecurityContext, userCalendarViewContainer).list();
		assertEquals(0, list.total);
	}

	@Test
	public void testList() throws ServerFault {
		CalendarView view = defaultCalendarView();
		String uid = "test_" + System.nanoTime();
		getCalendarViewService(userSecurityContext, userCalendarViewContainer).create(uid, view);

		ListResult<ItemValue<CalendarView>> list = getCalendarViewService(userSecurityContext,
				userCalendarViewContainer).list();
		assertEquals(1, list.total);

		CalendarView view2 = defaultCalendarView();
		String uid2 = "test_" + System.nanoTime();
		getCalendarViewService(userSecurityContext, userCalendarViewContainer).create(uid2, view2);

		list = getCalendarViewService(userSecurityContext, userCalendarViewContainer).list();
		assertEquals(2, list.total);

		getCalendarViewService(userSecurityContext, userCalendarViewContainer).delete(uid);

		list = getCalendarViewService(userSecurityContext, userCalendarViewContainer).list();
		assertEquals(1, list.total);

		getCalendarViewService(userSecurityContext, userCalendarViewContainer).delete(uid2);

		list = getCalendarViewService(userSecurityContext, userCalendarViewContainer).list();
		assertEquals(0, list.total);

	}

	@Test
	public void testDefaultView() throws ServerFault {

		ICalendarView service = getCalendarViewService(userSecurityContext, userCalendarViewContainer);
		CalendarView view = defaultCalendarView();
		String uid = "test_" + System.nanoTime();
		service.create(uid, view);

		ListResult<ItemValue<CalendarView>> list = getCalendarViewService(userSecurityContext,
				userCalendarViewContainer).list();

		assertEquals(1, list.total);
		CalendarView defaultView = list.values.get(0).value;
		assertFalse(defaultView.isDefault);

		service.setDefault(uid);

		list = getCalendarViewService(userSecurityContext, userCalendarViewContainer).list();
		assertEquals(1, list.total);
		defaultView = list.values.get(0).value;
		assertTrue(defaultView.isDefault);

		defaultView.calendars = new ArrayList<String>();
		defaultView.label = "w00t w00t";
		service.update(uid, defaultView);

		list = getCalendarViewService(userSecurityContext, userCalendarViewContainer).list();
		assertEquals(1, list.total);
		defaultView = list.values.get(0).value;
		assertTrue(defaultView.isDefault);
		assertEquals("$$calendarhome$$", defaultView.label);

		try {
			service.delete(uid);
			fail();
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.PERMISSION_DENIED, sf.getCode());
		}

	}

	private CalendarView defaultCalendarView() {
		CalendarView view = new CalendarView();
		view.label = "New view " + System.currentTimeMillis();
		view.type = CalendarViewType.WEEK;
		view.calendars = new ArrayList<String>(2);
		view.calendars.add("calendar1");
		view.calendars.add("calendar2");
		return view;
	}

	protected ICalendarView getCalendarViewService(SecurityContext context, Container container) throws ServerFault {
		return new CalendarViewService(new BmTestContext(context), container);
	}

}
