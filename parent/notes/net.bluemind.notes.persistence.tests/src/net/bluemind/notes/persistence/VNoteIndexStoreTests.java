/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.notes.persistence;

import static net.bluemind.notes.persistence.VNoteIndexStore.VNOTE_READ_ALIAS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.ItemStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.notes.api.VNote;
import net.bluemind.notes.api.VNoteQuery;
import net.bluemind.notes.persistence.VNoteIndexStore.IndexableVNote;

public class VNoteIndexStoreTests extends AbstractStoreTests {

	private ElasticsearchClient client;
	private VNoteIndexStore indexStore;
	private Container container;
	private ItemStore itemStore;

	@Before
	public void before() throws Exception {
		ElasticsearchTestHelper.getInstance().beforeTest();
		JdbcTestHelper.getInstance().beforeTest();

		SecurityContext securityContext = SecurityContext.ANONYMOUS;

		ContainerStore containerHome = new ContainerStore(null, JdbcTestHelper.getInstance().getDataSource(),
				securityContext);
		String containerId = "test" + System.nanoTime();
		container = Container.create(containerId, "test", "test", "me", true);
		container = containerHome.create(container);

		assertNotNull(container);

		itemStore = new ItemStore(JdbcTestHelper.getInstance().getDataSource(), container, securityContext);

		client = ElasticsearchTestHelper.getInstance().getClient();

		indexStore = new VNoteIndexStore(client, container, null);

	}

	@Test
	public void testCreate() throws SQLException, ElasticsearchException, IOException {
		VNote note = defaultVNote();
		String uid = "test" + System.nanoTime();
		Item item = Item.create(uid, UUID.randomUUID().toString());
		Item created = itemStore.create(item);

		indexStore.create(created, note);
		indexStore.refresh();

		SearchResponse<IndexableVNote> resp = client.search(
				s -> s.index(VNOTE_READ_ALIAS).query(q -> q.term(t -> t.field("uid").value(item.uid))),
				IndexableVNote.class);
		assertEquals(1, resp.hits().total().value());

		IndexableVNote hit = resp.hits().hits().get(0).source();
		assertEquals(uid, hit.uid);
		assertEquals(container.uid, hit.containerUid);
		assertEquals(note.subject, hit.value.subject);
	}

	@Test
	public void testUpdate() throws SQLException, ElasticsearchException, IOException {
		VNote note = defaultVNote();
		String uid = "test" + System.nanoTime();
		Item item = Item.create(uid, UUID.randomUUID().toString());
		Item created = itemStore.create(item);

		indexStore.create(created, note);
		indexStore.refresh();

		SearchResponse<IndexableVNote> resp = client.search(
				s -> s.index(VNOTE_READ_ALIAS).query(q -> q.term(t -> t.field("uid").value(item.uid))),
				IndexableVNote.class);
		assertEquals(1, resp.hits().total().value());

		IndexableVNote hit = resp.hits().hits().get(0).source();
		assertEquals(uid, hit.uid);
		assertEquals(container.uid, hit.containerUid);

		String updatedSubject = "updated" + System.currentTimeMillis();
		note.subject = updatedSubject;
		indexStore.update(created, note);
		indexStore.refresh();

		resp = client.search(
				s -> s.index(VNOTE_READ_ALIAS).query(q -> q.term(t -> t.field("uid").value(item.uid))),
				IndexableVNote.class);
		assertEquals(1, resp.hits().total().value());

		IndexableVNote updated = resp.hits().hits().get(0).source();
		assertEquals(uid, updated.uid);
		assertEquals(container.uid, updated.containerUid);
		assertEquals(updatedSubject, updated.value.subject);

	}

	@Test
	public void testDelete() throws SQLException, ElasticsearchException, IOException {
		VNote note = defaultVNote();
		String uid = "test" + System.nanoTime();
		Item item = Item.create(uid, UUID.randomUUID().toString());
		Item created = itemStore.create(item);

		indexStore.create(created, note);
		indexStore.refresh();

		SearchResponse<IndexableVNote> resp = client.search(
				s -> s.index(VNOTE_READ_ALIAS).query(q -> q.term(t -> t.field("uid").value(item.uid))),
				IndexableVNote.class);
		assertEquals(1, resp.hits().total().value());
		IndexableVNote hit = resp.hits().hits().get(0).source();
		assertNotNull(hit);

		indexStore.delete(created.id);
		indexStore.refresh();

		resp = client.search(
				s -> s.index(VNOTE_READ_ALIAS).query(q -> q.term(t -> t.field("uid").value(item.uid))),
				IndexableVNote.class);
		assertEquals(0, resp.hits().total().value());
	}

	@Test
	public void testDeleteAll() throws SQLException, ElasticsearchException, IOException {
		VNote note = defaultVNote();
		String uid = "test" + System.nanoTime();
		Item item = Item.create(uid, UUID.randomUUID().toString());
		Item created = itemStore.create(item);

		indexStore.create(created, note);
		indexStore.refresh();

		VNote note2 = defaultVNote();
		String uid2 = "test" + System.nanoTime();
		Item item2 = Item.create(uid2, UUID.randomUUID().toString());
		Item created2 = itemStore.create(item2);

		indexStore.create(created2, note2);
		indexStore.refresh();

		SearchResponse<IndexableVNote> resp = client.search(s -> s.index(VNOTE_READ_ALIAS)
				.query(q -> q.term(t -> t.field("containerUid").value(container.uid))), IndexableVNote.class);
		assertEquals(2, resp.hits().total().value());

		indexStore.deleteAll();
		indexStore.refresh();

		resp = client.search(s -> s.index(VNOTE_READ_ALIAS)
				.query(q -> q.term(t -> t.field("containerUid").value(container.uid))), IndexableVNote.class);
		assertEquals(0, resp.hits().total().value());
	}

	@Test
	public void testSearch_onSubject() throws SQLException {
		VNote note = defaultVNote();
		note.subject = "Yellow Subject Note";
		String uid = "test" + System.nanoTime();
		Item item = Item.create(uid, UUID.randomUUID().toString());
		Item created = itemStore.create(item);

		indexStore.create(created, note);
		indexStore.refresh();

		ListResult<String> res = indexStore.search(VNoteQuery.create("value.subject:Yellow"));

		assertEquals(1, res.values.size());
		assertEquals(uid, res.values.get(0));

		res = indexStore.search(VNoteQuery.create("value.subject:Diamonds"));
		assertEquals(0, res.values.size());

		VNote note2 = defaultVNote();
		note2.subject = "Yellow Subject Note 2";
		String uid2 = "test" + System.nanoTime();
		Item item2 = Item.create(uid2, UUID.randomUUID().toString());
		Item created2 = itemStore.create(item2);

		indexStore.create(created2, note2);
		indexStore.refresh();

		res = indexStore.search(VNoteQuery.create("value.subject:Yellow"));
		assertEquals(2, res.values.size());
		assertTrue(res.values.contains(uid));
		assertTrue(res.values.contains(uid2));
	}

	@Test
	public void testSearch_onBody() throws SQLException {
		VNote note = defaultVNote();
		note.body = "Yellow Body Note";
		String uid = "test" + System.nanoTime();
		Item item = Item.create(uid, UUID.randomUUID().toString());
		Item created = itemStore.create(item);

		indexStore.create(created, note);
		indexStore.refresh();

		ListResult<String> res = indexStore.search(VNoteQuery.create("value.body:Yellow"));

		assertEquals(1, res.values.size());
		assertEquals(uid, res.values.get(0));

		res = indexStore.search(VNoteQuery.create("value.body:Diamonds"));
		assertEquals(0, res.values.size());

		VNote note2 = defaultVNote();
		note2.body = "Yellow Body Note 2";
		String uid2 = "test" + System.nanoTime();
		Item item2 = Item.create(uid2, UUID.randomUUID().toString());
		Item created2 = itemStore.create(item2);

		indexStore.create(created2, note2);
		indexStore.refresh();

		res = indexStore.search(VNoteQuery.create("value.body:Yellow"));
		assertEquals(2, res.values.size());
		assertTrue(res.values.contains(uid));
		assertTrue(res.values.contains(uid2));
	}

	@Test
	public void testUpdates() throws SQLException {
		VNote note1 = defaultVNote();
		note1.subject = "coucou";
		String uid = "test_" + System.nanoTime();
		Item item = Item.create(uid, UUID.randomUUID().toString());
		Item created = itemStore.create(item);
		indexStore.create(created, note1);

		VNote note2 = defaultVNote();
		note2.subject = "yeah";
		String uid2 = "test_" + System.nanoTime();
		Item item2 = Item.create(uid2, UUID.randomUUID().toString());
		Item created2 = itemStore.create(item2);
		indexStore.create(created2, note2);
		indexStore.refresh();

		ListResult<String> res = indexStore.search(VNoteQuery.create("value.subject:coucou"));
		assertEquals(1, res.total);

		res = indexStore.search(VNoteQuery.create("value.subject:yeah"));
		assertEquals(1, res.total);

		note1.subject = "yata";
		note2.subject = "yolo";

		indexStore.updates(Arrays.asList(ItemValue.create(created, note1), ItemValue.create(created2, note2)));
		indexStore.refresh();

		res = indexStore.search(VNoteQuery.create("value.subject:coucou"));
		assertEquals(0, res.total);
		res = indexStore.search(VNoteQuery.create("value.subject:yata"));
		assertEquals(1, res.total);

		res = indexStore.search(VNoteQuery.create("value.subject:yeah"));
		assertEquals(0, res.total);
		res = indexStore.search(VNoteQuery.create("value.subject:yolo"));
		assertEquals(1, res.total);
	}

}
