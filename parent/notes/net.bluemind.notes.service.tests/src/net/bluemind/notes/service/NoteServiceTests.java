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
package net.bluemind.notes.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.Test;

import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ChangeLogEntry;
import net.bluemind.core.container.model.ContainerChangelog;
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemChangelog;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.persistence.ChangelogStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.tests.vertx.VertxEventChecker;
import net.bluemind.notes.api.INote;
import net.bluemind.notes.api.INoteIndexMgmt;
import net.bluemind.notes.api.INotes;
import net.bluemind.notes.api.VNote;
import net.bluemind.notes.api.VNote.Color;
import net.bluemind.notes.api.VNoteQuery;
import net.bluemind.notes.hook.NoteHookAddress;
import net.bluemind.tag.api.ITags;
import net.bluemind.tag.api.TagRef;
import net.bluemind.tag.persistence.ItemTagRef;

public class NoteServiceTests extends AbstractServiceTests {

	@Test
	public void testCreate() throws ServerFault, SQLException {
		VertxEventChecker<JsonObject> createdMessageChecker = new VertxEventChecker<>(NoteHookAddress.CREATED);

		VNote note = defaultVNote();
		String uid = "test_" + System.nanoTime();

		// test anonymous
		try {
			getServiceNote(SecurityContext.ANONYMOUS, container.uid).create(uid, note);
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		getServiceNote(defaultSecurityContext, container.uid).create(uid, note);

		Item item = itemStore.get(uid);
		assertNotNull(item);
		VNote vnote = vnoteStore.get(item);
		assertNotNull(vnote);

		List<ItemTagRef> tags = tagRefStore.get(item);
		assertNotNull(tags);
		assertEquals(2, tags.size());

		Message<JsonObject> message = createdMessageChecker.shouldSuccess();
		assertNotNull(message);
	}

	@Test
	public void testCreateWithItem() throws Exception {
		setGlobalExternalUrl();
		VertxEventChecker<JsonObject> createdMessageChecker = new VertxEventChecker<>(NoteHookAddress.CREATED);

		ItemValue<VNote> noteItem = defaultVNoteItem(40);

		// test anonymous
		try {
			getServiceNote(SecurityContext.ANONYMOUS, container.uid).createWithItem(noteItem);
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		getServiceNote(defaultSecurityContext, container.uid).createWithItem(noteItem);

		Item item = itemStore.get(noteItem.uid);
		assertItemEquals(noteItem.item(), item);
		VNote vnote = vnoteStore.get(item);
		assertNotNull(vnote);

		List<ItemTagRef> tags = tagRefStore.get(item);
		assertNotNull(tags);
		assertEquals(2, tags.size());

		Message<JsonObject> message = createdMessageChecker.shouldSuccess();
		assertNotNull(message);
	}

	@Test
	public void testUpdate() throws ServerFault {

		VertxEventChecker<JsonObject> updatedMessageChecker = new VertxEventChecker<>(NoteHookAddress.UPDATED);

		VNote note = defaultVNote();
		String uid = "test_" + System.nanoTime();
		getServiceNote(defaultSecurityContext, container.uid).create(uid, note);

		// test anonymous
		try {
			getServiceNote(SecurityContext.ANONYMOUS, container.uid).update(uid, note);
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		getServiceNote(defaultSecurityContext, container.uid).update(uid, note);

		Message<JsonObject> message = updatedMessageChecker.shouldSuccess();
		assertNotNull(message);
	}

	@Test
	public void testUpdateWithItem() throws Exception {
		setGlobalExternalUrl();
		VertxEventChecker<JsonObject> updatedMessageChecker = new VertxEventChecker<>(NoteHookAddress.UPDATED);

		VNote note = defaultVNote();
		String uid = "test_" + System.nanoTime();
		getServiceNote(defaultSecurityContext, container.uid).create(uid, note);
		Item item = itemStore.get(uid);
		assertNotNull(item);

		VNote vnote = vnoteStore.get(item);
		ItemValue<VNote> noteItem = ItemValue.create(item, vnote);
		noteItem.version += 10;
		noteItem.updated = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2021-07-26 11:48:00");

		// test anonymous
		try {
			getServiceNote(SecurityContext.ANONYMOUS, container.uid).updateWithItem(noteItem);
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		getServiceNote(defaultSecurityContext, container.uid).updateWithItem(noteItem);

		Item updated = itemStore.get(uid);
		assertItemEquals(noteItem.item(), updated);

		Message<JsonObject> message = updatedMessageChecker.shouldSuccess();
		assertNotNull(message);
	}

	@Test
	public void testDelete() throws ServerFault {

		VertxEventChecker<JsonObject> deletedMessageChecker = new VertxEventChecker<>(NoteHookAddress.DELETED);

		VNote note = defaultVNote();

		String uid = "test_" + System.nanoTime();
		getServiceNote(defaultSecurityContext, container.uid).create(uid, note);

		// test anonymous
		try {
			getServiceNote(SecurityContext.ANONYMOUS, container.uid).delete(uid);
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		getServiceNote(defaultSecurityContext, container.uid).delete(uid);

		ItemValue<VNote> vnote = getServiceNote(defaultSecurityContext, container.uid).getComplete(uid);
		assertNull(vnote);

		Message<JsonObject> message = deletedMessageChecker.shouldSuccess();
		assertNotNull(message);
	}

	@Test
	public void testReset() throws ServerFault {

		List<ItemValue<VNote>> vnotes = getServiceNote(defaultSecurityContext, container.uid).all();
		assertNotNull(vnotes);
		assertEquals(0, vnotes.size());

		VNote note = defaultVNote();
		String uid = "note-one";
		getServiceNote(defaultSecurityContext, container.uid).create(uid, note);
		note = defaultVNote();
		uid = "note-two";
		getServiceNote(defaultSecurityContext, container.uid).create(uid, note);
		note = defaultVNote();
		uid = "note-three";
		getServiceNote(defaultSecurityContext, container.uid).create(uid, note);
		// test anonymous
		try {
			getServiceNote(SecurityContext.ANONYMOUS, container.uid).reset();
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		getServiceNote(defaultSecurityContext, container.uid).reset();

		vnotes = getServiceNote(defaultSecurityContext, container.uid).all();
		assertNotNull(vnotes);
		assertEquals(0, vnotes.size());
	}

	@Test
	public void testGetComplete() throws ServerFault {
		VNote note = defaultVNote();
		String uid = "test_" + System.nanoTime();
		getServiceNote(defaultSecurityContext, container.uid).create(uid, note);

		// test anonymous
		try {
			getServiceNote(SecurityContext.ANONYMOUS, container.uid).getComplete(uid);
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		ItemValue<VNote> vnote = getServiceNote(defaultSecurityContext, container.uid).getComplete(uid);
		assertNotNull(vnote);

		assertNotNull(note.categories);
		assertEquals(2, note.categories.size());

		assertEquals(uid, vnote.uid);
		vnote = getServiceNote(defaultSecurityContext, container.uid).getComplete("nonExistant");
		assertNull(vnote);
	}

	@Test
	public void testAll() throws ServerFault {

		List<ItemValue<VNote>> vnotes = getServiceNote(defaultSecurityContext, container.uid).all();
		assertNotNull(vnotes);
		assertEquals(0, vnotes.size());

		VNote note = defaultVNote();
		String uid = "note-one";
		getServiceNote(defaultSecurityContext, container.uid).create(uid, note);
		note = defaultVNote();
		uid = "note-two";
		getServiceNote(defaultSecurityContext, container.uid).create(uid, note);
		note = defaultVNote();
		uid = "note-three";
		getServiceNote(defaultSecurityContext, container.uid).create(uid, note);
		// test anonymous
		try {
			getServiceNote(SecurityContext.ANONYMOUS, container.uid).all();
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		vnotes = getServiceNote(defaultSecurityContext, container.uid).all();
		assertNotNull(vnotes);

		assertEquals(3, vnotes.size());
	}

	@Test
	public void testMultipleGet() throws ServerFault {
		VNote note = defaultVNote();
		String uid = UUID.randomUUID().toString();
		getServiceNote(defaultSecurityContext, container.uid).create(uid, note);

		note = defaultVNote();
		String uid2 = UUID.randomUUID().toString();
		getServiceNote(defaultSecurityContext, container.uid).create(uid2, note);

		List<ItemValue<VNote>> items = getServiceNote(defaultSecurityContext, container.uid)
				.multipleGet(Arrays.asList(uid, uid2));
		assertNotNull(items);
		assertEquals(2, items.size());

		items = getServiceNote(defaultSecurityContext, container.uid).multipleGet(Arrays.asList("nonExistant"));

		assertNotNull(items);
		assertEquals(0, items.size());

		try {
			getServiceNote(SecurityContext.ANONYMOUS, container.uid).multipleGet(Arrays.asList(uid, uid2));
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}
	}

	@Test
	public void testMultipleGetById() throws ServerFault {
		VNote note = defaultVNote();
		String uid = UUID.randomUUID().toString();
		getServiceNote(defaultSecurityContext, container.uid).create(uid, note);

		note = defaultVNote();
		String uid2 = UUID.randomUUID().toString();
		getServiceNote(defaultSecurityContext, container.uid).create(uid2, note);

		List<ItemValue<VNote>> items = getServiceNote(defaultSecurityContext, container.uid)
				.multipleGet(Arrays.asList(uid, uid2));
		assertNotNull(items);
		assertEquals(2, items.size());

		try {
			getServiceNote(SecurityContext.ANONYMOUS, container.uid)
					.multipleGetById(Arrays.asList(items.get(0).internalId, items.get(1).internalId));
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		items = getServiceNote(defaultSecurityContext, container.uid)
				.multipleGetById(Arrays.asList(items.get(0).internalId, items.get(1).internalId));
		assertNotNull(items);
		assertEquals(2, items.size());

		items = getServiceNote(defaultSecurityContext, container.uid).multipleGetById(Arrays.asList(9876543L, 34567L));
		assertNotNull(items);
		assertEquals(0, items.size());

	}

	@Test
	public void testMultipleDeleteById() throws ServerFault {
		VNote note = defaultVNote();
		String uid = UUID.randomUUID().toString();
		getServiceNote(defaultSecurityContext, container.uid).create(uid, note);

		note = defaultVNote();
		String uid2 = UUID.randomUUID().toString();
		getServiceNote(defaultSecurityContext, container.uid).create(uid2, note);

		List<ItemValue<VNote>> items = getServiceNote(defaultSecurityContext, container.uid)
				.multipleGet(Arrays.asList(uid, uid2));
		assertNotNull(items);
		assertEquals(2, items.size());

		try {
			getServiceNote(SecurityContext.ANONYMOUS, container.uid)
					.multipleDeleteById(Arrays.asList(items.get(0).internalId, items.get(1).internalId));
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		getServiceNote(defaultSecurityContext, container.uid)
				.multipleDeleteById(Arrays.asList(items.get(0).internalId, items.get(1).internalId));

		items = getServiceNote(defaultSecurityContext, container.uid)
				.multipleGet(Arrays.asList(items.get(0).uid, items.get(1).uid));
		assertNotNull(items);
		assertEquals(0, items.size());
	}

	@Test
	public void testAllUids() throws ServerFault {

		List<ItemValue<VNote>> vnotes = getServiceNote(defaultSecurityContext, container.uid).all();
		assertNotNull(vnotes);
		assertEquals(0, vnotes.size());

		VNote note = defaultVNote();
		String uid = "note-one";
		getServiceNote(defaultSecurityContext, container.uid).create(uid, note);
		note = defaultVNote();
		uid = "note-two";
		getServiceNote(defaultSecurityContext, container.uid).create(uid, note);
		note = defaultVNote();
		uid = "note-three";
		getServiceNote(defaultSecurityContext, container.uid).create(uid, note);
		// test anonymous
		try {
			getServiceNote(SecurityContext.ANONYMOUS, container.uid).allUids();
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		List<String> uids = getServiceNote(defaultSecurityContext, container.uid).allUids();
		assertNotNull(uids);
		assertEquals(3, uids.size());
		assertTrue(uids.contains("note-one"));
		assertTrue(uids.contains("note-two"));
		assertTrue(uids.contains("note-three"));
	}

	@Test
	public void testCreateImproperVNote() throws ServerFault {
		VNote vnote = null;
		String uid = "test_" + System.nanoTime();

		try {
			getServiceNote(defaultSecurityContext, container.uid).create(uid, vnote);
			fail();
		} catch (ServerFault e) {
		}
	}

	@Test
	public void testChangelog() throws ServerFault {

		getServiceNote(defaultSecurityContext, container.uid).create("test1", defaultVNote());
		getServiceNote(defaultSecurityContext, container.uid).create("test2", defaultVNote());
		getServiceNote(defaultSecurityContext, container.uid).delete("test1");
		getServiceNote(defaultSecurityContext, container.uid).update("test2", defaultVNote());

		// begin tests
		ContainerChangelog log = getServiceNote(defaultSecurityContext, container.uid).containerChangelog(null);

		assertEquals(4, log.entries.size());

		for (ChangeLogEntry entry : log.entries) {
			System.out.println(entry.version);
		}
		log = getServiceNote(defaultSecurityContext, container.uid).containerChangelog(log.entries.get(0).version);
		assertEquals(3, log.entries.size());
	}

	@Test
	public void testChangeset() throws ServerFault {

		getServiceNote(defaultSecurityContext, container.uid).create("test1", defaultVNote());
		getServiceNote(defaultSecurityContext, container.uid).create("test2", defaultVNote());
		getServiceNote(defaultSecurityContext, container.uid).delete("test1");
		getServiceNote(defaultSecurityContext, container.uid).update("test2", defaultVNote());

		// begin tests
		ContainerChangeset<String> changeset = getServiceNote(defaultSecurityContext, container.uid).changeset(null);

		assertEquals(1, changeset.created.size());
		assertEquals("test2", changeset.created.get(0));

		assertEquals(0, changeset.deleted.size());

		getServiceNote(defaultSecurityContext, container.uid).delete("test2");
		changeset = getServiceNote(defaultSecurityContext, container.uid).changeset(changeset.version);

		assertEquals(0, changeset.created.size());
		assertEquals(0, changeset.updated.size());
		assertEquals(1, changeset.deleted.size());
		assertEquals("test2", changeset.deleted.get(0));
	}

	@Test
	public void testChangesetById() throws ServerFault {

		getServiceNote(defaultSecurityContext, container.uid).create("test1", defaultVNote());
		getServiceNote(defaultSecurityContext, container.uid).create("test2", defaultVNote());
		getServiceNote(defaultSecurityContext, container.uid).delete("test1");
		getServiceNote(defaultSecurityContext, container.uid).update("test2", defaultVNote());

		// begin tests
		ContainerChangeset<Long> changeset = getServiceNote(defaultSecurityContext, container.uid).changesetById(null);
		assertEquals(1, changeset.created.size());
		Long id = changeset.created.get(0);
		assertEquals(0, changeset.deleted.size());

		getServiceNote(defaultSecurityContext, container.uid).delete("test2");
		changeset = getServiceNote(defaultSecurityContext, container.uid).changesetById(changeset.version);

		assertEquals(0, changeset.created.size());
		assertEquals(0, changeset.updated.size());
		assertEquals(1, changeset.deleted.size());
		assertEquals(id, changeset.deleted.get(0));
	}

	@Test
	public void testItemChangelog() throws ServerFault {

		getServiceNote(defaultSecurityContext, container.uid).create("test1", defaultVNote());
		getServiceNote(defaultSecurityContext, container.uid).update("test1", defaultVNote());
		getServiceNote(defaultSecurityContext, container.uid).create("test2", defaultVNote());
		getServiceNote(defaultSecurityContext, container.uid).delete("test1");
		getServiceNote(defaultSecurityContext, container.uid).update("test2", defaultVNote());

		ItemChangelog itemChangeLog = getServiceNote(defaultSecurityContext, container.uid).itemChangelog("test1", 0L);
		assertEquals(3, itemChangeLog.entries.size());
		assertEquals(ChangeLogEntry.Type.Created, itemChangeLog.entries.get(0).type);
		assertEquals(ChangeLogEntry.Type.Updated, itemChangeLog.entries.get(1).type);
		assertEquals(ChangeLogEntry.Type.Deleted, itemChangeLog.entries.get(2).type);

		itemChangeLog = getServiceNote(defaultSecurityContext, container.uid).itemChangelog("test2", 0L);
		assertEquals(2, itemChangeLog.entries.size());
		assertEquals(ChangeLogEntry.Type.Created, itemChangeLog.entries.get(0).type);
		assertEquals(ChangeLogEntry.Type.Updated, itemChangeLog.entries.get(1).type);

	}

	@Test
	public void testNoCategories() throws ServerFault {
		VNote note = defaultVNote();
		note.categories = null;
		String uid = "test_" + System.nanoTime();

		getServiceNote(defaultSecurityContext, container.uid).create(uid, note);

		ItemValue<VNote> vnote = getServiceNote(defaultSecurityContext, container.uid).getComplete(uid);
		assertNotNull(vnote);
	}

	@Test
	public void testDeleteUnknownEvent() throws ServerFault {
		try {
			getServiceNote(defaultSecurityContext, container.uid).delete(UUID.randomUUID().toString());
		} catch (ServerFault sf) {
			fail();
		}
	}

	@Test
	public void testUpdateUnknownEvent() throws ServerFault {
		try {
			getServiceNote(defaultSecurityContext, container.uid).update(UUID.randomUUID().toString(), defaultVNote());
			fail();
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.NOT_FOUND, sf.getCode());
		}
	}

	@Test
	public void testUpdateColor() throws ServerFault {
		VNote note = defaultVNote();
		String uid = "test_" + System.nanoTime();
		getServiceNote(defaultSecurityContext, container.uid).create(uid, note);
		ItemValue<VNote> item = getServiceNote(defaultSecurityContext, container.uid).getComplete(uid);
		assertNotNull(item);

		note = item.value;
		assertEquals(VNote.Color.YELLOW, note.color);

		note.color = VNote.Color.BLUE;

		getServiceNote(defaultSecurityContext, container.uid).update(uid, note);

		item = getServiceNote(defaultSecurityContext, container.uid).getComplete(uid);
		note = item.value;
		assertEquals(VNote.Color.BLUE, note.color);
	}

	@Test
	public void testUpdateTag() throws ServerFault {
		VNote note = defaultVNote();
		note.categories = new ArrayList<TagRef>(1);
		note.categories.add(tagRef1);
		String uid = "test_" + System.nanoTime();
		getServiceNote(defaultSecurityContext, container.uid).create(uid, note);
		ItemValue<VNote> item = getServiceNote(defaultSecurityContext, container.uid).getComplete(uid);
		assertNotNull(item);

		note = item.value;
		assertNotNull(note.categories);
		assertEquals(1, note.categories.size());

		note.categories.add(tagRef2);

		getServiceNote(defaultSecurityContext, container.uid).update(uid, note);

		item = getServiceNote(defaultSecurityContext, container.uid).getComplete(uid);
		note = item.value;
		assertEquals(2, note.categories.size());
	}

	@Test
	public void testOnTagChanged() throws ServerFault, SQLException {
		VNote note = defaultVNote();
		String uid = "test_" + System.nanoTime();
		getServiceNote(defaultSecurityContext, container.uid).create(uid, note);

		ChangelogStore changelogStore = new ChangelogStore(dataDataSource, container);
		ContainerChangeset<String> changeset = changelogStore.changeset(0, Long.MAX_VALUE);
		long version = changeset.version;

		VertxEventChecker<JsonObject> changedMessageChecker = new VertxEventChecker<>(
				NoteHookAddress.getChangedEventAddress(container.uid));

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
	public void testSearch_onSubject() throws ServerFault {
		VNote todo = defaultVNote();
		todo.subject = "yay";
		String uid = "test_" + System.nanoTime();
		getServiceNote(defaultSecurityContext, container.uid).create(uid, todo);
		refreshIndex();

		VNoteQuery query = VNoteQuery.create("value.subject:yay");

		// test anonymous
		try {
			getServiceNote(SecurityContext.ANONYMOUS, container.uid).search(query);
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		ListResult<ItemValue<VNote>> res = getServiceNote(defaultSecurityContext, container.uid).search(query);
		assertEquals(1, res.values.size());
		ItemValue<VNote> itemValue = res.values.get(0);
		VNote found = itemValue.value;
		assertEquals("yay", found.subject);

		query = VNoteQuery.create("value.subject:what?");
		res = getServiceNote(defaultSecurityContext, container.uid).search(query);
		assertEquals(0, res.values.size());
	}

	@Test
	public void testSearch_onBody() throws ServerFault {
		VNote todo = defaultVNote();
		todo.body = "yay";
		String uid = "test_" + System.nanoTime();
		getServiceNote(defaultSecurityContext, container.uid).create(uid, todo);
		refreshIndex();

		VNoteQuery query = VNoteQuery.create("value.body:yay");

		// test anonymous
		try {
			getServiceNote(SecurityContext.ANONYMOUS, container.uid).search(query);
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		ListResult<ItemValue<VNote>> res = getServiceNote(defaultSecurityContext, container.uid).search(query);
		assertEquals(1, res.values.size());
		ItemValue<VNote> itemValue = res.values.get(0);
		VNote found = itemValue.value;
		assertEquals("yay", found.body);

		query = VNoteQuery.create("value.body:what?");
		res = getServiceNote(defaultSecurityContext, container.uid).search(query);
		assertEquals(0, res.values.size());
	}

	@Test
	public void testSearch_onColor() throws ServerFault {
		VNote todo = defaultVNote();
		String uid = "test_" + System.nanoTime();
		getServiceNote(defaultSecurityContext, container.uid).create(uid, todo);
		refreshIndex();

		VNoteQuery query = VNoteQuery.create("value.color:YELLOW");

		// test anonymous
		try {
			getServiceNote(SecurityContext.ANONYMOUS, container.uid).search(query);
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		ListResult<ItemValue<VNote>> res = getServiceNote(defaultSecurityContext, container.uid).search(query);
		assertEquals(1, res.values.size());
		ItemValue<VNote> itemValue = res.values.get(0);
		VNote found = itemValue.value;
		assertEquals(Color.YELLOW, found.color);

		query = VNoteQuery.create("value.color:GREEN");
		res = getServiceNote(defaultSecurityContext, container.uid).search(query);
		assertEquals(0, res.values.size());
	}

	@Override
	protected INote getServiceNote(SecurityContext context, String containerUid) throws ServerFault {
		return ServerSideServiceProvider.getProvider(context).instance(INote.class, containerUid);
	}

	@Override
	protected INoteIndexMgmt getServiceNoteMgmt(SecurityContext context) throws ServerFault {
		return ServerSideServiceProvider.getProvider(context).instance(INoteIndexMgmt.class, container.uid);
	}

	@Override
	protected INotes getServiceNotes(SecurityContext context) throws ServerFault {
		return null;
	}

	private ItemValue<VNote> defaultVNoteItem(long id) throws ParseException {
		Item item = new Item();
		item.id = id;
		item.uid = "test_" + System.nanoTime();
		item.externalId = "externalId" + System.nanoTime();
		item.displayName = "test";
		item.created = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2021-07-26 11:44:21");
		item.updated = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2021-07-26 11:46:00");
		item.version = 17;
		return ItemValue.create(item, defaultVNote());
	}

	private static <T> void assertItemEquals(Item expected, Item actual) {
		assertNotNull(actual);
		assertEquals(expected.id, actual.id);
		assertEquals(expected.uid, actual.uid);
		assertEquals(expected.externalId, actual.externalId);
		assertEquals(expected.updated, actual.updated);
		assertEquals(expected.version, actual.version);
	}
}
