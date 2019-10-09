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
package net.bluemind.addressbook.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.junit.Test;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

import net.bluemind.addressbook.api.AddressBookBusAddresses;
import net.bluemind.addressbook.api.IAddressBook;
import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCard.Communications.Email;
import net.bluemind.addressbook.api.VCard.Identification.FormatedName;
import net.bluemind.addressbook.api.VCard.Kind;
import net.bluemind.addressbook.api.VCard.Organizational.Member;
import net.bluemind.addressbook.api.VCardChanges;
import net.bluemind.addressbook.api.VCardInfo;
import net.bluemind.addressbook.api.VCardQuery;
import net.bluemind.addressbook.hook.internal.VCardMessage;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ChangeLogEntry;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ContainerChangelog;
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.container.model.ContainerUpdatesResult;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemChangelog;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.persistance.AclStore;
import net.bluemind.core.container.persistance.ChangelogStore;
import net.bluemind.core.container.persistance.ItemStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.LocalJsonObject;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.core.tests.vertx.VertxEventChecker;
import net.bluemind.tag.api.ITags;
import net.bluemind.tag.api.TagRef;
import net.bluemind.tag.persistance.ItemTagRef;

public class AddressBookServiceTests extends AbstractServiceTests {

	protected IAddressBook getService(SecurityContext context) throws ServerFault {
		return ServerSideServiceProvider.getProvider(context).instance(IAddressBook.class, container.uid);
	}

	@Test
	public void testCreate() throws ServerFault, InterruptedException, SQLException {
		VCard card = defaultVCard();
		String uid = "test_" + System.nanoTime();

		VertxEventChecker<LocalJsonObject<VCardMessage>> createdMessageChecker = new VertxEventChecker<>(
				AddressBookBusAddresses.CREATED);

		VertxEventChecker<JsonObject> changedMessageChecker = new VertxEventChecker<>(
				AddressBookBusAddresses.getChangedEventAddress(container.uid));

		getService(defaultSecurityContext).create(uid, card);

		Item item = itemStore.get(uid);
		assertNotNull(item);
		VCard vcard = vCardStore.get(item);
		assertNotNull(vcard);

		// TODO add test for photo == true
		assertEquals(false, vcard.identification.photo);

		assertEquals("Clara Morgane", vcard.related.spouse);
		assertEquals("David Phan", vcard.related.manager);
		assertEquals("Sylvain Garcia", vcard.related.assistant);

		assertEquals("Loser", vcard.organizational.title);
		assertEquals("Boss", vcard.organizational.role);
		assertEquals("Dev", vcard.organizational.org.department);

		List<ItemTagRef> tags = tagRefStore.get(item);
		assertNotNull(tags);
		assertEquals(2, tags.size());

		Message<LocalJsonObject<VCardMessage>> message = createdMessageChecker.shouldSuccess();
		assertNotNull(message);
		assertEquals(uid, message.body().getValue().itemUid);
		assertEquals(container.uid, message.body().getValue().container.uid);

		Message<JsonObject> containerMessage = changedMessageChecker.shouldSuccess();
		assertNotNull(containerMessage);

		createdMessageChecker = new VertxEventChecker<>(AddressBookBusAddresses.CREATED);

		changedMessageChecker = new VertxEventChecker<>(AddressBookBusAddresses.getChangedEventAddress(container.uid));
		// test anonymous
		try {
			getService(SecurityContext.ANONYMOUS).create(uid, card);
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}
		createdMessageChecker.shouldFail();
		changedMessageChecker.shouldFail();

		createdMessageChecker = new VertxEventChecker<>(AddressBookBusAddresses.CREATED);

		changedMessageChecker = new VertxEventChecker<>(AddressBookBusAddresses.getChangedEventAddress(container.uid));

		// test data validation
		try {
			card = defaultVCard();
			card.communications.emails = Arrays.asList(Email.create("ti#to@.com", Arrays.<VCard.Parameter>asList()));
			uid = "test_email" + System.nanoTime();
			getService(defaultSecurityContext).create(uid, card);
			fail();
		} catch (ServerFault e) {
		}

		createdMessageChecker.shouldFail();
		changedMessageChecker.shouldFail();
	}

	@Test
	public void testGetComplete() throws ServerFault {
		VCard card = defaultVCard();
		String uid = create(card);

		ItemValue<VCard> itemCard = getService(defaultSecurityContext).getComplete(uid);
		assertNotNull(itemCard);
		assertEquals(uid, itemCard.uid);
		assertEquals(card.identification.formatedName.value, itemCard.value.identification.formatedName.value);
		List<TagRef> tags = card.explanatory.categories;
		assertNotNull(tags);
		assertEquals(2, tags.size());

		itemCard = getService(defaultSecurityContext).getComplete("nonExistant");
		assertNull(itemCard);

		try {
			getService(SecurityContext.ANONYMOUS).getComplete(uid);
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}
	}

	@Test
	public void testMultipleGet() throws ServerFault {
		VCard card = defaultVCard();
		String uid = create(card);

		card = defaultVCard();
		String uid2 = create(card);

		List<ItemValue<VCard>> items = getService(defaultSecurityContext).multipleGet(Arrays.asList(uid, uid2));
		assertNotNull(items);
		assertEquals(2, items.size());

		items = getService(defaultSecurityContext).multipleGet(Arrays.asList("nonExistant"));

		assertNotNull(items);
		assertEquals(0, items.size());

		try {
			getService(SecurityContext.ANONYMOUS).multipleGet(Arrays.asList(uid, uid2));
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}
	}

	@Test
	public void testGetInfo() throws ServerFault {
		VCard card = defaultVCard();
		card.communications.emails = Arrays.asList(
				VCard.Communications.Email.create("test@gmail.com", Arrays.<VCard.Parameter>asList()),
				VCard.Communications.Email.create("test2@gmail.com", Arrays.<VCard.Parameter>asList()));
		String uid = create(card);

		ItemValue<VCardInfo> itemCard = getService(defaultSecurityContext).getInfo(uid);
		assertNotNull(itemCard);
		assertEquals(uid, itemCard.uid);
		assertEquals(card.kind, itemCard.value.kind);
		assertEquals("test@gmail.com", itemCard.value.mail);
		itemCard = getService(defaultSecurityContext).getInfo("nonExistant");
		assertNull(itemCard);

		try {
			getService(SecurityContext.ANONYMOUS).getInfo(uid);
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}
	}

	@Test
	public void testUpdate() throws ServerFault, SQLException {
		VCard card = defaultVCard();
		String uid = create(card);
		card.identification.name = VCard.Identification.Name.create("update", null, null, null, null,
				Collections.<VCard.Parameter>emptyList());

		card.explanatory.categories = Arrays.asList(tagRef1);

		VertxEventChecker<LocalJsonObject<VCardMessage>> createdMessageChecker = new VertxEventChecker<>(
				AddressBookBusAddresses.UPDATED);

		VertxEventChecker<JsonObject> changedMessageChecker = new VertxEventChecker<>(
				AddressBookBusAddresses.getChangedEventAddress(container.uid));

		getService(defaultSecurityContext).update(uid, card);

		Item item = itemStore.get(uid);
		assertNotNull(item);
		VCard vcard = vCardStore.get(item);
		assertNotNull(vcard);
		List<ItemTagRef> tags = tagRefStore.get(item);
		assertNotNull(tags);
		assertEquals(1, tags.size());

		Message<LocalJsonObject<VCardMessage>> message = createdMessageChecker.shouldSuccess();
		assertNotNull(message);
		assertEquals(uid, message.body().getValue().itemUid);
		assertEquals(container.uid, message.body().getValue().container.uid);

		Message<JsonObject> containerMessage = changedMessageChecker.shouldSuccess();
		assertNotNull(containerMessage);

		getService(defaultSecurityContext).update(uid, card);
		getService(defaultSecurityContext).update(uid, card);

		ItemValue<VCard> itemCard = getService(defaultSecurityContext).getComplete(uid);
		assertTrue(itemCard.version > 0);

		createdMessageChecker = new VertxEventChecker<>(AddressBookBusAddresses.UPDATED);

		changedMessageChecker = new VertxEventChecker<>(AddressBookBusAddresses.getChangedEventAddress(container.uid));

		try {
			getService(SecurityContext.ANONYMOUS).update(uid, card);
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}
		createdMessageChecker.shouldFail();
		changedMessageChecker.shouldFail();

	}

	@Test
	public void testDelete() throws SQLException, ServerFault {
		VCard card = defaultVCard();
		String uid = create(card);

		VertxEventChecker<LocalJsonObject<VCardMessage>> createdMessageChecker = new VertxEventChecker<>(
				AddressBookBusAddresses.UPDATED);

		VertxEventChecker<JsonObject> changedMessageChecker = new VertxEventChecker<>(
				AddressBookBusAddresses.getChangedEventAddress(container.uid));

		// test anonymous
		try {
			getService(SecurityContext.ANONYMOUS).delete(uid);
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}
		createdMessageChecker.shouldFail();
		changedMessageChecker.shouldFail();

		createdMessageChecker = new VertxEventChecker<>(AddressBookBusAddresses.DELETED);

		changedMessageChecker = new VertxEventChecker<>(AddressBookBusAddresses.getChangedEventAddress(container.uid));

		getService(defaultSecurityContext).delete(uid);

		Message<LocalJsonObject<VCardMessage>> message = createdMessageChecker.shouldSuccess();
		assertNotNull(message);
		assertEquals(uid, message.body().getValue().itemUid);
		assertEquals(container.uid, message.body().getValue().container.uid);

		Message<JsonObject> cmessage = changedMessageChecker.shouldSuccess();
		assertNotNull(cmessage);

		assertNull(itemStore.get(uid));

	}

	@Test
	public void testMUpdates() throws ServerFault, SQLException {
		VCard card = defaultVCard();
		String uid1 = create(card);
		String uid2 = create(defaultVCard());

		String uid3 = "testcreate_" + System.nanoTime();

		card.kind = Kind.group;
		card.identification.formatedName = FormatedName.create("test25");

		String uid4 = "testcreate_" + System.nanoTime();
		VCard group = defaultVCard();
		group.kind = Kind.group;
		String uid5 = "testcreate_" + System.nanoTime();
		group.organizational.member = Arrays.asList(Member.create(container.uid, uid5, "fakeName", "fake@email.la"));
		VCardChanges changes = VCardChanges.create(
				// add
				Arrays.asList(VCardChanges.ItemAdd.create(uid3, defaultVCard()),
						// Create group before member
						VCardChanges.ItemAdd.create(uid4, group), VCardChanges.ItemAdd.create(uid5, defaultVCard())

				),
				// modify
				Arrays.asList(VCardChanges.ItemModify.create(uid1, card)),
				// delete
				Arrays.asList(VCardChanges.ItemDelete.create(uid2)));

		ContainerUpdatesResult ret = getService(defaultSecurityContext).updates(changes);
		assertEquals(3, ret.added.size());
		assertEquals(1, ret.updated.size());
		assertEquals(1, ret.removed.size());

		List<Item> items = itemStore.getMultiple(Arrays.asList(uid1, uid2));
		assertEquals(1, items.size());
		assertEquals(uid1, items.get(0).uid);

		// vcard uid1
		VCard res = vCardStore.get(items.get(0));
		assertNotNull(res);
		assertEquals("test25", res.identification.formatedName.value);

		Item item3 = itemStore.get(uid3);
		assertNotNull(item3);
		VCard vcard = vCardStore.get(item3);
		assertNotNull(vcard);

		Item item4 = itemStore.get(uid4);
		assertNotNull(item4);
		VCard vgroup = vCardStore.get(item4);
		assertEquals(1, vgroup.organizational.member.size());
		assertEquals(container.uid, vgroup.organizational.member.get(0).containerUid);

	}

	@Test
	public void testSearchOrder() throws ServerFault {
		VCard card = defaultVCard();
		card.identification.name = VCard.Identification.Name.create(".bbbbb", null, null, null, null,
				Collections.emptyList());
		getService(defaultSecurityContext).create("testUid1", card);

		card = defaultVCard();
		card.identification.name = VCard.Identification.Name.create("baaaaaaa", null, null, null, null,
				Collections.emptyList());
		getService(defaultSecurityContext).create("testUid2", card);

		card = defaultVCard();
		card.identification.name = VCard.Identification.Name.create("bzzzzzzz", null, null, null, null,
				Collections.emptyList());
		getService(defaultSecurityContext).create("testUid3", card);

		VCardQuery query = VCardQuery.create(null);
		query.from = 0;
		query.size = 200;
		ListResult<ItemValue<VCardInfo>> res = getService(defaultSecurityContext).search(query);

		assertEquals(3, res.total);
		assertEquals(3, res.values.size());
		assertEquals("testUid1", res.values.get(0).uid);
		assertEquals("testUid2", res.values.get(1).uid);
		assertEquals("testUid3", res.values.get(2).uid);
	}

	@Test
	public void testSearchTotal() throws ServerFault {
		VCard card = defaultVCard();
		card.identification.name = VCard.Identification.Name.create(".bbbbb", null, null, null, null,
				Collections.emptyList());
		getService(defaultSecurityContext).create("testUid1", card);

		card = defaultVCard();
		card.identification.name = VCard.Identification.Name.create("baaaaaaa", null, null, null, null,
				Collections.emptyList());
		getService(defaultSecurityContext).create("testUid2", card);

		card = defaultVCard();
		card.identification.name = VCard.Identification.Name.create("bzzzzzzz", null, null, null, null,
				Collections.emptyList());
		getService(defaultSecurityContext).create("testUid3", card);

		VCardQuery query = VCardQuery.create(null);
		query.from = 0;
		query.size = 1;
		ListResult<ItemValue<VCardInfo>> res = getService(defaultSecurityContext).search(query);

		assertEquals(3, res.total);
		assertEquals(1, res.values.size());
	}

	@Test
	public void testSearch() throws ServerFault {
		VCard card = defaultVCard();
		card.identification.nickname = VCard.Identification.Nickname.create("Aachi");
		getService(defaultSecurityContext).create("testUid1", card);

		card = defaultVCard();
		card.explanatory.categories = Arrays.asList(tagRef1);
		card.organizational.org.company = "King Lothric Inc.";
		getService(defaultSecurityContext).create("testUid2", card);

		card = defaultVCard();
		card.explanatory.categories = Arrays.asList(tagRef2);
		getService(defaultSecurityContext).create("testUid3", card);

		refreshIndexes();
		ListResult<ItemValue<VCardInfo>> res = getService(defaultSecurityContext)
				.search(VCardQuery.create("value.identification.nickname.value:Aachi"));

		assertEquals(1, res.total);
		assertEquals(1, res.values.size());
		assertEquals(res.values.get(0).uid, "testUid1");

		res = getService(defaultSecurityContext).search(VCardQuery.create("value.explanatory.categories.label:tag1"));

		assertEquals(2, res.total);
		int matches = 0;
		for (ItemValue<VCardInfo> v : res.values) {
			if (v.uid.equals("testUid1") || v.uid.equals("testUid2")) {
				matches++;
			}
		}
		assertEquals(2, matches);

		res = getService(defaultSecurityContext).search(VCardQuery.create("value.explanatory.categories.label:tag2"));

		assertEquals(2, res.total);
		matches = 0;
		for (ItemValue<VCardInfo> v : res.values) {
			if (v.uid.equals("testUid1") || v.uid.equals("testUid3")) {
				matches++;
			}
		}
		assertEquals(2, matches);

		res = getService(defaultSecurityContext)
				.search(VCardQuery.create("value.organizational.org.company:King Lothric Inc."));

		assertEquals(1, res.total);
		assertEquals("testUid2", res.values.get(0).uid);

		// test anonymous
		try {
			getService(SecurityContext.ANONYMOUS)
					.search(VCardQuery.create("value.identification.nickname.value:Aachi"));
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

	}

	@Test
	public void testCopy() throws SQLException, ServerFault {
		String uid1 = create(defaultVCard());
		String uid2 = create(defaultVCard());
		String uid3 = create(defaultVCard());

		Container container2 = createTestContainer("not_mine");
		AclStore aclStore = new AclStore(new BmTestContext(SecurityContext.SYSTEM), dataDataSource);
		aclStore.store(container2,
				Arrays.asList(AccessControlEntry.create(defaultSecurityContext.getSubject(), Verb.All)));

		getService(defaultSecurityContext).copy(Arrays.asList(uid1, uid2), container2.uid);

		ItemStore itemStore2 = new ItemStore(dataDataSource, container2, defaultSecurityContext);

		assertNotNull(itemStore2.get(uid1));
		assertNotNull(itemStore2.get(uid2));
		assertNull(itemStore2.get(uid3));

		// test without write right
		aclStore.store(container2,
				Arrays.asList(AccessControlEntry.create(defaultSecurityContext.getSubject(), Verb.Read)));

		try {
			getService(defaultSecurityContext).copy(Arrays.asList(uid3), container2.uid);
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		// test with inexistant dest container
		try {
			getService(defaultSecurityContext).copy(Arrays.asList(uid3), "fakeContainerUid");
			fail();
		} catch (ServerFault e) {

		}
	}

	@Test
	public void testMove() throws SQLException, ServerFault {
		String uid1 = create(defaultVCard());
		String uid2 = create(defaultVCard());
		String uid3 = create(defaultVCard());

		Container container2 = createTestContainer("not_mine");
		AclStore aclStore = new AclStore(new BmTestContext(SecurityContext.SYSTEM), dataDataSource);
		aclStore.store(container2,
				Arrays.asList(AccessControlEntry.create(defaultSecurityContext.getSubject(), Verb.All)));

		getService(defaultSecurityContext).move(Arrays.asList(uid1, uid2), container2.uid);

		ItemStore itemStore2 = new ItemStore(dataDataSource, container2, defaultSecurityContext);

		// check items moved !
		assertNotNull(itemStore2.get(uid1));
		assertNotNull(itemStore2.get(uid2));
		assertNull(itemStore2.get(uid3));

		assertNull(itemStore.get(uid1));
		assertNull(itemStore.get(uid2));
		assertNotNull(itemStore.get(uid3));

		// test without write right
		aclStore.store(container2,
				Arrays.asList(AccessControlEntry.create(defaultSecurityContext.getSubject(), Verb.Read)));

		try {
			getService(defaultSecurityContext).move(Arrays.asList(uid3), container2.uid);
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		// test with inexistant dest container
		try {
			getService(defaultSecurityContext).move(Arrays.asList(uid3), "fakeContainerUid");
			fail();
		} catch (ServerFault e) {

		}
	}

	@Test
	public void testChangelog() throws ServerFault {

		getService(defaultSecurityContext).create("test1", defaultVCard());
		getService(defaultSecurityContext).create("test2", defaultVCard());
		getService(defaultSecurityContext).delete("test1");
		getService(defaultSecurityContext).update("test2", defaultVCard());

		// begin tests
		ContainerChangelog log = getService(defaultSecurityContext).containerChangelog(null);

		assertEquals(4, log.entries.size());

		for (ChangeLogEntry entry : log.entries) {
			System.out.println(entry.version);
		}
		log = getService(defaultSecurityContext).containerChangelog(log.entries.get(0).version);
		assertEquals(3, log.entries.size());
	}

	@Test
	public void testChangeset() throws ServerFault {

		getService(defaultSecurityContext).create("test1", defaultVCard());
		getService(defaultSecurityContext).create("test2", defaultVCard());
		getService(defaultSecurityContext).delete("test1");
		getService(defaultSecurityContext).update("test2", defaultVCard());

		// begin tests
		ContainerChangeset<String> changeset = getService(defaultSecurityContext).changeset(null);

		assertEquals(1, changeset.created.size());
		assertEquals(0, changeset.updated.size());
		assertEquals("test2", changeset.created.get(0));

		// 0 because deleted are not transmited into initial changeset
		assertEquals(0, changeset.deleted.size());

		getService(defaultSecurityContext).delete("test2");
		changeset = getService(defaultSecurityContext).changeset(changeset.version);

		assertEquals(0, changeset.created.size());
		assertEquals(0, changeset.updated.size());
		assertEquals(1, changeset.deleted.size());
		assertEquals("test2", changeset.deleted.get(0));

	}

	@Test
	public void testItemChangelog() throws ServerFault {

		getService(defaultSecurityContext).create("test1", defaultVCard());
		getService(defaultSecurityContext).update("test1", defaultVCard());
		getService(defaultSecurityContext).create("test2", defaultVCard());
		getService(defaultSecurityContext).delete("test1");
		getService(defaultSecurityContext).update("test2", defaultVCard());

		ItemChangelog itemChangeLog = getService(defaultSecurityContext).itemChangelog("test1", 0L);
		assertEquals(3, itemChangeLog.entries.size());
		assertEquals(ChangeLogEntry.Type.Created, itemChangeLog.entries.get(0).type);
		assertEquals(ChangeLogEntry.Type.Updated, itemChangeLog.entries.get(1).type);
		assertEquals(ChangeLogEntry.Type.Deleted, itemChangeLog.entries.get(2).type);

		itemChangeLog = getService(defaultSecurityContext).itemChangelog("test2", 0L);
		assertEquals(2, itemChangeLog.entries.size());
		assertEquals(ChangeLogEntry.Type.Created, itemChangeLog.entries.get(0).type);
		assertEquals(ChangeLogEntry.Type.Updated, itemChangeLog.entries.get(1).type);

	}

	// BookClientTests sync tests
	@Test
	public void testDoSync() throws ServerFault {
		ContainerChangeset<String> resp = getService(defaultSecurityContext).sync(null, VCardChanges.empty());

		assertNotNull(resp);
		assertNotNull(resp.deleted);
		assertNotNull(resp.updated);
		assertNotNull(resp.deleted);
		assertNotNull(resp.version);

		// FIXME should not be here (performance tests)
		System.out.println("Start getSync() speed test...");
		int count = 200;
		long time = System.currentTimeMillis();

		for (int i = 0; i < count; i++) {
			getService(defaultSecurityContext).sync(null, VCardChanges.empty());
		}
		time = System.currentTimeMillis() - time;
		System.out.println(
				count + " empty doSync() calls took " + time + "ms. Performing at " + (count * 1000) / time + "/sec");

	}

	@Test
	public void testOOMWithDoSync() throws ServerFault {
		int count = 100;
		createsWithDoSync(count);
	}

	private void createsWithDoSync(int count) throws ServerFault {

		List<VCardChanges.ItemAdd> al = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			VCard card = defaultVCard();
			al.add(VCardChanges.ItemAdd.create(UUID.randomUUID().toString(), card));
		}

		long time = System.currentTimeMillis();

		ContainerChangeset<String> resp = getService(defaultSecurityContext).sync(null, VCardChanges.create(al,
				Collections.<VCardChanges.ItemModify>emptyList(), Collections.<VCardChanges.ItemDelete>emptyList()));
		time = System.currentTimeMillis() - time;
		assertNotNull(resp);
		assertEquals(al.size(), resp.created.size());
		assertTrue(resp.deleted.isEmpty());

		assertTrue(resp.version > 0);
	}

	@Test
	public void testDoSyncWithClientChanges() throws ServerFault {
		ContainerChangeset<String> resp = getService(defaultSecurityContext).sync(null, VCardChanges.empty());

		assertNotNull(resp);
		assertNotNull(resp.created);
		assertNotNull(resp.updated);
		assertNotNull(resp.deleted);
		assertNotNull(resp.version);

		// create one from doSync

		VCard card = defaultVCard();
		String op = "op-" + System.currentTimeMillis();

		VCardChanges.ItemAdd change = VCardChanges.ItemAdd.create(op, card);

		long lastSync = 0;

		resp = getService(defaultSecurityContext).sync(lastSync, VCardChanges.create(Arrays.asList(change),
				Collections.<VCardChanges.ItemModify>emptyList(), Collections.<VCardChanges.ItemDelete>emptyList()));

		assertNotNull(resp);
		assertNotNull(resp.created);
		assertNotNull(resp.updated);
		assertNotNull(resp.deleted);
		assertNotNull(resp.version);

		assertEquals(1, resp.created.size());
		String cardUid = resp.created.get(0);
		assertEquals("ChangeResult size is not 1", 1, resp.created.size());

		assertTrue(resp.deleted.isEmpty());

		assertTrue(resp.version > lastSync);
		lastSync = resp.version;

		// update it from doSync
		op = "op-" + System.currentTimeMillis();
		card.explanatory.note = op;
		card.identification.birthday = createDate(2014, 04, 01);

		VCardChanges.ItemModify update = VCardChanges.ItemModify.create(cardUid, card);

		resp = getService(defaultSecurityContext).sync(lastSync,
				VCardChanges.create(Collections.<VCardChanges.ItemAdd>emptyList(), Arrays.asList(update),
						Collections.<VCardChanges.ItemDelete>emptyList()));

		assertNotNull(resp);

		assertEquals(1, resp.updated.size());

		assertEquals(cardUid, resp.updated.get(0));

		assertTrue(resp.deleted.isEmpty());

		assertTrue(resp.version > lastSync);
		lastSync = resp.version;

		// delete from doSync
		op = "op-" + System.currentTimeMillis();

		resp = getService(defaultSecurityContext).sync(lastSync,
				VCardChanges.create(Collections.<VCardChanges.ItemAdd>emptyList(),
						Collections.<VCardChanges.ItemModify>emptyList(),
						Arrays.asList(VCardChanges.ItemDelete.create(cardUid))));
		assertNotNull(resp);
		assertNotNull(resp.created);
		assertNotNull(resp.updated);
		assertNotNull(resp.deleted);
		assertNotNull(resp.version);

		assertEquals(0, resp.updated.size());

		assertEquals(1, resp.deleted.size());
		assertEquals(cardUid, resp.deleted.get(0));
		assertTrue(resp.version > lastSync);
		lastSync = resp.version;
	}

	@SuppressWarnings("deprecation")
	private Date createDate(int year, int month, int day) {
		return new Date(year, month, day);
	}

	@Test
	public void testMarkUpdated() throws ServerFault {

		VCard c = defaultVCard();
		c.identification.name = VCard.Identification.Name.create("twinLastContact", "twinFirstContact", null, null,
				null, Arrays.<VCard.Parameter>asList());

		String uid = "test" + System.currentTimeMillis();
		getService(defaultSecurityContext).create(uid, c);

		System.out.println("Created contact with uid: " + uid);

		ContainerChangeset<String> resp = getService(defaultSecurityContext).sync(null, VCardChanges.empty());

		System.out.println("Last update time : " + resp.version);
		assertEquals(1, resp.created.size());
		assertEquals(uid, resp.created.get(0));

		String uid2 = "test_2_" + System.currentTimeMillis();

		System.out.println("Created contact with uid: " + uid2);
		getService(defaultSecurityContext).create(uid2, c);

		resp = getService(defaultSecurityContext).sync(null, VCardChanges.empty());

		assertEquals(2, resp.created.size());
		boolean found = false;
		for (String s : resp.created) {
			if (uid2.equals(s)) {
				found = true;
			}
		}
		assertTrue(found);
		assertEquals(0, resp.updated.size());
		assertEquals(0, resp.deleted.size());
	}

	@Test
	public void testFullSync() throws ServerFault {

		Long lastSync = null;

		// Get synchronized

		ContainerChangeset<String> resp = getService(defaultSecurityContext).sync(null, VCardChanges.empty());

		lastSync = resp.version;
		// Test create

		VCard card = defaultVCard();
		card.identification.name.givenNames = "test";

		resp = getService(defaultSecurityContext).sync(lastSync,

				VCardChanges.create(Arrays.asList(VCardChanges.ItemAdd.create(UUID.randomUUID().toString(), card)),
						Collections.<VCardChanges.ItemModify>emptyList(),
						Collections.<VCardChanges.ItemDelete>emptyList()));

		assertEquals(1, resp.created.size());

		String createdUid = resp.created.get(0);

		lastSync = resp.version;
		// Test conflict updated
		// Modify on server

		card.identification.name.givenNames = "testserver_mod";

		getService(defaultSecurityContext).update(createdUid, card);

		// Send modified on client
		card.identification.name.givenNames = "testclient_mod";

		resp = getService(defaultSecurityContext).sync(lastSync,

				VCardChanges.create(Collections.<VCardChanges.ItemAdd>emptyList(),
						Arrays.asList(VCardChanges.ItemModify.create(createdUid, card)),
						Collections.<VCardChanges.ItemDelete>emptyList()));

		resp = getService(defaultSecurityContext).sync(lastSync,
				VCardChanges.create(Collections.<VCardChanges.ItemAdd>emptyList(),
						Arrays.asList(VCardChanges.ItemModify.create(createdUid, card)),
						Collections.<VCardChanges.ItemDelete>emptyList()));

		// -> asset client wined
		assertEquals(1, resp.updated.size());
		assertEquals(0, resp.created.size());
		assertEquals(0, resp.deleted.size());

		ItemValue<VCard> item = getService(defaultSecurityContext).getComplete(createdUid);
		assertEquals("testclient_mod", item.value.identification.name.givenNames);
		lastSync = resp.version;
		// Test delete on both sides
		getService(defaultSecurityContext).delete(createdUid);

		resp = getService(defaultSecurityContext).sync(lastSync,
				VCardChanges.create(Collections.<VCardChanges.ItemAdd>emptyList(),
						Collections.<VCardChanges.ItemModify>emptyList(),
						Arrays.asList(VCardChanges.ItemDelete.create(createdUid))));

		lastSync = resp.version;
		// -> assert ok
		assertEquals(1, resp.deleted.size());

		assertEquals(createdUid, resp.deleted.get(0));

		// Test create with sanity error BJR 51
		// FIXME Wtf ?
		// c = getTestContact();
		// String domain = p("login").split("@")[1];
		// c.addEmail("INTERNET;X-BM-Ref2", new Email("robert@" + domain));
		// cu = new ClientChange<Contact>(UUID.randomUUID().toString(), c);
		// updated.clear();
		// updated.add(cu);
		// removed.clear();
		// clientChanges.setRemoved(removed);
		// clientChanges.setUpdated(updated);
		// serverChanges = book.doSync(token, clientChanges, new SyncScope(),
		// lastSync);
		// lastSync = serverChanges.getLastSync();
		// // -> assert sanity error received and contact not created
		// assertEquals(0, serverChanges.getRemoved().size());
		// assertEquals(1, serverChanges.getChangeResults().size());
		// cr = serverChanges.getChangeResults().get(0);
		// assertEquals(cu.getOperationId(), cr.getOperationId());
		// assertTrue(
		// "Contact does not contain BJR51 email",
		// serverChanges.getUpdated().get(0).getEmails()
		// .containsKey("INTERNET;X-BM-Ref2"));

	}

	public void testFullSyncDelete() throws ServerFault {

		Long lastSync = null;

		// Get synchronized
		ContainerChangeset<String> resp = getService(defaultSecurityContext).sync(lastSync, VCardChanges.empty());
		lastSync = resp.version;

		// Send a wrong delete

		resp = getService(defaultSecurityContext).sync(lastSync,
				VCardChanges.create(Collections.<VCardChanges.ItemAdd>emptyList(),
						Collections.<VCardChanges.ItemModify>emptyList(),
						Arrays.asList(VCardChanges.ItemDelete.create("fakeId"))));

		assertTrue(resp.deleted.isEmpty());
	}

	@Test
	public void testChangesOnTagChange() throws ServerFault, SQLException {
		VCard card = defaultVCard();
		create(card);

		ChangelogStore changelogStore = new ChangelogStore(dataDataSource, container);
		ContainerChangeset<String> changeset = changelogStore.changeset(0, Long.MAX_VALUE);
		long version = changeset.version;
		VertxEventChecker<JsonObject> changedMessageChecker = new VertxEventChecker<>(
				AddressBookBusAddresses.getChangedEventAddress(container.uid));

		ITags tags = ServerSideServiceProvider.getProvider(defaultSecurityContext).instance(ITags.class,
				tagContainer.uid);

		tag1.label = "udpated";
		tags.update("tag1", tag1);

		Message<JsonObject> message = changedMessageChecker.shouldSuccess();
		assertNotNull(message);

		changeset = changelogStore.changeset(0, Long.MAX_VALUE);
		assertTrue(version < changeset.version);

	}

	@Test
	public void testReset() throws SQLException {
		VCard card = defaultVCard();
		String uid = "test_" + System.nanoTime();

		getService(defaultSecurityContext).create(uid, card);

		Item item = itemStore.get(uid);
		assertNotNull(item);
		VCard vcard = vCardStore.get(item);
		assertNotNull(vcard);

		try {
			getService(SecurityContext.ANONYMOUS).reset();
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		getService(defaultSecurityContext).reset();

		List<String> list = getService(defaultSecurityContext).allUids();
		assertEquals(0, list.size());

	}

	@Test
	public void testUpdateUpdatesGroup() throws ServerFault, SQLException {
		VCard card1 = defaultVCard();
		cardStoreService.create("vcard1", card1.identification.formatedName.value, card1);

		VCard card2 = defaultVCard();
		cardStoreService.create("vcard2", card2.identification.formatedName.value, card2);

		VCard gcard = new VCard();
		gcard.identification.formatedName = VCard.Identification.FormatedName.create("group",
				Arrays.<VCard.Parameter>asList());
		gcard.organizational.member = Arrays.<VCard.Organizational.Member>asList(
				Member.create(null, null, "gg", "gg@toto.com"), //
				Member.create(container.uid, "vcard1", "gg", "gg@toto.com"));
		cardStoreService.create("gcard", "gcard", gcard);

		card1.identification.formatedName = FormatedName.create("moi et toi");
		card1.communications.emails = Arrays.asList(Email.create("b@b.com"));
		card1.identification.name = VCard.Identification.Name.create("moi et toi", null, null, null, null,
				Collections.emptyList());
		getService(defaultSecurityContext).update("vcard1", card1);

		ItemValue<VCard> gcardItem = getService(defaultSecurityContext).getComplete("gcard");
		assertEquals(2, gcardItem.value.organizational.member.size());

		assertEquals("vcard1", gcardItem.value.organizational.member.get(1).itemUid);
		assertEquals("b@b.com", gcardItem.value.organizational.member.get(1).mailto);
		assertEquals("moi et toi", gcardItem.value.organizational.member.get(1).commonName);
	}

	@Test
	public void testMultipleUpadtesGroup() {
		// Vcard 1 in group1
		VCard card1 = defaultVCard();
		cardStoreService.create("vcard1", card1.identification.formatedName.value, card1);
		VCard gcard = new VCard();
		gcard.identification.formatedName = VCard.Identification.FormatedName.create("group",
				Arrays.<VCard.Parameter>asList());
		gcard.organizational.member = Arrays.<VCard.Organizational.Member>asList(
				Member.create(container.uid, "vcard1", "gg", "vcard1@toto.com"));
		cardStoreService.create("gcard", "gcard", gcard);
		long gcardVersion = getService(defaultSecurityContext).getComplete("gcard").version;

		// VCard 2 in group2
		VCard card2 = defaultVCard();
		cardStoreService.create("vcard2", card2.identification.formatedName.value, card2);
		VCard gcard2 = new VCard();
		gcard2.identification.formatedName = VCard.Identification.FormatedName.create("group",
				Arrays.<VCard.Parameter>asList());
		gcard2.organizational.member = Arrays.<VCard.Organizational.Member>asList(
				Member.create(container.uid, "vcard2", "gg2", "vcard2@toto.com"));
		cardStoreService.create("gcard2", "gcard2", gcard2);
		long gcard2Version = getService(defaultSecurityContext).getComplete("gcard2").version;

		card1.communications.emails = Arrays.asList(Email.create("updated-card1@toto.com"));
		card2.communications.emails = Arrays.asList(Email.create("updated-card2@toto.com"));

		VCardChanges changes = VCardChanges.create(Collections.emptyList(),
				Arrays.asList(VCardChanges.ItemModify.create("vcard1", card1),
						VCardChanges.ItemModify.create("vcard2", card2)),
				Collections.emptyList());

		getService(defaultSecurityContext).updates(changes);

		// both group updated
		ItemValue<VCard> group1 = getService(defaultSecurityContext).getComplete("gcard");
		assertTrue(group1.version > gcardVersion);
		assertEquals(card1.communications.emails.get(0).value, group1.value.organizational.member.get(0).mailto);

		ItemValue<VCard> group2 = getService(defaultSecurityContext).getComplete("gcard2");
		assertTrue(group2.version > gcard2Version);
		assertEquals(card2.communications.emails.get(0).value, group2.value.organizational.member.get(0).mailto);

	}

	@Test
	public void testUpdateVCard_NoGroupUpdate() throws ServerFault {
		VCard card1 = defaultVCard();
		cardStoreService.create("vcard1", card1.identification.formatedName.value, card1);

		VCard gcard = new VCard();
		gcard.identification.formatedName = VCard.Identification.FormatedName.create("group",
				Arrays.<VCard.Parameter>asList());
		gcard.organizational.member = Arrays.<VCard.Organizational.Member>asList(
				Member.create(container.uid, "vcard1", card1.identification.formatedName.value, card1.defaultMail()));
		cardStoreService.create("gcard", "gcard", gcard);
		ItemValue<VCard> gcardItem = getService(defaultSecurityContext).getComplete("gcard");
		long version = gcardItem.version;

		// minor update card1
		getService(defaultSecurityContext).update("vcard1", card1);

		gcardItem = getService(defaultSecurityContext).getComplete("gcard");
		assertEquals(version, gcardItem.version);
	}

	@Test
	public void testUpdateVCardDisplayName_GroupUpdate() throws ServerFault {
		VCard card1 = defaultVCard();
		cardStoreService.create("vcard1", card1.identification.formatedName.value, card1);

		VCard gcard = new VCard();
		gcard.identification.formatedName = VCard.Identification.FormatedName.create("group",
				Arrays.<VCard.Parameter>asList());
		gcard.organizational.member = Arrays.<VCard.Organizational.Member>asList(
				Member.create(container.uid, "vcard1", card1.identification.formatedName.value, card1.defaultMail()));
		cardStoreService.create("gcard", "gcard", gcard);
		ItemValue<VCard> gcardItem = getService(defaultSecurityContext).getComplete("gcard");
		long version = gcardItem.version;

		// major update card1
		card1.identification.formatedName = FormatedName.create("bangbang");
		getService(defaultSecurityContext).update("vcard1", card1);

		gcardItem = getService(defaultSecurityContext).getComplete("gcard");
		assertTrue(gcardItem.version > version);
	}

	@Test
	public void testUpdateVCardEmail_GroupUpdate() throws ServerFault {
		VCard card1 = defaultVCard();
		cardStoreService.create("vcard1", card1.identification.formatedName.value, card1);

		VCard gcard = new VCard();
		gcard.identification.formatedName = VCard.Identification.FormatedName.create("group",
				Arrays.<VCard.Parameter>asList());
		gcard.organizational.member = Arrays.<VCard.Organizational.Member>asList(
				Member.create(container.uid, "vcard1", card1.identification.formatedName.value, card1.defaultMail()));
		cardStoreService.create("gcard", "gcard", gcard);
		ItemValue<VCard> gcardItem = getService(defaultSecurityContext).getComplete("gcard");
		long version = gcardItem.version;

		// major update card1
		card1.communications.emails = Arrays.asList(Email.create("bangbang@bang.bang"));
		getService(defaultSecurityContext).update("vcard1", card1);

		gcardItem = getService(defaultSecurityContext).getComplete("gcard");
		assertTrue(gcardItem.version > version);
	}

	@Test
	public void testMUpdates_NoGroupUpdate() throws Exception {
		VCard card1 = defaultVCard();
		cardStoreService.create("vcard1", card1.identification.formatedName.value, card1);

		VCard gcard = new VCard();
		gcard.identification.formatedName = VCard.Identification.FormatedName.create("group",
				Arrays.<VCard.Parameter>asList());
		gcard.organizational.member = Arrays.<VCard.Organizational.Member>asList(
				Member.create(container.uid, "vcard1", card1.identification.formatedName.value, card1.defaultMail()));
		cardStoreService.create("gcard", "gcard", gcard);
		ItemValue<VCard> gcardItem = getService(defaultSecurityContext).getComplete("gcard");
		long version = gcardItem.version;

		// minor update card1
		VCardChanges changes = VCardChanges.create(
				// add
				Arrays.asList(),
				// modify
				Arrays.asList(VCardChanges.ItemModify.create("vcard1", card1)),
				// delete
				Arrays.asList());

		ContainerUpdatesResult ret = getService(defaultSecurityContext).updates(changes);
		assertEquals(0, ret.added.size());
		assertEquals(1, ret.updated.size());
		assertEquals(0, ret.removed.size());

		gcardItem = getService(defaultSecurityContext).getComplete("gcard");
		assertEquals(version, gcardItem.version);

	}

	@Test
	public void testMUpdates_GroupUpdate() throws Exception {
		VCard card1 = defaultVCard();
		cardStoreService.create("vcard1", card1.identification.formatedName.value, card1);

		VCard gcard = new VCard();
		gcard.identification.formatedName = VCard.Identification.FormatedName.create("group",
				Arrays.<VCard.Parameter>asList());
		gcard.organizational.member = Arrays.<VCard.Organizational.Member>asList(
				Member.create(container.uid, "vcard1", card1.identification.formatedName.value, card1.defaultMail()));
		cardStoreService.create("gcard", "gcard", gcard);
		ItemValue<VCard> gcardItem = getService(defaultSecurityContext).getComplete("gcard");
		long version = gcardItem.version;

		// major update card1
		card1.communications.emails = Arrays.asList(Email.create("bangbang@bang.bang"));
		VCardChanges changes = VCardChanges.create(
				// add
				Arrays.asList(),
				// modify
				Arrays.asList(VCardChanges.ItemModify.create("vcard1", card1)),
				// delete
				Arrays.asList());

		ContainerUpdatesResult ret = getService(defaultSecurityContext).updates(changes);
		assertEquals(0, ret.added.size());
		assertEquals(1, ret.updated.size());
		assertEquals(0, ret.removed.size());

		gcardItem = getService(defaultSecurityContext).getComplete("gcard");
		assertTrue(gcardItem.version > version);

	}

}
