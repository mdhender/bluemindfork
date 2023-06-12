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
package net.bluemind.lmtp.filter.imip.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.stream.Field;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.google.common.collect.Lists;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.attachment.api.AttachedFile;
import net.bluemind.calendar.api.ICalendar;
import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.api.VEventCounter;
import net.bluemind.calendar.api.VEventOccurrence;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.calendar.helper.ical4j.VEventServiceHelper;
import net.bluemind.core.api.Email;
import net.bluemind.core.api.Stream;
import net.bluemind.core.api.date.BmDateTime;
import net.bluemind.core.api.date.BmDateTime.Precision;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.rest.base.GenericStream;
import net.bluemind.core.sendmail.testhelper.FakeSendmail;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.delivery.lmtp.common.ResolvedBox;
import net.bluemind.delivery.lmtp.filter.testhelper.EnvelopeBuilder;
import net.bluemind.delivery.lmtp.filters.PermissionDeniedException.MailboxInvitationDeniedException;
import net.bluemind.dockerclient.DockerEnv;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
import net.bluemind.filehosting.api.IInternalBMFileSystem;
import net.bluemind.icalendar.api.ICalendarElement;
import net.bluemind.icalendar.api.ICalendarElement.Attendee;
import net.bluemind.icalendar.api.ICalendarElement.ParticipationStatus;
import net.bluemind.icalendar.api.ICalendarElement.RRule;
import net.bluemind.icalendar.api.ICalendarElement.RRule.Frequency;
import net.bluemind.icalendar.api.ICalendarElement.VAlarm;
import net.bluemind.imip.parser.IMIPInfos;
import net.bluemind.imip.parser.IMIPParserFactory;
import net.bluemind.imip.parser.ITIPMethod;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.lmtp.filter.imip.EventCancelHandler;
import net.bluemind.lmtp.filter.imip.EventCounterHandler;
import net.bluemind.lmtp.filter.imip.EventDeclineCounterHandler;
import net.bluemind.lmtp.filter.imip.EventReplyHandler;
import net.bluemind.lmtp.filter.imip.FakeEventRequestHandlerFactory;
import net.bluemind.lmtp.filter.imip.IIMIPHandler;
import net.bluemind.lmtp.filter.imip.IMIPResponse;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.mime4j.common.Mime4JHelper;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.resource.api.IResources;
import net.bluemind.resource.api.ResourceDescriptor;
import net.bluemind.server.api.Server;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;
import net.bluemind.utils.FileUtils;

public class ImipFilterVEventTests {
	private String domainUid = "domain.lan";
	private BmContext testContext;
	private String user1Uid;
	private ItemValue<User> user1;
	private String userNoDriveUid;
	private ItemValue<Mailbox> user1Mailbox;
	private ItemValue<Mailbox> userNoDriveMailbox;
	private ICalendar user1Calendar;
	private ICalendar userNoDriveCalendar;
	private ItemValue<Domain> domain;
	private ZoneId defaultTz = ZoneId.systemDefault();
	private ZoneId utcTz = ZoneId.of("UTC");

	@Rule
	public final TestName name = new TestName();

	@BeforeClass
	public static void oneShotBefore() {
		System.setProperty("es.mailspool.count", "1");
	}

	@Before
	public void before() throws Exception {
		System.setProperty("node.local.ipaddr", PopulateHelper.FAKE_CYRUS_IP);
		System.setProperty("imap.local.ipaddr", PopulateHelper.FAKE_CYRUS_IP);

		JdbcTestHelper.getInstance().beforeTest();

		ElasticsearchTestHelper.getInstance().beforeTest();

		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);

		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		System.out.println("IP " + esServer.ip);
		esServer.tags = Lists.newArrayList("bm/es");

		Server pipo = new Server();
		pipo.tags = Collections.singletonList("mail/imap");
		pipo.ip = PopulateHelper.FAKE_CYRUS_IP;

		Server nodeServer = new Server();
		nodeServer.ip = DockerEnv.getIp("bluemind/node-tests");
		nodeServer.tags = Lists.newArrayList("filehosting/data");

		PopulateHelper.initGlobalVirt(esServer, pipo, nodeServer);

		PopulateHelper.addDomainAdmin("admin0", "global.virt");

		PopulateHelper.createTestDomain(domainUid, esServer, pipo, nodeServer);

		PopulateHelper.addDomainAdmin("admin", domainUid, Mailbox.Routing.internal);

		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);

		testContext = new BmTestContext(SecurityContext.SYSTEM);

		user1Uid = PopulateHelper.addUser("user1", domainUid, Routing.none, "canRemoteAttach");
		userNoDriveUid = PopulateHelper.addUser("usernodrive", domainUid, Routing.none);
		user1 = testContext.provider().instance(IUser.class, domainUid).getComplete(user1Uid);
		user1Mailbox = testContext.provider().instance(IMailboxes.class, domainUid).getComplete(user1Uid);
		userNoDriveMailbox = testContext.provider().instance(IMailboxes.class, domainUid).getComplete(userNoDriveUid);
		user1Calendar = testContext.provider().instance(ICalendar.class, ICalendarUids.defaultUserCalendar(user1Uid));
		userNoDriveCalendar = testContext.provider().instance(ICalendar.class,
				ICalendarUids.defaultUserCalendar(userNoDriveUid));

		domain = testContext.provider().instance(IDomains.class).get(domainUid);
		System.out.println("test setup is complete for " + name.getMethodName());
	}

	@After
	public void after() throws Exception {
		System.out.println("ending " + name.getMethodName());
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void requestHandler() throws Exception {
		IIMIPHandler handler = new FakeEventRequestHandlerFactory().create();
		ItemValue<VEvent> event = defaultVEvent();

		IMIPInfos imip = imip(ITIPMethod.REQUEST, defaultExternalSenderVCard(), event.uid);

		imip.iCalendarElements = Arrays.asList(event.value);
		imip.uid = event.uid;
		ResolvedBox recipient = EnvelopeBuilder.lookupEmail("user1@domain.lan");

		ItemValue<VEventSeries> res = user1Calendar.getComplete(event.uid);
		assertNull(res);

		handler.handle(imip, recipient, domain, user1Mailbox);

		res = user1Calendar.getComplete(event.uid);
		assertNotNull(res);
		assertEquals(event.value.summary, res.value.main.summary);
		assertEquals(2, res.value.main.attendees.size());

		res.value.main.summary = "updated";
		imip.iCalendarElements = Arrays.asList(res.value.main);
		imip.sequence = 2;
		handler.handle(imip, recipient, domain, user1Mailbox);

		res = user1Calendar.getComplete(event.uid);
		assertNotNull(res);
		assertEquals("updated", res.value.main.summary);
		assertEquals(2, res.value.main.attendees.size());
	}

	@Test
	public void requestHandler_Event_CID_Attachments_without_name() throws Exception {
		setGlobalExternalUrl();
		IIMIPHandler handler = new FakeEventRequestHandlerFactory().create();

		IMIPInfos imip = null;

		try (InputStream in = Ex2003Tests.class.getClassLoader()
				.getResourceAsStream("ics/invitation_cid_without_name.eml"); Message parsed = Mime4JHelper.parse(in)) {
			imip = IMIPParserFactory.create().parse(parsed);
		}

		ResolvedBox recipient = EnvelopeBuilder.lookupEmail("user1@domain.lan");
		handler.handle(imip, recipient, domain, user1Mailbox);

		List<ItemValue<VEventSeries>> byIcsUid = user1Calendar.getByIcsUid(
				"040000008200E00074C5B7101A82E00800000000DEE9BEDA6DF2D7010000000000000000100000007F3854933A325346B9433160D5F41CEA");
		assertEquals(1, byIcsUid.size());

		VEvent main = byIcsUid.get(0).value.main;
		assertEquals(1, main.attachments.size());

		AttachedFile attachedFile = main.attachments.get(0);
		assertEquals("CID:a2008ab2-a526-4687-9bfb-259fd6c5bbdc", attachedFile.cid);
		assertEquals("<a2008ab2-a526-4687-9bfb-259fd6c5bbdc>", attachedFile.name);
		assertNotNull(attachedFile.publicUrl);

		byte[] image = download(attachedFile.publicUrl);
		assertEquals(7522, image.length);
		assertEquals("<a2008ab2-a526-4687-9bfb-259fd6c5bbdc>", attachedFile.name);
	}

	@Test
	public void requestHandler_Event_Google_Attachments() throws Exception {
		setGlobalExternalUrl();
		IIMIPHandler handler = new FakeEventRequestHandlerFactory().create();

		IMIPInfos imip = null;

		try (InputStream in = Ex2003Tests.class.getClassLoader().getResourceAsStream("ics/google_attachment.eml");
				Message parsed = Mime4JHelper.parse(in)) {
			imip = IMIPParserFactory.create().parse(parsed);
		}

		ResolvedBox recipient = EnvelopeBuilder.lookupEmail("user1@domain.lan");
		handler.handle(imip, recipient, domain, user1Mailbox);

		List<ItemValue<VEventSeries>> byIcsUid = user1Calendar.getByIcsUid("5r5fab74aetove4tjjuhq6e7kr@google.com");
		assertEquals(1, byIcsUid.size());

		VEvent main = byIcsUid.get(0).value.main;
		assertEquals(1, main.attachments.size());

		AttachedFile attachedFile = main.attachments.get(0);
		assertEquals("ZOOM - BLOGPOST BLUEMIND - 060623", attachedFile.name);
		assertNotNull(attachedFile.publicUrl);
	}

	@Test
	public void requestHandler_Event_CID_Attachments() throws Exception {
		setGlobalExternalUrl();
		IIMIPHandler handler = new FakeEventRequestHandlerFactory().create();

		IMIPInfos imip = null;

		try (InputStream in = Ex2003Tests.class.getClassLoader()
				.getResourceAsStream("ics/office365_invitation_inline_image.eml");
				Message parsed = Mime4JHelper.parse(in)) {
			imip = IMIPParserFactory.create().parse(parsed);
		}

		ResolvedBox recipient = EnvelopeBuilder.lookupEmail("user1@domain.lan");
		handler.handle(imip, recipient, domain, user1Mailbox);

		List<ItemValue<VEventSeries>> byIcsUid = user1Calendar.getByIcsUid(
				"040000008200E00074C5B7101A82E00800000000DEE9BEDA6DF2D7010000000000000000100000007F3854933A325346B9433160D5F41CEA");
		assertEquals(1, byIcsUid.size());

		VEvent main = byIcsUid.get(0).value.main;
		assertEquals(1, main.attachments.size());

		AttachedFile attachedFile = main.attachments.get(0);
		assertEquals("CID:a2008ab2-a526-4687-9bfb-259fd6c5bbdc", attachedFile.cid);
		assertNotNull(attachedFile.publicUrl);

		byte[] image = download(attachedFile.publicUrl);
		assertEquals(7522, image.length);
		assertEquals("Screenshot 2021-12-16 at 10.30.00.png", attachedFile.name);
	}

	@Test
	public void requestHandler_Event_CID_Attachments_Disposition() throws Exception {
		setGlobalExternalUrl();
		IIMIPHandler handler = new FakeEventRequestHandlerFactory().create();

		IMIPInfos imip = null;

		try (InputStream in = Ex2003Tests.class.getClassLoader()
				.getResourceAsStream("ics/bluemind_mapi_invitation_inline_disposition.eml");
				Message parsed = Mime4JHelper.parse(in)) {
			imip = IMIPParserFactory.create().parse(parsed);
		}

		ResolvedBox recipient = EnvelopeBuilder.lookupEmail("user1@domain.lan");
		handler.handle(imip, recipient, domain, user1Mailbox);

		List<ItemValue<VEventSeries>> byIcsUid = user1Calendar.getByIcsUid(
				"040000008200E00074C5B7101A82E00800000000DEE9BEDA6DF2D7010000000000000000100000007F3854933A325346B9433160D5F41CEA");
		assertEquals(1, byIcsUid.size());

		VEvent main = byIcsUid.get(0).value.main;
		assertEquals(1, main.attachments.size());

		AttachedFile attachedFile = main.attachments.get(0);
		assertEquals("CID:a2008ab2-a526-4687-9bfb-259fd6c5bbdc", attachedFile.cid);
		assertNotNull(attachedFile.publicUrl);

		byte[] image = download(attachedFile.publicUrl);
		assertEquals(7522, image.length);
		assertEquals("Screenshot 2021-12-16 at 10.30.00.png", attachedFile.name);
	}

	@Test
	public void requestHandler_Event_CID_Attachments_RecipientWithoutRoleShouldNotBlock() throws Exception {
		setGlobalExternalUrl();
		IIMIPHandler handler = new FakeEventRequestHandlerFactory().create();

		IMIPInfos imip = null;

		try (InputStream in = Ex2003Tests.class.getClassLoader()
				.getResourceAsStream("ics/office365_invitation_inline_image.eml");
				Message parsed = Mime4JHelper.parse(in)) {
			imip = IMIPParserFactory.create().parse(parsed);
		}

		ResolvedBox recipient = EnvelopeBuilder.lookupEmail("usernodrive@domain.lan");
		handler.handle(imip, recipient, domain, userNoDriveMailbox);

		List<ItemValue<VEventSeries>> byIcsUid = userNoDriveCalendar.getByIcsUid(
				"040000008200E00074C5B7101A82E00800000000DEE9BEDA6DF2D7010000000000000000100000007F3854933A325346B9433160D5F41CEA");
		assertEquals(1, byIcsUid.size());

		VEvent main = byIcsUid.get(0).value.main;
		assertEquals(1, main.attachments.size());

		AttachedFile attachedFile = main.attachments.get(0);
		assertEquals("CID:a2008ab2-a526-4687-9bfb-259fd6c5bbdc", attachedFile.cid);
		assertEquals(attachedFile.cid, attachedFile.publicUrl);
	}

	private Map<String, String> setGlobalExternalUrl() {
		ISystemConfiguration systemConfiguration = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(ISystemConfiguration.class);
		Map<String, String> sysValues = systemConfiguration.getValues().values;
		sysValues.put(SysConfKeys.external_url.name(), "localhost");
		systemConfiguration.updateMutableValues(sysValues);
		return sysValues;
	}

	@Test
	public void requestHandler_Event_CID_Attachments_BlueMind_MAPI() throws Exception {
		IIMIPHandler handler = new FakeEventRequestHandlerFactory().create();

		IMIPInfos imip = null;

		try (InputStream in = Ex2003Tests.class.getClassLoader()
				.getResourceAsStream("ics/bluemind_mapi_invitation_inline.eml");
				Message parsed = Mime4JHelper.parse(in)) {
			imip = IMIPParserFactory.create().parse(parsed);
		}

		ResolvedBox recipient = EnvelopeBuilder.lookupEmail("user1@domain.lan");
		handler.handle(imip, recipient, domain, user1Mailbox);

		List<ItemValue<VEventSeries>> byIcsUid = user1Calendar.getByIcsUid(
				"040000008200e00074c5b7101a82e00800000000e015ba780bfcd7010000000000000000100000000bfbc82d3f1c81458f25ba118ea29e2d");
		assertEquals(1, byIcsUid.size());

		VEvent main = byIcsUid.get(0).value.main;
		assertEquals(1, main.attachments.size());

		AttachedFile attachedFile = main.attachments.get(0);
		assertEquals("image001.gif@01D7FC0B.78B7A4E0", attachedFile.cid);
		assertEquals("cd0006cbfcfa0aab9671db2c9d0a3f6d.gif", attachedFile.name);
		assertNotNull(attachedFile.publicUrl);

	}

	private byte[] download(String publicUrl) throws Exception {
		int idx = publicUrl.indexOf("fh/bm-fh/");
		String uid = publicUrl.substring(idx + "fh/bm-fh/".length());

		IInternalBMFileSystem fh = testContext.provider().instance(IInternalBMFileSystem.class);
		Stream sharedFile = fh.getSharedFile(uid);

		return GenericStream.streamToBytes(sharedFile);
	}

	@Test
	public void requestHandler_DefaultAlert() throws Exception {
		IIMIPHandler handler = new FakeEventRequestHandlerFactory().create();
		ItemValue<VEvent> event = defaultVEvent();

		IMIPInfos imip = imip(ITIPMethod.REQUEST, defaultExternalSenderVCard(), event.uid);

		imip.iCalendarElements = Arrays.asList(event.value);
		imip.uid = event.uid;
		ResolvedBox recipient = EnvelopeBuilder.lookupEmail("user1@domain.lan");

		handler.handle(imip, recipient, domain, user1Mailbox);

		ItemValue<VEventSeries> res = user1Calendar.getComplete(event.uid);
		assertEquals(1, res.value.main.alarm.size());
		assertEquals(-900, res.value.main.alarm.get(0).trigger.intValue());

		// ensure alarms are not erased
		res.value.main.alarm.add(VAlarm.create(-60));
		user1Calendar.update(event.uid, res.value, false);

		handler.handle(imip, recipient, domain, user1Mailbox);

		res = user1Calendar.getComplete(event.uid);
		assertEquals(2, res.value.main.alarm.size());
	}

	@Test
	public void testRequestHandlerEventHeader() throws Exception {
		IIMIPHandler handler = new FakeEventRequestHandlerFactory().create();

		ItemValue<VEvent> event = defaultVEvent();
		IMIPInfos imip = imip(ITIPMethod.REQUEST, defaultExternalSenderVCard(), event.uid);

		imip.iCalendarElements = Arrays.asList(event.value);
		ResolvedBox recipient = EnvelopeBuilder.lookupEmail("user1@domain.lan");

		IMIPResponse response = handler.handle(imip, recipient, domain, user1Mailbox);

		List<Field> headerFields = response.headerFields;
		boolean checked = false;
		for (Field f : headerFields) {
			if (f.getName().equalsIgnoreCase("X-BM-EVENT")) {
				checked = true;
				assertEquals(String.format("%s; rsvp=\"true\"", event.uid), f.getBody());
			}
		}
		assertTrue(checked);
	}

	@Test
	public void testRequestHandlerMeetingWithoutAttendeesShoudAddRecipientAsAttendee() throws Exception {
		ResolvedBox recipient = EnvelopeBuilder.lookupEmail("user1@domain.lan");
		IIMIPHandler handler = new FakeEventRequestHandlerFactory().create();

		ItemValue<VEvent> master = defaultVEvent();
		master.value.attendees = new ArrayList<>();

		IMIPInfos imip = imip(ITIPMethod.REQUEST, defaultExternalSenderVCard(), master.uid);
		imip.iCalendarElements = Arrays.asList(master.value);
		IMIPResponse response = handler.handle(imip, recipient, domain, user1Mailbox);

		ItemValue<VEventSeries> evt = user1Calendar.getComplete(master.uid);

		assertEquals("external@ext-domain.lan", evt.value.main.organizer.mailto);
		assertEquals("user1@domain.lan", evt.value.main.attendees.get(0).mailto);
	}

	@Test
	public void requestHandler_Event_Html_Invite() throws Exception {
		setGlobalExternalUrl();

		try (InputStream in = Ex2003Tests.class.getClassLoader().getResourceAsStream("ics/html_invite.eml");
				Message parsed = Mime4JHelper.parse(in)) {
			IMIPInfos imip = IMIPParserFactory.create().parse(parsed);
			List<ICalendarElement> iCalendarElements = imip.iCalendarElements;
			assertFalse(iCalendarElements.isEmpty());
			iCalendarElements.forEach(c -> assertTrue(c.description.contains("<br>")));
		}
	}

	@Test
	public void testRequestHandlerRecEventHeader() throws Exception {
		ResolvedBox recipient = EnvelopeBuilder.lookupEmail("user1@domain.lan");
		IIMIPHandler handler = new FakeEventRequestHandlerFactory().create();

		ItemValue<VEvent> master = defaultVEvent();
		IMIPInfos imip = imip(ITIPMethod.REQUEST, defaultExternalSenderVCard(), master.uid);
		imip.iCalendarElements = Arrays.asList(master.value);
		handler.handle(imip, recipient, domain, user1Mailbox);

		ItemValue<VEvent> event = defaultVEvent();
		event.uid = master.uid;
		imip = imip(ITIPMethod.REQUEST, defaultExternalSenderVCard(), event.uid);

		BmDateTime now = BmDateTimeWrapper.fromTimestamp(System.currentTimeMillis());
		VEventOccurrence occurrence = VEventOccurrence.fromEvent(event.value, now);

		imip.iCalendarElements = Arrays.asList(occurrence);

		IMIPResponse response = handler.handle(imip, recipient, domain, user1Mailbox);

		List<Field> headerFields = response.headerFields;
		boolean checked = false;
		for (Field f : headerFields) {
			if (f.getName().equalsIgnoreCase("X-BM-EVENT")) {
				checked = true;
				assertEquals(String.format("%s; recurid=\"%s\"; rsvp=\"true\"", event.uid, now.iso8601), f.getBody());
			}
		}
		assertTrue(checked);
	}

	@Test
	public void testRequestHandlerNOTChangingEventDateShouldLeaveExceptionsUntouched() throws Exception {
		IIMIPHandler handler = new FakeEventRequestHandlerFactory().create();

		ItemValue<VEvent> event = defaultVEvent();
		IMIPInfos imip = imip(ITIPMethod.REQUEST, defaultExternalSenderVCard(), event.uid);

		event.value.summary = "i am a master event";
		RRule daily = new RRule();
		daily.frequency = Frequency.DAILY;
		event.value.rrule = daily;

		imip.iCalendarElements = Arrays.asList(event.value);
		ResolvedBox recipient = EnvelopeBuilder.lookupEmail("user1@domain.lan");

		handler.handle(imip, recipient, domain, user1Mailbox);

		ItemValue<VEventSeries> evt = user1Calendar.getComplete(event.uid);
		assertNotNull(evt);

		VEventOccurrence exception = VEventOccurrence.fromEvent(evt.value.main.copy(),
				BmDateTimeWrapper.fromTimestamp(System.currentTimeMillis()));
		exception.rrule = null;
		exception.summary = "i am an exception";
		imip.iCalendarElements = Arrays.asList(exception);

		handler.handle(imip, recipient, domain, user1Mailbox);

		ItemValue<VEventSeries> master = user1Calendar.getComplete(evt.uid);
		// update master without changing date
		imip.iCalendarElements = Arrays.asList(master.value.main);

		handler.handle(imip, recipient, domain, user1Mailbox);

		master = user1Calendar.getComplete(evt.uid);
		// verify that exception has been deleted
		assertEquals(1, master.value.occurrences.size());

	}

	@Test
	public void testRequestHandlerChangingEventDateShouldResetEventExceptions() throws Exception {
		IIMIPHandler handler = new FakeEventRequestHandlerFactory().create();
		ItemValue<VEvent> event = defaultVEvent();
		IMIPInfos imip = imip(ITIPMethod.REQUEST, defaultExternalSenderVCard(), event.uid);

		event.value.summary = "i am a master event";
		RRule daily = new RRule();
		daily.frequency = Frequency.DAILY;
		event.value.rrule = daily;

		imip.iCalendarElements = Arrays.asList(event.value);
		ResolvedBox recipient = EnvelopeBuilder.lookupEmail("user1@domain.lan");

		handler.handle(imip, recipient, domain, user1Mailbox);

		ItemValue<VEventSeries> evt = user1Calendar.getComplete(event.uid);
		assertNotNull(evt);

		VEventOccurrence exception = VEventOccurrence.fromEvent(evt.value.main.copy(),
				BmDateTimeWrapper.fromTimestamp(System.currentTimeMillis()));
		exception.rrule = null;
		exception.summary = "i am an exception";
		imip.iCalendarElements = Arrays.asList(exception);

		handler.handle(imip, recipient, domain, user1Mailbox);

		ItemValue<VEventSeries> master = user1Calendar.getComplete(evt.uid);

		assertEquals(1, master.value.occurrences.size());

		master.value.main.dtstart = BmDateTimeWrapper.create(ZonedDateTime.of(2021, 2, 13, 0, 0, 0, 0, defaultTz),
				Precision.DateTime);
		imip.iCalendarElements = Arrays.asList(master.value.main);

		System.out.println(JdbcActivator.getInstance().getSchemaName());

		System.out.println("going to delete exp");
		handler.handle(imip, recipient, domain, user1Mailbox);
		System.out.println("going to delete exp - done");

		master = user1Calendar.getComplete(evt.uid);

		// verify that exception has been deleted
		assertEquals(0, master.value.occurrences.size());

		// now we test dtend changes

		exception = VEventOccurrence.fromEvent(evt.value.main.copy(),
				BmDateTimeWrapper.fromTimestamp(System.currentTimeMillis()));
		exception.rrule = null;
		exception.summary = "i am an exception";
		imip.iCalendarElements = Arrays.asList(exception);

		handler.handle(imip, recipient, domain, user1Mailbox);
		master = user1Calendar.getComplete(evt.uid);

		assertEquals(1, master.value.occurrences.size());

		master.value.main.dtend = BmDateTimeWrapper.create(ZonedDateTime.of(2028, 2, 13, 0, 0, 0, 0, defaultTz),
				Precision.DateTime);
		imip.iCalendarElements = Arrays.asList(master.value.main);

		handler.handle(imip, recipient, domain, user1Mailbox);

		master = user1Calendar.getComplete(evt.uid);

		// verify that exception has been deleted
		assertEquals(0, master.value.occurrences.size());

	}

	@Test
	public void testRequestHandler_ChangingMaster_ShouldAdjustExceptionsValues() throws Exception {
		IIMIPHandler handler = new FakeEventRequestHandlerFactory().create();

		ItemValue<VEvent> event = defaultVEvent();
		IMIPInfos imip = imip(ITIPMethod.REQUEST, defaultExternalSenderVCard(), event.uid);

		event.value.summary = "summary";
		event.value.description = "description";
		event.value.location = "@home";
		RRule daily = new RRule();
		daily.frequency = Frequency.DAILY;
		event.value.rrule = daily;

		imip.iCalendarElements = Arrays.asList(event.value);
		ResolvedBox recipient = EnvelopeBuilder.lookupEmail("user1@domain.lan");

		handler.handle(imip, recipient, domain, user1Mailbox);

		ItemValue<VEventSeries> evt = user1Calendar.getComplete(event.uid);

		VEventOccurrence exception = VEventOccurrence.fromEvent(evt.value.main.copy(),
				BmDateTimeWrapper.fromTimestamp(System.currentTimeMillis()));
		imip.iCalendarElements = Arrays.asList(exception);

		handler.handle(imip, recipient, domain, user1Mailbox);

		VEventOccurrence exception2 = VEventOccurrence.fromEvent(evt.value.main.copy(),
				BmDateTimeWrapper.fromTimestamp(System.currentTimeMillis() + 5000));
		imip.iCalendarElements = Arrays.asList(exception2);

		handler.handle(imip, recipient, domain, user1Mailbox);

		evt = user1Calendar.getComplete(event.uid);
		assertEquals(2, evt.value.occurrences.size());

		evt.value.main.description = "hey, im an updated description";
		evt.value.main.summary = "hey, im an updated summary";
		evt.value.main.location = "@work";

		imip.iCalendarElements = Arrays.asList(evt.value.main);

		handler.handle(imip, recipient, domain, user1Mailbox);

		evt = user1Calendar.getComplete(event.uid);

		assertEquals(2, evt.value.occurrences.size());

		for (VEventOccurrence evtt : evt.value.occurrences) {
			assertEquals(evtt.description, "hey, im an updated description");
			assertEquals(evtt.summary, "hey, im an updated summary");
			assertEquals(evtt.location, "@work");
		}
	}

	@Test
	public void testRequestHandlerChangingMaster_ShouldLeaveExceptionsValuesUntouched_IfTheyHaveBeenModifiedInException()
			throws Exception {
		IIMIPHandler handler = new FakeEventRequestHandlerFactory().create();

		ItemValue<VEvent> event = defaultVEvent();
		IMIPInfos imip = imip(ITIPMethod.REQUEST, defaultExternalSenderVCard(), event.uid);

		event.value.summary = "summary";
		event.value.description = "description";
		event.value.location = "@home";

		RRule daily = new RRule();
		daily.frequency = Frequency.DAILY;
		event.value.rrule = daily;

		imip.iCalendarElements = Arrays.asList(event.value);
		ResolvedBox recipient = EnvelopeBuilder.lookupEmail("user1@domain.lan");

		handler.handle(imip, recipient, domain, user1Mailbox);

		ItemValue<VEventSeries> evt = user1Calendar.getComplete(event.uid);

		VEvent.Attendee additionalAttendee = VEvent.Attendee.create(VEvent.CUType.Individual, "", VEvent.Role.Chair,
				VEvent.ParticipationStatus.Accepted, true, "", "", "", "external", "", "", null,
				"external2@ext-domain.lan");

		VEventOccurrence exception = VEventOccurrence.fromEvent(evt.value.main.copy(),
				BmDateTimeWrapper.fromTimestamp(System.currentTimeMillis()));
		exception.attendees = copyListAndAdd(exception.attendees, additionalAttendee);
		exception.summary = "summary - modified - exception 1";
		exception.description = "description - modified - exception 1";
		exception.location = "@home - modified - exception 1";

		imip.iCalendarElements = Arrays.asList(exception);

		handler.handle(imip, recipient, domain, user1Mailbox);

		VEventOccurrence exception2 = VEventOccurrence.fromEvent(evt.value.main.copy(),
				BmDateTimeWrapper.fromTimestamp(System.currentTimeMillis() + 5000));
		exception2.rrule = null;
		exception2.attendees = copyListAndAdd(exception2.attendees, additionalAttendee);
		exception2.summary = "summary - modified - exception 2";
		exception2.description = "description - modified - exception 2";
		exception2.location = "@home - modified - exception 2";
		imip.iCalendarElements = Arrays.asList(exception2);

		handler.handle(imip, recipient, domain, user1Mailbox);

		ItemValue<VEventSeries> master = user1Calendar.getComplete(event.uid);

		master.value.main.description = "hey, im an updated description";
		master.value.main.summary = "hey, im an updated summary";
		master.value.main.location = "@work";

		imip.iCalendarElements = Arrays.asList(master.value.main);

		handler.handle(imip, recipient, domain, user1Mailbox);

		master = user1Calendar.getComplete(event.uid);

		assertEquals(master.value.main.description, "hey, im an updated description");
		assertEquals(master.value.main.summary, "hey, im an updated summary");
		assertEquals(master.value.main.location, "@work");
		assertEquals(2, master.value.main.attendees.size());

		assertEquals(2, master.value.occurrences.size());

		for (VEventOccurrence evtt : master.value.occurrences) {
			assertTrue(evtt.description.contains("- modified - "));
			assertTrue(evtt.summary.contains("- modified - "));
			assertTrue(evtt.location.contains("- modified - "));
			assertEquals(3, evtt.attendees.size());
		}

	}

	@Test
	public void testRequestHandlerChangingMaster_ShouldAdjustExceptionsAttendees() throws Exception {
		IIMIPHandler handler = new FakeEventRequestHandlerFactory().create();

		ItemValue<VEvent> master = defaultVEvent();
		VEvent evt = master.value.copy();
		IMIPInfos imip = imip(ITIPMethod.REQUEST, defaultExternalSenderVCard(), master.uid);

		master.value.summary = "summary";
		master.value.description = "description";
		master.value.location = "@home";

		RRule daily = new RRule();
		daily.frequency = Frequency.DAILY;
		master.value.rrule = daily;

		imip.iCalendarElements = Arrays.asList(master.value);
		ResolvedBox recipient = EnvelopeBuilder.lookupEmail("user1@domain.lan");

		handler.handle(imip, recipient, domain, user1Mailbox);

		ItemValue<VEventSeries> series = user1Calendar.getComplete(master.uid);

		VEvent.Attendee additionalAttendee = VEvent.Attendee.create(VEvent.CUType.Individual, "", VEvent.Role.Chair,
				VEvent.ParticipationStatus.Accepted, true, "", "", "", "external", "", "", null,
				"external2@ext-domain.lan");

		VEventOccurrence exception = VEventOccurrence.fromEvent(master.value.copy(),
				BmDateTimeWrapper.fromTimestamp(System.currentTimeMillis()));
		exception.summary = "summary - modified - exception 1";
		exception.description = "description - modified - exception 1";
		exception.location = "@home - modified - exception 1";

		exception.recurid = BmDateTimeWrapper.fromTimestamp(System.currentTimeMillis());
		imip.iCalendarElements = Arrays.asList(exception);

		handler.handle(imip, recipient, domain, user1Mailbox);

		series = user1Calendar.getComplete(series.uid);
		series.value.main.attendees = copyListAndAdd(series.value.main.attendees, additionalAttendee);

		imip.iCalendarElements = Arrays.asList(series.value.main);

		handler.handle(imip, recipient, domain, user1Mailbox);

		series = user1Calendar.getComplete(series.uid);

		assertEquals(3, series.value.main.attendees.size());

		series.value.main.attendees = new ArrayList<>();
		imip.iCalendarElements = Arrays.asList(series.value.main);

		handler.handle(imip, recipient, domain, user1Mailbox);

		series = user1Calendar.getComplete(series.uid);
		assertEquals(1, series.value.main.attendees.size()); // main has no attendees but an organizer, recipient has
																// been auto-added
		for (VEventOccurrence occurrence : series.value.occurrences) {
			assertEquals(0, occurrence.attendees.size());
		}

		imip.method = ITIPMethod.CANCEL;
		imip.iCalendarElements = Arrays.asList(exception);
		IIMIPHandler cancelHandler = new EventCancelHandler(recipient, null);
		cancelHandler.handle(imip, recipient, domain, user1Mailbox);

		imip.method = ITIPMethod.REQUEST;
		exception = VEventOccurrence.fromEvent(evt.copy(), BmDateTimeWrapper.fromTimestamp(System.currentTimeMillis()));
		exception.organizer = defaultVEvent().value.organizer;
		exception.summary = "summary - modified - exception 1";
		exception.description = "description - modified - exception 1";
		exception.location = "@home - modified - exception 1";

		VEvent.Attendee additionalAttendee3 = VEvent.Attendee.create(VEvent.CUType.Individual, "", VEvent.Role.Chair,
				VEvent.ParticipationStatus.Accepted, true, "", "", "", "external", "", "", null,
				"external3@ext-domain.lan");

		exception.attendees = copyListAndAdd(exception.attendees, additionalAttendee3);
		imip.iCalendarElements = Arrays.asList(exception);

		handler.handle(imip, recipient, domain, user1Mailbox);

		series = user1Calendar.getComplete(series.uid);

		assertEquals(1, series.value.main.attendees.size()); // main has no attendees but an organizer, recipient has
																// been auto-added
		for (VEventOccurrence occurrence : series.value.occurrences) {
			assertEquals(3, occurrence.attendees.size());
		}

	}

	private <T extends Object> List<T> copyListAndAdd(List<T> currentList, T additionalElement) {
		List<T> atts = new ArrayList<>();
		for (T attendee : currentList) {
			atts.add(attendee);
		}
		atts.add(additionalElement);
		return atts;
	}

	@Test
	public void requestHandlerResource() throws Exception {
		ItemValue<ResourceDescriptor> resource = createResource();
		ItemValue<Mailbox> resourceMailbox = testContext.provider().instance(IMailboxes.class, domainUid)
				.getComplete(resource.uid);
		ICalendar resourceCalendar = testContext.provider().instance(ICalendar.class,
				ICalendarUids.TYPE + ":" + resource.uid);
		IIMIPHandler handler = new FakeEventRequestHandlerFactory().create();

		ItemValue<VEvent> event = defaultVEvent();
		IMIPInfos imip = imip(ITIPMethod.REQUEST, defaultExternalSenderVCard(), event.uid);

		List<Attendee> attendees = new ArrayList<>();
		for (Attendee a : event.value.attendees) {
			if (a.mailto.equals(user1.value.defaultEmailAddress(domainUid))) {
				continue;
			}

			attendees.add(a);
		}
		attendees.add(VEvent.Attendee.create(VEvent.CUType.Individual, "", VEvent.Role.Chair,
				VEvent.ParticipationStatus.NeedsAction, true, "", "", "", resource.value.label, "", "", null,
				resource.value.emails.iterator().next().address));
		event.value.attendees = attendees;

		imip.iCalendarElements = Arrays.asList(event.value);
		ResolvedBox recipient = EnvelopeBuilder.lookupEmail(resource.value.emails.iterator().next().address);

		System.err.println("handle req 1...");
		handler.handle(imip, recipient, domain, resourceMailbox);
		System.err.println("req 1 handled.");
		Thread.sleep(2000);

		ItemValue<VEventSeries> evt = resourceCalendar.getComplete(event.uid);
		assertNotNull(evt);
		assertEquals(event.value.summary, evt.value.main.summary);
		assertEquals(2, evt.value.main.attendees.size());

		evt.value.main.summary = "updated";
		imip.iCalendarElements = Arrays.asList(evt.value.main);
		imip.sequence = 2;

		System.err.println("handle req 2...");
		handler.handle(imip, recipient, domain, resourceMailbox);
		System.err.println("req 2 handled.");

		evt = resourceCalendar.getComplete(event.uid);
		assertNotNull(evt);
		assertEquals("updated", evt.value.main.summary);
		assertEquals(2, evt.value.main.attendees.size());

	}

	@Test
	public void requestHandlerForbidden() throws Exception {
		PopulateHelper.addUser("user2", domainUid);

		ItemValue<ResourceDescriptor> resource = createResource();
		ItemValue<Mailbox> resourceMailbox = testContext.provider().instance(IMailboxes.class, domainUid)
				.getComplete(resource.uid);
		ICalendar resourceCalendar = testContext.provider().instance(ICalendar.class,
				ICalendarUids.TYPE + ":" + resource.uid);

		FakeSendmail mailer = new FakeSendmail();
		IIMIPHandler handler = new FakeEventRequestHandlerFactory().create(mailer);

		ItemValue<VEvent> event = defaultVEvent();
		event.value.organizer = new VEvent.Organizer("user2@" + domainUid);

		IMIPInfos imip = imip(ITIPMethod.REQUEST, defaultExternalSenderVCard(), event.uid);
		imip.organizerEmail = event.value.organizer.mailto;

		List<Attendee> attendees = new ArrayList<>();
		for (Attendee a : event.value.attendees) {
			attendees.add(a);
		}
		attendees.add(VEvent.Attendee.create(VEvent.CUType.Individual, "", VEvent.Role.Chair,
				VEvent.ParticipationStatus.NeedsAction, true, "", "", "", resource.value.label, "", "", null,
				resource.value.emails.iterator().next().address));
		event.value.attendees = attendees;

		imip.iCalendarElements = Arrays.asList(event.value);

		ResolvedBox recipient = EnvelopeBuilder.lookupEmail(user1Mailbox.value.defaultEmail().address);

		try {
			handler.handle(imip, recipient, domain, resourceMailbox);
			fail("User 2 invite User 1 : Imip filter did not throw a forbidden recipient exception");
		} catch (ServerFault e) {
			Throwable cause = e.getCause();
			assertTrue("User 2 invite User 1 : Handler exception is not a forbidden recipient exception",
					cause instanceof MailboxInvitationDeniedException);
		}
		ItemValue<VEventSeries> evt = user1Calendar.getComplete(event.uid);
		assertNull("Event should not have been created in user1 calendar", evt);

		recipient = EnvelopeBuilder.lookupEmail(resource.value.emails.iterator().next().address);

		try {
			handler.handle(imip, recipient, domain, resourceMailbox);
			fail("User 2 invite Resource : Imip filter did not throw a forbidden recipient exception");
		} catch (ServerFault e) {
			Throwable cause = e.getCause();
			assertTrue("User 2 invite Resource :Handler exception is not a forbidden recipient exception",
					cause instanceof MailboxInvitationDeniedException);
		}
		evt = resourceCalendar.getComplete(event.uid);
		assertNull("Event should not have been created in resource calendar", evt);

	}

	private ItemValue<ResourceDescriptor> createResource() throws ServerFault {
		String resourceUid = "resource-uuid";
		ResourceDescriptor r = new ResourceDescriptor();
		r.label = "resource";
		r.emails = Arrays.asList(Email.create(r.label + "@" + domainUid, true));
		r.typeIdentifier = "default";
		r.dataLocation = new BmConfIni().get("imap-role");

		testContext.provider().instance(IResources.class, domainUid).create(resourceUid, r);
		return ItemValue.create(Item.create(resourceUid, ""), r);
	}

	@Test
	public void cancelHandler() throws Exception {
		IIMIPHandler handler = new FakeEventRequestHandlerFactory().create();
		ItemValue<VEvent> event = defaultVEvent();
		IMIPInfos imip = imip(ITIPMethod.REQUEST, defaultExternalSenderVCard(), event.uid);

		imip.iCalendarElements = Arrays.asList(event.value);
		ResolvedBox recipient = EnvelopeBuilder.lookupEmail(user1Mailbox.value.defaultEmail().address);

		ItemValue<VEventSeries> evt = user1Calendar.getComplete(event.uid);
		assertNull(evt);

		handler.handle(imip, recipient, domain, user1Mailbox);

		evt = user1Calendar.getComplete(event.uid);
		assertNotNull(evt);

		IIMIPHandler cancelHandler = new EventCancelHandler(recipient, null);

		imip.method = ITIPMethod.CANCEL;

		cancelHandler.handle(imip, recipient, domain, user1Mailbox);
		evt = user1Calendar.getComplete(event.uid);

		assertNull(evt);

	}

	@Test
	public void cancelHandlerResource() throws Exception {
		ItemValue<ResourceDescriptor> resource = createResource();
		ItemValue<Mailbox> resourceMailbox = testContext.provider().instance(IMailboxes.class, domainUid)
				.getComplete(resource.uid);
		ICalendar resourceCalendar = testContext.provider().instance(ICalendar.class,
				ICalendarUids.TYPE + ":" + resource.uid);

		IIMIPHandler handler = new FakeEventRequestHandlerFactory().create();

		ItemValue<VEvent> event = defaultVEvent();
		IMIPInfos imip = imip(ITIPMethod.REQUEST, defaultExternalSenderVCard(), event.uid);

		event.value.attendees.remove(1); // remove user1 from attendee
		event.value.attendees.add(VEvent.Attendee.create(VEvent.CUType.Individual, "", VEvent.Role.Chair,
				VEvent.ParticipationStatus.NeedsAction, true, "", "", "", resource.value.label, "", "", null,
				resource.value.emails.iterator().next().address));

		imip.iCalendarElements = Arrays.asList(event.value);

		ResolvedBox recipient = EnvelopeBuilder.lookupEmail(resource.value.emails.iterator().next().address);

		ItemValue<VEventSeries> evt = resourceCalendar.getComplete(event.uid);
		assertNull(evt);

		handler.handle(imip, recipient, domain, resourceMailbox);

		evt = resourceCalendar.getComplete(event.uid);
		assertNotNull(evt);

		IIMIPHandler cancelHandler = new EventCancelHandler(recipient, null);

		imip.method = ITIPMethod.CANCEL;

		cancelHandler.handle(imip, recipient, domain, resourceMailbox);
		evt = resourceCalendar.getComplete(event.uid);
		assertNull(evt);
	}

	@Test
	public void replyHandler() throws Exception {
		IIMIPHandler handler = new FakeEventRequestHandlerFactory().create();

		ItemValue<VEvent> event = defaultVEvent();
		IMIPInfos imip = imip(ITIPMethod.REQUEST, defaultExternalSenderVCard(), event.uid);

		imip.iCalendarElements = Arrays.asList(event.value);
		ResolvedBox recipient = EnvelopeBuilder.lookupEmail(user1Mailbox.value.defaultEmail().address);

		ItemValue<VEventSeries> evt = user1Calendar.getComplete(event.uid);
		assertNull(evt);

		handler.handle(imip, recipient, domain, user1Mailbox);

		evt = user1Calendar.getComplete(event.uid);
		assertNotNull(evt);
		assertEquals(2, evt.value.main.attendees.size());

		// recreate event, could have been modified during last run
		event = defaultVEvent(event.uid);
		event.value.attendees = new ArrayList<>();
		VEvent.Attendee org = VEvent.Attendee.create(VEvent.CUType.Individual, "", VEvent.Role.Chair,
				VEvent.ParticipationStatus.Declined, true, "", "", "", "external", "", "", null,
				"external@ext-domain.lan");
		event.value.attendees.add(org);
		imip.iCalendarElements = Arrays.asList(event.value);

		imip.method = ITIPMethod.REPLY;
		IIMIPHandler replyHandler = new EventReplyHandler(recipient, null);
		replyHandler.handle(imip, recipient, domain, user1Mailbox);

		evt = user1Calendar.getComplete(event.uid);
		assertNotNull(evt);
		assertEquals(2, evt.value.main.attendees.size());

		boolean found = false;
		for (VEvent.Attendee attendee : evt.value.main.attendees) {
			if (attendee.mailto.equals("external@ext-domain.lan")) {
				assertEquals(VEvent.ParticipationStatus.Declined, attendee.partStatus);
				found = true;
			}
		}

		assertTrue(found);
	}

	@Test
	public void testReplyHandler_AttendeeReplyOccurrenceOfAllDayEventHandler() throws Exception {
		IIMIPHandler handler = new FakeEventRequestHandlerFactory().create();

		ItemValue<VEvent> event = defaultVEvent();
		IMIPInfos imip = imip(ITIPMethod.REQUEST, defaultExternalSenderVCard(), event.uid);

		event.value.dtstart = BmDateTimeWrapper.create(ZonedDateTime.of(2022, 2, 13, 0, 0, 0, 0, utcTz),
				Precision.Date);
		event.value.dtend = BmDateTimeWrapper.create(ZonedDateTime.of(2022, 2, 14, 0, 0, 0, 0, utcTz), Precision.Date);
		RRule daily = new RRule();
		daily.frequency = Frequency.DAILY;
		event.value.rrule = daily;

		imip.iCalendarElements = Arrays.asList(event.value);
		ResolvedBox recipient = EnvelopeBuilder.lookupEmail(user1Mailbox.value.defaultEmail().address);

		ItemValue<VEventSeries> evt = user1Calendar.getComplete(event.uid);
		assertNull(evt);

		handler.handle(imip, recipient, domain, user1Mailbox);

		evt = user1Calendar.getComplete(event.uid);
		assertNotNull(evt);
		assertEquals(2, evt.value.main.attendees.size());

		// Create Exception on first occurrence
		VEventOccurrence exception = VEventOccurrence.fromEvent(evt.value.main.copy(),
				BmDateTimeWrapper.create(ZonedDateTime.of(2022, 2, 13, 0, 0, 0, 0, utcTz), Precision.Date));
		exception.dtstart = BmDateTimeWrapper.create(ZonedDateTime.of(2022, 2, 13, 0, 0, 0, 0, utcTz), Precision.Date);
		exception.dtend = BmDateTimeWrapper.create(ZonedDateTime.of(2022, 2, 14, 0, 0, 0, 0, utcTz), Precision.Date);

		exception.attendees = new ArrayList<>();
		VEvent.Attendee org = VEvent.Attendee.create(VEvent.CUType.Individual, "", VEvent.Role.Chair,
				VEvent.ParticipationStatus.Declined, true, "", "", "", "external", "", "", null,
				"external@ext-domain.lan");
		exception.attendees.add(org);
		imip.iCalendarElements = Arrays.asList(exception);

		imip.method = ITIPMethod.REQUEST;
		IIMIPHandler requestHandler = new FakeEventRequestHandlerFactory().create();
		requestHandler.handle(imip, recipient, domain, user1Mailbox);

		imip.method = ITIPMethod.REPLY;
		IIMIPHandler replyHandler = new EventReplyHandler(recipient, null);
		replyHandler.handle(imip, recipient, domain, user1Mailbox);

		evt = user1Calendar.getComplete(event.uid);
		assertNotNull(evt);
		assertEquals(2, evt.value.main.attendees.size());

		boolean found = false;
		for (VEvent.Attendee attendee : evt.value.occurrences.get(0).attendees) {
			if (attendee.mailto.equals("external@ext-domain.lan")) {
				assertEquals(VEvent.ParticipationStatus.Declined, attendee.partStatus);
				found = true;
			}
		}

		assertTrue(found);
	}

	@Test
	public void testReplyHandler_AttendeeReplyOccurrenceWithRecurIdWithoutTimeEventHandler() throws Exception {
		IIMIPHandler handler = new FakeEventRequestHandlerFactory().create();

		ItemValue<VEvent> event = defaultVEvent();
		IMIPInfos imip = imip(ITIPMethod.REQUEST, defaultExternalSenderVCard(), event.uid);
		ZoneId tz = ZoneId.of("Europe/Paris");

		event.value.dtstart = BmDateTimeWrapper.create(ZonedDateTime.of(2022, 2, 13, 3, 0, 0, 0, tz),
				Precision.DateTime);
		event.value.dtend = BmDateTimeWrapper.create(ZonedDateTime.of(2022, 2, 13, 4, 0, 0, 0, tz), Precision.DateTime);
		RRule daily = new RRule();
		daily.frequency = Frequency.DAILY;
		event.value.rrule = daily;

		imip.iCalendarElements = Arrays.asList(event.value);
		ResolvedBox recipient = EnvelopeBuilder.lookupEmail(user1Mailbox.value.defaultEmail().address);

		ItemValue<VEventSeries> evt = user1Calendar.getComplete(event.uid);
		assertNull(evt);

		handler.handle(imip, recipient, domain, user1Mailbox);

		evt = user1Calendar.getComplete(event.uid);
		assertNotNull(evt);
		assertEquals(2, evt.value.main.attendees.size());

		// Create Exception with a a recuri id at 00:00:00 instead of 03:00:00
		VEventOccurrence exception = VEventOccurrence.fromEvent(evt.value.main.copy(),
				BmDateTimeWrapper.create(ZonedDateTime.of(2022, 2, 16, 0, 0, 0, 0, tz), Precision.DateTime));
		exception.dtstart = BmDateTimeWrapper.create(ZonedDateTime.of(2022, 2, 16, 3, 0, 0, 0, tz), Precision.DateTime);
		exception.dtend = BmDateTimeWrapper.create(ZonedDateTime.of(2022, 2, 16, 4, 0, 0, 0, tz), Precision.DateTime);

		exception.attendees = new ArrayList<>();
		VEvent.Attendee org = VEvent.Attendee.create(VEvent.CUType.Individual, "", VEvent.Role.Chair,
				VEvent.ParticipationStatus.Declined, true, "", "", "", "external", "", "", null,
				"external@ext-domain.lan");
		exception.attendees.add(org);
		imip.iCalendarElements = Arrays.asList(exception);

		imip.method = ITIPMethod.REQUEST;
		IIMIPHandler requestHandler = new FakeEventRequestHandlerFactory().create();
		requestHandler.handle(imip, recipient, domain, user1Mailbox);

		imip.method = ITIPMethod.REPLY;
		IIMIPHandler replyHandler = new EventReplyHandler(recipient, null);
		replyHandler.handle(imip, recipient, domain, user1Mailbox);

		evt = user1Calendar.getComplete(event.uid);
		assertNotNull(evt);
		assertEquals(2, evt.value.main.attendees.size());

		boolean found = false;
		for (VEvent.Attendee attendee : evt.value.occurrences.get(0).attendees) {
			if (attendee.mailto.equals("external@ext-domain.lan")) {
				assertEquals(VEvent.ParticipationStatus.Declined, attendee.partStatus);
				found = true;
			}
		}

		assertTrue(found);
	}

	@Test
	public void replyHandlerResource() throws Exception {
		ItemValue<ResourceDescriptor> resource = createResource();
		ItemValue<Mailbox> resourceMailbox = testContext.provider().instance(IMailboxes.class, domainUid)
				.getComplete(resource.uid);
		ICalendar resourceCalendar = testContext.provider().instance(ICalendar.class,
				ICalendarUids.TYPE + ":" + resource.uid);

		IIMIPHandler handler = new FakeEventRequestHandlerFactory().create();
		ItemValue<VEvent> event = defaultVEvent();
		IMIPInfos imip = imip(ITIPMethod.REQUEST, defaultExternalSenderVCard(), event.uid);

		event.value.attendees.remove(1); // remove user1 as attendee
		event.value.attendees.add(VEvent.Attendee.create(VEvent.CUType.Individual, "", VEvent.Role.Chair,
				VEvent.ParticipationStatus.NeedsAction, true, "", "", "", resource.value.label, "", "", null,
				resource.value.emails.iterator().next().address));

		imip.iCalendarElements = Arrays.asList(event.value);
		ResolvedBox recipient = EnvelopeBuilder.lookupEmail(resource.value.emails.iterator().next().address);

		ItemValue<VEventSeries> evt = testContext.provider()
				.instance(ICalendar.class, ICalendarUids.TYPE + ":" + resource.uid).getComplete(event.uid);
		assertNull(evt);

		handler.handle(imip, recipient, domain, resourceMailbox);
		evt = resourceCalendar.getComplete(event.uid);

		assertNotNull(evt);
		assertEquals(2, evt.value.main.attendees.size());

		event = defaultVEvent(event.uid);
		event.value.attendees = new ArrayList<>(1);
		VEvent.Attendee org = VEvent.Attendee.create(VEvent.CUType.Individual, "", VEvent.Role.Chair,
				VEvent.ParticipationStatus.Declined, true, "", "", "", "external", "", "", null,
				"external@ext-domain.lan");
		event.value.attendees.add(org);
		imip.iCalendarElements = Arrays.asList(event.value);
		Thread.sleep(500);
		CalendarTestAsyncHook.reset();

		imip.method = ITIPMethod.REPLY;
		IIMIPHandler replyHandler = new EventReplyHandler(recipient, null);

		replyHandler.handle(imip, recipient, domain, resourceMailbox);
		assertEquals(CalendarTestAsyncHook.Action.UPDATE, CalendarTestAsyncHook.action());
		Thread.sleep(500);

		evt = resourceCalendar.getComplete(event.uid);
		assertNotNull(evt);

		assertEquals(2, evt.value.main.attendees.size());

		boolean found = false;
		for (VEvent.Attendee attendee : evt.value.main.attendees) {
			if (attendee.mailto.equals("external@ext-domain.lan")) {
				assertEquals(VEvent.ParticipationStatus.Declined, attendee.partStatus);
				found = true;
			}
		}

		assertTrue(found);
	}

	private ItemValue<VEvent> defaultVEvent(String uid) {
		VEvent event = new VEvent();
		ZoneId tz = ZoneId.of("Asia/Ho_Chi_Minh");
		event.dtstart = BmDateTimeWrapper.create(ZonedDateTime.of(2022, 2, 13, 0, 0, 0, 0, tz), Precision.DateTime);
		event.dtend = BmDateTimeWrapper.create(ZonedDateTime.of(2022, 2, 13, 2, 0, 0, 0, tz), Precision.DateTime);
		event.summary = "event " + uid;
		event.location = "Toulouse";
		event.description = "Lorem ipsum";
		event.transparency = VEvent.Transparency.Opaque;
		event.classification = VEvent.Classification.Public;
		event.status = VEvent.Status.Confirmed;
		event.priority = 3;
		event.organizer = new VEvent.Organizer("external@ext-domain.lan");

		List<VEvent.Attendee> attendees = new ArrayList<>(2);

		VEvent.Attendee org = VEvent.Attendee.create(VEvent.CUType.Individual, "", VEvent.Role.Chair,
				VEvent.ParticipationStatus.Accepted, true, "", "", "", "external", "", "", null,
				"external@ext-domain.lan");
		attendees.add(org);

		VEvent.Attendee me = VEvent.Attendee.create(VEvent.CUType.Individual, "", VEvent.Role.Chair,
				VEvent.ParticipationStatus.NeedsAction, true, "", "", "",
				user1.value.contactInfos.identification.formatedName.value, "", "", null,
				user1.value.defaultEmailAddress(domainUid));
		attendees.add(me);

		event.attendees = attendees;

		return ItemValue.create(uid, event);
	}

	protected ItemValue<VEvent> defaultVEvent() {
		return defaultVEvent(UUID.randomUUID().toString());
	}

	private VCard defaultExternalSenderVCard() {
		VCard sender = new VCard();
		sender.identification = new VCard.Identification();
		sender.identification.formatedName = VCard.Identification.FormatedName.create("external",
				Arrays.<VCard.Parameter>asList());
		sender.communications.emails = Arrays
				.asList(VCard.Communications.Email.create("external@ext-domain.lan", Arrays.<VCard.Parameter>asList()));
		return sender;
	}

	private IMIPInfos imip(ITIPMethod method, VCard sender, String icsUid) {

		IMIPInfos imip = new IMIPInfos();
		imip.method = method;
		imip.messageId = UUID.randomUUID().toString();
		imip.organizerEmail = "external@ext-domain.lan";
		imip.uid = icsUid;
		imip.sequence = 0;
		imip.cid = Collections.emptyMap();

		return imip;
	}

	protected List<ItemValue<VEventSeries>> getVEventsFromIcs(String filename) throws IOException {
		InputStream in = this.getClass().getClassLoader().getResourceAsStream("ics/" + filename);
		String ics = FileUtils.streamString(in, true);
		in.close();
		List<ItemValue<VEventSeries>> vevents = convertToVEventList(ics, Optional.empty());
		return vevents;
	}

	private List<ItemValue<VEventSeries>> convertToVEventList(String ics, Optional<String> owner) {
		List<ItemValue<VEventSeries>> events = new LinkedList<>();
		Consumer<ItemValue<VEventSeries>> consumer = series -> events.add(series);
		VEventServiceHelper.parseCalendar(new ByteArrayInputStream(ics.getBytes()), Optional.empty(),
				Collections.emptyList(), consumer);
		return events;
	}

	@Test
	public void testTeamsRequestProducesCleanHtml() throws Exception {
		ResolvedBox recipient = EnvelopeBuilder.lookupEmail("user1@domain.lan");

		List<ItemValue<VEventSeries>> events = getVEventsFromIcs("teams.request.ics");
		ItemValue<VEventSeries> event = events.get(0);
		IIMIPHandler handler = new FakeEventRequestHandlerFactory().create();
		IMIPInfos imip = imip(ITIPMethod.REQUEST, defaultExternalSenderVCard(), event.uid);
		imip.iCalendarElements = Arrays.asList(event.value.main);
		handler.handle(imip, recipient, domain, user1Mailbox);

		ItemValue<VEventSeries> evt = user1Calendar.getComplete(event.uid);
		assertNotNull(evt);
		String desc = event.value.mainOccurrence().description;
		assertNotNull(desc);
		System.err.println(desc);

	}

	@Test
	public void testZoomInvitation() throws Exception {
		List<ItemValue<VEventSeries>> events = getVEventsFromIcs("vtz.ics");
		ItemValue<VEventSeries> event = events.get(0);
		assertEquals("America/Indianapolis", event.value.main.dtstart.timezone);
	}

	@Test
	public void testCancelException() throws Exception {
		ResolvedBox recipient = EnvelopeBuilder.lookupEmail("user1@domain.lan");

		// new request
		List<ItemValue<VEventSeries>> events = getVEventsFromIcs("bluemind.request.ics");
		ItemValue<VEventSeries> event = events.get(0);
		IIMIPHandler handler = new FakeEventRequestHandlerFactory().create();
		IMIPInfos imip = imip(ITIPMethod.REQUEST, defaultExternalSenderVCard(), event.uid);
		imip.iCalendarElements = Arrays.asList(event.value.main);
		handler.handle(imip, recipient, domain, user1Mailbox);

		ItemValue<VEventSeries> evt = user1Calendar.getComplete(event.uid);

		// request exception
		events = getVEventsFromIcs("bluemind.exception.ics");
		ItemValue<VEventSeries> exception = events.get(0);
		imip = imip(ITIPMethod.REQUEST, defaultExternalSenderVCard(), exception.uid);
		imip.iCalendarElements = Arrays.asList(exception.value.occurrences.get(0));
		handler.handle(imip, recipient, domain, user1Mailbox);
		evt = user1Calendar.getComplete(event.uid);

		assertEquals(1, evt.value.occurrences.size());

		// cancel exception
		IIMIPHandler cancelHandler = new EventCancelHandler(recipient, null);
		imip = imip(ITIPMethod.CANCEL, defaultExternalSenderVCard(), exception.uid);
		imip.iCalendarElements = Arrays.asList(exception.value.occurrences.get(0));
		cancelHandler.handle(imip, recipient, domain, user1Mailbox);

		evt = user1Calendar.getComplete(event.uid);
		assertNotNull(evt.value.main.rrule);
		assertEquals(1, evt.value.main.exdate.size());
		assertEquals(exception.value.occurrences.get(0).recurid, evt.value.main.exdate.iterator().next());
	}

	@Test
	public void testCancelException_AttendeeOnlyAttendsToException() throws Exception {
		ResolvedBox recipient = EnvelopeBuilder.lookupEmail("user1@domain.lan");

		// new request
		List<ItemValue<VEventSeries>> events = getVEventsFromIcs("bluemind.request.ics");
		ItemValue<VEventSeries> event = events.get(0);
		String imipUid = events.get(0).uid;
		IIMIPHandler handler = new FakeEventRequestHandlerFactory().create();
		IMIPInfos imip = imip(ITIPMethod.REQUEST, defaultExternalSenderVCard(), imipUid);
		imip.iCalendarElements = Arrays.asList(event.value.main);
		handler.handle(imip, recipient, domain, user1Mailbox);

		ItemValue<VEventSeries> evt = user1Calendar.getComplete(imipUid);
		assertNotNull(evt);

		// request (new attendee)
		String user2Uid = PopulateHelper.addUser("user2", domainUid);
		ItemValue<Mailbox> user2Mailbox = testContext.provider().instance(IMailboxes.class, domainUid)
				.getComplete(user2Uid);
		ICalendar user2Calendar = testContext.provider().instance(ICalendar.class,
				ICalendarUids.defaultUserCalendar(user2Uid));

		events = getVEventsFromIcs("bluemind.exception.new.attendee.ics");
		VEvent exception = events.get(0).value.occurrences.get(0);

		imip = imip(ITIPMethod.REQUEST, defaultExternalSenderVCard(), imipUid);
		imip.iCalendarElements = Arrays.asList(exception);
		handler.handle(imip, recipient, domain, user2Mailbox);
		evt = user2Calendar.getComplete(imipUid);
		assertNull(evt);
		List<ItemValue<VEventSeries>> all = user2Calendar.getByIcsUid(imipUid);
		assertEquals(1, all.size());
		assertEquals(1, all.get(0).value.occurrences.size());

		// cancel (new attendee)
		IIMIPHandler cancelHandler = new EventCancelHandler(recipient, null);
		imip = imip(ITIPMethod.CANCEL, defaultExternalSenderVCard(), imipUid);
		imip.iCalendarElements = Arrays.asList(exception);
		cancelHandler.handle(imip, recipient, domain, user2Mailbox);

		all = user2Calendar.getByIcsUid(imipUid);
		assertEquals(0, all.size());

	}

	@Test
	public void testCancelException_ExceptionNotInAttendeeCalendar() throws Exception {
		ResolvedBox recipient = EnvelopeBuilder.lookupEmail("user1@domain.lan");
		// If the exception is created from an other attendee reply the
		// exception is not
		// in the current attendee calendar but is in organizer calendar.
		// If organizer cancel the meeting, the attendee must add an exdate to
		// the parent
		// even if the exception is not found.

		// new request
		List<ItemValue<VEventSeries>> events = getVEventsFromIcs("bluemind.request.ics");
		List<ICalendarElement> eventList = events.stream().map(e -> e.value.main).collect(Collectors.toList());
		IIMIPHandler handler = new FakeEventRequestHandlerFactory().create();
		IMIPInfos imip = imip(ITIPMethod.REQUEST, defaultExternalSenderVCard(), events.get(0).uid);
		imip.iCalendarElements = Arrays.asList(eventList.get(0));
		handler.handle(imip, recipient, domain, user1Mailbox);

		ItemValue<VEventSeries> evt = user1Calendar.getComplete(events.get(0).uid);

		// request exception
		events = getVEventsFromIcs("bluemind.exception.ics");
		VEventOccurrence exception = events.get(0).value.occurrences.get(0);

		// cancel exception
		IIMIPHandler cancelHandler = new EventCancelHandler(recipient, null);
		imip = imip(ITIPMethod.CANCEL, defaultExternalSenderVCard(), events.get(0).uid);
		imip.iCalendarElements = Arrays.asList(exception);
		cancelHandler.handle(imip, recipient, domain, user1Mailbox);

		evt = user1Calendar.getComplete(events.get(0).uid);

		assertNotNull(evt.value.main.rrule);
		assertEquals(1, evt.value.main.exdate.size());
		assertEquals(exception.recurid, evt.value.main.exdate.iterator().next());

	}

	@Test
	public void testRequestHandlerMaster_ShouldRemoveOrphanExceptions() throws Exception {
		IIMIPHandler handler = new FakeEventRequestHandlerFactory().create();

		ItemValue<VEvent> master = defaultVEvent();
		IMIPInfos imip = imip(ITIPMethod.REQUEST, defaultExternalSenderVCard(), master.uid);

		master.value.summary = "summary";
		master.value.description = "description";
		master.value.location = "@home";

		RRule daily = new RRule();
		daily.frequency = Frequency.DAILY;
		master.value.rrule = daily;

		ResolvedBox recipient = EnvelopeBuilder.lookupEmail("user1@domain.lan");

		VEventOccurrence orphan = VEventOccurrence.fromEvent(master.value.copy(),
				BmDateTimeWrapper.fromTimestamp(System.currentTimeMillis()));
		orphan.summary = "summary - modified - exception 1";
		orphan.description = "description - modified - exception 1";
		orphan.location = "@home - modified - exception 1";

		orphan.recurid = BmDateTimeWrapper.fromTimestamp(System.currentTimeMillis());
		imip.iCalendarElements = Arrays.asList(orphan);

		handler.handle(imip, recipient, domain, user1Mailbox);

		List<ItemValue<VEventSeries>> series = user1Calendar.getByIcsUid(master.uid);
		assertEquals(1, series.size());
		assertNull(series.get(0).value.main);
		assertEquals(1, series.get(0).value.occurrences.size());

		imip.iCalendarElements = Arrays.asList(master.value, orphan);

		handler.handle(imip, recipient, domain, user1Mailbox);

		series = user1Calendar.getByIcsUid(master.uid);

		assertEquals(1, series.size());
		assertNotNull(series.get(0).value.main);
		assertEquals(1, series.get(0).value.occurrences.size());
	}

	@Test
	public void testRequestHandlerUpdateException_ShouldNotAddExceptions() throws Exception {
		IIMIPHandler handler = new FakeEventRequestHandlerFactory().create();

		ItemValue<VEvent> master = defaultVEvent();
		IMIPInfos imip = imip(ITIPMethod.REQUEST, defaultExternalSenderVCard(), master.uid);

		master.value.summary = "summary";
		master.value.description = "description";
		master.value.location = "@home";

		RRule daily = new RRule();
		daily.frequency = Frequency.DAILY;
		master.value.rrule = daily;

		ResolvedBox recipient = EnvelopeBuilder.lookupEmail("user1@domain.lan");

		VEventOccurrence orphan = VEventOccurrence.fromEvent(master.value.copy(),
				BmDateTimeWrapper.fromTimestamp(System.currentTimeMillis()));
		orphan.summary = "summary - modified - exception 1";
		orphan.description = "description - modified - exception 1";
		orphan.location = "@home - modified - exception 1";

		orphan.recurid = BmDateTimeWrapper.fromTimestamp(System.currentTimeMillis());
		imip.iCalendarElements = Arrays.asList(master.value, orphan);

		handler.handle(imip, recipient, domain, user1Mailbox);

		List<ItemValue<VEventSeries>> series = user1Calendar.getByIcsUid(master.uid);

		assertEquals(1, series.size());
		assertNotNull(series.get(0).value.main);
		assertEquals(1, series.get(0).value.occurrences.size());
		orphan.summary = "summary - Updated - exception 1";

		imip.iCalendarElements = Arrays.asList(orphan);

		handler.handle(imip, recipient, domain, user1Mailbox);

		series = user1Calendar.getByIcsUid(master.uid);

		assertEquals(1, series.size());
		assertNotNull(series.get(0).value.main);
		assertEquals(1, series.get(0).value.occurrences.size());
	}

	@Test
	public void testReplyHandler_ExternalAttendeeReplyOccurrenceUtcReccurId() {
		ItemValue<VEvent> event = defaultVEvent();

		ZoneId tz = ZoneId.of("Europe/Paris");

		event.value.dtstart = BmDateTimeWrapper.create(ZonedDateTime.of(2022, 2, 13, 3, 0, 0, 0, tz),
				Precision.DateTime);
		event.value.dtend = BmDateTimeWrapper.create(ZonedDateTime.of(2022, 2, 13, 4, 0, 0, 0, tz), Precision.DateTime);
		RRule daily = new RRule();
		daily.frequency = Frequency.DAILY;
		event.value.rrule = daily;
		event.value.organizer = new VEvent.Organizer(user1.value.defaultEmailAddress(domainUid));
		event.value.attendees = new ArrayList<Attendee>(1);
		VEvent.Attendee ext = VEvent.Attendee.create(VEvent.CUType.Individual, "", VEvent.Role.Chair,
				VEvent.ParticipationStatus.NeedsAction, true, "", "", "", "external", "", "", null,
				"external@ext-domain.lan");
		event.value.attendees.add(ext);

		// exception on 1st occurrence
		VEventOccurrence exception = VEventOccurrence.fromEvent(event.value.copy(),
				BmDateTimeWrapper.create(ZonedDateTime.of(2022, 2, 13, 3, 0, 0, 0, tz), Precision.DateTime));
		exception.dtstart = BmDateTimeWrapper.create(ZonedDateTime.of(2022, 2, 13, 15, 0, 0, 0, tz),
				Precision.DateTime);
		exception.dtend = BmDateTimeWrapper.create(ZonedDateTime.of(2022, 2, 13, 16, 0, 0, 0, tz), Precision.DateTime);
		exception.attendees = event.value.attendees;

		VEventSeries series = new VEventSeries();
		series.main = event.value;
		series.occurrences = new ArrayList<VEventOccurrence>(1);
		series.occurrences.add(exception);

		user1Calendar.create(event.uid, series, false);

		ItemValue<VEventSeries> evt = user1Calendar.getComplete(event.uid);
		assertNotNull(evt);

		IMIPInfos imip = imip(ITIPMethod.REPLY, defaultExternalSenderVCard(), event.uid);
		exception.recurid = BmDateTimeWrapper.create(ZonedDateTime.of(2022, 2, 13, 2, 0, 0, 0, utcTz),
				Precision.DateTime);
		ext = VEvent.Attendee.create(VEvent.CUType.Individual, "", VEvent.Role.Chair,
				VEvent.ParticipationStatus.Accepted, true, "", "", "", "external", "", "", null,
				"external@ext-domain.lan");
		exception.attendees = new ArrayList<>();
		exception.attendees.add(ext);

		imip.iCalendarElements = Arrays.asList(exception);
		ResolvedBox recipient = EnvelopeBuilder.lookupEmail(user1Mailbox.value.defaultEmail().address);

		IIMIPHandler replyHandler = new EventReplyHandler(recipient, null);
		replyHandler.handle(imip, recipient, domain, user1Mailbox);

		evt = user1Calendar.getComplete(event.uid);

		assertEquals(1, evt.value.occurrences.size());
		VEventOccurrence occ = evt.value.occurrences.get(0);
		assertEquals(1, occ.attendees.size());

		ext = occ.attendees.get(0);

		assertEquals("external@ext-domain.lan", ext.mailto);
		assertEquals(ParticipationStatus.Accepted, ext.partStatus);

	}

	@Test
	public void testReplyHandler_ExternalAttendeeReplyOccurrenceExcoticTzReccurId() {
		ItemValue<VEvent> event = defaultVEvent();

		ZoneId tz = ZoneId.of("Europe/Paris");

		event.value.dtstart = BmDateTimeWrapper.create(ZonedDateTime.of(2022, 2, 13, 3, 0, 0, 0, tz),
				Precision.DateTime);
		event.value.dtend = BmDateTimeWrapper.create(ZonedDateTime.of(2022, 2, 13, 4, 0, 0, 0, tz), Precision.DateTime);
		RRule daily = new RRule();
		daily.frequency = Frequency.DAILY;
		event.value.rrule = daily;
		event.value.organizer = new VEvent.Organizer(user1.value.defaultEmailAddress(domainUid));
		event.value.attendees = new ArrayList<Attendee>(1);
		VEvent.Attendee ext = VEvent.Attendee.create(VEvent.CUType.Individual, "", VEvent.Role.Chair,
				VEvent.ParticipationStatus.NeedsAction, true, "", "", "", "external", "", "", null,
				"external@ext-domain.lan");
		event.value.attendees.add(ext);

		// exception on 1st occurrence
		VEventOccurrence exception = VEventOccurrence.fromEvent(event.value.copy(),
				BmDateTimeWrapper.create(ZonedDateTime.of(2022, 2, 13, 3, 0, 0, 0, tz), Precision.DateTime));
		exception.dtstart = BmDateTimeWrapper.create(ZonedDateTime.of(2022, 2, 13, 15, 0, 0, 0, tz),
				Precision.DateTime);
		exception.dtend = BmDateTimeWrapper.create(ZonedDateTime.of(2022, 2, 13, 16, 0, 0, 0, tz), Precision.DateTime);
		exception.attendees = event.value.attendees;

		VEventSeries series = new VEventSeries();
		series.main = event.value;
		series.occurrences = new ArrayList<VEventOccurrence>(1);
		series.occurrences.add(exception);

		user1Calendar.create(event.uid, series, false);

		ItemValue<VEventSeries> evt = user1Calendar.getComplete(event.uid);
		assertNotNull(evt);

		IMIPInfos imip = imip(ITIPMethod.REPLY, defaultExternalSenderVCard(), event.uid);
		exception.recurid = BmDateTimeWrapper
				.create(ZonedDateTime.of(2022, 2, 13, 9, 0, 0, 0, ZoneId.of("Asia/Ho_Chi_Minh")), Precision.DateTime);
		ext = VEvent.Attendee.create(VEvent.CUType.Individual, "", VEvent.Role.Chair,
				VEvent.ParticipationStatus.Accepted, true, "", "", "", "external", "", "", null,
				"external@ext-domain.lan");
		exception.attendees = new ArrayList<>();
		exception.attendees.add(ext);

		imip.iCalendarElements = Arrays.asList(exception);
		ResolvedBox recipient = EnvelopeBuilder.lookupEmail(user1Mailbox.value.defaultEmail().address);

		IIMIPHandler replyHandler = new EventReplyHandler(recipient, null);
		replyHandler.handle(imip, recipient, domain, user1Mailbox);

		evt = user1Calendar.getComplete(event.uid);

		assertEquals(1, evt.value.occurrences.size());
		VEventOccurrence occ = evt.value.occurrences.get(0);
		assertEquals(1, occ.attendees.size());

		ext = occ.attendees.get(0);

		assertEquals("external@ext-domain.lan", ext.mailto);
		assertEquals(ParticipationStatus.Accepted, ext.partStatus);

	}

	@Test
	public void testReplyHandler_ExternalAttendeeReplyOccurrenceAllDayReccurId() {
		ItemValue<VEvent> event = defaultVEvent();

		event.value.dtstart = BmDateTimeWrapper.create(ZonedDateTime.of(2022, 2, 13, 0, 0, 0, 0, defaultTz),
				Precision.Date);
		event.value.dtend = BmDateTimeWrapper.create(ZonedDateTime.of(2022, 2, 14, 0, 0, 0, 0, defaultTz),
				Precision.Date);
		RRule daily = new RRule();
		daily.frequency = Frequency.DAILY;
		event.value.rrule = daily;
		event.value.organizer = new VEvent.Organizer(user1.value.defaultEmailAddress(domainUid));
		event.value.attendees = new ArrayList<Attendee>(1);
		VEvent.Attendee ext = VEvent.Attendee.create(VEvent.CUType.Individual, "", VEvent.Role.Chair,
				VEvent.ParticipationStatus.NeedsAction, true, "", "", "", "external", "", "", null,
				"external@ext-domain.lan");
		event.value.attendees.add(ext);

		// exception on 1st occurrence
		VEventOccurrence exception = VEventOccurrence.fromEvent(event.value.copy(),
				BmDateTimeWrapper.create(ZonedDateTime.of(2022, 2, 13, 0, 0, 0, 0, defaultTz), Precision.Date));
		exception.dtstart = BmDateTimeWrapper.create(ZonedDateTime.of(2022, 2, 14, 0, 0, 0, 0, defaultTz),
				Precision.Date);
		exception.dtend = BmDateTimeWrapper.create(ZonedDateTime.of(2022, 2, 15, 0, 0, 0, 0, defaultTz),
				Precision.Date);
		exception.attendees = event.value.attendees;

		VEventSeries series = new VEventSeries();
		series.main = event.value;
		series.occurrences = new ArrayList<VEventOccurrence>(1);
		series.occurrences.add(exception);

		user1Calendar.create(event.uid, series, false);

		ItemValue<VEventSeries> evt = user1Calendar.getComplete(event.uid);
		assertNotNull(evt);

		IMIPInfos imip = imip(ITIPMethod.REPLY, defaultExternalSenderVCard(), event.uid);
		exception.recurid = BmDateTimeWrapper.create(ZonedDateTime.of(2022, 2, 13, 0, 0, 0, 0, defaultTz),
				Precision.Date);
		ext = VEvent.Attendee.create(VEvent.CUType.Individual, "", VEvent.Role.Chair,
				VEvent.ParticipationStatus.Accepted, true, "", "", "", "external", "", "", null,
				"external@ext-domain.lan");
		exception.attendees = new ArrayList<>();
		exception.attendees.add(ext);

		imip.iCalendarElements = Arrays.asList(exception);
		ResolvedBox recipient = EnvelopeBuilder.lookupEmail(user1Mailbox.value.defaultEmail().address);

		IIMIPHandler replyHandler = new EventReplyHandler(recipient, null);
		replyHandler.handle(imip, recipient, domain, user1Mailbox);

		evt = user1Calendar.getComplete(event.uid);

		assertEquals(1, evt.value.occurrences.size());
		VEventOccurrence occ = evt.value.occurrences.get(0);
		assertEquals(1, occ.attendees.size());

		ext = occ.attendees.get(0);

		assertEquals("external@ext-domain.lan", ext.mailto);
		assertEquals(ParticipationStatus.Accepted, ext.partStatus);

	}

	@Test
	public void testCounter_DeclineEvent() throws Exception {
		ResolvedBox recipient = EnvelopeBuilder.lookupEmail("user1@domain.lan");

		// REQUEST
		List<ItemValue<VEventSeries>> events = getVEventsFromIcs("bluemind.request.ics");
		ItemValue<VEventSeries> event = events.get(0);
		String imipUid = events.get(0).uid;
		IIMIPHandler handler = new FakeEventRequestHandlerFactory().create();
		IMIPInfos imip = imip(ITIPMethod.REQUEST, defaultExternalSenderVCard(), imipUid);
		imip.iCalendarElements = Arrays.asList(event.value.main);
		handler.handle(imip, recipient, domain, user1Mailbox);

		ItemValue<VEventSeries> evt = user1Calendar.getComplete(imipUid);
		assertNotNull(evt);

		// COUNTER
		events = getVEventsFromIcs("bluemind.counter.ics");
		VEvent e = events.get(0).value.flatten().get(0);
		imipUid = events.get(0).uid;
		imip = imip(ITIPMethod.COUNTER, defaultExternalSenderVCard(), imipUid);
		imip.iCalendarElements = Arrays.asList(e);
		handler = new EventCounterHandler(null, null);
		handler.handle(imip, recipient, domain, user1Mailbox);

		evt = user1Calendar.getComplete(imipUid);

		assertEquals(1, evt.value.counters.size());
		VEventCounter counter = evt.value.counters.get(0);
		assertEquals(1, counter.counter.attendees.size());
		assertEquals("user1 DOMAIN.LAN", counter.counter.attendees.get(0).commonName);
		assertEquals("user1@domain.lan", counter.counter.attendees.get(0).mailto);
		assertEquals("2022-02-14T00:00:00.000+07:00", counter.counter.dtstart.iso8601);
		assertEquals("2022-02-14T02:00:00.000+07:00", counter.counter.dtend.iso8601);

		// DECLINECOUNTER
		events = getVEventsFromIcs("bluemind.declinecounter.ics");
		e = events.get(0).value.flatten().get(0);
		imipUid = events.get(0).uid;
		imip = imip(ITIPMethod.DECLINECOUNTER, defaultExternalSenderVCard(), imipUid);
		imip.iCalendarElements = Arrays.asList(e);
		handler = new EventDeclineCounterHandler(null, null);
		handler.handle(imip, recipient, domain, user1Mailbox);

		evt = user1Calendar.getComplete(imipUid);
		assertEquals(0, evt.value.counters.size());
	}

}
