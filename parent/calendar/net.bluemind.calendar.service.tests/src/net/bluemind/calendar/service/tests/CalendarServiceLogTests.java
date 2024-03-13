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
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.OptionalInt;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

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
import net.bluemind.core.api.date.BmDateTime;
import net.bluemind.core.api.date.BmDateTime.Precision;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.auditlogs.AuditLogEntry;
import net.bluemind.core.auditlogs.client.loader.config.AuditLogConfig;
import net.bluemind.core.container.model.ChangeLogEntry.Type;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.icalendar.api.ICalendarElement.Attendee;
import net.bluemind.icalendar.api.ICalendarElement.CUType;
import net.bluemind.icalendar.api.ICalendarElement.Classification;
import net.bluemind.icalendar.api.ICalendarElement.ParticipationStatus;
import net.bluemind.icalendar.api.ICalendarElement.RRule;
import net.bluemind.icalendar.api.ICalendarElement.RRule.Frequency;
import net.bluemind.icalendar.api.ICalendarElement.RRule.WeekDay;
import net.bluemind.icalendar.api.ICalendarElement.Role;
import net.bluemind.icalendar.api.ICalendarElement.Status;
import net.bluemind.icalendar.api.ICalendarElement.VAlarm;
import net.bluemind.lib.elasticsearch.ESearchActivator;
import net.bluemind.tag.api.TagRef;
import net.bluemind.tests.defaultdata.BmDateTimeHelper;

public class CalendarServiceLogTests extends AbstractCalendarTests {
	private static final String CRLF = "\r\n";

	private final String dataStreamName = AuditLogConfig.resolveDataStreamName(domainUid);

	@Test
	public void testCreate() throws ServerFault, ElasticsearchException, IOException {

		ElasticsearchClient esClient = ESearchActivator.getClient();

		VEventSeries event = defaultVEvent();

		event.main.description = """
				<META HTTP-EQUIV=\\\"Content-Type\\\" CONTENT=\\\"text/html; charset=iso-8857-1\\\">
							\\r\\n<div style=\\\"font-family: Montserrat, montserrat, &quot;Source Sans&quot;, &quot;Helvetica Neue&quot;, Helvetica, Arial, sans-serif; font-size: 9.75pt; color: rgb(31, 31, 31);\\\"><br></div>
							<div data-bm-signature=\\\"a9cbedd6-76b7-4cbd-a9c9-b693cb7d77fc\\\">
							   <div>-- <br></div>
							   <img src=\\\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAA+gAAAFoCAYAAADEjNEVAAAACXBIWXMAAAsTAAALEwEAmpwYAAAHP2lUWHRYTUw6Y29tLmFkb2JlLnhtcAAAAAAAPD94cGFja2V0IGJlZ2luPSLvu78iIGlkPSJXNU0wTXBDZWhpSHpyZVN6TlRjemtjOWQiPz4gPHg6eG1wbWV0YSB4bWxuczp4PSJhZG9iZTpuczptZXRhLyIgeDp4bXB0az0iQWRvYmUgWE1QIENvcmUgNS42LWMxNDIgNzkuMTYwOTI0LCAyMDE3LzA3LzEzLTAxOjA2OjM5ICAgICAgICAiPiA8cmRmOlJERiB4bWxuczpyZGY9Imh0dHA6Ly93d3cudzMub3JnLzE5OTkvMDIvMjItcmRmLXN5bnRheC1ucyMiPiA8cmRmOkRlc2NyaXB0aW9uIHJkZjphYm91dD0iIiB4bWxuczp4bXA9Imh0dHA6Ly9ucy5hZG9iZS5jb20veGFwLzEuMC8iIHhtbG5zOmRjPSJodHRwOi8vcHVybC5vcmcvZGMvZWxlbWVudHMvMS4xLyIgeG1sbnM6cGhvdG9zaG9wPSJodHRwOi8vbnMuYWRvYmUuY29tL3Bob3Rvc2hvcC8xLjAvIiB4bWxuczp4bXBNTT0iaHR0cDovL25zLmFkb2JlLmNvbS94YXAvMS4wL21tLyIgeG1sbnM6c3RFdnQ9Imh0dHA6Ly9ucy5hZG9iZS5jb20veGFwLzEuMC9zVHlwZS9SZXNvdXJjZUV2ZW50IyIgeG1wOkNyZWF0b3JUb29sPSJBZG9iZSBQaG90b3Nob3AgQ0MgMjAxOCAoV2luZG93cykiIHhtcDpDcmVhdGVEYXRlPSIyMDE4LTA2LTA4VDEyOjAwOjU1KzAyOjAwIiB4bXA6TW9kaWZ5RGF0ZT0iMjAxOS0wMy0yOFQxNTowNTowNiswMTowMCIgeG1wOk1ldGFkYXRhRGF0ZT0iMjAxOS0wMy0yOFQxNTowNTowNiswMTowMCIgZGM6Zm9ybWF0PSJpbWFnZS9wbmciIHBob3Rvc2hvcDpDb2xvck1vZGU9IjMiIHhtcE1NOkluc3RhbmNlSUQ9InhtcC5paWQ6ZGQzM2Q4ZmUtZGVkYy1mMzRhLTlkZTktMTc5MDA2YTM3OTlkIiB4bXBNTTpEb2N1bWVudElEPSJhZG9iZTpkb2NpZDpwaG90b3Nob3A6YzY3NGU2N2EtOGM5Yy1hNDQ1LWE2YjktODU3Yzc3NTIxY2Q5IiB4bXBNTTpPcmlnaW5hbERvY3VtZW50SUQ9InhtcC5kaWQ6M2RiODZjMTItZDIzZi05NDQ1LThlYmQtNTBlZTU2MmYzYTQ1Ij4gPHBob3Rvc2hvcDpEb2N1bWVudEFuY2VzdG9ycz4gPHJkZjpCYWc+IDxyZGY6bGk+YWRvYmU6ZG9jaWQ6cGhvdG9zaG9wOjhmNjExZmIwLTgyNDItNDg0Mi05MjBhLTY5NTQyN2M1MTQxZjwvcmRmOmxpPiA8L3JkZjpCYWc+IDwvcGhvdG9zaG9wOkRvY3VtZW50QW5jZXN0b3JzPiA8eG1wTU06SGlzdG9yeT4gPHJkZjpTZXE+IDxyZGY6bGkgc3RFdnQ6YWN0aW9uPSJjcmVhdGVkIiBzdEV2dDppbnN0YW5jZUlEPSJ4bXAuaWlkOjNkYjg2YzEyLWQyM2YtOTQ0NS04ZWJkLTUwZWU1NjJmM2E0NSIgc3RFdnQ6d2hlbj0iMjAxOC0wNi0wOFQxMjowMDo1NSswMjowMCIgc3RFdnQ6c29mdHdhcmVBZ2VudD0iQWRvYmUgUGhvdG9zaG9wIENDIDIwMTggKFdpbmRvd3MpIi8+IDxyZGY6bGkgc3RFdnQ6YWN0aW9uPSJzYXZlZCIgc3RFdnQ6aW5zdGFuY2VJRD0ieG1wLmlpZDoyOTE1NGUzMi1kYWM0LWJmNDgtYmM4Yy02MGI4YTQ3Yjk1YmYiIHN0RXZ0OndoZW49IjIwMTktMDItMDdUMTQ6Mzk6MTkrMDE6MDAiIHN0RXZ0OnNvZnR3YXJlQWdlbnQ9IkFkb2JlIFBob3Rvc2hvcCBDQyAyMDE4IChXaW5kb3dzKSIgc3RFdnQ6Y2hhbmdlZD0iLyIvPiA8cmRmOmxpIHN0RXZ0OmFjdGlvbj0ic2F2ZWQiIHN0RXZ0Omluc3RhbmNlSUQ9InhtcC5paWQ6ZGQzM2Q4ZmUtZGVkYy1mMzRhLTlkZTktMTc5MDA2YTM3OTlkIiBzdEV2dDp3aGVuPSIyMDE5LTAzLTI4VDE1OjA1OjA2KzAxOjAwIiBzdEV2dDpzb2Z0d2FyZUFnZW50PSJBZG9iZSBQaG90b3Nob3AgQ0MgMjAxOCAoV2lu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
							   <div>
							      <pre><b><span style=\\\"font-family: arial, sans-serif; white-space: normal;\\\">toto </span><br style=\\\"font-family: arial, sans-serif; white-space: normal;\\\"></b><div style=\\\"white-space: normal; font-family: Helvetica; font-size: 12px; word-wrap: break-word; -webkit-nbsp-mode: space; -webkit-line-break: after-white-space;\\\"><span style=\\\"orphans: 2; widows: 2;\\\">toto@devenv.blue</span>
							   </div>
							   <div style=\\\"white-space: normal; font-family: Helvetica; font-size: 12px; word-wrap: break-word; -webkit-nbsp-mode: space; -webkit-line-break: after-white-space;\\\"><br>
							</div>
							'\r\n;

											""";
		String uid = "test_" + System.nanoTime();

		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, event, sendNotifications);

		assertEquals(CREATE, CalendarTestSyncHook.action());
		assertEquals(CREATE, CalendarTestAsyncHook.action());
		assertNotNull(CalendarTestSyncHook.message());
		ESearchActivator.refreshIndex(dataStreamName);

		Awaitility.await().atMost(2, TimeUnit.SECONDS).until(() -> {
			SearchResponse<AuditLogEntry> response = esClient.search(s -> s //
					.index(dataStreamName) //
					.query(q -> q.bool(b -> b
							.must(TermQuery.of(t -> t.field("logtype").value(userCalendarContainer.type))._toQuery())
							.must(TermQuery.of(t -> t.field("action").value(Type.Created.toString()))._toQuery()))),
					AuditLogEntry.class);
			return 1L == response.hits().total().value();
		});
		SearchResponse<AuditLogEntry> response = esClient.search(s -> s //
				.index(dataStreamName) //
				.query(q -> q.bool(
						b -> b.must(TermQuery.of(t -> t.field("logtype").value(userCalendarContainer.type))._toQuery())
								.must(TermQuery.of(t -> t.field("action").value(Type.Created.toString()))._toQuery()))),
				AuditLogEntry.class);

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
		ESearchActivator.refreshIndex(dataStreamName);

		Awaitility.await().atMost(2, TimeUnit.SECONDS).until(() -> {
			SearchResponse<AuditLogEntry> response = esClient.search(s -> s //
					.index(dataStreamName) //
					.query(q -> q.bool(b -> b
							.must(TermQuery.of(t -> t.field("logtype").value(userCalendarContainer.type))._toQuery())
							.must(TermQuery.of(t -> t.field("action").value(Type.Updated.toString()))._toQuery()))),
					AuditLogEntry.class);
			return 1L == response.hits().total().value();
		});
		SearchResponse<AuditLogEntry> response = esClient.search(s -> s //
				.index(dataStreamName) //
				.query(q -> q.bool(
						b -> b.must(TermQuery.of(t -> t.field("logtype").value(userCalendarContainer.type))._toQuery())
								.must(TermQuery.of(t -> t.field("action").value(Type.Updated.toString()))._toQuery()))),
				AuditLogEntry.class);

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
	public void testUpdateRemoveAttendee() throws ServerFault, ElasticsearchException, IOException {

		ElasticsearchClient esClient = ESearchActivator.getClient();
		VEventSeries event = defaultVEvent();
		String uid = "test_" + System.nanoTime();
		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, event, sendNotifications);

		event.main.attendees = new ArrayList<>(1);
		getCalendarService(userSecurityContext, userCalendarContainer).update(uid, event, sendNotifications);
		ESearchActivator.refreshIndex(dataStreamName);

		Awaitility.await().atMost(2, TimeUnit.SECONDS).until(() -> {
			SearchResponse<AuditLogEntry> response = esClient.search(s -> s//
					.index(dataStreamName) //
					.query(q -> q.bool(b -> b
							.must(TermQuery.of(t -> t.field("logtype").value(userCalendarContainer.type))._toQuery())
							.must(TermQuery.of(t -> t.field("action").value(Type.Updated.toString()))._toQuery()))),
					AuditLogEntry.class);
			return 1L == response.hits().total().value();
		});

		SearchResponse<AuditLogEntry> response = esClient.search(s -> s//
				.index(dataStreamName) //
				.query(q -> q.bool(
						b -> b.must(TermQuery.of(t -> t.field("logtype").value(userCalendarContainer.type))._toQuery())
								.must(TermQuery.of(t -> t.field("action").value(Type.Updated.toString()))._toQuery()))),
				AuditLogEntry.class);
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
	public void testUpdateAddedAttendee() throws ServerFault, ElasticsearchException, IOException {

		ElasticsearchClient esClient = ESearchActivator.getClient();
		VEventSeries event = defaultVEvent();
		String uid = "test_" + System.nanoTime();
		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, event, sendNotifications);

		Attendee sylvain = Attendee.create(CUType.Individual, "", Role.Chair, ParticipationStatus.Accepted, true, "",
				"", "", "sylvain", null, null, null, "sylvain@attendee.lan");
		Attendee nico = Attendee.create(CUType.Individual, "", Role.Chair, ParticipationStatus.Accepted, true, "", "",
				"", "nico", null, null, null, "nico@attendee.lan");
		event.main.attendees.addAll(Arrays.asList(sylvain, nico));
		getCalendarService(userSecurityContext, userCalendarContainer).update(uid, event, sendNotifications);
		ESearchActivator.refreshIndex(dataStreamName);

		Awaitility.await().atMost(3, TimeUnit.SECONDS).until(() -> {
			SearchResponse<AuditLogEntry> response = esClient.search(s -> s //
					.index(dataStreamName) //
					.query(q -> q.bool(b -> b
							.must(TermQuery.of(t -> t.field("logtype").value(userCalendarContainer.type))._toQuery())
							.must(TermQuery.of(t -> t.field("action").value(Type.Updated.toString()))._toQuery()))),
					AuditLogEntry.class);
			return 1L == response.hits().total().value();
		});

		SearchResponse<AuditLogEntry> response = esClient.search(s -> s //
				.index(dataStreamName) //
				.query(q -> q.bool(
						b -> b.must(TermQuery.of(t -> t.field("logtype").value(userCalendarContainer.type))._toQuery())
								.must(TermQuery.of(t -> t.field("action").value(Type.Updated.toString()))._toQuery()))),
				AuditLogEntry.class);
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
	public void testUpdateAttendeesStatus() throws ServerFault, ElasticsearchException, IOException {

		ElasticsearchClient esClient = ESearchActivator.getClient();
		VEventSeries event = defaultVEvent();
		String uid = "test_" + System.nanoTime();
		Attendee sylvain = Attendee.create(CUType.Individual, "", Role.Chair, ParticipationStatus.NeedsAction, true, "",
				"", "", "sylvain", null, null, null, "sylvain@attendee.lan");
		Attendee nico = Attendee.create(CUType.Individual, "", Role.Chair, ParticipationStatus.NeedsAction, true, "",
				"", "", "nico", null, null, null, "nico@attendee.lan");
		event.main.attendees.addAll(Arrays.asList(sylvain, nico));
		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, event, sendNotifications);

		// Change attendee status
		OptionalInt matchInt = IntStream.range(0, event.main.attendees.size())
				.filter(i -> "sylvain@attendee.lan".equals(event.main.attendees.get(i).mailto)).findFirst();
		assertTrue(matchInt.isPresent());
		event.main.attendees.get(matchInt.getAsInt()).partStatus = ParticipationStatus.Accepted;

		getCalendarService(userSecurityContext, userCalendarContainer).update(uid, event, sendNotifications);
		ESearchActivator.refreshIndex(dataStreamName);

		Awaitility.await().atMost(2, TimeUnit.SECONDS).until(() -> {
			SearchResponse<AuditLogEntry> response = esClient.search(s -> s //
					.index(dataStreamName) //
					.query(q -> q.bool(b -> b
							.must(TermQuery.of(t -> t.field("logtype").value(userCalendarContainer.type))._toQuery())
							.must(TermQuery.of(t -> t.field("action").value(Type.Updated.toString()))._toQuery()))),
					AuditLogEntry.class);
			return 1L == response.hits().total().value();
		});

		SearchResponse<AuditLogEntry> response = esClient.search(s -> s //
				.index(dataStreamName) //
				.query(q -> q.bool(
						b -> b.must(TermQuery.of(t -> t.field("logtype").value(userCalendarContainer.type))._toQuery())
								.must(TermQuery.of(t -> t.field("action").value(Type.Updated.toString()))._toQuery()))),
				AuditLogEntry.class);
		AuditLogEntry firstEntry = response.hits().hits().get(0).source();
		assertTrue(firstEntry.updatemessage
				.equals("sylvain@attendee.lan: participation status changed from 'NeedsAction' to 'Accepted'"));
	}

	@Test
	public void testUpdateDescription() throws ServerFault, ElasticsearchException, IOException {

		ElasticsearchClient esClient = ESearchActivator.getClient();
		VEventSeries event = defaultVEvent();
		String uid = "test_" + System.nanoTime();
		Attendee sylvain = Attendee.create(CUType.Individual, "", Role.Chair, ParticipationStatus.NeedsAction, true, "",
				"", "", "sylvain", null, null, null, "sylvain@attendee.lan");
		Attendee nico = Attendee.create(CUType.Individual, "", Role.Chair, ParticipationStatus.NeedsAction, true, "",
				"", "", "nico", null, null, null, "nico@attendee.lan");
		event.main.attendees.addAll(Arrays.asList(sylvain, nico));
		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, event, sendNotifications);

		// change description
		event.main.description = """
				<META HTTP-EQUIV=\\\"Content-Type\\\" CONTENT=\\\"text/html; charset=iso-8857-1\\\">
							\\r\\n<div style=\\\"font-family: Montserrat, montserrat, &quot;Source Sans&quot;, &quot;Helvetica Neue&quot;, Helvetica, Arial, sans-serif; font-size: 9.75pt; color: rgb(31, 31, 31);\\\"><br></div>
							<div data-bm-signature=\\\"a9cbedd6-76b7-4cbd-a9c9-b693cb7d77fc\\\">
							   <div>-- <br></div>
							   <img src=\\\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAA+gAAAFoCAYAAADEjNEVAAAACXBIWXMAAAsTAAALEwEAmpwYAAAHP2lUWHRYTUw6Y29tLmFkb2JlLnhtcAAAAAAAPD94cGFja2V0IGJlZ2luPSLvu78iIGlkPSJXNU0wTXBDZWhpSHpyZVN6TlRjemtjOWQiPz4gPHg6eG1wbWV0YSB4bWxuczp4PSJhZG9iZTpuczptZXRhLyIgeDp4bXB0az0iQWRvYmUgWE1QIENvcmUgNS42LWMxNDIgNzkuMTYwOTI0LCAyMDE3LzA3LzEzLTAxOjA2OjM5ICAgICAgICAiPiA8cmRmOlJERiB4bWxuczpyZGY9Imh0dHA6Ly93d3cudzMub3JnLzE5OTkvMDIvMjItcmRmLXN5bnRheC1ucyMiPiA8cmRmOkRlc2NyaXB0aW9uIHJkZjphYm91dD0iIiB4bWxuczp4bXA9Imh0dHA6Ly9ucy5hZG9iZS5jb20veGFwLzEuMC8iIHhtbG5zOmRjPSJodHRwOi8vcHVybC5vcmcvZGMvZWxlbWVudHMvMS4xLyIgeG1sbnM6cGhvdG9zaG9wPSJodHRwOi8vbnMuYWRvYmUuY29tL3Bob3Rvc2hvcC8xLjAvIiB4bWxuczp4bXBNTT0iaHR0cDovL25zLmFkb2JlLmNvbS94YXAvMS4wL21tLyIgeG1sbnM6c3RFdnQ9Imh0dHA6Ly9ucy5hZG9iZS5jb20veGFwLzEuMC9zVHlwZS9SZXNvdXJjZUV2ZW50IyIgeG1wOkNyZWF0b3JUb29sPSJBZG9iZSBQaG90b3Nob3AgQ0MgMjAxOCAoV2luZG93cykiIHhtcDpDcmVhdGVEYXRlPSIyMDE4LTA2LTA4VDEyOjAwOjU1KzAyOjAwIiB4bXA6TW9kaWZ5RGF0ZT0iMjAxOS0wMy0yOFQxNTowNTowNiswMTowMCIgeG1wOk1ldGFkYXRhRGF0ZT0iMjAxOS0wMy0yOFQxNTowNTowNiswMTowMCIgZGM6Zm9ybWF0PSJpbWFnZS9wbmciIHBob3Rvc2hvcDpDb2xvck1vZGU9IjMiIHhtcE1NOkluc3RhbmNlSUQ9InhtcC5paWQ6ZGQzM2Q4ZmUtZGVkYy1mMzRhLTlkZTktMTc5MDA2YTM3OTlkIiB4bXBNTTpEb2N1bWVudElEPSJhZG9iZTpkb2NpZDpwaG90b3Nob3A6YzY3NGU2N2EtOGM5Yy1hNDQ1LWE2YjktODU3Yzc3NTIxY2Q5IiB4bXBNTTpPcmlnaW5hbERvY3VtZW50SUQ9InhtcC5kaWQ6M2RiODZjMTItZDIzZi05NDQ1LThlYmQtNTBlZTU2MmYzYTQ1Ij4gPHBob3Rvc2hvcDpEb2N1bWVudEFuY2VzdG9ycz4gPHJkZjpCYWc+IDxyZGY6bGk+YWRvYmU6ZG9jaWQ6cGhvdG9zaG9wOjhmNjExZmIwLTgyNDItNDg0Mi05MjBhLTY5NTQyN2M1MTQxZjwvcmRmOmxpPiA8L3JkZjpCYWc+IDwvcGhvdG9zaG9wOkRvY3VtZW50QW5jZXN0b3JzPiA8eG1wTU06SGlzdG9yeT4gPHJkZjpTZXE+IDxyZGY6bGkgc3RFdnQ6YWN0aW9uPSJjcmVhdGVkIiBzdEV2dDppbnN0YW5jZUlEPSJ4bXAuaWlkOjNkYjg2YzEyLWQyM2YtOTQ0NS04ZWJkLTUwZWU1NjJmM2E0NSIgc3RFdnQ6d2hlbj0iMjAxOC0wNi0wOFQxMjowMDo1NSswMjowMCIgc3RFdnQ6c29mdHdhcmVBZ2VudD0iQWRvYmUgUGhvdG9zaG9wIENDIDIwMTggKFdpbmRvd3MpIi8+IDxyZGY6bGkgc3RFdnQ6YWN0aW9uPSJzYXZlZCIgc3RFdnQ6aW5zdGFuY2VJRD0ieG1wLmlpZDoyOTE1NGUzMi1kYWM0LWJmNDgtYmM4Yy02MGI4YTQ3Yjk1YmYiIHN0RXZ0OndoZW49IjIwMTktMDItMDdUMTQ6Mzk6MTkrMDE6MDAiIHN0RXZ0OnNvZnR3YXJlQWdlbnQ9IkFkb2JlIFBob3Rvc2hvcCBDQyAyMDE4IChXaW5kb3dzKSIgc3RFdnQ6Y2hhbmdlZD0iLyIvPiA8cmRmOmxpIHN0RXZ0OmFjdGlvbj0ic2F2ZWQiIHN0RXZ0Omluc3RhbmNlSUQ9InhtcC5paWQ6ZGQzM2Q4ZmUtZGVkYy1mMzRhLTlkZTktMTc5MDA2YTM3OTlkIiBzdEV2dDp3aGVuPSIyMDE5LTAzLTI4VDE1OjA1OjA2KzAxOjAwIiBzdEV2dDpzb2Z0d2FyZUFnZW50PSJBZG9iZSBQaG90b3Nob3AgQ0MgMjAxOCAoV2lu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
							   <div>
							      <pre><b><span style=\\\"font-family: arial, sans-serif; white-space: normal;\\\">toto </span><br style=\\\"font-family: arial, sans-serif; white-space: normal;\\\"></b><div style=\\\"white-space: normal; font-family: Helvetica; font-size: 12px; word-wrap: break-word; -webkit-nbsp-mode: space; -webkit-line-break: after-white-space;\\\"><span style=\\\"orphans: 2; widows: 2;\\\">toto@devenv.blue</span>
							   </div>
							   <div style=\\\"white-space: normal; font-family: Helvetica; font-size: 12px; word-wrap: break-word; -webkit-nbsp-mode: space; -webkit-line-break: after-white-space;\\\"><br>
							</div>
							'\r\n;
							""";
		getCalendarService(userSecurityContext, userCalendarContainer).update(uid, event, sendNotifications);
		ESearchActivator.refreshIndex(dataStreamName);

		Awaitility.await().atMost(2, TimeUnit.SECONDS).until(() -> {
			SearchResponse<AuditLogEntry> response = esClient.search(s -> s //
					.index(dataStreamName) //
					.query(q -> q.bool(b -> b
							.must(TermQuery.of(t -> t.field("logtype").value(userCalendarContainer.type))._toQuery())
							.must(TermQuery.of(t -> t.field("action").value(Type.Updated.toString()))._toQuery()))),
					AuditLogEntry.class);
			return 1L == response.hits().total().value();
		});

		SearchResponse<AuditLogEntry> response = esClient.search(s -> s //
				.index(dataStreamName) //
				.query(q -> q.bool(
						b -> b.must(TermQuery.of(t -> t.field("logtype").value(userCalendarContainer.type))._toQuery())
								.must(TermQuery.of(t -> t.field("action").value(Type.Updated.toString()))._toQuery()))),
				AuditLogEntry.class);
		AuditLogEntry firstEntry = response.hits().hits().get(0).source();
		assertTrue(firstEntry.updatemessage.startsWith("event description changed: 'Lorem ipsum' -> '\\r\\n"));
	}

	@Test
	public void testUpdateChangedLocation() throws ServerFault, ElasticsearchException, IOException {

		ElasticsearchClient esClient = ESearchActivator.getClient();
		VEventSeries event = defaultVEvent();
		String uid = "test_" + System.nanoTime();
		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, event, sendNotifications);

		event.main.location = "Marseillette";
		getCalendarService(userSecurityContext, userCalendarContainer).update(uid, event, sendNotifications);
		ESearchActivator.refreshIndex(dataStreamName);

		Awaitility.await().atMost(2, TimeUnit.SECONDS).until(() -> {
			SearchResponse<AuditLogEntry> response = esClient.search(s -> s //
					.index(dataStreamName) //
					.query(q -> q.bool(b -> b
							.must(TermQuery.of(t -> t.field("logtype").value(userCalendarContainer.type))._toQuery())
							.must(TermQuery.of(t -> t.field("action").value(Type.Updated.toString()))._toQuery()))),
					AuditLogEntry.class);
			return 1L == response.hits().total().value();
		});

		SearchResponse<AuditLogEntry> response = esClient.search(s -> s //
				.index(dataStreamName) //
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
	public void testUpdateChangedEndDate() throws ServerFault, ElasticsearchException, IOException {
		ElasticsearchClient esClient = ESearchActivator.getClient();
		VEventSeries event = defaultVEvent();
		String uid = "test_" + System.nanoTime();
		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, event, sendNotifications);

		ZonedDateTime newDtEndDate = ZonedDateTime.of(2023, 9, 25, 10, 0, 0, 0, tz);
		BmDateTime newDate = BmDateTimeWrapper.create(newDtEndDate, Precision.DateTime);
		BmDateTime oldDate = event.main.dtend;
		event.main.dtend = newDate;
		getCalendarService(userSecurityContext, userCalendarContainer).update(uid, event, sendNotifications);
		ESearchActivator.refreshIndex(dataStreamName);

		Awaitility.await().atMost(2, TimeUnit.SECONDS).until(() -> {
			SearchResponse<AuditLogEntry> response = esClient.search(s -> s //
					.index(dataStreamName) //
					.query(q -> q.bool(b -> b
							.must(TermQuery.of(t -> t.field("logtype").value(userCalendarContainer.type))._toQuery())
							.must(TermQuery.of(t -> t.field("action").value(Type.Updated.toString()))._toQuery()))),
					AuditLogEntry.class);
			return 1L == response.hits().total().value();
		});

		SearchResponse<AuditLogEntry> response = esClient.search(s -> s //
				.index(dataStreamName) //
				.query(q -> q.bool(
						b -> b.must(TermQuery.of(t -> t.field("logtype").value(userCalendarContainer.type))._toQuery())
								.must(TermQuery.of(t -> t.field("action").value(Type.Updated.toString()))._toQuery()))),
				AuditLogEntry.class);
		AuditLogEntry firstEntry = response.hits().hits().get(0).source();
		assertTrue(firstEntry.updatemessage
				.equals("event end date changed: '" + oldDate.iso8601 + "' -> '" + newDate.iso8601 + "'"));
	}

	@Test
	public void testUpdateChangedStartDate() throws ServerFault, ElasticsearchException, IOException {
		ElasticsearchClient esClient = ESearchActivator.getClient();
		VEventSeries event = defaultVEvent();
		String uid = "test_" + System.nanoTime();
		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, event, sendNotifications);

		ZonedDateTime newDtEndDate = ZonedDateTime.of(1973, 9, 25, 10, 0, 0, 0, tz);
		BmDateTime newDate = BmDateTimeWrapper.create(newDtEndDate, Precision.DateTime);
		BmDateTime oldDate = event.main.dtend;
		event.main.dtstart = newDate;
		getCalendarService(userSecurityContext, userCalendarContainer).update(uid, event, sendNotifications);
		ESearchActivator.refreshIndex(dataStreamName);

		Awaitility.await().atMost(2, TimeUnit.SECONDS).until(() -> {
			SearchResponse<AuditLogEntry> response = esClient.search(s -> s //
					.index(dataStreamName) //
					.query(q -> q.bool(b -> b
							.must(TermQuery.of(t -> t.field("logtype").value(userCalendarContainer.type))._toQuery())
							.must(TermQuery.of(t -> t.field("action").value(Type.Updated.toString()))._toQuery()))),
					AuditLogEntry.class);
			return 1L == response.hits().total().value();
		});

		SearchResponse<AuditLogEntry> response = esClient.search(s -> s //
				.index(dataStreamName) //
				.query(q -> q.bool(
						b -> b.must(TermQuery.of(t -> t.field("logtype").value(userCalendarContainer.type))._toQuery())
								.must(TermQuery.of(t -> t.field("action").value(Type.Updated.toString()))._toQuery()))),
				AuditLogEntry.class);
		AuditLogEntry firstEntry = response.hits().hits().get(0).source();
		assertTrue(firstEntry.updatemessage
				.equals("event start date changed: '" + oldDate.iso8601 + "' -> '" + newDate.iso8601 + "'"));
	}

	@Test
	public void testUpdateAddedReccurence() throws ServerFault, ElasticsearchException, IOException {
		ElasticsearchClient esClient = ESearchActivator.getClient();
		VEventSeries event = defaultVEvent();
		String uid = "test_" + System.nanoTime();
		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, event, sendNotifications);

		event.main.rrule = new RRule();
		event.main.rrule.frequency = Frequency.WEEKLY;
		event.main.rrule.interval = 10;
		event.main.rrule.byDay = Arrays.asList(WeekDay.tu(), WeekDay.fr());
		getCalendarService(userSecurityContext, userCalendarContainer).update(uid, event, sendNotifications);
		ESearchActivator.refreshIndex(dataStreamName);

		Awaitility.await().atMost(2, TimeUnit.SECONDS).until(() -> {
			SearchResponse<AuditLogEntry> response = esClient.search(s -> s //
					.index(dataStreamName) //
					.query(q -> q.bool(b -> b
							.must(TermQuery.of(t -> t.field("logtype").value(userCalendarContainer.type))._toQuery())
							.must(TermQuery.of(t -> t.field("action").value(Type.Updated.toString()))._toQuery()))),
					AuditLogEntry.class);
			return 1L == response.hits().total().value();
		});

		SearchResponse<AuditLogEntry> response = esClient.search(s -> s //
				.index(dataStreamName) //
				.query(q -> q.bool(
						b -> b.must(TermQuery.of(t -> t.field("logtype").value(userCalendarContainer.type))._toQuery())
								.must(TermQuery.of(t -> t.field("action").value(Type.Updated.toString()))._toQuery()))),
				AuditLogEntry.class);
		AuditLogEntry firstEntry = response.hits().hits().get(0).source();
		assertTrue(firstEntry.updatemessage.contains("event end date changed:"));
	}

	@Test
	public void testUpdateChangedReccurenceFrequency() throws ServerFault, ElasticsearchException, IOException {
		ElasticsearchClient esClient = ESearchActivator.getClient();
		VEventSeries event = defaultVEvent();
		String uid = "test_" + System.nanoTime();
		event.main.rrule = new RRule();
		event.main.rrule.frequency = Frequency.WEEKLY;
		event.main.rrule.interval = 10;
		event.main.rrule.byDay = Arrays.asList(WeekDay.tu(), WeekDay.fr());
		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, event, sendNotifications);

		event.main.rrule.frequency = Frequency.MONTHLY;
		getCalendarService(userSecurityContext, userCalendarContainer).update(uid, event, sendNotifications);
		ESearchActivator.refreshIndex(dataStreamName);

		Awaitility.await().atMost(2, TimeUnit.SECONDS).until(() -> {
			SearchResponse<AuditLogEntry> response = esClient.search(s -> s //
					.index(dataStreamName) //
					.query(q -> q.bool(b -> b
							.must(TermQuery.of(t -> t.field("logtype").value(userCalendarContainer.type))._toQuery())
							.must(TermQuery.of(t -> t.field("action").value(Type.Updated.toString()))._toQuery()))),
					AuditLogEntry.class);
			return 1L == response.hits().total().value();
		});

		SearchResponse<AuditLogEntry> response = esClient.search(s -> s //
				.index(dataStreamName) //
				.query(q -> q.bool(
						b -> b.must(TermQuery.of(t -> t.field("logtype").value(userCalendarContainer.type))._toQuery())
								.must(TermQuery.of(t -> t.field("action").value(Type.Updated.toString()))._toQuery()))),
				AuditLogEntry.class);
		AuditLogEntry firstEntry = response.hits().hits().get(0).source();
		assertTrue(firstEntry.updatemessage.equals("Changed event occurence frequency: 'WEEKLY' -> 'MONTHLY'"));
	}

	@Test
	public void testUpdateChangedReccurenceDays() throws ServerFault, ElasticsearchException, IOException {
		ElasticsearchClient esClient = ESearchActivator.getClient();
		VEventSeries event = defaultVEvent();
		String uid = "test_" + System.nanoTime();
		event.main.rrule = new RRule();
		event.main.rrule.frequency = Frequency.WEEKLY;
		event.main.rrule.interval = 10;
		event.main.rrule.byDay = Arrays.asList(WeekDay.tu(), WeekDay.fr());
		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, event, sendNotifications);

		event.main.rrule.byDay = Arrays.asList(WeekDay.tu(), WeekDay.sa());
		getCalendarService(userSecurityContext, userCalendarContainer).update(uid, event, sendNotifications);
		ESearchActivator.refreshIndex(dataStreamName);

		Awaitility.await().atMost(2, TimeUnit.SECONDS).until(() -> {
			SearchResponse<AuditLogEntry> response = esClient.search(s -> s //
					.index(dataStreamName) //
					.query(q -> q.bool(b -> b
							.must(TermQuery.of(t -> t.field("logtype").value(userCalendarContainer.type))._toQuery())
							.must(TermQuery.of(t -> t.field("action").value(Type.Updated.toString()))._toQuery()))),
					AuditLogEntry.class);
			return 1L == response.hits().total().value();
		});

		SearchResponse<AuditLogEntry> response = esClient.search(s -> s //
				.index(dataStreamName) //
				.query(q -> q.bool(
						b -> b.must(TermQuery.of(t -> t.field("logtype").value(userCalendarContainer.type))._toQuery())
								.must(TermQuery.of(t -> t.field("action").value(Type.Updated.toString()))._toQuery()))),
				AuditLogEntry.class);
		AuditLogEntry firstEntry = response.hits().hits().get(0).source();
		assertTrue(firstEntry.updatemessage.equals("Changed event occurence day: '[TU, FR] -> [TU, SA]'"));
	}

	@Test
	public void testUpdateChangedReccurenceMinutes() throws ServerFault, ElasticsearchException, IOException {
		ElasticsearchClient esClient = ESearchActivator.getClient();
		VEventSeries event = defaultVEvent();
		String uid = "test_" + System.nanoTime();
		event.main.rrule = new RRule();
		event.main.rrule.frequency = Frequency.WEEKLY;
		event.main.rrule.interval = 10;
		event.main.rrule.byDay = Arrays.asList(WeekDay.tu(), WeekDay.fr());
		event.main.rrule.byMinute = Arrays.asList(0, 30);
		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, event, sendNotifications);

		event.main.rrule.byMinute = Arrays.asList(0, 40);
		getCalendarService(userSecurityContext, userCalendarContainer).update(uid, event, sendNotifications);
		ESearchActivator.refreshIndex(dataStreamName);

		Awaitility.await().atMost(2, TimeUnit.SECONDS).until(() -> {
			SearchResponse<AuditLogEntry> response = esClient.search(s -> s //
					.index(dataStreamName) //
					.query(q -> q.bool(b -> b
							.must(TermQuery.of(t -> t.field("logtype").value(userCalendarContainer.type))._toQuery())
							.must(TermQuery.of(t -> t.field("action").value(Type.Updated.toString()))._toQuery()))),
					AuditLogEntry.class);
			return 1L == response.hits().total().value();
		});

		SearchResponse<AuditLogEntry> response = esClient.search(s -> s //
				.index(dataStreamName) //
				.query(q -> q.bool(
						b -> b.must(TermQuery.of(t -> t.field("logtype").value(userCalendarContainer.type))._toQuery())
								.must(TermQuery.of(t -> t.field("action").value(Type.Updated.toString()))._toQuery()))),
				AuditLogEntry.class);
		AuditLogEntry firstEntry = response.hits().hits().get(0).source();
		assertTrue(firstEntry.updatemessage.equals("Changed event occurence minute: '[0, 30] -> [0, 40]'"));
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
		ESearchActivator.refreshIndex(dataStreamName);

		Awaitility.await().atMost(2, TimeUnit.SECONDS).until(() -> {
			SearchResponse<AuditLogEntry> response = esClient.search(s -> s //
					.index(dataStreamName) //
					.query(q -> q.bool(b -> b
							.must(TermQuery.of(t -> t.field("logtype").value(userCalendarContainer.type))._toQuery())
							.must(TermQuery.of(t -> t.field("action").value(Type.Deleted.toString()))._toQuery()))),
					AuditLogEntry.class);
			return 1L == response.hits().total().value();
		});

		SearchResponse<AuditLogEntry> response = esClient.search(s -> s //
				.index(dataStreamName) //
				.query(q -> q.bool(
						b -> b.must(TermQuery.of(t -> t.field("logtype").value(userCalendarContainer.type))._toQuery())
								.must(TermQuery.of(t -> t.field("action").value(Type.Created.toString()))._toQuery()))),
				AuditLogEntry.class);
		assertEquals(1L, response.hits().total().value());

		response = esClient.search(s -> s //
				.index(dataStreamName) //
				.query(q -> q.bool(
						b -> b.must(TermQuery.of(t -> t.field("logtype").value(userCalendarContainer.type))._toQuery())
								.must(TermQuery.of(t -> t.field("action").value(Type.Updated.toString()))._toQuery()))),
				AuditLogEntry.class);
		assertEquals(0L, response.hits().total().value());

		response = esClient.search(s -> s //
				.index(dataStreamName) //
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

	@Override
	protected VEventSeries defaultVEvent() {
		ZoneId tz = ZoneId.of("Asia/Ho_Chi_Minh");
		return defaultVEvent(ZonedDateTime.of(2022, 2, 13, 1, 0, 0, 0, tz));
	}

	/**
	 * @return
	 */
	@Override
	protected VEventSeries defaultVEvent(ZonedDateTime start) {
		VEventSeries series = new VEventSeries();
		VEvent event = new VEvent();
		event.dtstart = BmDateTimeHelper.time(start);
		event.summary = "event " + System.currentTimeMillis();
		event.location = "Toulouse";
		event.description = "Lorem ipsum";
		event.transparency = VEvent.Transparency.Opaque;
		event.classification = Classification.Private;
		event.status = Status.Confirmed;
		event.priority = 3;
		event.url = "https://www.devenv.blue";
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

		List<Attendee> attendees = new ArrayList<>(1);
		Attendee me = Attendee.create(CUType.Individual, "", Role.Chair, ParticipationStatus.Accepted, true, "", "", "",
				"osef", null, null, null, "external@attendee.lan");
		Attendee david = Attendee.create(CUType.Individual, "", Role.Chair, ParticipationStatus.Accepted, true, "", "",
				"", "david", null, null, null, "david@attendee.lan");
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