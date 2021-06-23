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
package net.bluemind.mailbox.service.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import net.bluemind.authentication.api.IAuthentication;
import net.bluemind.authentication.api.LoginResponse;
import net.bluemind.core.api.Email;
import net.bluemind.core.api.date.BmDateTime;
import net.bluemind.core.api.date.BmDateTime.Precision;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.api.ITask;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.core.tests.vertx.VertxEventChecker;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.imap.sieve.SieveClient;
import net.bluemind.imap.sieve.SieveClient.SieveConnectionData;
import net.bluemind.imap.sieve.SieveScript;
import net.bluemind.mailbox.api.IMailboxAclUids;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.MailFilter;
import net.bluemind.mailbox.api.MailFilter.Rule;
import net.bluemind.mailbox.api.MailFilter.Vacation;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.mailbox.api.MailboxBusAddresses;
import net.bluemind.mailbox.api.MailboxConfig;
import net.bluemind.mailbox.service.IInCoreMailboxes;
import net.bluemind.mailbox.service.internal.MailboxStoreService;
import net.bluemind.mailshare.api.IMailshare;
import net.bluemind.mailshare.api.Mailshare;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.scheduledjob.api.JobExitStatus;
import net.bluemind.scheduledjob.scheduler.IScheduledJob;
import net.bluemind.scheduledjob.scheduler.IScheduledJobRunId;
import net.bluemind.scheduledjob.scheduler.IScheduler;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

public class MailboxesServiceTests extends AbstractMailboxServiceTests {

	@Test
	public void testDataLocation() throws SQLException {
		Mailbox mailshare = defaultMailshare("mailshare");
		String uid = UUID.randomUUID().toString();
		getService(defaultSecurityContext).create(uid, mailshare);

		// create missing dirEntry, needed for datalocation
		Item i = itemStore.get(uid);
		dirEntryStore.create(i, DirEntry.create(null, i.uid, DirEntry.Kind.MAILSHARE, i.uid, "domain", null, true, true,
				true, mailshare.dataLocation));

		ItemValue<Mailbox> item = getService(defaultSecurityContext).getComplete(uid);
		assertEquals(mailshare.dataLocation, item.value.dataLocation);
	}

	@Test
	public void allDatalocation() throws SQLException {
		Mailbox mailshare = defaultMailshare("mailshare");
		String uid = UUID.randomUUID().toString();
		getService(defaultSecurityContext).create(uid, mailshare);

		// create missing dirEntry, needed for datalocation
		Item i = itemStore.get(uid);
		dirEntryStore.create(i, DirEntry.create(null, i.uid, DirEntry.Kind.MAILSHARE, i.uid, "domain", null, true, true,
				true, mailshare.dataLocation));

		ItemValue<Mailbox> item = getService(defaultSecurityContext).getComplete(uid);
		assertEquals(mailshare.dataLocation, item.value.dataLocation);

		Mailbox mailshare2 = defaultMailshare("mailshare2");
		String uid2 = UUID.randomUUID().toString();
		getService(defaultSecurityContext).create(uid2, mailshare2);

		// create missing dirEntry, needed for datalocation
		Item i2 = itemStore.get(uid2);
		dirEntryStore.create(i2, DirEntry.create(null, i2.uid, DirEntry.Kind.MAILSHARE, i2.uid, "domain", null, true,
				true, true, mailshare.dataLocation));
		ItemValue<Mailbox> item2 = getService(defaultSecurityContext).getComplete(uid2);
		assertEquals(mailshare2.dataLocation, item2.value.dataLocation);

		List<ItemValue<Mailbox>> all = getService(defaultSecurityContext).list();

		int found = 0;
		for (ItemValue<Mailbox> m : all) {
			if (m.uid.equals(uid)) {
				assertEquals(mailshare.dataLocation, m.value.dataLocation);
				found++;
			}
			if (m.uid.equals(uid2)) {
				assertEquals(mailshare2.dataLocation, m.value.dataLocation);
				found++;
			}
		}

		assertEquals(2, found);

	}

	@Test
	public void create() throws ServerFault, SQLException {

		VertxEventChecker<JsonObject> createdMessageChecker = new VertxEventChecker<>(MailboxBusAddresses.CREATED);

		Mailbox mailshare = defaultMailshare("mailshare");
		String uid = UUID.randomUUID().toString();

		try {
			getService(SecurityContext.ANONYMOUS).create(uid, mailshare);
			fail("Anonymous create succeed");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		try {
			getService(userSecurityContext).create(uid, mailshare);
			fail("User create succeed");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		// create missing dirEntry, needed for datalocation
		itemStore.create(Item.create(uid, null));
		Item i = itemStore.get(uid);
		dirEntryStore.create(i, DirEntry.create(null, i.uid, DirEntry.Kind.MAILSHARE, i.uid, "domain", null, true, true,
				true, mailshare.dataLocation));

		getService(defaultSecurityContext).create(uid, mailshare);

		ItemValue<Mailbox> item = getService(defaultSecurityContext).getComplete(uid);
		assertNotNull(item);

		Container mailboxAclsContainer = new ContainerStore(null, JdbcTestHelper.getInstance().getDataSource(),
				SecurityContext.SYSTEM).get(IMailboxAclUids.uidForMailbox(uid));
		assertNotNull(mailboxAclsContainer);

		Message<JsonObject> message = createdMessageChecker.shouldSuccess();
		assertNotNull(message);
	}

	@Test
	public void delete() throws ServerFault, SQLException {
		VertxEventChecker<JsonObject> deletedMessageChecker = new VertxEventChecker<>(MailboxBusAddresses.DELETED);

		String uid = UUID.randomUUID().toString();

		try {
			getService(defaultSecurityContext).delete(uid);
		} catch (ServerFault e) {
			fail("Delete on a non-existant mailshare should not fail");
		}

		Mailbox mailshare = defaultMailshare("mailshare");

		new BmTestContext(SecurityContext.SYSTEM).provider().instance(IMailshare.class, domainUid).create(uid,
				Mailshare.fromMailbox(mailshare));

		try {
			getService(SecurityContext.ANONYMOUS).delete(uid);
			fail("Anonymous delete succeed");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		try {
			getService(userSecurityContext).delete(uid);
			fail("User delete succeed");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		// Add all ACLs to user
		ArrayList<AccessControlEntry> accessControlEntries = new ArrayList<AccessControlEntry>();
		accessControlEntries.add(AccessControlEntry.create(userSecurityContext.getSubject(), Verb.All));
		accessControlEntries.add(AccessControlEntry.create(testUserUid, Verb.All));
		getService(defaultSecurityContext).setMailboxAccessControlList(uid, accessControlEntries);
		try {
			getService(userSecurityContext).delete(uid);
			fail("User delete succeed");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		getService(defaultSecurityContext).delete(uid);

		Container mailboxAclsContainer = new ContainerStore(null, JdbcTestHelper.getInstance().getDataSource(),
				SecurityContext.SYSTEM).get(IMailboxAclUids.uidForMailbox(uid));
		assertNull(mailboxAclsContainer);

		Message<JsonObject> message = deletedMessageChecker.shouldSuccess();
		assertNotNull(message);

		ItemValue<Mailbox> item = getService(defaultSecurityContext).getComplete(uid);
		assertNull(item == null ? null : item.value);

	}

	@Test
	public void update() throws ServerFault, SQLException {
		VertxEventChecker<JsonObject> updatedMessageChecker = new VertxEventChecker<>(MailboxBusAddresses.UPDATED);

		String uid = UUID.randomUUID().toString();
		Mailbox mailshare = defaultMailshare("mailshare");

		try {
			getService(defaultSecurityContext).update(uid, mailshare);
			fail("Update on a non-existant mailshare should not fail");
		} catch (ServerFault e) {
		}

		// create missing dirEntry, needed for datalocation
		itemStore.create(Item.create(uid, null));
		Item i = itemStore.get(uid);
		dirEntryStore.create(i, DirEntry.create(null, i.uid, DirEntry.Kind.MAILSHARE, i.uid, "domain", null, true, true,
				true, mailshare.dataLocation));

		getService(defaultSecurityContext).create(uid, mailshare);

		ItemValue<Mailbox> item = getService(defaultSecurityContext).getComplete(uid);
		assertNotNull(item);

		long created = item.version;

		try {
			getService(SecurityContext.ANONYMOUS).update(uid, mailshare);
			fail("Anonymous update succeed");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		try {
			getService(userSecurityContext).update(uid, mailshare);
			fail("User update succeed");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		mailshare.emails.add(Email.create("update@bm.lan", false));
		getService(defaultSecurityContext).update(uid, mailshare);
		item = getService(defaultSecurityContext).getComplete(uid);
		assertNotNull(item);
		assertTrue(created < item.version);
		Message<JsonObject> message = updatedMessageChecker.shouldSuccess();
		assertNotNull(message);
	}

	@Test
	public void userWithAllPermsUpdate() throws ServerFault, SQLException {
		String uid = UUID.randomUUID().toString();
		Mailbox mailshare = defaultMailshare("mailshare");

		new BmTestContext(SecurityContext.SYSTEM).provider().instance(IMailshare.class, domainUid).create(uid,
				Mailshare.fromMailbox(mailshare));

		ItemValue<Mailbox> item = getService(defaultSecurityContext).getComplete(uid);
		assertNotNull(item);

		ArrayList<AccessControlEntry> accessControlEntries = new ArrayList<AccessControlEntry>();
		accessControlEntries.add(AccessControlEntry.create(userSecurityContext.getSubject(), Verb.All));
		getService(defaultSecurityContext).setMailboxAccessControlList(uid, accessControlEntries);

		mailshare.emails.add(Email.create("update@bm.lan", false));

		try {
			getService(userSecurityContext).update(uid, mailshare);
			fail("User update succeed");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}
	}

	@Test
	public void getComplete() throws ServerFault, SQLException {

		String uid = UUID.randomUUID().toString();
		Mailbox mailshare = defaultMailshare("mailshare");

		ItemValue<Mailbox> iv = getService(defaultSecurityContext).getComplete(uid);
		assertNull(iv);

		// create missing dirEntry, needed for datalocation
		itemStore.create(Item.create(uid, null));
		Item i = itemStore.get(uid);
		dirEntryStore.create(i, DirEntry.create(null, i.uid, DirEntry.Kind.MAILSHARE, i.uid, "domain", null, true, true,
				true, mailshare.dataLocation));

		getService(defaultSecurityContext).create(uid, mailshare);

		try {
			getService(SecurityContext.ANONYMOUS).getComplete(uid);
			fail("Anonymous get succeed");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		ItemValue<Mailbox> mailshareItem = getService(defaultSecurityContext).getComplete(uid);
		assertNotNull(mailshareItem);
		assertEquals(uid, mailshareItem.uid);
		Mailbox m = mailshareItem.value;

		assertNotNull(m.name);
		System.err.println("Mailshare: " + m.name);

	}

	@Test
	public void anonymousNameInUse() throws ServerFault, SQLException {

		String uid = UUID.randomUUID().toString();
		Mailbox mailshare = defaultMailshare("mailshare");

		getService(defaultSecurityContext).create(uid, mailshare);
		// create missing dirEntry, needed for datalocation
		Item i = itemStore.get(uid);
		dirEntryStore.create(i, DirEntry.create(null, i.uid, DirEntry.Kind.MAILSHARE, i.uid, "domain", null, true, true,
				true, mailshare.dataLocation));

		try {
			getService(SecurityContext.ANONYMOUS).create(uid, mailshare);
			fail("Anonymous create succeed");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		String uid2 = UUID.randomUUID().toString();
		Mailbox mailshare2 = defaultMailshare("mailshare2");

		getService(defaultSecurityContext).create(uid2, mailshare2);
		// create missing dirEntry, needed for datalocation
		i = itemStore.get(uid2);
		dirEntryStore.create(i, DirEntry.create(null, i.uid, DirEntry.Kind.MAILSHARE, i.uid, "domain", null, true, true,
				true, mailshare2.dataLocation));

		try {
			mailshare.name = "mailshare2";
			getService(SecurityContext.ANONYMOUS).create(uid, mailshare);
			fail("Anonymous update succeed");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

	}

	@Test
	public void searchByEmail() throws ServerFault, SQLException {
		String uid = UUID.randomUUID().toString();
		Mailbox mailshare = defaultMailshare("mailshare");

		// create missing dirEntry, needed for datalocation
		itemStore.create(Item.create(uid, null));
		Item i = itemStore.get(uid);
		dirEntryStore.create(i, DirEntry.create(null, i.uid, DirEntry.Kind.MAILSHARE, i.uid, "domain", null, true, true,
				true, mailshare.dataLocation));

		getService(defaultSecurityContext).create(uid, mailshare);

		ItemValue<Mailbox> result = getService(defaultSecurityContext).byEmail("mailshare@bm.lan");
		assertNotNull(result);

		// match alias
		result = getService(defaultSecurityContext).byEmail("mailshare@aliasbm.lan");
		assertNotNull(result);

		// not match unkown alias
		result = getService(defaultSecurityContext).byEmail("mailshare@gmail.com");
		assertNull(result);
	}

	@Test
	public void searchByName() throws ServerFault, SQLException {
		String uid = UUID.randomUUID().toString();
		Mailbox mailshare = defaultMailshare("mailshare");

		// create missing dirEntry, needed for datalocation
		itemStore.create(Item.create(uid, null));
		Item i = itemStore.get(uid);
		dirEntryStore.create(i, DirEntry.create(null, i.uid, DirEntry.Kind.MAILSHARE, i.uid, "domain", null, true, true,
				true, mailshare.dataLocation));

		getService(defaultSecurityContext).create(uid, mailshare);

		ItemValue<Mailbox> result = getService(defaultSecurityContext).byName("mailshare");
		assertNotNull(result);
	}

	@Override
	protected IMailboxes getService(SecurityContext context) throws ServerFault {
		return ServerSideServiceProvider.getProvider(context).instance(IMailboxes.class, domainUid);
	}

	@Test
	public void setAndGetMailboxAcls() throws ServerFault {
		String uid = UUID.randomUUID().toString();
		Mailbox mailshare = defaultMailshare("mailshare");
		new BmTestContext(SecurityContext.SYSTEM).provider().instance(IMailshare.class, domainUid).create(uid,
				Mailshare.fromMailbox(mailshare));

		ArrayList<AccessControlEntry> accessControlEntries = new ArrayList<AccessControlEntry>();
		accessControlEntries.add(AccessControlEntry.create(UUID.randomUUID().toString(), Verb.Write));
		accessControlEntries.add(AccessControlEntry.create(UUID.randomUUID().toString(), Verb.Read));

		try {
			getService(SecurityContext.ANONYMOUS).setMailboxAccessControlList(uid, accessControlEntries);
			fail("Anonymous set succeed");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		doUserSetAcls(true, uid, accessControlEntries);

		getService(defaultSecurityContext).setMailboxAccessControlList(uid, accessControlEntries);

		List<AccessControlEntry> accessControlEntriesStored;
		try {
			accessControlEntriesStored = getService(SecurityContext.ANONYMOUS).getMailboxAccessControlList(uid);
			fail("Anonymous get succeed");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		try {
			getService(userSecurityContext).getMailboxAccessControlList(uid);
			fail("User get succeed");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		accessControlEntriesStored = getService(defaultSecurityContext).getMailboxAccessControlList(uid);

		assertEquals(accessControlEntries.size(), accessControlEntriesStored.size());

		for (AccessControlEntry ace : accessControlEntries) {
			boolean found = false;

			for (AccessControlEntry aces : accessControlEntriesStored) {
				if (aces.subject.equals(ace.subject) && aces.verb == ace.verb) {
					found = true;
					break;
				}
			}

			assertTrue(found);
		}
	}

	@Test
	public void userGetMailboxAcls() throws ServerFault {
		String uid = UUID.randomUUID().toString();
		Mailbox mailshare = defaultMailshare("mailshare");

		new BmTestContext(SecurityContext.SYSTEM).provider().instance(IMailshare.class, domainUid).create(uid,
				Mailshare.fromMailbox(mailshare));

		ArrayList<AccessControlEntry> accessControlEntries = new ArrayList<AccessControlEntry>();
		accessControlEntries.add(AccessControlEntry.create(userSecurityContext.getSubject(), Verb.Read));

		getService(defaultSecurityContext).setMailboxAccessControlList(uid, accessControlEntries);

		try {
			getService(userSecurityContext).getMailboxAccessControlList(uid);
			fail("User get succeed");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		accessControlEntries = new ArrayList<AccessControlEntry>();
		accessControlEntries.add(AccessControlEntry.create(userSecurityContext.getSubject(), Verb.Write));

		getService(defaultSecurityContext).setMailboxAccessControlList(uid, accessControlEntries);

		try {
			getService(userSecurityContext).getMailboxAccessControlList(uid);
			fail("User get succeed");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		accessControlEntries = new ArrayList<AccessControlEntry>();
		accessControlEntries.add(AccessControlEntry.create(userSecurityContext.getSubject(), Verb.All));

		getService(defaultSecurityContext).setMailboxAccessControlList(uid, accessControlEntries);

		List<AccessControlEntry> accessControlEntriesStored = getService(userSecurityContext)
				.getMailboxAccessControlList(uid);
		assertEquals(accessControlEntries.size(), accessControlEntriesStored.size());

		for (AccessControlEntry ace : accessControlEntries) {
			boolean found = false;

			for (AccessControlEntry aces : accessControlEntriesStored) {
				if (aces.subject.equals(ace.subject) && aces.verb == ace.verb) {
					found = true;
					break;
				}
			}

			assertTrue(found);
		}
	}

	@Test
	public void userSetMailboxAcls() throws ServerFault {
		String uid = UUID.randomUUID().toString();
		Mailbox mailshare = defaultMailshare("mailshare");

		new BmTestContext(SecurityContext.SYSTEM).provider().instance(IMailshare.class, domainUid).create(uid,
				Mailshare.fromMailbox(mailshare));

		ArrayList<AccessControlEntry> accessControlEntries = new ArrayList<AccessControlEntry>();

		accessControlEntries.add(AccessControlEntry.create(userSecurityContext.getSubject(), Verb.Read));
		doUserSetAcls(true, uid, accessControlEntries);

		getService(defaultSecurityContext).setMailboxAccessControlList(uid, accessControlEntries);
		doUserSetAcls(true, uid, accessControlEntries);

		accessControlEntries.add(AccessControlEntry.create(userSecurityContext.getSubject(), Verb.Write));
		getService(defaultSecurityContext).setMailboxAccessControlList(uid, accessControlEntries);
		doUserSetAcls(true, uid, accessControlEntries);

		accessControlEntries.add(AccessControlEntry.create(userSecurityContext.getSubject(), Verb.All));
		getService(defaultSecurityContext).setMailboxAccessControlList(uid, accessControlEntries);
		doUserSetAcls(false, uid, accessControlEntries);
	}

	@Test
	public void testShareMyMailbox() throws ServerFault {
		ArrayList<AccessControlEntry> accessControlEntries = new ArrayList<AccessControlEntry>();
		accessControlEntries.add(AccessControlEntry.create(userSecurityContext.getSubject(), Verb.Read));

		getService(defaultSecurityContext).setMailboxAccessControlList(defaultSecurityContext.getSubject(),
				accessControlEntries);
	}

	// Wait for bmsysadmin PR
	// @Test
	// public void testMailshareSieve() throws ServerFault {
	//
	// IMailboxes service = getService(defaultSecurityContext);
	// Mailbox mailshare = defaultMailshare("mailshare");
	// String uid = UUID.randomUUID().toString();
	// service.create(uid, mailshare);
	//
	// MailFilter.Rule rule = new MailFilter.Rule();
	// rule.active = true;
	// rule.criteria = "SUBJECT:IS: SubjectTest";
	// rule.deliver = "test";
	//
	// MailFilter filter = MailFilter.create(rule);
	//
	// service.setMailboxFilter(uid, filter);
	// }

	@Test
	public void testUserSieve() throws Exception {

		IMailboxes service = getService(defaultSecurityContext);

		MailFilter.Rule rule = new MailFilter.Rule();
		rule.active = true;
		rule.criteria = "SUBJECT:IS: SubjectTest";
		rule.deliver = "test";

		MailFilter filter = MailFilter.create(rule);

		service.setMailboxFilter("admin", filter);
		SieveConnectionData connectionData = new SieveConnectionData("admin@" + domainUid, "admin",
				new BmConfIni().get("imap-role"));
		try (SieveClient sc = new SieveClient(connectionData)) {
			assertTrue(sc.login());
			List<SieveScript> scripts = sc.listscripts();
			assertEquals(1, scripts.size());
		}

	}

	@Test
	public void testGetAll() throws Exception {
		IMailboxes service = getService(defaultSecurityContext);
		String uid1 = UUID.randomUUID().toString();
		Mailbox mbox1 = defaultMailshare("mbox1");
		service.create(uid1, mbox1);

		String uid2 = UUID.randomUUID().toString();
		Mailbox mbox2 = defaultMailshare("mbox2");
		mbox2.quota = 1024;
		service.create(uid2, mbox2);

		List<ItemValue<Mailbox>> list = service.list();
		for (ItemValue<Mailbox> m : list) {
			System.err.println(m.displayName + " " + m.value.name);
		}
		// 5 : admin, testuser, mbox1, mbox2, _user and _admin
		assertEquals(6, list.size());

		boolean mb1found = false;
		boolean mb2found = false;
		for (ItemValue<Mailbox> i : list) {
			if (uid1.equals(i.uid)) {
				mb1found = true;
				assertEquals(null, i.value.quota);
			} else if (uid2.equals(i.uid)) {
				mb2found = true;
				assertEquals(1024, i.value.quota.intValue());
			}
		}

		assertTrue(mb1found);
		assertTrue(mb2found);
	}

	@Test
	public void testDomainSieve() throws Exception {
		IMailboxes service = getService(defaultSecurityContext);

		MailFilter.Rule rule = new MailFilter.Rule();
		rule.active = true;
		rule.criteria = "SUBJECT:IS: SubjectTest";
		rule.deliver = "test";

		MailFilter filter = MailFilter.create(rule);

		service.setDomainFilter(filter);

		LoginResponse su = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IAuthentication.class)
				.su("bmhiddensysadmin@" + domainUid);

		SieveConnectionData connectionData = new SieveConnectionData("bmhiddensysadmin@" + domainUid, su.authKey,
				new BmConfIni().get("imap-role"));

		try (SieveClient sc = new SieveClient(connectionData)) {
			assertTrue(sc.login());
			List<SieveScript> scripts = sc.listscripts();
			assertEquals(1, scripts.size());
			assertEquals(domainUid + ".sieve", scripts.get(0).getName());
		}
	}

	private void doUserSetAcls(boolean mustFail, String uid, ArrayList<AccessControlEntry> accessControlEntries)
			throws ServerFault {
		try {
			getService(userSecurityContext).setMailboxAccessControlList(uid, accessControlEntries);
			if (mustFail) {
				fail("User set succeed");
			}
		} catch (ServerFault e) {
			if (!mustFail && e.getCode() == ErrorCode.PERMISSION_DENIED) {
				fail("User set failed");
			}
		}
	}

	@Test
	public void searchByRouting() throws ServerFault, SQLException {
		String uid2 = UUID.randomUUID().toString();
		Mailbox mailshare2 = defaultMailshare("mailshare2");
		mailshare2.routing = Routing.none;

		// create missing dirEntry, needed for datalocation
		itemStore.create(Item.create(uid2, null));
		Item i = itemStore.get(uid2);
		dirEntryStore.create(i, DirEntry.create(null, i.uid, DirEntry.Kind.MAILSHARE, i.uid, "domain", null, true, true,
				true, mailshare2.dataLocation));

		getService(defaultSecurityContext).create(uid2, mailshare2);

		List<String> result = getService(defaultSecurityContext).byRouting(Routing.internal);
		assertEquals(2, result.size());
		assertTrue(result.contains("admin"));
		assertTrue(result.contains(testUserUid));
	}

	public TaskStatus waitEnd(TaskRef ref) throws Exception {
		TaskStatus status = null;
		while (true) {
			ITask task = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(ITask.class, ref.id);
			status = task.status();
			if (status.state.ended) {
				break;
			}
		}

		return status;
	}

	@Test
	public void testGetMailboxConfig() throws Exception {

		MailboxConfig mailConfig = getService(userSecurityContext).getMailboxConfig(userSecurityContext.getSubject());

		assertNull(mailConfig.messageMaxSize);
		new BmTestContext(SecurityContext.SYSTEM).provider().instance(ISystemConfiguration.class)
				.updateMutableValues(ImmutableMap.of(SysConfKeys.message_size_limit.name(), "1000"));

		mailConfig = getService(userSecurityContext).getMailboxConfig(userSecurityContext.getSubject());
		assertEquals(Integer.valueOf(1000), mailConfig.messageMaxSize);
	}

	@Test
	public void test_MailboxPublicAcl_Forbidden() throws ServerFault {
		String uid = UUID.randomUUID().toString();
		Mailbox mailshare = defaultMailshare("public-mailshare");
		new BmTestContext(SecurityContext.SYSTEM).provider().instance(IMailshare.class, domainUid).create(uid,
				Mailshare.fromMailbox(mailshare));

		ArrayList<AccessControlEntry> accessControlEntries = new ArrayList<AccessControlEntry>();
		accessControlEntries.add(AccessControlEntry.create(domainUid, Verb.Write));
		accessControlEntries.add(AccessControlEntry.create(UUID.randomUUID().toString(), Verb.Read));

		try {
			getService(defaultSecurityContext).setMailboxAccessControlList(uid, accessControlEntries);
			fail("Public ACL is forbidden");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.FORBIDDEN, e.getCode());
		}
	}

	@Test
	public void refreshOutOfOffice() {
		User u1 = defaultUser("u" + System.currentTimeMillis());
		String u1Uid = UUID.randomUUID().toString();
		new BmTestContext(SecurityContext.SYSTEM).provider().instance(IUser.class, domainUid).create(u1Uid, u1);

		Vacation vacation = new MailFilter.Vacation();
		vacation.enabled = true;
		vacation.subject = "subject";
		vacation.text = "content";

		Calendar c = Calendar.getInstance();
		c.add(Calendar.DAY_OF_YEAR, -1);
		BmDateTime bmDateTimeStart = new BmDateTime(new SimpleDateFormat("yyyy-MM-dd").format(c.getTime()), null,
				Precision.Date);
		long toTimestamp = new BmDateTimeWrapper(bmDateTimeStart).toUTCTimestamp();
		vacation.start = new Date(toTimestamp);
		c.add(Calendar.DAY_OF_YEAR, 3);
		bmDateTimeStart = new BmDateTime(new SimpleDateFormat("yyyy-MM-dd").format(c.getTime()), null, Precision.Date);
		toTimestamp = new BmDateTimeWrapper(bmDateTimeStart).toUTCTimestamp();
		vacation.end = new Date(toTimestamp);

		MailFilter mailFilter = new MailFilter();
		mailFilter.vacation = vacation;
		new MailboxStoreService(JdbcTestHelper.getInstance().getDataSource(), defaultSecurityContext, container)
				.setFilter(u1Uid, mailFilter);

		TestScheduler testScheduler = new TestScheduler();
		JobExitStatus result = new BmTestContext(SecurityContext.SYSTEM).provider()
				.instance(IInCoreMailboxes.class, domainUid).refreshOutOfOffice(testScheduler, null);
		assertEquals(JobExitStatus.SUCCESS, result);
		assertEquals(0, testScheduler.info.size());
		assertEquals(0, testScheduler.warn.size());
		assertEquals(0, testScheduler.error.size());
	}

	@Test
	public void refreshOutOfOffice_noVacation() {
		User u1 = defaultUser("u" + System.currentTimeMillis());
		String u1Uid = UUID.randomUUID().toString();
		new BmTestContext(SecurityContext.SYSTEM).provider().instance(IUser.class, domainUid).create(u1Uid, u1);

		Rule r1 = new Rule();
		r1.active = true;
		r1.criteria = "SUBJECT:IS: toredirect";
		r1.delete = true;

		new MailboxStoreService(JdbcTestHelper.getInstance().getDataSource(), defaultSecurityContext, container)
				.setFilter(u1Uid, MailFilter.create(r1));

		TestScheduler testScheduler = new TestScheduler();
		JobExitStatus result = new BmTestContext(SecurityContext.SYSTEM).provider()
				.instance(IInCoreMailboxes.class, domainUid).refreshOutOfOffice(testScheduler, null);
		assertEquals(JobExitStatus.SUCCESS, result);
		assertEquals(0, testScheduler.info.size());
		assertEquals(0, testScheduler.warn.size());
		assertEquals(0, testScheduler.error.size());
	}

	@Test
	public void refreshOutOfOffice_invalidFilter_allInError() {
		User u1 = defaultUser("u" + System.currentTimeMillis());
		String u1Uid = UUID.randomUUID().toString();
		new BmTestContext(SecurityContext.SYSTEM).provider().instance(IUser.class, domainUid).create(u1Uid, u1);

		Vacation vacation = new MailFilter.Vacation();
		vacation.enabled = true;
		vacation.subject = "subject";
		vacation.text = "content";

		Calendar c = Calendar.getInstance();
		c.add(Calendar.DAY_OF_YEAR, -1);
		BmDateTime bmDateTime = new BmDateTime(new SimpleDateFormat("yyyy-MM-dd").format(c.getTime()), null,
				Precision.Date);
		long toTimestamp = new BmDateTimeWrapper(bmDateTime).toUTCTimestamp();
		vacation.start = new Date(toTimestamp);
		c.add(Calendar.DAY_OF_YEAR, 3);
		bmDateTime = new BmDateTime(new SimpleDateFormat("yyyy-MM-dd").format(c.getTime()), null, Precision.Date);
		toTimestamp = new BmDateTimeWrapper(bmDateTime).toUTCTimestamp();
		vacation.end = new Date(toTimestamp);

		Rule r1 = new Rule();
		r1.active = true;

		r1.criteria = "space header:IS: fdss";
		r1.delete = true;

		MailFilter mailFilter = new MailFilter();
		mailFilter.vacation = vacation;
		mailFilter.rules = Arrays.asList(r1);

		new MailboxStoreService(JdbcTestHelper.getInstance().getDataSource(), defaultSecurityContext, container)
				.setFilter(u1Uid, mailFilter);

		TestScheduler testScheduler = new TestScheduler();
		JobExitStatus result = new BmTestContext(SecurityContext.SYSTEM).provider()
				.instance(IInCoreMailboxes.class, domainUid).refreshOutOfOffice(testScheduler, null);
		assertEquals(JobExitStatus.FAILURE, result);
		assertEquals(1, testScheduler.info.size());
		assertEquals(0, testScheduler.warn.size());
		assertEquals(1, testScheduler.error.size());
	}

	@Test
	public void refreshOutOfOffice_invalidFilter_someInError() {
		User u1 = defaultUser("u" + System.currentTimeMillis());
		String u1Uid = UUID.randomUUID().toString();
		new BmTestContext(SecurityContext.SYSTEM).provider().instance(IUser.class, domainUid).create(u1Uid, u1);

		User u2 = defaultUser("u" + System.currentTimeMillis());
		String u2Uid = UUID.randomUUID().toString();
		new BmTestContext(SecurityContext.SYSTEM).provider().instance(IUser.class, domainUid).create(u2Uid, u2);

		Vacation vacation = new MailFilter.Vacation();
		vacation.enabled = true;
		vacation.subject = "subject";
		vacation.text = "content";

		Calendar c = Calendar.getInstance();
		c.add(Calendar.DAY_OF_YEAR, -1);
		BmDateTime bmDateTime = new BmDateTime(new SimpleDateFormat("yyyy-MM-dd").format(c.getTime()), null,
				Precision.Date);
		long toTimestamp = new BmDateTimeWrapper(bmDateTime).toUTCTimestamp();
		vacation.start = new Date(toTimestamp);
		c.add(Calendar.DAY_OF_YEAR, 3);
		bmDateTime = new BmDateTime(new SimpleDateFormat("yyyy-MM-dd").format(c.getTime()), null, Precision.Date);
		toTimestamp = new BmDateTimeWrapper(bmDateTime).toUTCTimestamp();
		vacation.end = new Date(toTimestamp);

		Rule r1 = new Rule();
		r1.active = true;
		r1.criteria = "space header:IS: fdss";
		r1.delete = true;

		MailFilter mailFilter = new MailFilter();
		mailFilter.vacation = vacation;
		mailFilter.rules = Arrays.asList(r1);

		new MailboxStoreService(JdbcTestHelper.getInstance().getDataSource(), defaultSecurityContext, container)
				.setFilter(u1Uid, mailFilter);

		mailFilter = new MailFilter();
		mailFilter.vacation = vacation;
		new MailboxStoreService(JdbcTestHelper.getInstance().getDataSource(), defaultSecurityContext, container)
				.setFilter(u2Uid, mailFilter);

		TestScheduler testScheduler = new TestScheduler();
		JobExitStatus result = new BmTestContext(SecurityContext.SYSTEM).provider()
				.instance(IInCoreMailboxes.class, domainUid).refreshOutOfOffice(testScheduler, null);
		assertEquals(JobExitStatus.COMPLETED_WITH_WARNINGS, result);
		assertEquals(1, testScheduler.info.size());
		assertEquals(0, testScheduler.warn.size());
		assertEquals(1, testScheduler.error.size());
	}

	private class TestScheduler implements IScheduler {
		public final List<String> info = new ArrayList<>();
		public final List<String> warn = new ArrayList<>();
		public final List<String> error = new ArrayList<>();

		@Override
		public void warn(IScheduledJobRunId rid, String locale, String logEntry) {
			if ("en".equals(locale)) {
				warn.add(logEntry);
			}
		}

		@Override
		public IScheduledJobRunId requestSlot(String domainName, IScheduledJob bj, Date startDate) throws ServerFault {
			return null;
		}

		@Override
		public void reportProgress(IScheduledJobRunId rid, int percent) {
		}

		@Override
		public void info(IScheduledJobRunId rid, String locale, String logEntry) {
			if ("en".equals(locale)) {
				info.add(logEntry);
			}
		}

		@Override
		public void finish(IScheduledJobRunId rid, JobExitStatus status) {
		}

		@Override
		public void error(IScheduledJobRunId rid, String locale, String logEntry) {
			if ("en".equals(locale)) {
				error.add(logEntry);
			}
		}
	};
}
