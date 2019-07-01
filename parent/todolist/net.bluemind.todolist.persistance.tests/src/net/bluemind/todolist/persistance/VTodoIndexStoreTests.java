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
package net.bluemind.todolist.persistance;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.core.api.ListResult;
import net.bluemind.core.api.date.BmDateTime.Precision;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.persistance.ContainerStore;
import net.bluemind.core.container.persistance.ItemStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.icalendar.api.ICalendarElement.Status;
import net.bluemind.todolist.api.VTodo;
import net.bluemind.todolist.api.VTodoQuery;

public class VTodoIndexStoreTests {

	private Client client;
	private Container container;
	private ItemStore itemStore;
	private VTodoIndexStore indexStore;

	@Before
	public void before() throws Exception {
		ElasticsearchTestHelper.getInstance().beforeTest();
		JdbcTestHelper.getInstance().beforeTest();

		SecurityContext securityContext = SecurityContext.ANONYMOUS;

		ContainerStore containerHome = new ContainerStore(JdbcTestHelper.getInstance().getDataSource(),
				securityContext);
		String containerId = "test" + System.nanoTime();
		container = Container.create(containerId, "test", "test", "me", true);
		container = containerHome.create(container);

		assertNotNull(container);

		itemStore = new ItemStore(JdbcTestHelper.getInstance().getDataSource(), container, securityContext);

		client = ElasticsearchTestHelper.getInstance().getClient();

		indexStore = new VTodoIndexStore(client, container);

	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testCreate() throws SQLException {
		VTodo todo = defaultVTodo();
		String uid = "test" + System.nanoTime();
		todo.uid = uid;
		Item item = Item.create(uid, UUID.randomUUID().toString());
		itemStore.create(item);

		indexStore.create(item.uid, todo);
		indexStore.refresh();

		SearchResponse resp = client.prepareSearch(VTodoIndexStore.VTODO_INDEX).setTypes(VTodoIndexStore.VTODO_TYPE)
				.setQuery(QueryBuilders.termQuery("uid", item.uid)).execute().actionGet();
		assertEquals(1, resp.getHits().getTotalHits());
		SearchHit hit = resp.getHits().getAt(0);
		Map<String, Object> source = hit.getSourceAsMap();
		assertEquals(uid, source.get("uid"));
		assertEquals(container.uid, source.get("containerUid"));

		@SuppressWarnings("unchecked")
		Map<String, String> sourceTodo = (Map<String, String>) source.get("value");

		sourceTodo.get("uid");
		assertEquals(uid, sourceTodo.get("uid"));
		assertEquals(todo.summary, sourceTodo.get("summary"));
	}

	@Test
	public void testUpdate() throws SQLException {
		VTodo todo = defaultVTodo();
		String uid = "test" + System.nanoTime();
		Item item = Item.create(uid, UUID.randomUUID().toString());
		itemStore.create(item);

		indexStore.create(item.uid, todo);
		indexStore.refresh();

		SearchResponse resp = client.prepareSearch(VTodoIndexStore.VTODO_INDEX).setTypes(VTodoIndexStore.VTODO_TYPE)
				.setQuery(QueryBuilders.termQuery("uid", item.uid)).execute().actionGet();
		assertEquals(1, resp.getHits().getTotalHits());
		SearchHit hit = resp.getHits().getAt(0);
		Map<String, Object> source = hit.getSourceAsMap();
		assertEquals(uid, source.get("uid"));
		assertEquals(container.uid, source.get("containerUid"));

		String updatedSummary = "updated" + System.currentTimeMillis();
		todo.summary = updatedSummary;
		indexStore.update(uid, todo);
		indexStore.refresh();

		resp = client.prepareSearch(VTodoIndexStore.VTODO_INDEX).setTypes(VTodoIndexStore.VTODO_TYPE)
				.setQuery(QueryBuilders.termQuery("uid", item.uid)).execute().actionGet();
		assertEquals(1, resp.getHits().getTotalHits());
		hit = resp.getHits().getAt(0);
		source = hit.getSourceAsMap();
		assertEquals(uid, source.get("uid"));
		assertEquals(container.uid, source.get("containerUid"));

		@SuppressWarnings("unchecked")
		Map<String, String> sourceTodo = (Map<String, String>) source.get("value");
		assertEquals(updatedSummary, sourceTodo.get("summary"));

	}

	@Test
	public void testDelete() throws SQLException {
		VTodo todo = defaultVTodo();
		String uid = "test" + System.nanoTime();
		Item item = Item.create(uid, UUID.randomUUID().toString());
		itemStore.create(item);

		indexStore.create(item.uid, todo);
		indexStore.refresh();

		SearchResponse resp = client.prepareSearch(VTodoIndexStore.VTODO_INDEX).setTypes(VTodoIndexStore.VTODO_TYPE)
				.setQuery(QueryBuilders.termQuery("uid", item.uid)).execute().actionGet();
		assertEquals(1, resp.getHits().getTotalHits());

		SearchHit hit = resp.getHits().getAt(0);
		assertNotNull(hit);

		indexStore.delete(item.uid);
		indexStore.refresh();
		resp = client.prepareSearch(VTodoIndexStore.VTODO_INDEX).setTypes(VTodoIndexStore.VTODO_TYPE)
				.setQuery(QueryBuilders.termQuery("uid", item.uid)).execute().actionGet();

		assertEquals(0, resp.getHits().getTotalHits());
	}

	@Test
	public void testDeleteAll() throws SQLException {
		VTodo todo = defaultVTodo();
		String uid = "test" + System.nanoTime();
		Item item = Item.create(uid, UUID.randomUUID().toString());
		itemStore.create(item);

		indexStore.create(item.uid, todo);
		indexStore.refresh();

		VTodo todo2 = defaultVTodo();
		String uid2 = "test" + System.nanoTime();
		Item item2 = Item.create(uid2, UUID.randomUUID().toString());
		itemStore.create(item2);

		indexStore.create(item2.uid, todo2);
		indexStore.refresh();

		SearchResponse resp = client.prepareSearch(VTodoIndexStore.VTODO_INDEX).setTypes(VTodoIndexStore.VTODO_TYPE)
				.setQuery(QueryBuilders.termQuery("containerUid", container.uid)).execute().actionGet();
		assertEquals(2, resp.getHits().getTotalHits());

		indexStore.deleteAll();
		indexStore.refresh();

		resp = client.prepareSearch(VTodoIndexStore.VTODO_INDEX).setTypes(VTodoIndexStore.VTODO_TYPE)
				.setQuery(QueryBuilders.termQuery("containerUid", container.uid)).execute().actionGet();
		assertEquals(0, resp.getHits().getTotalHits());
	}

	@Test
	public void testSearch() throws SQLException {
		VTodo todo = defaultVTodo();
		todo.summary = "Yellow Summary";
		String uid = "test" + System.nanoTime();
		Item item = Item.create(uid, UUID.randomUUID().toString());
		itemStore.create(item);

		indexStore.create(item.uid, todo);
		indexStore.refresh();

		ListResult<String> res = indexStore.search(VTodoQuery.create("value.summary:Yellow"));

		assertEquals(1, res.values.size());
		assertEquals(uid, res.values.get(0));

		res = indexStore.search(VTodoQuery.create("value.summary:Diamonds"));
		assertEquals(0, res.values.size());

		VTodo todo2 = defaultVTodo();
		String uid2 = "test" + System.nanoTime();
		Item item2 = Item.create(uid2, UUID.randomUUID().toString());
		itemStore.create(item2);

		indexStore.create(item2.uid, todo2);
		indexStore.refresh();

		res = indexStore.search(VTodoQuery.create("value.location:Toulouse"));
		assertEquals(2, res.values.size());
		assertTrue(res.values.contains(uid));
		assertTrue(res.values.contains(uid2));
	}

	@Test
	public void testSearchByOrganizer() throws SQLException {
		VTodo todo = defaultVTodo();

		todo.organizer = new VTodo.Organizer("Mehdi Rande", "mehdi@bm.lan");

		String uid = "test" + System.nanoTime();
		Item item = Item.create(uid, UUID.randomUUID().toString());
		itemStore.create(item);

		indexStore.create(item.uid, todo);
		indexStore.refresh();

		ListResult<String> res = indexStore.search(VTodoQuery.create("value.organizer.mailto:mehdi@bm.lan"));

		assertEquals(1, res.values.size());
		assertEquals(uid, res.values.get(0));

		res = indexStore.search(VTodoQuery.create("value.organizer.commonName:Mehdi"));
		assertEquals(1, res.values.size());
		assertEquals(uid, res.values.get(0));

		res = indexStore.search(VTodoQuery.create("value.organizer.commonName:Kevin"));
		assertEquals(0, res.values.size());
	}

	@Test
	public void testNullOrganizer() throws SQLException {
		VTodo todo = defaultVTodo();
		todo.summary = "testNullOrganizer";
		todo.organizer = null;
		String uid = "test" + System.nanoTime();
		Item item = Item.create(uid, UUID.randomUUID().toString());
		itemStore.create(item);

		indexStore.create(item.uid, todo);
		indexStore.refresh();

		ListResult<String> res = indexStore.search(VTodoQuery.create("value.organizer.mailto:John"));
		assertEquals(0, res.values.size());

		res = indexStore.search(VTodoQuery.create("value.summary:testNullOrganizer"));
		assertEquals(1, res.values.size());
		assertEquals(uid, res.values.get(0));
	}

	@Test
	public void testSearchByDateInterval() throws SQLException {
		VTodo todo = defaultVTodo();
		todo.dtstart = BmDateTimeWrapper.create(new DateTime(1979, 2, 13, 0, 0, 0), Precision.Date);
		todo.due = BmDateTimeWrapper.create(new DateTime(1979, 2, 15, 0, 0, 0), Precision.Date);
		String uid = "test_" + System.nanoTime();
		Item item = Item.create(uid, UUID.randomUUID().toString());
		itemStore.create(item);
		indexStore.create(item.uid, todo);
		indexStore.refresh();

		DateTime from = new DateTime(1979, 2, 1, 0, 0, 0);
		DateTime to = new DateTime(1979, 3, 1, 0, 0, 0);
		VTodoQuery query = VTodoQuery.create(BmDateTimeWrapper.create(from, Precision.Date),
				BmDateTimeWrapper.create(to, Precision.Date));
		ListResult<String> res = indexStore.search(query);
		assertEquals(1, res.values.size());
		assertEquals(uid, res.values.get(0));
		// create an todo not in search range
		VTodo todo2 = defaultVTodo();
		todo2.dtstart = BmDateTimeWrapper.create(new DateTime(1976, 6, 16, 0, 0, 0), Precision.Date);
		todo2.due = BmDateTimeWrapper.create(new DateTime(1976, 2, 15, 0, 0, 0), Precision.Date);

		String uid2 = "test_" + System.nanoTime();
		Item item2 = Item.create(uid2, UUID.randomUUID().toString());
		itemStore.create(item2);
		indexStore.create(item2.uid, todo2);
		indexStore.refresh();

		res = indexStore.search(query);
		assertEquals(1, res.values.size());
		assertEquals(uid, res.values.get(0));

		// create an todo in search range
		VTodo todo3 = defaultVTodo();
		todo3.due = BmDateTimeWrapper.create(new DateTime(1979, 2, 22, 0, 0, 0), Precision.Date);
		String uid3 = "test_" + System.nanoTime();
		Item item3 = Item.create(uid3, UUID.randomUUID().toString());
		itemStore.create(item3);
		indexStore.create(item3.uid, todo3);
		indexStore.refresh();

		res = indexStore.search(query);
		assertEquals(2, res.values.size());

		boolean found1 = false;
		boolean found3 = false;
		for (String s : res.values) {
			if (uid.equals(s)) {
				found1 = true;
			}
			if (uid3.equals(s)) {
				found3 = true;
			}
		}

		assertTrue(found1);
		assertTrue(found3);
	}

	@Test
	public void testSearchRRule() throws SQLException {
		VTodo todo = defaultVTodo();
		todo.dtstart = BmDateTimeWrapper.create(new DateTime(1979, 2, 13, 0, 0, 0), Precision.Date);

		VTodo.RRule rrule = new VTodo.RRule();
		rrule.frequency = VTodo.RRule.Frequency.YEARLY;
		rrule.until = BmDateTimeWrapper.create(new DateTime(2079, 2, 13, 0, 0, 0), Precision.Date);

		todo.rrule = rrule;

		String uid = "test_" + System.nanoTime();
		Item item = Item.create(uid, UUID.randomUUID().toString());
		itemStore.create(item);
		indexStore.create(item.uid, todo);
		indexStore.refresh();

		ListResult<String> res = indexStore
				.search(VTodoQuery.create("value.rrule.until.iso8601:" + "\"" + rrule.until.iso8601 + "\""));
		assertEquals(1, res.values.size());

		DateTime from = new DateTime(1979, 2, 12, 0, 0, 0);
		DateTime to = new DateTime(1979, 2, 14, 0, 0, 0);
		VTodoQuery query = VTodoQuery.create(BmDateTimeWrapper.create(from, Precision.Date),
				BmDateTimeWrapper.create(to, Precision.Date));
		res = indexStore.search(query);
		assertEquals(1, res.values.size());
		assertEquals(uid, res.values.get(0));

		from = new DateTime(1979, 2, 14, 0, 0, 0);
		to = new DateTime(1979, 2, 15, 0, 0, 0);
		query = VTodoQuery.create(BmDateTimeWrapper.create(from, Precision.Date),
				BmDateTimeWrapper.create(to, Precision.Date));
		res = indexStore.search(query);
		assertEquals(1, res.values.size()); // not 0 because of rrule.until

		from = new DateTime(2014, 2, 12, 0, 0, 0);
		to = new DateTime(2014, 2, 14, 0, 0, 0);
		query = VTodoQuery.create(BmDateTimeWrapper.create(from, Precision.Date),
				BmDateTimeWrapper.create(to, Precision.Date));
		res = indexStore.search(query);
		assertEquals(1, res.values.size());
		assertEquals(uid, res.values.get(0));
	}

	private VTodo defaultVTodo() {
		VTodo todo = new VTodo();
		todo.uid = UUID.randomUUID().toString();
		DateTime now = new DateTime();
		todo.dtstart = BmDateTimeWrapper.create(now, Precision.DateTime);
		todo.due = BmDateTimeWrapper.create(now.plusMonths(1), Precision.DateTime);
		todo.summary = "Todo " + System.currentTimeMillis();
		todo.location = "Toulouse";
		todo.description = "Lorem ipsum";
		todo.classification = VTodo.Classification.Private;
		todo.status = Status.NeedsAction;
		todo.percent = 25;
		todo.priority = 42;

		todo.organizer = new VTodo.Organizer();
		todo.organizer.uri = UUID.randomUUID().toString();

		return todo;
	}

}
