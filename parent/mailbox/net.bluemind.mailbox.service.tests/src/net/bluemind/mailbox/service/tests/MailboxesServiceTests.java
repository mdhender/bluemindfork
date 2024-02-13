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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import net.bluemind.addressbook.api.VCard.Identification.FormatedName;
import net.bluemind.core.api.Email;
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
import net.bluemind.directory.api.DirEntryQuery;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.mailbox.api.IMailboxAclUids;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.MailFilter;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.mailbox.api.MailboxBusAddresses;
import net.bluemind.mailbox.api.MailboxConfig;
import net.bluemind.mailbox.api.rules.DelegationRule;
import net.bluemind.mailbox.api.rules.MailFilterRule;
import net.bluemind.mailbox.api.rules.conditions.MailFilterRuleCondition;
import net.bluemind.mailshare.api.IMailshare;
import net.bluemind.mailshare.api.Mailshare;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.tests.defaultdata.PopulateHelper;
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
	public void update_keepDisplayName() throws ServerFault, IOException, InterruptedException {
		User user = PopulateHelper.getUser("testuser" + System.currentTimeMillis(), domainUid,
				Mailbox.Routing.internal);
		user.contactInfos.identification.formatedName = FormatedName.create("User formatedName");

		ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IUser.class, domainUid)
				.create(user.login, user);

		IDirectory directoryService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDirectory.class, domainUid);

		ItemValue<Mailbox> mailbox = getService(defaultSecurityContext).getComplete(user.login);
		getService(defaultSecurityContext).update(mailbox.uid, mailbox.value);

		directoryService.search(DirEntryQuery.filterNameOrEmail(user.login)).values.forEach(dev -> {
			assertEquals("User formatedName", dev.displayName);
			assertEquals("User formatedName", dev.value.displayName);
		});
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

	protected IUser getUserService(SecurityContext context) throws ServerFault {
		return ServerSideServiceProvider.getProvider(context).instance(IUser.class, domainUid);
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
		assertACLMatch(accessControlEntries, accessControlEntriesStored);

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
		assertACLMatch(accessControlEntries, accessControlEntriesStored);

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

		assertEquals(0, service.getDomainFilter().rules.size());

		MailFilterRule rule = new MailFilterRule();
		rule.conditions.add(MailFilterRuleCondition.equal("subject", "SubjectTest"));
		rule.addMove("test");
		MailFilter filter = MailFilter.create(rule);

		service.setDomainFilter(filter);

		MailFilter retrievedFilter = service.getDomainFilter();
		assertEquals(1, retrievedFilter.rules.size());
	}

	@Test
	public void testGetMailboxRules() throws Exception {
		IMailboxes service = getService(defaultSecurityContext);
		String uid1 = UUID.randomUUID().toString();
		Mailbox mbox1 = defaultMailshare("mbox1");
		service.create(uid1, mbox1);

		MailFilterRule rule1 = new MailFilterRule();
		rule1.name = "rule1";
		rule1.client = "client1";
		rule1.conditions.add(MailFilterRuleCondition.equal("subject", "SubjectTest"));
		rule1.addMove("test");

		MailFilterRule rule2 = new MailFilterRule();
		rule2.name = "rule2";
		rule2.conditions.add(MailFilterRuleCondition.equal("subject", "Toto"));
		rule2.addMove("totomails");

		MailFilter filter = MailFilter.create(rule1, rule2);
		service.setMailboxFilter(uid1, filter);

		List<MailFilterRule> mailboxRules = service.getMailboxRules(uid1);
		assertTrue(mailboxRules.stream().anyMatch(r -> rule1.name.equals(r.name)));
		assertTrue(mailboxRules.stream().anyMatch(r -> rule2.name.equals(r.name)));

		mailboxRules = service.getMailboxRulesByClient(uid1, "client1");
		assertTrue(mailboxRules.stream().anyMatch(r -> rule1.name.equals(r.name)));
		assertTrue(mailboxRules.stream().noneMatch(r -> rule2.name.equals(r.name)));
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
	public void test_createDelegationRule() {
		DelegationRule delegationRule = createDelegationRule();
		ItemValue<Mailbox> mboxD = getService(defaultSecurityContext).getComplete(delegationRule.delegatorUid);
		getService(defaultSecurityContext).setMailboxDelegationRule(mboxD.uid, delegationRule);

		DelegationRule mailboxDelegationRule = getService(defaultSecurityContext).getMailboxDelegationRule(mboxD.uid);
		assertNotNull(mailboxDelegationRule);
		assertTrue(mailboxDelegationRule.readOnly);
		assertEquals(2, mailboxDelegationRule.delegateUids.size());
		assertEquals(mailboxDelegationRule.delegatorCalendarUid, "Calendar:default:" + delegationRule.delegatorUid);
		assertEquals(mailboxDelegationRule.delegatorUid, mboxD.uid);
	}

	@Test
	public void test_updateDelegationRule() {
		DelegationRule delegationRule = createDelegationRule();
		ItemValue<Mailbox> mboxD = getService(defaultSecurityContext).getComplete(delegationRule.delegatorUid);
		getService(defaultSecurityContext).setMailboxDelegationRule(mboxD.uid, delegationRule);

		DelegationRule mailboxDelegationRule = getService(defaultSecurityContext).getMailboxDelegationRule(mboxD.uid);
		assertNotNull(mailboxDelegationRule);
		assertTrue(mailboxDelegationRule.readOnly);
		assertEquals(2, mailboxDelegationRule.delegateUids.size());
		assertEquals(mailboxDelegationRule.delegatorCalendarUid, "Calendar:default:" + delegationRule.delegatorUid);
		assertEquals(mailboxDelegationRule.delegatorUid, mboxD.uid);

		mailboxDelegationRule.readOnly = false;
		getService(defaultSecurityContext).setMailboxDelegationRule(delegationRule.delegatorUid, mailboxDelegationRule);

		mailboxDelegationRule = getService(defaultSecurityContext).getMailboxDelegationRule(mboxD.uid);
		assertNotNull(mailboxDelegationRule);
		assertFalse(mailboxDelegationRule.readOnly);
		assertEquals(2, mailboxDelegationRule.delegateUids.size());
		assertEquals(mailboxDelegationRule.delegatorCalendarUid, "Calendar:default:" + delegationRule.delegatorUid);
		assertEquals(mailboxDelegationRule.delegatorUid, mboxD.uid);
	}

	@Test
	public void test_updateDelegationRule_withOthers() {

		DelegationRule delegationRule = createDelegationRule();
		ItemValue<Mailbox> mboxD = getService(defaultSecurityContext).getComplete(delegationRule.delegatorUid);

		createMailFilter(mboxD.uid);
		MailFilter mailboxFilters = getService(defaultSecurityContext).getMailboxFilter(mboxD.uid);
		assertEquals(2, mailboxFilters.rules.size());

		getService(defaultSecurityContext).setMailboxDelegationRule(mboxD.uid, delegationRule);

		DelegationRule mailboxDelegationRule = getService(defaultSecurityContext).getMailboxDelegationRule(mboxD.uid);
		assertNotNull(mailboxDelegationRule);
		assertTrue(mailboxDelegationRule.readOnly);
		assertEquals(2, mailboxDelegationRule.delegateUids.size());
		assertEquals(mailboxDelegationRule.delegatorCalendarUid, "Calendar:default:" + delegationRule.delegatorUid);
		assertEquals(mailboxDelegationRule.delegatorUid, mboxD.uid);

		mailboxFilters = getService(defaultSecurityContext).getMailboxFilter(mboxD.uid);
		System.err.println("final rules => " + mailboxFilters.rules.stream().map(r -> r.name).toList());
		assertEquals(3, mailboxFilters.rules.size());
	}

	private void createMailFilter(String mailboxUid) {
		MailFilterRule rule1 = new MailFilterRule();
		rule1.name = "rule1";
		rule1.client = "client1";
		rule1.conditions.add(MailFilterRuleCondition.equal("subject", "SubjectTest"));
		rule1.addMove("test");

		MailFilterRule rule2 = new MailFilterRule();
		rule2.name = "rule2";
		rule1.client = "client1";
		rule2.conditions.add(MailFilterRuleCondition.equal("subject", "Toto"));
		rule2.addMove("totomails");

		MailFilter filter = MailFilter.create(rule1, rule2);

		getService(defaultSecurityContext).setMailboxFilter(mailboxUid, filter);
	}

	private DelegationRule createDelegationRule() {
		String loginDelegate1 = "test." + System.nanoTime();
		User userDelegate1 = defaultUser(loginDelegate1);
		String uidDelegate1 = loginDelegate1;
		getUserService(defaultSecurityContext).create(uidDelegate1, userDelegate1);

		String loginDelegate2 = "test." + System.nanoTime();
		User userDelegate2 = defaultUser(loginDelegate2);
		String uidDelegate2 = loginDelegate2;
		getUserService(defaultSecurityContext).create(uidDelegate2, userDelegate2);

		String loginDelegator = "test." + System.nanoTime();
		User userDelegator = defaultUser(loginDelegator);
		String uidDelegator = loginDelegator;
		getUserService(defaultSecurityContext).create(uidDelegator, userDelegator);

		DelegationRule delegationRule = new DelegationRule();
		delegationRule.delegateUids = Arrays.asList(uidDelegate1, uidDelegate2);
		delegationRule.delegatorCalendarUid = "Calendar:default:" + uidDelegator;
		delegationRule.delegatorUid = uidDelegator;
		delegationRule.readOnly = true;

		return delegationRule;
	}
}
