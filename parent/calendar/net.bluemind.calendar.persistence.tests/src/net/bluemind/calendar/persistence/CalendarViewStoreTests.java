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
package net.bluemind.calendar.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.calendar.api.CalendarView;
import net.bluemind.calendar.api.CalendarView.CalendarViewType;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.ItemStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;

public class CalendarViewStoreTests {

	private CalendarViewStore store;
	private ItemStore itemStore;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		SecurityContext securityContext = SecurityContext.ANONYMOUS;

		ContainerStore containerHome = new ContainerStore(null, JdbcTestHelper.getInstance().getDataSource(),
				securityContext);
		String containerId = "test_" + System.nanoTime();
		Container container = Container.create(containerId, "test", "test", "me", true);
		container = containerHome.create(container);

		assertNotNull(container);

		itemStore = new ItemStore(JdbcTestHelper.getInstance().getDataSource(), container, securityContext);

		store = new CalendarViewStore(JdbcTestHelper.getInstance().getDataSource(), container);

	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testSchemaIsWellRegistered() {
		assertNotNull(JdbcTestHelper.getInstance().getDbSchemaService().getSchemaDescriptor("calendarview-schema"));
	}

	@Test
	public void testStoreAndRetrieveWithUid() throws SQLException {
		CalendarView view = defaultCalendarView();

		String uid = "test_" + System.nanoTime();
		itemStore.create(Item.create(uid, UUID.randomUUID().toString()));
		Item item = itemStore.get(uid);
		store.create(item, view);

		CalendarView created = store.get(item);
		assertNotNull(created);
		assertEquals(view.label, created.label);
		assertEquals(view.type, created.type);
		assertEquals(view.calendars.size(), view.calendars.size());
		assertTrue(created.calendars.contains(view.calendars.get(0)));
		assertTrue(created.calendars.contains(view.calendars.get(1)));

		item.id = new Random().nextInt();
		created = store.get(item);
		assertNull(created);
	}

	@Test
	public void testStoreAndUpdateWithUid() throws SQLException {
		CalendarView view = defaultCalendarView();

		String uid = "test_" + System.nanoTime();
		itemStore.create(Item.create(uid, UUID.randomUUID().toString()));
		Item item = itemStore.get(uid);
		store.create(item, view);

		CalendarView created = store.get(item);
		assertNotNull(created);
		assertEquals(view.label, created.label);
		assertEquals(view.type, created.type);
		assertEquals(2, view.calendars.size());
		assertTrue(created.calendars.contains(view.calendars.get(0)));
		assertTrue(created.calendars.contains(view.calendars.get(1)));

		created.label = "updated";
		created.type = CalendarViewType.MONTH;
		created.calendars = new ArrayList<String>(3);
		created.calendars.add("calendar1");
		created.calendars.add("calendar3");
		created.calendars.add("calendar42");
		store.update(item, created);

		CalendarView updated = store.get(item);
		assertNotNull(updated);
		assertEquals(created.label, updated.label);
		assertEquals(created.type, updated.type);
		assertEquals(created.calendars.size(), updated.calendars.size());
		assertTrue(updated.calendars.contains(created.calendars.get(0)));
		assertTrue(updated.calendars.contains(created.calendars.get(1)));
		assertTrue(updated.calendars.contains(created.calendars.get(2)));
	}

	@Test
	public void testDelete() throws SQLException {
		CalendarView view = defaultCalendarView();

		String uid = "test_" + System.nanoTime();
		itemStore.create(Item.create(uid, UUID.randomUUID().toString()));
		Item item = itemStore.get(uid);
		store.create(item, view);
		CalendarView created = store.get(item);
		assertNotNull(created);

		store.delete(item);
		assertNull(store.get(item));
	}

	@Test
	public void testDeleteAll() throws SQLException {
		CalendarView view1 = defaultCalendarView();

		String uid1 = "test_" + System.nanoTime();
		itemStore.create(Item.create(uid1, UUID.randomUUID().toString()));
		Item item1 = itemStore.get(uid1);
		store.create(item1, view1);
		CalendarView created1 = store.get(item1);
		assertNotNull(created1);

		CalendarView view2 = defaultCalendarView();

		String uid2 = "test_" + System.nanoTime();
		itemStore.create(Item.create(uid2, UUID.randomUUID().toString()));
		Item item2 = itemStore.get(uid2);
		store.create(item2, view2);
		CalendarView created2 = store.get(item2);
		assertNotNull(created2);

		store.deleteAll();
		assertNull(store.get(item1));
		assertNull(store.get(item2));
	}

	@Test
	public void testDefaultView() throws SQLException {
		CalendarView view = defaultCalendarView();
		String uid = "test_" + System.nanoTime();

		itemStore.create(Item.create(uid, UUID.randomUUID().toString()));
		Item item = itemStore.get(uid);
		store.create(item, view);

		CalendarView defaultView = store.get(item);
		assertNotNull(defaultView);
		assertFalse(defaultView.isDefault);

		store.setDefault(item);
		defaultView = store.get(item);
		assertNotNull(defaultView);
		assertTrue(defaultView.isDefault);
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

}
