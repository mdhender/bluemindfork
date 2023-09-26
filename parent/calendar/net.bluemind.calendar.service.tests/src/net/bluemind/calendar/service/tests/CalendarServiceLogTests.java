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

import static net.bluemind.calendar.service.tests.CalendarTestHook.Action.CREATE;
import static net.bluemind.calendar.service.tests.CalendarTestHook.Action.DELETE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.text.ParseException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.junit.After;
import org.junit.Test;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import net.bluemind.attachment.api.AttachedFile;
import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.calendar.service.AbstractCalendarTests;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.auditlogs.AuditLogEntry;
import net.bluemind.core.container.model.ChangeLogEntry.Type;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.icalendar.api.ICalendarElement.Attendee;
import net.bluemind.icalendar.api.ICalendarElement.CUType;
import net.bluemind.icalendar.api.ICalendarElement.ParticipationStatus;
import net.bluemind.icalendar.api.ICalendarElement.Role;
import net.bluemind.icalendar.api.ICalendarElement.VAlarm;
import net.bluemind.lib.elasticsearch.ESearchActivator;
import net.bluemind.tag.api.TagRef;
import net.bluemind.tests.defaultdata.BmDateTimeHelper;

public class CalendarServiceLogTests extends AbstractCalendarTests {

	@Test
	public void testCreate() throws ServerFault, ElasticsearchException, IOException {

		ElasticsearchClient esClient = ESearchActivator.getClient();

		VEventSeries event = defaultVEvent();
		String uid = "test_" + System.nanoTime();

		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, event, sendNotifications);

		assertEquals(CREATE, CalendarTestSyncHook.action());
		assertEquals(CREATE, CalendarTestAsyncHook.action());
		assertNotNull(CalendarTestSyncHook.message());
		ESearchActivator.refreshIndex("audit_log");

		Awaitility.await().atMost(2, TimeUnit.SECONDS).until(() -> {
			SearchResponse<AuditLogEntry> response = esClient.search(s -> s //
					.index("audit_log") //
					.query(q -> q.bool(b -> b
							.must(TermQuery.of(t -> t.field("logtype").value(userCalendarContainer.type))._toQuery())
							.must(TermQuery.of(t -> t.field("action").value(Type.Created.toString()))._toQuery()))),
					AuditLogEntry.class);
			return 1L == response.hits().total().value();
		});
		SearchResponse<AuditLogEntry> response = esClient.search(s -> s //
				.index("audit_log") //
				.query(q -> q.bool(
						b -> b.must(TermQuery.of(t -> t.field("logtype").value(userCalendarContainer.type))._toQuery())
								.must(TermQuery.of(t -> t.field("action").value(Type.Created.toString()))._toQuery()))),
				AuditLogEntry.class);
		assertEquals(1L, response.hits().total().value());

		AuditLogEntry firstEntry = response.hits().hits().get(0).source();
		assertEquals(6L, firstEntry.content.with().size());
		assertEquals(2L, firstEntry.content.author().size());
		assertTrue(firstEntry.content.newValue() != null);

		assertEquals(testUser.uid, firstEntry.securityContext.uid());
		assertEquals(testUser.displayName, firstEntry.securityContext.displayName());
		assertEquals("unknown-origin", firstEntry.securityContext.origin());
		assertEquals(testUser.value.defaultEmailAddress(), firstEntry.securityContext.email());

		assertEquals(userCalendarContainer.name, firstEntry.container.name());
		assertEquals(userCalendarContainer.name, firstEntry.container.ownerElement().displayName());

		assertTrue(firstEntry.item.id() > 0);
		assertTrue(firstEntry.item.version() > 0);

		assertEquals(uid, firstEntry.content.key());
		assertEquals(event.main.summary, firstEntry.content.description());
		assertTrue(!firstEntry.content.newValue().isBlank());
		assertTrue(firstEntry.content.is() != null);
		assertTrue(firstEntry.content.has() != null);
	}

	@Test
	public void testUpdateEndDate() throws ServerFault, ElasticsearchException, IOException {

		ElasticsearchClient esClient = ESearchActivator.getClient();
		VEventSeries event = defaultVEvent();
		String uid = "test_" + System.nanoTime();
		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, event, sendNotifications);

		event.main.dtend.iso8601 = "2022-02-13T02:00:00.000+07:00";
		getCalendarService(userSecurityContext, userCalendarContainer).update(uid, event, sendNotifications);
		ESearchActivator.refreshIndex("audit_log");

		Awaitility.await().atMost(2, TimeUnit.SECONDS).until(() -> {
			SearchResponse<AuditLogEntry> response = esClient.search(s -> s //
					.index("audit_log") //
					.query(q -> q.bool(b -> b
							.must(TermQuery.of(t -> t.field("logtype").value(userCalendarContainer.type))._toQuery())
							.must(TermQuery.of(t -> t.field("action").value(Type.Updated.toString()))._toQuery()))),
					AuditLogEntry.class);
			return 1L == response.hits().total().value();
		});
		SearchResponse<AuditLogEntry> response = esClient.search(s -> s //
				.index("audit_log") //
				.query(q -> q.bool(
						b -> b.must(TermQuery.of(t -> t.field("logtype").value(userCalendarContainer.type))._toQuery())
								.must(TermQuery.of(t -> t.field("action").value(Type.Updated.toString()))._toQuery()))),
				AuditLogEntry.class);
		assertEquals(1L, response.hits().total().value());

		AuditLogEntry firstEntry = response.hits().hits().get(0).source();
		assertTrue(firstEntry.updatemessage.contains("event end date changed:"));

		assertEquals(testUser.uid, firstEntry.securityContext.uid());
		assertEquals(testUser.displayName, firstEntry.securityContext.displayName());
		assertEquals("unknown-origin", firstEntry.securityContext.origin());
		assertEquals(testUser.value.defaultEmailAddress(), firstEntry.securityContext.email());

		assertEquals(userCalendarContainer.name, firstEntry.container.name());
		assertEquals(userCalendarContainer.name, firstEntry.container.ownerElement().displayName());

		assertTrue(firstEntry.item.id() > 0);
		assertTrue(firstEntry.item.version() > 0);

		assertEquals(uid, firstEntry.content.key());
		assertEquals(event.main.summary, firstEntry.content.description());
		assertEquals(6L, firstEntry.content.with().size());
		assertTrue(!firstEntry.content.newValue().isBlank());
		assertEquals(2L, firstEntry.content.author().size());
		assertTrue(firstEntry.content.is() != null);
		assertTrue(firstEntry.content.has() != null);
	}

	@Test
	public void testUpdateRemoveAttendee() throws ServerFault, ElasticsearchException, IOException, ParseException {

		ElasticsearchClient esClient = ESearchActivator.getClient();
		VEventSeries event = defaultVEvent();
		String uid = "test_" + System.nanoTime();
		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, event, sendNotifications);

		event.main.attendees = new ArrayList<>(1);
		getCalendarService(userSecurityContext, userCalendarContainer).update(uid, event, sendNotifications);
		ESearchActivator.refreshIndex("audit_log");
		Awaitility.await().atMost(2, TimeUnit.SECONDS).until(() -> {
			SearchResponse<AuditLogEntry> response = esClient.search(s -> s //
					.index("audit_log") //
					.query(q -> q.bool(b -> b
							.must(TermQuery.of(t -> t.field("logtype").value(userCalendarContainer.type))._toQuery())
							.must(TermQuery.of(t -> t.field("action").value(Type.Updated.toString()))._toQuery()))),
					AuditLogEntry.class);
			return 1L == response.hits().total().value();
		});

		SearchResponse<AuditLogEntry> response = esClient.search(s -> s //
				.index("audit_log") //
				.query(q -> q.bool(
						b -> b.must(TermQuery.of(t -> t.field("logtype").value(userCalendarContainer.type))._toQuery())
								.must(TermQuery.of(t -> t.field("action").value(Type.Updated.toString()))._toQuery()))),
				AuditLogEntry.class);
		assertEquals(1L, response.hits().total().value());
		AuditLogEntry firstEntry = response.hits().hits().get(0).source();
		assertTrue(firstEntry.updatemessage.contains("removed attendees: 'external@attendee.lan,david@attendee.lan'"));

		assertEquals(testUser.uid, firstEntry.securityContext.uid());
		assertEquals(testUser.displayName, firstEntry.securityContext.displayName());
		assertEquals("unknown-origin", firstEntry.securityContext.origin());
		assertEquals(testUser.value.defaultEmailAddress(), firstEntry.securityContext.email());

		assertEquals(userCalendarContainer.name, firstEntry.container.name());
		assertEquals(userCalendarContainer.name, firstEntry.container.ownerElement().displayName());

		assertTrue(firstEntry.item.id() > 0);
		assertTrue(firstEntry.item.version() > 0);

		assertEquals(uid, firstEntry.content.key());
		assertEquals(event.main.summary, firstEntry.content.description());
		assertTrue(firstEntry.content.with().isEmpty());
		assertTrue(!firstEntry.content.newValue().isBlank());
		assertTrue(firstEntry.content.is() != null);
		assertTrue(firstEntry.content.has() != null);
	}

	@Test
	public void testUpdateAddedAttendee()
			throws ServerFault, ElasticsearchException, IOException, ParseException, InterruptedException {

		ElasticsearchClient esClient = ESearchActivator.getClient();
		VEventSeries event = defaultVEvent();
		String uid = "test_" + System.nanoTime();
		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, event, sendNotifications);

		VEvent.Attendee sylvain = VEvent.Attendee.create(VEvent.CUType.Individual, "", VEvent.Role.Chair,
				VEvent.ParticipationStatus.Accepted, true, "", "", "", "sylvain", null, null, null,
				"sylvain@attendee.lan");
		VEvent.Attendee nico = VEvent.Attendee.create(VEvent.CUType.Individual, "", VEvent.Role.Chair,
				VEvent.ParticipationStatus.Accepted, true, "", "", "", "nico", null, null, null, "nico@attendee.lan");
		event.main.attendees.addAll(Arrays.asList(sylvain, nico));
		getCalendarService(userSecurityContext, userCalendarContainer).update(uid, event, sendNotifications);

		ESearchActivator.refreshIndex("audit_log");
		Awaitility.await().atMost(2, TimeUnit.SECONDS).until(() -> {
			SearchResponse<AuditLogEntry> response = esClient.search(s -> s //
					.index("audit_log") //
					.query(q -> q.bool(b -> b
							.must(TermQuery.of(t -> t.field("logtype").value(userCalendarContainer.type))._toQuery())
							.must(TermQuery.of(t -> t.field("action").value(Type.Updated.toString()))._toQuery()))),
					AuditLogEntry.class);
			return 1L == response.hits().total().value();
		});

		SearchResponse<AuditLogEntry> response = esClient.search(s -> s //
				.index("audit_log") //
				.query(q -> q.bool(
						b -> b.must(TermQuery.of(t -> t.field("logtype").value(userCalendarContainer.type))._toQuery())
								.must(TermQuery.of(t -> t.field("action").value(Type.Updated.toString()))._toQuery()))),
				AuditLogEntry.class);
		assertEquals(1L, response.hits().total().value());
		AuditLogEntry firstEntry = response.hits().hits().get(0).source();
		assertTrue(firstEntry.updatemessage.contains("added attendees: 'sylvain@attendee.lan,nico@attendee.lan'"));

		assertTrue(firstEntry.content.newValue() != null);

		assertEquals(testUser.uid, firstEntry.securityContext.uid());
		assertEquals(testUser.displayName, firstEntry.securityContext.displayName());
		assertEquals("unknown-origin", firstEntry.securityContext.origin());
		assertEquals(testUser.value.defaultEmailAddress(), firstEntry.securityContext.email());

		assertEquals(userCalendarContainer.name, firstEntry.container.name());
		assertEquals(userCalendarContainer.name, firstEntry.container.ownerElement().displayName());

		assertTrue(firstEntry.item.id() > 0);
		assertTrue(firstEntry.item.version() > 0);

		assertEquals(uid, firstEntry.content.key());
		assertEquals(event.main.summary, firstEntry.content.description());
		assertEquals(10L, firstEntry.content.with().size());
		assertTrue(!firstEntry.content.newValue().isBlank());
		assertEquals(2L, firstEntry.content.author().size());
		assertTrue(firstEntry.content.is() != null);
		assertTrue(firstEntry.content.has() != null);
	}

	@Test
	public void testUpdateChangedLocation() throws ServerFault, ElasticsearchException, IOException, ParseException {

		ElasticsearchClient esClient = ESearchActivator.getClient();
		VEventSeries event = defaultVEvent();
		String uid = "test_" + System.nanoTime();
		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, event, sendNotifications);

		event.main.location = "Marseillette";
		getCalendarService(userSecurityContext, userCalendarContainer).update(uid, event, sendNotifications);
		ESearchActivator.refreshIndex("audit_log");
		Awaitility.await().atMost(2, TimeUnit.SECONDS).until(() -> {
			SearchResponse<AuditLogEntry> response = esClient.search(s -> s //
					.index("audit_log") //
					.query(q -> q.bool(b -> b
							.must(TermQuery.of(t -> t.field("logtype").value(userCalendarContainer.type))._toQuery())
							.must(TermQuery.of(t -> t.field("action").value(Type.Updated.toString()))._toQuery()))),
					AuditLogEntry.class);
			return 1L == response.hits().total().value();
		});

		SearchResponse<AuditLogEntry> response = esClient.search(s -> s //
				.index("audit_log") //
				.query(q -> q.bool(
						b -> b.must(TermQuery.of(t -> t.field("logtype").value(userCalendarContainer.type))._toQuery())
								.must(TermQuery.of(t -> t.field("action").value(Type.Updated.toString()))._toQuery()))),
				AuditLogEntry.class);
		assertEquals(1L, response.hits().total().value());
		AuditLogEntry firstEntry = response.hits().hits().get(0).source();
		assertTrue(firstEntry.updatemessage.contains("event location changed: 'Toulouse' -> 'Marseillette'"));

		assertEquals(testUser.uid, firstEntry.securityContext.uid());
		assertEquals(testUser.displayName, firstEntry.securityContext.displayName());
		assertEquals("unknown-origin", firstEntry.securityContext.origin());
		assertEquals(testUser.value.defaultEmailAddress(), firstEntry.securityContext.email());

		assertEquals(userCalendarContainer.name, firstEntry.container.name());
		assertEquals(userCalendarContainer.name, firstEntry.container.ownerElement().displayName());

		assertTrue(firstEntry.item.id() > 0);
		assertTrue(firstEntry.item.version() > 0);

		assertEquals(uid, firstEntry.content.key());
		assertEquals(event.main.summary, firstEntry.content.description());
		assertEquals(6L, firstEntry.content.with().size());
		assertTrue(!firstEntry.content.newValue().isBlank());
		assertEquals(2L, firstEntry.content.author().size());
		assertTrue(firstEntry.content.is() != null);
		assertTrue(firstEntry.content.has() != null);
	}

	@Test
	public void testDelete() throws ServerFault, ElasticsearchException, IOException {
		ElasticsearchClient esClient = ESearchActivator.getClient();

		VEventSeries event = defaultVEvent();
		// add attendee1
		Attendee attendee = Attendee.create(CUType.Individual, "", Role.RequiredParticipant,
				ParticipationStatus.NeedsAction, true, "", "", "",
				attendee1.value.contactInfos.identification.formatedName.value, "", "", null,
				attendee1.value.login + "@bm.lan");
		event.main.attendees.add(attendee);

		String uid = "test_" + System.nanoTime();
		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, event, sendNotifications);
		getCalendarService(userSecurityContext, userCalendarContainer).delete(uid, sendNotifications);

		ItemValue<VEventSeries> vevent = getCalendarService(userSecurityContext, userCalendarContainer)
				.getComplete(uid);
		assertNull(vevent);

		vevent = getCalendarService(attendee1SecurityContext, attendee1CalendarContainer).getComplete(uid);
		assertNull(vevent);

		assertEquals(DELETE, CalendarTestSyncHook.action());
		assertEquals(DELETE, CalendarTestAsyncHook.action());
		assertNotNull(CalendarTestSyncHook.message());
		ESearchActivator.refreshIndex("audit_log");
		Awaitility.await().atMost(2, TimeUnit.SECONDS).until(() -> {
			var response = esClient.search(s -> s //
					.index("audit_log") //
					.query(q -> q.bool(b -> b
							.must(TermQuery.of(t -> t.field("logtype").value(userCalendarContainer.type))._toQuery())
							.must(TermQuery.of(t -> t.field("action").value(Type.Created.toString()))._toQuery()))),
					AuditLogEntry.class);
			return 1L == response.hits().total().value();
		});

		SearchResponse<AuditLogEntry> response = esClient.search(s -> s //
				.index("audit_log") //
				.query(q -> q.bool(
						b -> b.must(TermQuery.of(t -> t.field("logtype").value(userCalendarContainer.type))._toQuery())
								.must(TermQuery.of(t -> t.field("action").value(Type.Updated.toString()))._toQuery()))),
				AuditLogEntry.class);
		assertEquals(0L, response.hits().total().value());

		response = esClient.search(s -> s //
				.index("audit_log") //
				.query(q -> q.bool(
						b -> b.must(TermQuery.of(t -> t.field("logtype").value(userCalendarContainer.type))._toQuery())
								.must(TermQuery.of(t -> t.field("action").value(Type.Created.toString()))._toQuery()))),
				AuditLogEntry.class);
		assertEquals(1L, response.hits().total().value());

		response = esClient.search(s -> s //
				.index("audit_log") //
				.query(q -> q.bool(
						b -> b.must(TermQuery.of(t -> t.field("logtype").value(userCalendarContainer.type))._toQuery())
								.must(TermQuery.of(t -> t.field("action").value(Type.Deleted.toString()))._toQuery()))),
				AuditLogEntry.class);
		assertEquals(1L, response.hits().total().value());

		AuditLogEntry firstEntry = response.hits().hits().get(0).source();
		assertEquals(testUser.uid, firstEntry.securityContext.uid());
		assertEquals(testUser.displayName, firstEntry.securityContext.displayName());
		assertEquals("unknown-origin", firstEntry.securityContext.origin());
		assertEquals(testUser.value.defaultEmailAddress(), firstEntry.securityContext.email());

		assertEquals(userCalendarContainer.name, firstEntry.container.name());
		assertEquals(userCalendarContainer.name, firstEntry.container.ownerElement().displayName());

		assertTrue(firstEntry.item.id() > 0);
		assertTrue(firstEntry.item.version() > 0);

		assertEquals(uid, firstEntry.content.key());
		assertEquals(event.main.summary, firstEntry.content.description());
		assertEquals(8L, firstEntry.content.with().size());
		assertTrue(!firstEntry.content.newValue().isBlank());
		assertEquals(2L, firstEntry.content.author().size());
		assertTrue(firstEntry.content.is() != null);
		assertTrue(firstEntry.content.has() != null);
	}

	@After
	public void tearDown() {
		CalendarTestSyncHook.reset();
		CalendarTestAsyncHook.reset();
	}

	protected VEventSeries defaultVEvent() {
		ZoneId tz = ZoneId.of("Asia/Ho_Chi_Minh");
		return defaultVEvent(ZonedDateTime.of(2022, 2, 13, 1, 0, 0, 0, tz));
	}

	/**
	 * @return
	 */
	protected VEventSeries defaultVEvent(ZonedDateTime start) {
		VEventSeries series = new VEventSeries();
		VEvent event = new VEvent();
		event.dtstart = BmDateTimeHelper.time(start);
		event.summary = "event " + System.currentTimeMillis();
		event.location = "Toulouse";
		event.description = "Lorem ipsum";
		event.transparency = VEvent.Transparency.Opaque;
		event.classification = VEvent.Classification.Private;
		event.status = VEvent.Status.Confirmed;
		event.priority = 3;
		event.url = "https://www.bluemind.net";
		event.conference = "https//vi.sio.com/xxx";
		event.conferenceConfiguration.put("conf1", "val1");
		event.conferenceConfiguration.put("conf2", "val2");

		event.attachments = new ArrayList<>();
		AttachedFile attachment1 = new AttachedFile();
		attachment1.publicUrl = "http://somewhere/1";
		attachment1.name = "test.gif";
		attachment1.cid = "cid0123456789";
		event.attachments.add(attachment1);
		AttachedFile attachment2 = new AttachedFile();
		attachment2.publicUrl = "http://somewhere/2";
		attachment2.name = "test.png";
		event.attachments.add(attachment2);

		event.organizer = new VEvent.Organizer(testUser.value.login + "@bm.lan");

		List<VEvent.Attendee> attendees = new ArrayList<>(1);
		VEvent.Attendee me = VEvent.Attendee.create(VEvent.CUType.Individual, "", VEvent.Role.Chair,
				VEvent.ParticipationStatus.Accepted, true, "", "", "", "osef", null, null, null,
				"external@attendee.lan");
		VEvent.Attendee david = VEvent.Attendee.create(VEvent.CUType.Individual, "", VEvent.Role.Chair,
				VEvent.ParticipationStatus.Accepted, true, "", "", "", "david", null, null, null, "david@attendee.lan");
		attendees.addAll(Arrays.asList(me, david));

		event.attendees = attendees;

		event.categories = new ArrayList<TagRef>(2);
		event.categories.add(tagRef1);
		event.categories.add(tagRef2);

		VAlarm vAlarm = new VAlarm();
		vAlarm.action = VAlarm.Action.Email;
		vAlarm.description = "description";
		vAlarm.duration = 3600;
		vAlarm.repeat = 10;
		vAlarm.summary = "summary";
		vAlarm.trigger = 5;
		event.alarm = new ArrayList<>();
		event.alarm.add(vAlarm);

		TagRef tag1 = new TagRef();
		tag1.color = "color";
		tag1.containerUid = "containerUid";
		tag1.itemUid = "itemUid";
		tag1.label = "label";

		event.categories = new ArrayList<>();
		event.categories.add(tag1);

		series.main = event;
		return series;
	}
}
