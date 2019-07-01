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
package net.bluemind.mailbox.persistance;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.Email;
import net.bluemind.core.api.date.BmDateTime;
import net.bluemind.core.api.date.BmDateTime.Precision;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.persistance.ContainerStore;
import net.bluemind.core.container.persistance.ItemStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.mailbox.api.MailFilter;
import net.bluemind.mailbox.api.Mailbox;

public class MailFilterStoreTests {
	private static Logger logger = LoggerFactory.getLogger(MailFilterStoreTests.class);
	private MailboxStore mailshareStore;
	private ItemStore itemStore;
	private String uid;
	private MailFilterStore mailfilterStore;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		SecurityContext securityContext = SecurityContext.ANONYMOUS;

		ContainerStore containerStore = new ContainerStore(JdbcTestHelper.getInstance().getDataSource(),
				securityContext);
		String containerId = "test_" + System.nanoTime() + ".fr";
		Container mailboxes = Container.create(containerId, "mailshare", containerId, "me", true);
		mailboxes = containerStore.create(mailboxes);

		this.uid = "test_" + System.nanoTime();

		assertNotNull(mailboxes);

		itemStore = new ItemStore(JdbcTestHelper.getInstance().getDataSource(), mailboxes, securityContext);

		mailshareStore = new MailboxStore(JdbcTestHelper.getInstance().getDataSource(), mailboxes);

		mailfilterStore = new MailFilterStore(JdbcTestHelper.getInstance().getDataSource(), mailboxes);
		logger.debug("stores: {} {}", itemStore, mailshareStore);

	}

	@After
	public void after() throws Exception {
		// JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testSetAndGetAndDelete() throws Exception {
		itemStore.create(Item.create(uid, null));
		Item item = itemStore.get(uid);
		Mailbox u = getDefaultMailbox();
		mailshareStore.create(item, u);

		MailFilter filter = MailFilter.create(defaultRule(), defaultRule());
		filter.rules.get(0).active = true;
		filter.forwarding = new MailFilter.Forwarding();
		filter.forwarding.enabled = true;
		filter.vacation = new MailFilter.Vacation();
		mailfilterStore.set(item, filter);

		MailFilter created = mailfilterStore.get(item);
		assertNotNull("Nothing found", created);
		assertNotNull(created.vacation);
		assertNotNull(created.forwarding);
		assertTrue(created.forwarding.enabled);
		assertTrue(created.forwarding.emails.isEmpty());

		assertEquals(2, created.rules.size());
		assertTrue(created.rules.get(0).active);
		assertTrue(created.rules.get(0).forward.emails.isEmpty());
		assertFalse(created.rules.get(1).active);
		assertTrue(created.rules.get(1).forward.emails.isEmpty());

		mailfilterStore.set(item, MailFilter.create());

		MailFilter updated = mailfilterStore.get(item);
		assertNotNull("Nothing found", updated);
		assertEquals(0, updated.rules.size());
		assertNotNull(updated.vacation);
		assertNotNull(updated.forwarding);
		assertFalse(updated.forwarding.enabled);

		Calendar calendar = Calendar.getInstance();
		calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
		updated.vacation.start = new BmDateTime("1950-06-30", null, Precision.Date);
		updated.vacation.end = new BmDateTime("1950-07-31", null, Precision.Date);
		mailfilterStore.set(item, updated);
		updated = mailfilterStore.get(item);
		calendar.set(Calendar.MILLISECOND, 0);
		calendar.set(1950, Calendar.JUNE, 30, 00, 00, 0);
		assertEquals(calendar.getTime().toInstant(),
				new BmDateTimeWrapper(updated.vacation.start).toDateTime().toInstant());
		calendar.set(1950, Calendar.JULY, 31, 00, 00, 0);
		assertEquals(calendar.getTime().toInstant(),
				new BmDateTimeWrapper(updated.vacation.end).toDateTime().toInstant());
	}

	private static Date d1 = Date.from(LocalDate.of(2020, 02, 01).atStartOfDay(ZoneId.systemDefault()).toInstant());
	private static Date d2 = Date.from(LocalDate.of(2020, 02, 02).atStartOfDay(ZoneId.systemDefault()).toInstant());
	private static Date d3 = Date.from(LocalDate.of(2020, 02, 22).atStartOfDay(ZoneId.systemDefault()).toInstant());

	@Test
	public void testOneDayVacation() throws SQLException {
		itemStore.create(Item.create(uid, null));
		Item item = itemStore.get(uid);
		Mailbox u = getDefaultMailbox();
		mailshareStore.create(item, u);
		MailFilter filter = MailFilter.create(defaultRule(), defaultRule());
		filter.rules.get(0).active = true;
		filter.forwarding = new MailFilter.Forwarding();
		filter.forwarding.enabled = true;
		filter.vacation = new MailFilter.Vacation();
		mailfilterStore.set(item, filter);

		filter.vacation.enabled = true;
		filter.vacation.start = new BmDateTime("2020-02-01", null, Precision.Date);
		filter.vacation.end = new BmDateTime("2020-02-02", null, Precision.Date);
		mailfilterStore.set(item, filter);

		// activate filter
		mailfilterStore.markOutOfOffice(item, true);

		List<String> res = mailfilterStore.findOutOfOffice(d1);
		// should return 0 because already activate
		assertEquals(0, res.size());

		res = mailfilterStore.findInOfOffice(d1);
		// should return 0 because nothing to do
		assertEquals(0, res.size());
		res = mailfilterStore.findInOfOffice(d2);
		// should return 1, need to deactivate
		assertEquals(1, res.size());

		res = mailfilterStore.findOutOfOffice(d2);
		// should return 0 because nothing to do
		assertEquals(0, res.size());
	}

	@Test
	public void testVacationAndMarker() throws Exception {
		itemStore.create(Item.create(uid, null));
		Item item = itemStore.get(uid);
		Mailbox u = getDefaultMailbox();
		mailshareStore.create(item, u);
		MailFilter filter = MailFilter.create(defaultRule(), defaultRule());
		filter.rules.get(0).active = true;
		filter.forwarding = new MailFilter.Forwarding();
		filter.forwarding.enabled = true;
		filter.vacation = new MailFilter.Vacation();
		mailfilterStore.set(item, filter);

		filter.vacation.enabled = true;
		filter.vacation.start = new BmDateTime("2020-02-01", null, Precision.Date);
		filter.vacation.end = new BmDateTime("2020-02-21", null, Precision.Date);
		mailfilterStore.set(item, filter);

		filter = mailfilterStore.get(item);
		List<String> res = mailfilterStore.findOutOfOffice(d2);
		assertEquals(1, res.size());
		assertEquals(uid, res.get(0));

		res = mailfilterStore.findOutOfOffice(d3);
		assertEquals(0, res.size());

		mailfilterStore.markOutOfOffice(item, true);
		res = mailfilterStore.findOutOfOffice(d2);
		assertEquals(0, res.size());

		res = mailfilterStore.findInOfOffice(d2);
		assertEquals(0, res.size());

		res = mailfilterStore.findInOfOffice(d3);
		assertEquals(1, res.size());

		mailfilterStore.markOutOfOffice(item, false);
		res = mailfilterStore.findOutOfOffice(d2);
		assertEquals(1, res.size());
		res = mailfilterStore.findInOfOffice(d3);
		assertEquals(0, res.size());

		filter.vacation.enabled = false;
		filter.vacation.start = new BmDateTime("2020-02-01", null, Precision.Date);
		filter.vacation.end = new BmDateTime("2020-02-21", null, Precision.Date);
		mailfilterStore.set(item, filter);
		res = mailfilterStore.findOutOfOffice(d2);
		assertEquals(0, res.size());
	}

	private Mailbox getDefaultMailbox() {
		Mailbox m = new Mailbox();
		m.name = "test" + System.nanoTime();
		m.type = Mailbox.Type.user;
		m.routing = Mailbox.Routing.internal;
		m.hidden = false;
		m.system = false;
		Email e = new Email();
		e.address = m.name + "@blue-mind.loc";
		m.emails = Arrays.asList(e);
		m.dataLocation = "fakeServerUid";
		return m;
	}

	private MailFilter.Rule defaultRule() {
		MailFilter.Rule sf = new MailFilter.Rule();
		sf.criteria = "from:bm.junit.roberto@gmail.com";
		sf.star = true;
		sf.active = false;
		return sf;
	}

	@Test
	public void nullForwardAndVacation() throws Exception {
		itemStore.create(Item.create(uid, null));
		Item item = itemStore.get(uid);
		Mailbox u = getDefaultMailbox();
		mailshareStore.create(item, u);

		MailFilter filter = MailFilter.create(defaultRule(), defaultRule());
		filter.rules.get(0).active = true;
		filter.forwarding = null;
		filter.vacation = null;
		mailfilterStore.set(item, filter);

		MailFilter filterGet = mailfilterStore.get(item);
		assertNotNull(filterGet.vacation);
		assertNotNull(filterGet.forwarding);
	}

	@Test
	public void forwardWithCopy() throws Exception {
		itemStore.create(Item.create(uid, null));
		Item item = itemStore.get(uid);
		Mailbox u = getDefaultMailbox();
		mailshareStore.create(item, u);

		MailFilter.Rule rule = new MailFilter.Rule();
		rule.criteria = "from:david@bm.com";
		rule.active = true;
		rule.forward.emails.add("fwd@bm.lan");
		rule.forward.localCopy = true;

		MailFilter filter = MailFilter.create(rule);
		mailfilterStore.set(item, filter);
		filter = mailfilterStore.get(item);
		assertEquals(1, filter.rules.size());
		rule = filter.rules.get(0);
		assertNotNull(rule.forward);
		assertTrue(rule.forward.localCopy);

		rule.forward.localCopy = false;

		filter = MailFilter.create(rule);
		mailfilterStore.set(item, filter);
		filter = mailfilterStore.get(item);
		assertEquals(1, filter.rules.size());
		rule = filter.rules.get(0);
		assertNotNull(rule.forward);
		assertFalse(rule.forward.localCopy);
	}
}
