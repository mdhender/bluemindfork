/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2019
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.calendar.sync.tests;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.SettableFuture;

import net.bluemind.calendar.api.CalendarDescriptor;
import net.bluemind.calendar.api.ICalendar;
import net.bluemind.calendar.api.ICalendarsMgmt;
import net.bluemind.calendar.api.IVEvent;
import net.bluemind.calendar.sync.CalendarContainerSync;
import net.bluemind.calendar.sync.CalendarSyncVerticle;
import net.bluemind.core.api.Stream;
import net.bluemind.core.container.api.IContainerSync;
import net.bluemind.core.container.model.ContainerSyncStatus;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.rest.base.GenericStream;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.domain.api.IDomainSettings;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.PopulateHelper;

/** @see CalendarSyncVerticle */
public class CalendarSyncVerticleTests {
	private static final String ICS_FILE_1_EVENT = "resources/ics-test-1-event.ics";
	private static final String ICS_FILE_2_EVENTS = "resources/ics-test-2-events.ics";
	private static final String ICS_FILE_3_EVENTS = "resources/ics-test-3-events.ics";
	private static final String ICS_FILE_4_EVENTS = "resources/ics-test-4-events.ics";
	private static final String ICS_FILE_52_EVENTS = "resources/ics-test-52-events.ics";
	private static final String ICS_FILE_BAD_CONTENT = "resources/ics-test-bad-content.ics";
	private static final String ICS_FILE_BAD_CONTENT_2 = "resources/ics-test-bad-content-2.ics";
	private static final String ICS_URL = "http://localhost:8091/ics";
	private static final String CALENDAR_UID = "bluemind-test-calendar-id";
	private static final String ETAG_1 = "W/\"etag-1-token\"";
	private static final String ETAG_2 = "W/\"etag-2-token\"";
	private static final String ETAG_3 = "W/\"etag-3-token\"";
	private static final ZonedDateTime LAST_MODIF_DATE = ZonedDateTime.of(LocalDateTime.now(), ZoneId.systemDefault());
	/** This embedded HTTP server simulates an external calendar server. */
	private static HttpServer icsHttpServer;
	private String domain = "bm.lan";
	private String userUid = "jdoe";
	private SecurityContext johnDoeContext;
	private ServerSideServiceProvider systemProvider;

	/** Store the last sync date for comparisons. */
	private Date lastSync;

	/** Store the last ICS date for comparisons. */
	private String previousIcsContent;

	/**
	 * The next HTTP response the embedded server will do. It is 'prepared' by
	 * each test.
	 */
	private PreparedResponse nextResponse;
	private boolean lastSyncHasUpdatedCalendar;

	private static class PreparedResponse {
		String returnedIcs;
		Map<String, String> headers;
		public ZonedDateTime lastModified;
		public String etag;

		public PreparedResponse() {
			this(ICS_FILE_1_EVENT);
		}

		public PreparedResponse(final String returnedIcs) {
			this.returnedIcs = returnedIcs;
			this.headers = new HashMap<>();
		}
	}

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		ElasticsearchTestHelper.getInstance().beforeTest();
		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		esServer.tags = Lists.newArrayList("bm/es");

		PopulateHelper.initGlobalVirt(esServer);

		final SettableFuture<Void> future = SettableFuture.<Void>create();
		Handler<AsyncResult<Void>> done = new Handler<AsyncResult<Void>>() {

			@Override
			public void handle(AsyncResult<Void> event) {
				future.set(null);
			}
		};
		VertxPlatform.spawnVerticles(done);

		future.get();

		this.johnDoeContext = new SecurityContext("sid", userUid, Arrays.<String>asList(), Arrays.<String>asList(),
				domain);

		Sessions.get().put(johnDoeContext.getSessionId(), johnDoeContext);

		PopulateHelper.createTestDomain(domain);
		PopulateHelper.addDomainAdmin("admin", domain);
		PopulateHelper.addUser(userUid, domain);

		this.systemProvider = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);

		// here we can detect if a calendar update has been made
		VertxPlatform.eventBus().registerLocalHandler("bm.calendar.hook.changed", event -> {
			this.lastSyncHasUpdatedCalendar = true;
		});
	}

	@Before
	public void startIcsHttpServer() {
		icsHttpServer = VertxPlatform.getVertx().createHttpServer();
		RouteMatcher router = new RouteMatcher();
		router.noMatch(req -> {
			req.response().setStatusCode(400).end();
		});

		router.head("/ics", this::handleHead);
		router.get("/ics", this::handleGet);

		icsHttpServer.requestHandler(router).listen(8091);
	}

	private void handleHead(final HttpServerRequest event) {
		this.nextResponse.headers.forEach((key, value) -> event.response().putHeader(key, value));
		event.response().setStatusCode(this.computeStatus(event)).end();
	}

	private void handleGet(final HttpServerRequest event) {
		this.nextResponse.headers.forEach((key, value) -> event.response().putHeader(key, value));
		event.response().setStatusCode(this.computeStatus(event)).sendFile(this.nextResponse.returnedIcs).end();
	}

	private int computeStatus(final HttpServerRequest event) {
		final int status;
		if (this.lastModifiedHasNotChanged(event) || this.sameEtag(event)) {
			status = 304;
		} else {
			status = 200;
		}
		return status;
	}

	@After
	public void stopIcsHttpServer() {
		icsHttpServer.close();
	}

	/**
	 * Trigger several times the sync for a never-changing ics ("MD5 mechanism"
	 * is triggered).
	 */
	@Test
	public void testNoChanges() throws InterruptedException {
		this.init();

		// first sync replaces the initial empty ics
		this.nextResponse = new PreparedResponse();
		this.checkSyncOkWithChanges();

		// other syncs are done but calendar does not change
		this.checkSyncOkNoUpdates();
		this.checkSyncOkNoUpdates();
		this.checkSyncOkNoUpdates();
	}

	/**
	 * Trigger several times the sync for a changing ics (one more event each
	 * time).
	 */
	@Test
	public void testWithChanges() throws InterruptedException {
		this.init();

		// first sync replaces the initial empty ics
		this.nextResponse = new PreparedResponse();
		this.checkSyncOkWithChanges();

		// other syncs are done and calendars changed
		this.nextResponse = new PreparedResponse(ICS_FILE_2_EVENTS);
		this.checkSyncOkWithChanges();
		this.nextResponse = new PreparedResponse(ICS_FILE_3_EVENTS);
		this.checkSyncOkWithChanges();
		this.nextResponse = new PreparedResponse(ICS_FILE_4_EVENTS);
		this.checkSyncOkWithChanges();
	}

	/**
	 * Due to a big minimum delay between synchronizations, no more than one
	 * sync should be done.
	 */
	@Test
	public void testWithChangesBigDelay() throws InterruptedException {
		this.init(5000);

		// first sync replaces the initial empty ics
		this.nextResponse = new PreparedResponse();
		this.checkSyncOkWithChanges();

		// no other sync should be done
		this.nextResponse = new PreparedResponse(ICS_FILE_2_EVENTS);
		this.checkNoSync();
		this.nextResponse = new PreparedResponse(ICS_FILE_3_EVENTS);
		this.checkNoSync();
		this.nextResponse = new PreparedResponse(ICS_FILE_4_EVENTS);
		this.checkNoSync();
	}

	/**
	 * Test an ICS with too much changes, it should not be sync-able the 4th
	 * time.<br>
	 * <i>Note: In this test, {@link #icsHttpServer} will alternatively return a
	 * calendar with 51 events or just 1 each time a sync is requested (in order
	 * to reach the 'too much changes error' limit</i>
	 */
	@Test
	public void testTooMuchChanges() throws InterruptedException {
		this.init();

		// first sync replaces the initial empty ics
		this.nextResponse = new PreparedResponse();
		this.checkSyncOkWithChanges();

		// do N big syncs (N=CalendarSyncVerticle.SYNC_ERRORS_LIMIT)
		for (int i = 0; i < CalendarSyncVerticle.syncErrorLimit(); i++) {
			if (i % 2 == 0) {
				this.nextResponse = new PreparedResponse(ICS_FILE_52_EVENTS);
			} else {
				this.nextResponse = new PreparedResponse(ICS_FILE_1_EVENT);
			}
			this.checkSyncOkWithChanges(3000);
		}

		// the N+1 sync should not pass
		this.nextResponse = new PreparedResponse();
		this.checkNoSync();
	}

	/**
	 * Test updating a calendar with bad ICS content. Change ICS content to
	 * avoid "MD5 mechanism".
	 */
	@Test
	public void testBadIcsContent() throws InterruptedException {
		this.init();

		// first sync replaces the initial empty ics
		this.nextResponse = new PreparedResponse();
		this.checkSyncOkWithChanges();

		// do N bad syncs (N=CalendarSyncVerticle.SYNC_ERRORS_LIMIT)
		for (int i = 0; i < CalendarSyncVerticle.syncErrorLimit(); i++) {
			if (i % 2 == 0) {
				this.nextResponse = new PreparedResponse(ICS_FILE_BAD_CONTENT);
			} else {
				this.nextResponse = new PreparedResponse(ICS_FILE_BAD_CONTENT_2);
			}

			this.checkSyncOkNoUpdates();
		}

		// the N+1 sync should not pass
		this.nextResponse = new PreparedResponse();
		this.checkNoSync();
	}

	/**
	 * Check we do not change the ICS content when the entity-tag (a.k.a. ETag)
	 * has not changed.
	 */
	@Test
	public void testETagNoChange() throws InterruptedException {
		this.init();

		// first sync replaces the initial empty ics
		this.nextResponse = new PreparedResponse();
		this.nextResponse.headers.put("ETag", ETAG_1);
		this.checkSyncOkWithChanges();

		// other syncs are done but calendars not changed
		this.nextResponse = new PreparedResponse(ICS_FILE_2_EVENTS);
		this.nextResponse.etag = ETAG_1;
		this.nextResponse.headers.put("ETag", ETAG_1);
		this.checkSyncOkNoUpdates();
		this.nextResponse = new PreparedResponse(ICS_FILE_3_EVENTS);
		this.nextResponse.etag = ETAG_1;
		this.nextResponse.headers.put("ETag", ETAG_1);
		this.checkSyncOkNoUpdates();
		this.nextResponse = new PreparedResponse(ICS_FILE_4_EVENTS);
		this.nextResponse.etag = ETAG_1;
		this.nextResponse.headers.put("ETag", ETAG_1);
		this.checkSyncOkNoUpdates();
	}

	/**
	 * Check we change the ICS content when the entity-tag (a.k.a. ETag /
	 * If-None-Match)) changes.
	 */
	@Test
	public void testETagChanges() throws InterruptedException {
		this.init();

		// first sync replaces the initial empty ics
		this.nextResponse = new PreparedResponse();
		this.nextResponse.headers.put("ETag", ETAG_1);
		this.checkSyncOkWithChanges();

		// other syncs done and calendar changed
		this.nextResponse = new PreparedResponse(ICS_FILE_2_EVENTS);
		this.nextResponse.etag = ETAG_2;
		this.nextResponse.headers.put("ETag", ETAG_2);
		this.checkSyncOkWithChanges();

		this.nextResponse = new PreparedResponse(ICS_FILE_3_EVENTS);
		this.nextResponse.etag = ETAG_3;
		this.nextResponse.headers.put("ETag", ETAG_3);
		this.checkSyncOkWithChanges();

	}

	/**
	 * Check we do not change the ICS content when the Last-Modified
	 * (If-Modified-Since) header does not change.
	 */
	@Test
	public void testLastModifiedNoChange() throws InterruptedException {
		this.init();

		final ZonedDateTime utcLastModified = LAST_MODIF_DATE
				.withZoneSameInstant(ZoneId.ofOffset("UTC", ZoneOffset.UTC));
		final String formattedDate = DateTimeFormatter.RFC_1123_DATE_TIME.format(utcLastModified);

		// first sync replaces the initial empty ics
		this.nextResponse = new PreparedResponse();
		this.nextResponse.headers.put("Last-Modified", formattedDate);
		// RFC_1123 dates drop milliseconds, so add at least 1sec sleep to see
		// differences in times comparisons
		this.checkSyncOkWithChanges(1500);

		// other syncs are done but calendars not updated
		this.nextResponse = new PreparedResponse(ICS_FILE_2_EVENTS);
		this.nextResponse.lastModified = utcLastModified;
		this.nextResponse.headers.put("Last-Modified", formattedDate);
		this.checkSyncOkNoUpdates(1500);

		this.nextResponse = new PreparedResponse(ICS_FILE_3_EVENTS);
		this.nextResponse.lastModified = utcLastModified;
		this.nextResponse.headers.put("Last-Modified", formattedDate);
		this.checkSyncOkNoUpdates(1500);
	}

	/**
	 * Check we change the ICS content when Last-Modified (If-Modified-Since)
	 * changes.
	 */
	@Test
	public void testLastModifiedChanges() throws InterruptedException {
		this.init();

		// first sync replaces the initial empty ics
		ZonedDateTime utcLastModified = LAST_MODIF_DATE.withZoneSameInstant(ZoneId.ofOffset("UTC", ZoneOffset.UTC));
		this.nextResponse = new PreparedResponse();
		this.nextResponse.headers.put("Last-Modified", DateTimeFormatter.RFC_1123_DATE_TIME.format(utcLastModified));
		// RFC_1123 dates drop milliseconds, so add at least 1sec sleep to see
		// differences in times comparisons
		this.checkSyncOkWithChanges(1500);

		// other syncs are done and calendars changed
		this.nextResponse = new PreparedResponse(ICS_FILE_2_EVENTS);
		ZonedDateTime now = ZonedDateTime.of(LocalDateTime.now(), ZoneId.systemDefault());
		utcLastModified = now.withZoneSameInstant(ZoneId.ofOffset("UTC", ZoneOffset.UTC));
		this.nextResponse.lastModified = utcLastModified;
		this.nextResponse.headers.put("Last-Modified", DateTimeFormatter.RFC_1123_DATE_TIME.format(utcLastModified));
		this.checkSyncOkWithChanges(1500);

		this.nextResponse = new PreparedResponse(ICS_FILE_3_EVENTS);
		this.nextResponse.lastModified = utcLastModified;
		now = ZonedDateTime.of(LocalDateTime.now(), ZoneId.systemDefault());
		utcLastModified = now.withZoneSameInstant(ZoneId.ofOffset("UTC", ZoneOffset.UTC));
		this.nextResponse.lastModified = utcLastModified;
		this.nextResponse.headers.put("Last-Modified", DateTimeFormatter.RFC_1123_DATE_TIME.format(utcLastModified));
		this.checkSyncOkWithChanges(1500);
	}

	/**
	 * Check we do not change the ICS content when the
	 * Content-Disposition/modification-date (If-Modified-Since) header does not
	 * change.
	 */
	@Test
	public void testLastModifiedModificationDateNoChange() throws InterruptedException {
		this.init();

		final ZonedDateTime utcLastModified = LAST_MODIF_DATE
				.withZoneSameInstant(ZoneId.ofOffset("UTC", ZoneOffset.UTC));
		final String formattedDate = DateTimeFormatter.RFC_1123_DATE_TIME.format(utcLastModified);

		// first sync replaces the initial empty ics
		this.nextResponse = new PreparedResponse();
		this.nextResponse.headers.put("Content-Disposition",
				String.format("John=\"Doe\";\n modification-date=\"%s\";\n Tata=\"Suzanne\"", formattedDate));
		// RFC_1123 dates drop milliseconds, so add at least 1sec sleep to see
		// differences in times comparisons
		this.checkSyncOkWithChanges(1500);

		// other syncs are done but calendars not updated
		this.nextResponse = new PreparedResponse(ICS_FILE_2_EVENTS);
		this.nextResponse.lastModified = utcLastModified;
		this.nextResponse.headers.put("Content-Disposition",
				String.format("John=\"Doe\";\n modification-date=\"%s\";\n Tata=\"Suzanne\"", formattedDate));
		this.checkSyncOkNoUpdates(1500);

		this.nextResponse = new PreparedResponse(ICS_FILE_3_EVENTS);
		this.nextResponse.lastModified = utcLastModified;
		this.nextResponse.headers.put("Content-Disposition",
				String.format("John=\"Doe\";\n modification-date=\"%s\";\n Tata=\"Suzanne\"", formattedDate));
		this.checkSyncOkNoUpdates(1500);
	}

	/**
	 * Check we change the ICS content when
	 * Content-Disposition/modification-date (If-Modified-Since) changes.
	 */
	@Test
	public void testLastModifiedModificationDateChanges() throws InterruptedException {
		this.init();

		// first sync replaces the initial empty ics
		ZonedDateTime utcLastModified = LAST_MODIF_DATE.withZoneSameInstant(ZoneId.ofOffset("UTC", ZoneOffset.UTC));
		this.nextResponse = new PreparedResponse();
		this.nextResponse.headers.put("Last-Modified", DateTimeFormatter.RFC_1123_DATE_TIME.format(utcLastModified));
		// RFC_1123 dates drop milliseconds, so add at least 1sec sleep to see
		// differences in times comparisons
		this.checkSyncOkWithChanges(1500);

		// other syncs are done and calendars are changed
		this.nextResponse = new PreparedResponse(ICS_FILE_2_EVENTS);
		this.nextResponse.lastModified = utcLastModified;
		ZonedDateTime now = ZonedDateTime.of(LocalDateTime.now(), ZoneId.systemDefault());
		utcLastModified = now.withZoneSameInstant(ZoneId.ofOffset("UTC", ZoneOffset.UTC));
		this.nextResponse.headers.put("Content-Disposition",
				String.format("John=\"Doe\";\n modification-date=\"%s\";\n Tata=\"Suzanne\"",
						DateTimeFormatter.RFC_1123_DATE_TIME.format(utcLastModified)));
		this.nextResponse.lastModified = utcLastModified;
		this.checkSyncOkWithChanges(1500);

		this.nextResponse = new PreparedResponse(ICS_FILE_3_EVENTS);
		this.nextResponse.lastModified = utcLastModified;
		now = ZonedDateTime.of(LocalDateTime.now(), ZoneId.systemDefault());
		utcLastModified = now.withZoneSameInstant(ZoneId.ofOffset("UTC", ZoneOffset.UTC));
		this.nextResponse.headers.put("Content-Disposition",
				String.format("John=\"Doe\";\n modification-date=\"%s\";\n Tata=\"Suzanne\"",
						DateTimeFormatter.RFC_1123_DATE_TIME.format(utcLastModified)));
		this.nextResponse.lastModified = utcLastModified;
		this.checkSyncOkWithChanges(1500);

	}

	/**
	 * Check we do not sync when the Expire header is set to tomorrow.<br>
	 * <i>Note: 'Expire' races against the min delay (set in domain settings).
	 * We always take the longer.</i>
	 */
	@Test
	public void testNextSyncExpireTomorrow() throws InterruptedException {
		this.init();

		// first sync replaces the initial empty ics
		this.nextResponse = new PreparedResponse();
		final ZonedDateTime tomorrow = ZonedDateTime.of(LocalDateTime.now(), ZoneId.systemDefault()).plusDays(1);
		this.nextResponse.headers.put("Expires", DateTimeFormatter.RFC_1123_DATE_TIME.format(tomorrow));
		this.checkSyncOkWithChanges();

		// no other sync should be done
		this.nextResponse = new PreparedResponse(ICS_FILE_2_EVENTS);
		this.checkNoSync();
		this.nextResponse = new PreparedResponse(ICS_FILE_3_EVENTS);
		this.checkNoSync();
		this.nextResponse = new PreparedResponse(ICS_FILE_4_EVENTS);
		this.checkNoSync();
	}

	/**
	 * Check we do have a sync when the Expire header is set to yesterday.
	 */
	@Test
	public void testNextSyncExpireYesterday() throws InterruptedException {
		this.init();

		// first sync replaces the initial empty ics
		this.nextResponse = new PreparedResponse();
		final ZonedDateTime yesterday = ZonedDateTime.of(LocalDateTime.now(), ZoneId.systemDefault()).minusDays(1);
		this.nextResponse.headers.put("Expires", DateTimeFormatter.RFC_1123_DATE_TIME.format(yesterday));
		this.checkSyncOkWithChanges();

		// no other sync should be done
		this.nextResponse = new PreparedResponse(ICS_FILE_2_EVENTS);
		this.nextResponse.headers.put("Expires", DateTimeFormatter.RFC_1123_DATE_TIME.format(yesterday));
		this.checkSyncOkWithChanges();
		this.nextResponse = new PreparedResponse(ICS_FILE_3_EVENTS);
		this.nextResponse.headers.put("Expires", DateTimeFormatter.RFC_1123_DATE_TIME.format(yesterday));
		this.checkSyncOkWithChanges();
		this.nextResponse = new PreparedResponse(ICS_FILE_4_EVENTS);
		this.nextResponse.headers.put("Expires", DateTimeFormatter.RFC_1123_DATE_TIME.format(yesterday));
		this.checkSyncOkWithChanges();
	}

	/**
	 * Check we do not sync when the Cache-Control/max-age header is set to
	 * tomorrow.<br>
	 * <i>Note: 'max-age' races against the min delay (set in domain settings).
	 * We always take the longer.</i>
	 */
	@Test
	public void testNextSyncMaxAgeTomorrow() throws InterruptedException {
		this.init();

		// first sync replaces the initial empty ics
		this.nextResponse = new PreparedResponse();
		final ZonedDateTime tomorrow = ZonedDateTime.of(LocalDateTime.now(), ZoneId.systemDefault()).plusDays(1);
		final long maxAge = tomorrow.toInstant().toEpochMilli() - System.currentTimeMillis();
		this.nextResponse.headers.put("Cache-Control",
				String.format("martine-fait-un-tour-de-poney, max-age=%d, public", maxAge));
		this.checkSyncOkWithChanges();

		// no other sync should be done
		this.nextResponse = new PreparedResponse(ICS_FILE_2_EVENTS);
		this.checkNoSync();
		this.nextResponse = new PreparedResponse(ICS_FILE_3_EVENTS);
		this.checkNoSync();
		this.nextResponse = new PreparedResponse(ICS_FILE_4_EVENTS);
		this.checkNoSync();
	}

	/**
	 * Check we do have a sync when the Cache-Control/max-age header is set to
	 * yesterday.
	 */
	@Test
	public void testNextSyncMaxAgeYesterday() throws InterruptedException {
		this.init();

		// first sync replaces the initial empty ics
		this.nextResponse = new PreparedResponse();
		final ZonedDateTime yesterday = ZonedDateTime.of(LocalDateTime.now(), ZoneId.systemDefault()).minusDays(1);
		final long maxAge = yesterday.toInstant().toEpochMilli() - System.currentTimeMillis();
		this.nextResponse.headers.put("Cache-Control",
				String.format("martine-fait-un-tour-de-poney, max-age=%d, public", maxAge));
		this.checkSyncOkWithChanges();

		// other syncs done and calendars changed
		this.nextResponse = new PreparedResponse(ICS_FILE_2_EVENTS);
		this.checkSyncOkWithChanges();
		this.nextResponse = new PreparedResponse(ICS_FILE_3_EVENTS);
		this.checkSyncOkWithChanges();
		this.nextResponse = new PreparedResponse(ICS_FILE_4_EVENTS);
		this.checkSyncOkWithChanges();
	}

	/**
	 * Check we do not update a calendar when its MD5 checksum has not changed,
	 * even if we receive a 200 response.
	 */
	@Test
	public void testMd5NoChanges() throws InterruptedException {
		this.init();

		// first sync replaces the initial empty ics
		this.nextResponse = new PreparedResponse();
		this.checkSyncOkWithChanges();

		// other syncs done but calendars not changed
		this.nextResponse = new PreparedResponse();
		this.checkSyncOkNoUpdates();

		this.nextResponse = new PreparedResponse();
		this.checkSyncOkNoUpdates();
	}

	/**
	 * Test the priority mechanism. The less errors and the older syncs should
	 * go first. Error count is more important than last sync date.
	 */
	@Test
	public void testPriority() {
		final PriorityBlockingQueue<ContainerSyncStatus> priorityQueue = new PriorityBlockingQueue<ContainerSyncStatus>(
				256, new CalendarSyncVerticle()::syncStatusComparator);

		final List<ContainerSyncStatus> syncStatuses = new ArrayList<>(7);
		final String syncStatusName = "nameForTests";

		// top priority, min errors, max lastSync
		final ContainerSyncStatus syncStatusPrio1 = new ContainerSyncStatus();
		syncStatusPrio1.errors = 0;
		syncStatusPrio1.lastSync = new Date(0);
		syncStatusPrio1.syncTokens.put(syncStatusName, "syncStatusPrio1");
		syncStatuses.add(syncStatusPrio1);

		// just one error, max lastSync
		final ContainerSyncStatus syncStatusPrio2 = new ContainerSyncStatus();
		syncStatusPrio2.errors = 1;
		syncStatusPrio2.lastSync = new Date(0);
		syncStatusPrio2.syncTokens.put(syncStatusName, "syncStatusPrio2");
		syncStatuses.add(syncStatusPrio2);

		// just one error, min lastSync
		final ContainerSyncStatus syncStatusPrio3 = new ContainerSyncStatus();
		syncStatusPrio3.errors = 1;
		syncStatusPrio3.lastSync = new Date();
		syncStatusPrio3.syncTokens.put(syncStatusName, "syncStatusPrio3");
		syncStatuses.add(syncStatusPrio3);

		// two errors, min lastSync
		final ContainerSyncStatus syncStatusPrio4 = new ContainerSyncStatus();
		syncStatusPrio4.errors = 2;
		syncStatusPrio4.lastSync = new Date();
		syncStatusPrio4.syncTokens.put(syncStatusName, "syncStatusPrio4");
		syncStatuses.add(syncStatusPrio4);

		// a lot of errors, max lastSync
		final ContainerSyncStatus syncStatusPrio5 = new ContainerSyncStatus();
		syncStatusPrio5.errors = 9999;
		syncStatusPrio5.lastSync = new Date(0);
		syncStatusPrio5.syncTokens.put(syncStatusName, "syncStatusPrio5");
		syncStatuses.add(syncStatusPrio5);

		// a lot of errors, lastSync set to one day ago
		final ContainerSyncStatus syncStatusPrio6 = new ContainerSyncStatus();
		syncStatusPrio6.errors = 9999;
		syncStatusPrio6.lastSync = new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1));
		syncStatusPrio6.syncTokens.put(syncStatusName, "syncStatusPrio6");
		syncStatuses.add(syncStatusPrio6);

		// a lot of errors, min lastSync
		final ContainerSyncStatus syncStatusPrio7 = new ContainerSyncStatus();
		syncStatusPrio7.errors = 9999;
		syncStatusPrio7.lastSync = new Date();
		syncStatusPrio7.syncTokens.put(syncStatusName, "syncStatusPrio7");
		syncStatuses.add(syncStatusPrio7);

		// add randomly to the queue
		Collections.shuffle(syncStatuses);
		for (final ContainerSyncStatus syncStatus : syncStatuses) {
			priorityQueue.add(syncStatus);
		}

		// check the queue delivers in the expected order
		final ContainerSyncStatus result1 = priorityQueue.poll();
		Assert.assertSame(String.format("Expected %s, got %s", syncStatusPrio1.syncTokens.get(syncStatusName),
				result1.syncTokens.get(syncStatusName)), syncStatusPrio1, result1);
		final ContainerSyncStatus result2 = priorityQueue.poll();
		Assert.assertSame(String.format("Expected %s, got %s", syncStatusPrio2.syncTokens.get(syncStatusName),
				result2.syncTokens.get(syncStatusName)), syncStatusPrio2, result2);
		final ContainerSyncStatus result3 = priorityQueue.poll();
		Assert.assertSame(String.format("Expected %s, got %s", syncStatusPrio3.syncTokens.get(syncStatusName),
				result3.syncTokens.get(syncStatusName)), syncStatusPrio3, result3);
		final ContainerSyncStatus result4 = priorityQueue.poll();
		Assert.assertSame(String.format("Expected %s, got %s", syncStatusPrio4.syncTokens.get(syncStatusName),
				result4.syncTokens.get(syncStatusName)), syncStatusPrio4, result4);
		final ContainerSyncStatus result5 = priorityQueue.poll();
		Assert.assertSame(String.format("Expected %s, got %s", syncStatusPrio5.syncTokens.get(syncStatusName),
				result5.syncTokens.get(syncStatusName)), syncStatusPrio5, result5);
		final ContainerSyncStatus result6 = priorityQueue.poll();
		Assert.assertSame(String.format("Expected %s, got %s", syncStatusPrio6.syncTokens.get(syncStatusName),
				result6.syncTokens.get(syncStatusName)), syncStatusPrio6, result6);
		final ContainerSyncStatus result7 = priorityQueue.poll();
		Assert.assertSame(String.format("Expected %s, got %s", syncStatusPrio7.syncTokens.get(syncStatusName),
				result7.syncTokens.get(syncStatusName)), syncStatusPrio7, result7);
	}

	/** Check no sync could be done at all. */
	private void checkNoSync() throws InterruptedException {
		final Date lastSync2 = this.triggerSync(500);
		final String icsContent = this.retrieveCurrentIcsContent();

		Assert.assertNotNull("Unexpected NULL sync status", lastSync2);

		// check no sync and changes have been made
		Assert.assertFalse("Did not expect a sync here", lastSync2.after(lastSync));
		Assert.assertFalse("Did not expect an update of the calendar", this.lastSyncHasUpdatedCalendar);
		final int previousNumberOfEvents = this.countEvents(previousIcsContent);
		final int numberOfEvents = this.countEvents(icsContent);
		Assert.assertEquals(previousNumberOfEvents, numberOfEvents);

		lastSync = lastSync2;
		previousIcsContent = icsContent;
	}

	/** Sync is done but we do not even try to update the calendar. */
	private void checkSyncOkNoUpdates() throws InterruptedException {
		this.checkSyncOkNoUpdates(500);
	}

	/** Sync is done but we do not even try to update the calendar. */
	private void checkSyncOkNoUpdates(final long syncWait) throws InterruptedException {
		final Date lastSync2 = this.triggerSync(syncWait);

		// if the lastSync's date has changed it means we had a sync
		Assert.assertNotNull("Unexpected NULL sync status", lastSync2);
		Assert.assertTrue("No sync has been done", lastSync2.after(this.lastSync));

		// check calendar has not been updated
		Assert.assertFalse("Did not expect an update of the calendar", this.lastSyncHasUpdatedCalendar);

		this.lastSync = lastSync2;
		this.previousIcsContent = this.retrieveCurrentIcsContent();
	}

	/** Sync done, calendar updated and changes are made. */
	private void checkSyncOkWithChanges() throws InterruptedException {
		this.checkSyncOkWithChanges(500);
	}

	/** Sync done, calendar updated and changes are made. */
	private void checkSyncOkWithChanges(final long syncWait) throws InterruptedException {
		final String previousIcsContent = this.retrieveCurrentIcsContent();
		final Date lastSync2 = this.triggerSync(syncWait);
		final String icsContent = this.retrieveCurrentIcsContent();

		// if the lastSync's date has changed it means we had a sync
		Assert.assertNotNull("Unexpected NULL sync status", lastSync2);
		Assert.assertTrue("No sync has been done", lastSync2.after(this.lastSync));

		// check calendar has been updated
		Assert.assertTrue("No update of the calendar has been done", this.lastSyncHasUpdatedCalendar);

		// check ICS content has changed
		final int previousNumberOfEvents = this.countEvents(previousIcsContent);
		final int numberOfEvents = this.countEvents(icsContent);
		Assert.assertNotEquals(previousNumberOfEvents, numberOfEvents);

		this.lastSync = lastSync2;
		this.previousIcsContent = icsContent;
	}

	private String retrieveCurrentIcsContent() {
		final Stream icsStream = this.systemProvider.instance(IVEvent.class, CALENDAR_UID).exportAll();
		return GenericStream.streamToString(icsStream);
	}

	/** Setup the calendar, settings... */
	private void init() {
		this.lastSync = this.init(50, CALENDAR_UID);
	}

	/** Setup the calendar, settings... */
	private void init(final long syncDelay) {
		this.lastSync = this.init(syncDelay, CALENDAR_UID);
	}

	/** Setup the calendar, settings... */
	private Date init(final long syncDelay, final String calendarId) {
		// calendar creation with a varying ICS URL
		final CalendarDescriptor calendarDescriptor = new CalendarDescriptor();
		calendarDescriptor.domainUid = domain;
		calendarDescriptor.name = "name-" + calendarId;
		calendarDescriptor.owner = "jdoe";
		calendarDescriptor.settings = new HashMap<>(1);
		calendarDescriptor.settings.put("icsUrl", ICS_URL);
		ServerSideServiceProvider.getProvider(johnDoeContext).instance(ICalendarsMgmt.class).create(calendarId,
				calendarDescriptor);

		// check the container sync init has been done
		final IContainerSync containerSyncService = this.systemProvider.instance(IContainerSync.class, calendarId);
		Assert.assertNotNull(containerSyncService);
		Date lastSync = containerSyncService.getLastSync();
		Assert.assertNotNull(lastSync);

		// modify the min delay between syncs
		final IDomainSettings domainSettingsService = this.systemProvider.instance(IDomainSettings.class, domain);
		final Map<String, String> domainSettings = domainSettingsService.get();
		domainSettings.put(CalendarContainerSync.DOMAIN_SETTING_MIN_DELAY_KEY, String.valueOf(syncDelay));
		domainSettingsService.set(domainSettings);

		return lastSync;
	}

	/** Wake-up the VertX event bus in order to launch a calendar sync. */
	private Date triggerSync(final long sleep) throws InterruptedException {
		return this.triggerSync(sleep, CALENDAR_UID);
	}

	/** Wake-up the VertX event bus in order to launch a calendar sync. */
	private Date triggerSync(final long sleep, final String calendarId) throws InterruptedException {
		this.lastSyncHasUpdatedCalendar = false;

		// when addressing the ICalendar API (creating a new instance) then
		// the sync mechanism is triggered
		ServerSideServiceProvider.getProvider(johnDoeContext).instance(ICalendar.class, calendarId)
				.getComplete(calendarId);

		// wait a little bit for the sync to be done
		// (executor scheduling + http request + sync)
		Thread.sleep(sleep);

		return this.systemProvider.instance(IContainerSync.class, calendarId).getLastSync();
	}

	private static final Pattern COUNT_EVENTS_PATTERN = Pattern.compile("BEGIN:VEVENT", Pattern.LITERAL);

	private int countEvents(final String icsContent) {
		int count = 0;
		final Matcher matcher = COUNT_EVENTS_PATTERN.matcher(icsContent);
		while (matcher.find()) {
			count++;
		}
		return count;
	}

	private boolean sameEtag(final HttpServerRequest event) {
		final String actualEtag = event.headers().get("If-None-Match");
		if (actualEtag == null) {
			return false;
		}
		final String etagToken = this.nextResponse.etag != null ? this.nextResponse.etag : "";
		return etagToken.equals(actualEtag);
	}

	private boolean lastModifiedHasNotChanged(final HttpServerRequest event) {
		final String actualLastModified = event.headers().get("If-Modified-Since");
		final ZonedDateTime lastModInHeader;
		if (actualLastModified != null) {
			final TemporalAccessor temporal = DateTimeFormatter.RFC_1123_DATE_TIME.parse(actualLastModified);
			final ZoneId zoneId = temporal.query(ZoneId::from);
			final LocalDateTime localDateTime = temporal.query(LocalDateTime::from);
			lastModInHeader = ZonedDateTime.of(localDateTime, zoneId);
		} else {
			return false;
		}

		final ZonedDateTime lastModified;
		if (this.nextResponse.lastModified == null) {
			return false;
		} else {
			// /!\ RFC_1123 conversion drops milliseconds, "format" then "parse"
			// simulates this behavior:
			lastModified = ZonedDateTime.from(DateTimeFormatter.RFC_1123_DATE_TIME
					.parse(DateTimeFormatter.RFC_1123_DATE_TIME.format(this.nextResponse.lastModified)));
		}
		return lastModInHeader.isEqual(lastModified) || lastModified.isBefore(lastModInHeader);
	}
}
