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

import static net.bluemind.calendar.persistence.VEventIndexStore.VEVENT_READ_ALIAS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.sql.SQLException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.api.VEventAttendeeQuery;
import net.bluemind.calendar.api.VEventQuery;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.calendar.persistence.VEventIndexStore.IndexableVEventSeries;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.api.date.BmDateTime.Precision;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.icalendar.api.ICalendarElement.Classification;
import net.bluemind.icalendar.api.ICalendarElement.ParticipationStatus;

public class VEventIndexStoreTests {

	private ElasticsearchClient client;
	private Container container;
	private VEventIndexStore indexStore;

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

		client = ElasticsearchTestHelper.getInstance().getClient();

		indexStore = new VEventIndexStore(client, container, null);
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testCreate() throws SQLException, ElasticsearchException, IOException {
		ItemValue<VEventSeries> event = defaultVEvent();

		indexStore.create(Item.create(event.uid, System.nanoTime()), event.value);
		indexStore.refresh();

		SearchResponse<IndexableVEventSeries> resp = client.search(
				s -> s.index(VEVENT_READ_ALIAS).query(q -> q.term(t -> t.field("uid").value(event.uid))),
				IndexableVEventSeries.class);
		assertEquals(1, resp.hits().total().value());

		IndexableVEventSeries hit = resp.hits().hits().get(0).source();
		assertEquals(event.uid, hit.uid);
		assertEquals(container.uid, hit.containerUid);

		List<VEvent> events = hit.value;
		assertEquals(1, events.size());

		VEvent esEvent = events.get(0);
		assertEquals(event.value.main.summary, esEvent.summary);
		assertEquals(event.value.main.location, esEvent.location);
	}

	@Test
	public void testUpdate() throws SQLException, ElasticsearchException, IOException {
		ItemValue<VEventSeries> event = defaultVEvent();

		Item item = Item.create(event.uid, System.nanoTime());

		indexStore.create(item, event.value);
		indexStore.refresh();

		SearchResponse<IndexableVEventSeries> resp = client.search(
				s -> s.index(VEVENT_READ_ALIAS).query(q -> q.term(t -> t.field("uid").value(event.uid))),
				IndexableVEventSeries.class);
		assertEquals(1, resp.hits().total().value());

		IndexableVEventSeries hit = resp.hits().hits().get(0).source();
		assertEquals(event.uid, hit.uid);
		assertEquals(container.uid, hit.containerUid);

		String updatedSummary = "updated" + System.currentTimeMillis();
		event.value.main.summary = updatedSummary;
		indexStore.update(item, event.value);
		indexStore.refresh();

		resp = client.search(s -> s.index(VEVENT_READ_ALIAS).query(q -> q.term(t -> t.field("uid").value(event.uid))),
				IndexableVEventSeries.class);
		assertEquals(1, resp.hits().total().value());

		IndexableVEventSeries updateHit = resp.hits().hits().get(0).source();
		assertEquals(event.uid, updateHit.uid);
		assertEquals(container.uid, updateHit.containerUid);

		assertEquals(1, updateHit.value.size());

		VEvent esEvent = updateHit.value.get(0);
		assertEquals(updatedSummary, esEvent.summary);
	}

	@Test
	public void testDelete() throws SQLException, ElasticsearchException, IOException {
		ItemValue<VEventSeries> event = defaultVEvent();

		Item item = Item.create(event.uid, System.nanoTime());
		indexStore.create(item, event.value);
		indexStore.refresh();

		SearchResponse<IndexableVEventSeries> resp = client.search(
				s -> s.index(VEVENT_READ_ALIAS).query(q -> q.term(t -> t.field("uid").value(event.uid))),
				IndexableVEventSeries.class);
		assertEquals(1, resp.hits().total().value());

		Hit<IndexableVEventSeries> hit = resp.hits().hits().get(0);
		assertNotNull(hit);

		indexStore.delete(item.id);
		indexStore.refresh();

		resp = client.search(s -> s.index(VEVENT_READ_ALIAS).query(q -> q.term(t -> t.field("uid").value(event.uid))),
				IndexableVEventSeries.class);
		assertEquals(0, resp.hits().total().value());
	}

	@Test
	public void testDeleteAll() throws SQLException, ElasticsearchException, IOException {
		ItemValue<VEventSeries> event = defaultVEvent();

		indexStore.create(Item.create(event.uid, System.nanoTime()), event.value);
		indexStore.refresh();

		ItemValue<VEventSeries> event2 = defaultVEvent();

		indexStore.create(Item.create(event2.uid, System.nanoTime()), event2.value);
		indexStore.refresh();

		SearchResponse<IndexableVEventSeries> resp = client.search(
				s -> s.index(VEVENT_READ_ALIAS).query(q -> q.term(t -> t.field("containerUid").value(container.uid))),
				IndexableVEventSeries.class);
		assertEquals(2, resp.hits().total().value());

		indexStore.deleteAll();
		indexStore.refresh();

		resp = client.search(
				s -> s.index(VEVENT_READ_ALIAS).query(q -> q.term(t -> t.field("containerUid").value(container.uid))),
				IndexableVEventSeries.class);
		assertEquals(0, resp.hits().total().value());
	}

	@Test
	public void testSearch_25000() throws Exception {
		int max = 25000;
		List<ItemValue<VEventSeries>> allEvents = new ArrayList<>();
		for (int i = 0; i < 25000; i++) {
			ItemValue<VEventSeries> event = defaultVEvent();
			event.value.main.summary = "event" + i;
			allEvents.add(ItemValue.create(Item.create(event.uid, System.nanoTime()), event.value));
		}
		indexStore.updates(allEvents);

		indexStore.refresh();

		// test with specific unique summary
		ListResult<String> res = indexStore.search(VEventQuery.create("value.summary:event11111"));
		assertEquals(1, res.total);

		// test empty result
		res = indexStore.search(VEventQuery.create("value.summary:fakeEvent"));
		assertEquals(0, res.total);

		// test more than 10000
		VEventQuery q = VEventQuery.create("value.summary:event*");
		q.size = 1000;
		q.from = 15000;
		res = indexStore.search(q);
		assertEquals(max, res.total);

		// test specific from and size = 0
		q.size = 0;
		q.from = 15000;
		res = indexStore.search(q);
		assertEquals(max, res.total);

		// test specific from and size = -1
		q.size = -1;
		q.from = 20000;
		res = indexStore.search(q);
		assertEquals(max, res.total);

		// test specific size = -1
		q.size = -1;
		q.from = 0;
		res = indexStore.search(q);
		// 10 is default size forced by ES
		assertEquals(max, res.total);
	}

	@Test
	public void testSearch() throws Exception {
		ItemValue<VEventSeries> event = defaultVEvent();
		event.value.main.summary = "yay";

		indexStore.create(Item.create(event.uid, System.nanoTime()), event.value);
		indexStore.refresh();

		ListResult<String> res = indexStore.search(VEventQuery.create("value.summary:yay"));

		assertEquals(1, res.values.size());
		assertEquals(event.uid, res.values.get(0));

		res = indexStore.search(VEventQuery.create("value.summary:what?"));
		assertEquals(0, res.values.size());

		ItemValue<VEventSeries> event2 = defaultVEvent();

		indexStore.create(Item.create(event2.uid, System.nanoTime()), event2.value);

		ItemValue<VEventSeries> event3 = defaultVEvent();
		event3.value.main.classification = Classification.Private;
		indexStore.create(Item.create(event3.uid, System.nanoTime()), event3.value);

		indexStore.refresh();

		res = indexStore.search(VEventQuery.create("value.location:Toulouse"), false);
		assertEquals(2, res.values.size());
		assertTrue(res.values.contains(event.uid));
		assertTrue(res.values.contains(event2.uid));

		res = indexStore.search(VEventQuery.create("value.location:Toulouse"), true);
		assertEquals(3, res.values.size());
		assertTrue(res.values.contains(event.uid));
		assertTrue(res.values.contains(event2.uid));
		assertTrue(res.values.contains(event3.uid));
	}

	@Test
	public void testSearchByOrganizer() throws Exception {
		ItemValue<VEventSeries> event = defaultVEvent();

		event.value.main.organizer = new VEvent.Organizer("David Phan", "david@bm.lan");

		indexStore.create(Item.create(event.uid, System.nanoTime()), event.value);
		indexStore.refresh();

		ListResult<String> res = indexStore.search(VEventQuery.create("value.organizer.mailto:david@bm.lan"));

		assertEquals(1, res.values.size());
		assertEquals(event.uid, res.values.get(0));

		res = indexStore.search(VEventQuery.create("value.organizer.commonName:David"));
		assertEquals(1, res.values.size());
		assertEquals(event.uid, res.values.get(0));

		res = indexStore.search(VEventQuery.create("value.organizer.commonName:Kevin"));
		assertEquals(0, res.values.size());
	}

	@Test
	public void testNullOrganizer() throws Exception {
		ItemValue<VEventSeries> event = defaultVEvent();
		event.value.main.summary = "testNullOrganizer";
		event.value.main.organizer = null;

		indexStore.create(Item.create(event.uid, System.nanoTime()), event.value);
		indexStore.refresh();

		ListResult<String> res = indexStore.search(VEventQuery.create("value.organizer.mailto:John"));
		assertEquals(0, res.values.size());

		res = indexStore.search(VEventQuery.create("value.summary:testNullOrganizer"));
		assertEquals(1, res.values.size());
		assertEquals(event.uid, res.values.get(0));
	}

	@Test
	public void testSearchByDateInterval() throws Exception {
		ItemValue<VEventSeries> event = defaultVEvent();
		event.value.main.dtstart = BmDateTimeWrapper.create(ZonedDateTime.of(1983, 2, 13, 0, 0, 0, 0, ZoneId.of("UTC")),
				Precision.Date);
		indexStore.create(Item.create(event.uid, System.nanoTime()), event.value);
		indexStore.refresh();

		ZonedDateTime dateMin = ZonedDateTime.of(1983, 2, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
		ZonedDateTime dateMax = ZonedDateTime.of(1983, 3, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
		VEventQuery query = VEventQuery.create(BmDateTimeWrapper.create(dateMin, Precision.Date),
				BmDateTimeWrapper.create(dateMax, Precision.Date));

		ListResult<String> res = indexStore.search(query);
		assertEquals(1, res.values.size());
		assertEquals(event.uid, res.values.get(0));

		// create an event not in search range
		ItemValue<VEventSeries> event2 = defaultVEvent();
		event2.value.main.dtstart = BmDateTimeWrapper
				.create(ZonedDateTime.of(1986, 6, 16, 0, 0, 0, 0, ZoneId.of("UTC")), Precision.Date);
		indexStore.create(Item.create(event2.uid, System.nanoTime()), event2.value);
		indexStore.refresh();

		res = indexStore.search(query);
		assertEquals(1, res.values.size());
		assertEquals(event.uid, res.values.get(0));

		// create an event in search range
		ItemValue<VEventSeries> event3 = defaultVEvent();
		event3.value.main.dtstart = BmDateTimeWrapper
				.create(ZonedDateTime.of(1983, 2, 22, 0, 0, 0, 0, ZoneId.of("UTC")), Precision.Date);
		indexStore.create(Item.create(event3.uid, System.nanoTime()), event3.value);
		indexStore.refresh();

		res = indexStore.search(query);
		assertEquals(2, res.values.size());

		// create an event at dateEnd (excluded from range)
		ItemValue<VEventSeries> event4 = defaultVEvent();
		event4.value.main.dtstart = BmDateTimeWrapper.create(dateMax, Precision.Date);
		indexStore.create(Item.create(event4.uid, System.nanoTime()), event4.value);
		indexStore.refresh();

		res = indexStore.search(query);
		assertEquals(2, res.values.size());

		// create an event at dateBegin (included in range)
		ItemValue<VEventSeries> event5 = defaultVEvent();
		event5.value.main.dtstart = BmDateTimeWrapper.create(dateMin, Precision.Date);
		indexStore.create(Item.create(event5.uid, System.nanoTime()), event5.value);
		indexStore.refresh();

		res = indexStore.search(query);
		assertEquals(3, res.values.size());

		boolean found1 = false;
		boolean found2 = false;
		boolean found3 = false;
		boolean found4 = false;
		boolean found5 = false;
		for (String s : res.values) {
			if (event.uid.equals(s)) {
				found1 = true;
			}
			if (event2.uid.equals(s)) {
				found2 = true;
			}
			if (event3.uid.equals(s)) {
				found3 = true;
			}
			if (event4.uid.equals(s)) {
				found4 = true;
			}
			if (event5.uid.equals(s)) {
				found5 = true;
			}
		}

		assertTrue(found1);
		assertFalse(found2);
		assertTrue(found3);
		assertFalse(found4);
		assertTrue(found5);
	}

	@Test
	public void testSearchByAttendee() throws Exception {
		ItemValue<VEventSeries> event = defaultVEvent();
		indexStore.create(Item.create(event.uid, System.nanoTime()), event.value);

		ItemValue<VEventSeries> event2 = defaultVEvent();
		event2.value.main.attendees.get(1).partStatus = ParticipationStatus.Accepted;
		indexStore.create(Item.create(event2.uid, System.nanoTime()), event2.value);

		indexStore.refresh();

		VEventQuery q = new VEventQuery();
		q.attendee = new VEventAttendeeQuery();
		q.attendee.partStatus = ParticipationStatus.NeedsAction;
		q.attendee.dir = "bm://local.lan/uid2";
		ListResult<String> res = indexStore.search(q);
		assertEquals(1, res.values.size());
		assertEquals(event.uid, res.values.get(0));

		q = new VEventQuery();
		q.attendee = new VEventAttendeeQuery();
		q.attendee.partStatus = ParticipationStatus.Accepted;
		q.attendee.dir = "bm://local.lan/uid1";
		res = indexStore.search(q);
		assertEquals(2, res.values.size());

		q = new VEventQuery();
		q.attendee = new VEventAttendeeQuery();
		q.attendee.dir = "bm://local.lan/uid1";
		res = indexStore.search(q);
		assertEquals(2, res.values.size());

		q = new VEventQuery();
		q.attendee = new VEventAttendeeQuery();
		q.attendee.dir = "bm://local.lan/uid2";
		res = indexStore.search(q);
		assertEquals(2, res.values.size());
	}

	@Test
	public void testBug3286() throws Exception {
		ItemValue<VEventSeries> event = defaultVEvent();
		event.value.main.dtstart = BmDateTimeWrapper.create(ZonedDateTime.of(2014, 5, 29, 8, 0, 0, 0, ZoneId.of("UTC")),
				Precision.Date);
		event.value.main.dtend = BmDateTimeWrapper.create(ZonedDateTime.of(2014, 5, 29, 9, 0, 0, 0, ZoneId.of("UTC")),
				Precision.Date);

		VEvent.RRule rrule = new VEvent.RRule();
		rrule.frequency = VEvent.RRule.Frequency.WEEKLY;
		rrule.until = BmDateTimeWrapper.create(ZonedDateTime.of(2014, 6, 4, 0, 0, 0, 0, ZoneId.of("UTC")),
				Precision.Date);
		List<VEvent.RRule.WeekDay> weekDay = new ArrayList<VEvent.RRule.WeekDay>(4);
		weekDay.add(VEvent.RRule.WeekDay.mo());
		weekDay.add(VEvent.RRule.WeekDay.tu());
		weekDay.add(VEvent.RRule.WeekDay.th());
		weekDay.add(VEvent.RRule.WeekDay.fr());
		rrule.byDay = weekDay;

		event.value.main.rrule = rrule;
		indexStore.create(Item.create(event.uid, System.nanoTime()), event.value);
		indexStore.refresh();

		ZonedDateTime dateMin = ZonedDateTime.of(2014, 5, 26, 0, 0, 0, 0, ZoneId.of("UTC"));
		ZonedDateTime dateMax = ZonedDateTime.of(2014, 6, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
		VEventQuery query = VEventQuery.create(BmDateTimeWrapper.create(dateMin, Precision.Date),
				BmDateTimeWrapper.create(dateMax, Precision.Date));
		ListResult<String> res = indexStore.search(query);
		assertEquals(1, res.values.size());
		assertEquals(event.uid, res.values.get(0));
	}

	@Test
	public void testSearchRRule() throws Exception {
		ItemValue<VEventSeries> event = defaultVEvent();
		event.value.main.dtstart = BmDateTimeWrapper.create(ZonedDateTime.of(1983, 2, 13, 0, 0, 0, 0, ZoneId.of("UTC")),
				Precision.Date);

		VEvent.RRule rrule = new VEvent.RRule();
		rrule.frequency = VEvent.RRule.Frequency.YEARLY;
		rrule.until = BmDateTimeWrapper.create(ZonedDateTime.of(2083, 2, 13, 0, 0, 0, 0, ZoneId.of("UTC")),
				Precision.Date);

		event.value.main.rrule = rrule;

		indexStore.create(Item.create(event.uid, System.nanoTime()), event.value);
		indexStore.refresh();

		ListResult<String> res = indexStore
				.search(VEventQuery.create("value.rrule.until.iso8601:" + "\"" + rrule.until.iso8601 + "\""));
		assertEquals(1, res.values.size());

		ZonedDateTime dateMin = ZonedDateTime.of(1983, 2, 12, 0, 0, 0, 0, ZoneId.of("UTC"));
		ZonedDateTime dateMax = ZonedDateTime.of(1983, 2, 14, 0, 0, 0, 0, ZoneId.of("UTC"));
		VEventQuery query = VEventQuery.create(BmDateTimeWrapper.create(dateMin, Precision.Date),
				BmDateTimeWrapper.create(dateMax, Precision.Date));
		res = indexStore.search(query);
		assertEquals(1, res.values.size());
		assertEquals(event.uid, res.values.get(0));

		dateMin = ZonedDateTime.of(1983, 2, 14, 0, 0, 0, 0, ZoneId.of("UTC"));
		dateMax = ZonedDateTime.of(1983, 2, 15, 0, 0, 0, 0, ZoneId.of("UTC"));
		query = VEventQuery.create(BmDateTimeWrapper.create(dateMin, Precision.Date),
				BmDateTimeWrapper.create(dateMax, Precision.Date));
		res = indexStore.search(query);
		assertEquals(1, res.values.size()); // not 0 because of rrule.until

		dateMin = ZonedDateTime.of(2014, 2, 12, 0, 0, 0, 0, ZoneId.of("UTC"));
		dateMax = ZonedDateTime.of(2014, 2, 14, 0, 0, 0, 0, ZoneId.of("UTC"));
		query = VEventQuery.create(BmDateTimeWrapper.create(dateMin, Precision.Date),
				BmDateTimeWrapper.create(dateMax, Precision.Date));
		res = indexStore.search(query);
		assertEquals(1, res.values.size());
		assertEquals(event.uid, res.values.get(0));
	}

	@Test
	public void testSearchRRule_daily() throws Exception {
		ItemValue<VEventSeries> event = defaultVEvent();
		event.value.main.dtstart = BmDateTimeWrapper.create(ZonedDateTime.of(2002, 2, 13, 0, 0, 0, 0, ZoneId.of("UTC")),
				Precision.Date);

		VEvent.RRule rrule = new VEvent.RRule();
		rrule.frequency = VEvent.RRule.Frequency.DAILY;
		rrule.until = BmDateTimeWrapper.create(ZonedDateTime.of(2002, 2, 15, 0, 0, 0, 0, ZoneId.of("UTC")),
				Precision.Date);

		event.value.main.rrule = rrule;

		indexStore.create(Item.create(event.uid, System.nanoTime()), event.value);
		indexStore.refresh();

		ZonedDateTime dateMin = ZonedDateTime.of(2002, 2, 15, 0, 0, 0, 0, ZoneId.of("UTC"));
		VEventQuery query = VEventQuery.create(BmDateTimeWrapper.create(dateMin, Precision.Date), null);
		ListResult<String> res = indexStore.search(query);
		assertEquals(1, res.values.size());
	}

	private ItemValue<VEventSeries> defaultVEvent() {
		VEventSeries series = new VEventSeries();
		VEvent event = new VEvent();
		event.dtstart = BmDateTimeWrapper.create(ZonedDateTime.now(), Precision.DateTime);
		event.summary = "event " + System.currentTimeMillis();
		event.location = "Toulouse";
		event.description = "Lorem ipsum";
		event.transparency = VEvent.Transparency.Opaque;
		event.classification = VEvent.Classification.Public;
		event.status = VEvent.Status.Confirmed;
		event.priority = 42;

		event.organizer = new VEvent.Organizer();
		event.organizer.uri = UUID.randomUUID().toString();
		event.organizer.dir = "bm://users/org";
		List<VEvent.Attendee> attendees = new ArrayList<>(2);

		VEvent.Attendee john = VEvent.Attendee.create(VEvent.CUType.Individual, "", VEvent.Role.Chair,
				VEvent.ParticipationStatus.Accepted, true, "", "", "", "John Bang", "bm://local.lan/uid1", "", "uid1",
				"john.bang@bm.lan");
		attendees.add(john);
		john.responseComment = "I will be there to see jane";

		VEvent.Attendee jane = VEvent.Attendee.create(VEvent.CUType.Individual, "", VEvent.Role.RequiredParticipant,
				VEvent.ParticipationStatus.NeedsAction, true, "", "", "", "Jane Bang", "bm://local.lan/uid2", "",
				"uid2", "jane.bang@bm.lan");

		attendees.add(jane);

		event.attendees = attendees;
		series.main = event;
		return ItemValue.create(UUID.randomUUID().toString(), series);
	}

	@Test
	public void testSearchSummary() throws Exception {
		ItemValue<VEventSeries> event = defaultVEvent();
		event.value.main.summary = "kamoulox";

		indexStore.create(Item.create(event.uid, System.nanoTime()), event.value);
		indexStore.refresh();

		ListResult<String> res = indexStore.search(VEventQuery.create("value.summary:kam"));

		assertEquals(1, res.values.size());
	}

	@Test
	public void testSearchSentence() throws Exception {
		ItemValue<VEventSeries> event = defaultVEvent();
		event.value.main.summary = "comité de direction";
		indexStore.create(Item.create(event.uid, System.nanoTime()), event.value);

		ItemValue<VEventSeries> event2 = defaultVEvent();
		event2.value.main.summary = "COMITE DE DIRECTION";
		indexStore.create(Item.create(event2.uid, System.nanoTime()), event2.value);

		ItemValue<VEventSeries> event3 = defaultVEvent();
		event3.value.main.summary = "trop de résultats";
		indexStore.create(Item.create(event3.uid, System.nanoTime()), event3.value);

		indexStore.refresh();

		ListResult<String> res = indexStore.search(VEventQuery.create("value.summary:comité de direction"));
		assertEquals(2, res.values.size());
		assertFalse(res.values.stream().filter(uid -> uid.equals(event3.uid)).findFirst().isPresent());

		res = indexStore.search(VEventQuery.create("value.summary:comité"));
		assertEquals(2, res.values.size());
		assertFalse(res.values.stream().filter(uid -> uid.equals(event3.uid)).findFirst().isPresent());

		res = indexStore.search(VEventQuery.create("value.summary:de"));
		assertEquals(3, res.values.size());
	}

	@Test
	public void testUpdates() throws Exception {
		ItemValue<VEventSeries> event = defaultVEvent();
		event.internalId = System.nanoTime();
		event.value.main.summary = "coucou";
		indexStore.create(Item.create(event.uid, event.internalId), event.value);

		ItemValue<VEventSeries> event2 = defaultVEvent();
		event2.internalId = System.nanoTime();
		event2.value.main.summary = "yeah";
		indexStore.create(Item.create(event2.uid, event2.internalId), event2.value);

		indexStore.refresh();
		ListResult<String> res = indexStore.search(VEventQuery.create("value.summary:coucou"));
		assertEquals(1, res.total);

		res = indexStore.search(VEventQuery.create("value.summary:yeah"));
		assertEquals(1, res.total);

		event.value.main.summary = "yata";
		event2.value.main.summary = "yolo";

		indexStore.updates(Arrays.asList(event, event2));
		indexStore.refresh();

		res = indexStore.search(VEventQuery.create("value.summary:coucou"));
		assertEquals(0, res.total);
		res = indexStore.search(VEventQuery.create("value.summary:yata"));
		assertEquals(1, res.total);

		res = indexStore.search(VEventQuery.create("value.summary:yeah"));
		assertEquals(0, res.total);
		res = indexStore.search(VEventQuery.create("value.summary:yolo"));
		assertEquals(1, res.total);
	}

}
