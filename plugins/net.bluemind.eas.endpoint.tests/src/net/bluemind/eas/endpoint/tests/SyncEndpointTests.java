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
package net.bluemind.eas.endpoint.tests;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import io.vertx.core.buffer.Buffer;
import net.bluemind.addressbook.api.IAddressBook;
import net.bluemind.addressbook.api.VCard;
import net.bluemind.authentication.api.IAuthentication;
import net.bluemind.authentication.api.LoginResponse;
import net.bluemind.calendar.api.ICalendar;
import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.config.Token;
import net.bluemind.core.api.date.BmDateTime.Precision;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.container.api.ContainerSubscription;
import net.bluemind.core.container.api.IContainerManagement;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.http.ClientSideServiceProvider;
import net.bluemind.eas.command.folder.sync.FolderSyncEndpoint;
import net.bluemind.eas.command.sync.SyncEndpoint;
import net.bluemind.eas.dto.sync.SyncStatus;
import net.bluemind.eas.endpoint.tests.helpers.TestMail;
import net.bluemind.eas.http.wbxml.WbxmlHandlerBase;
import net.bluemind.eas.testhelper.mock.ResponseObject;
import net.bluemind.eas.testhelper.mock.TimeoutException;
import net.bluemind.eas.utils.DOMUtils;
import net.bluemind.eas.wbxml.WBXMLTools;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.MQ.IMQConnectHandler;
import net.bluemind.hornetq.client.OOPMessage;
import net.bluemind.hornetq.client.Producer;
import net.bluemind.hornetq.client.Topic;
import net.bluemind.icalendar.api.ICalendarElement.RRule;
import net.bluemind.icalendar.api.ICalendarElement.RRule.Frequency;
import net.bluemind.icalendar.api.ICalendarElement.RRule.WeekDay;
import net.bluemind.icalendar.api.ICalendarElement.Status;
import net.bluemind.imap.Flag;
import net.bluemind.imap.FlagsList;
import net.bluemind.tag.api.TagRef;
import net.bluemind.todolist.api.ITodoList;
import net.bluemind.todolist.api.VTodo;
import net.bluemind.user.api.IUserSubscription;

public class SyncEndpointTests extends AbstractEndpointTest {

	private int inboxServerId;
	private Producer mailNotification;

	private static final int PAGE_SIZE = 8;

	public void setUp() throws Exception {
		super.setUp();
		fetchFolderIds();
		for (int i = 0; i < PAGE_SIZE + 1; i++) {
			appendEmail("eas junit " + getName() + " " + (i + 1));
		}
		final CountDownLatch mqLatch = new CountDownLatch(1);
		MQ.init(new IMQConnectHandler() {

			@Override
			public void connected() {
				mailNotification = MQ.registerProducer(Topic.MAPI_ITEM_NOTIFICATIONS);
				mqLatch.countDown();
			}
		});
		mqLatch.await(5, TimeUnit.SECONDS);

	}

	private void fetchFolderIds() throws IOException {
		FolderSyncEndpoint fse = new FolderSyncEndpoint();
		Document document = DOMUtils.createDoc("FolderHierarchy", "FolderSync");
		Element root = document.getDocumentElement();
		Element sk = document.createElementNS("FolderHierarchy", "SyncKey");
		sk.setTextContent("0");
		root.appendChild(sk);
		ResponseObject folderSyncResponse = runEndpoint(fse, document);
		Buffer content = folderSyncResponse.content;
		Document folderSync = WBXMLTools.toXml(content.getBytes());
		assertNotNull(folderSync);
		NodeList added = folderSync.getDocumentElement().getElementsByTagNameNS("FolderHierarchy", "Add");
		for (int i = 0; i < added.getLength(); i++) {
			Element el = (Element) added.item(i);
			String folderType = DOMUtils.getUniqueElement(el, "Type").getTextContent();
			if ("2".equals(folderType)) { // INBOX
				inboxServerId = Integer.parseInt(DOMUtils.getUniqueElement(el, "ServerId").getTextContent());
			}
		}
		assertNotNull(inboxServerId);
		System.out.println("Inbox has serverId:" + inboxServerId);
	}

	public void tearDown() throws Exception {
		mailNotification.close();
		super.tearDown();
	}

	private void emitNewMailNotification() {
		OOPMessage msg = MQ.newMessage();
		msg.putStringProperty("mailbox", login);
		msg.putStringProperty("imapFolder", "INBOX");
		msg.putIntProperty("imapUid", 666);
		mailNotification.send(msg);
	}

	@Override
	public WbxmlHandlerBase createEndpoint() {
		return new SyncEndpoint();
	}

	public void testEmptySync() {
		Document syncInbox = DOMUtils.createDoc("AirSync", "Sync");
		ResponseObject response = runEndpoint(syncInbox);
		assertEquals(200, response.getStatusCode());
	}

	public void testSyncNoOptions() throws Exception {
		// <?xml version="1.0" encoding="UTF-8"?><Sync xmlns="AirSync">
		// <Collections>
		// <Collection>
		// <SyncKey>5643c87c-c7e2-4664-a4b4-3d8b065c5a1b</SyncKey>
		// <CollectionId>86976</CollectionId>
		// <GetChanges/>
		// <WindowSize>100</WindowSize>
		// </Collection>
		// </Collections>
		// </Sync>
		Document initSyncKeyChain = runInboxSyncEndpoint("0");
		String syncKey = syncKey(initSyncKeyChain);
		assertNotNull(syncKey);

		Document syncInbox = DOMUtils.createDoc("AirSync", "Sync");
		Element root = syncInbox.getDocumentElement();
		Element collections = DOMUtils.createElement(root, "Collections");
		Element collection = DOMUtils.createElement(collections, "Collection");
		DOMUtils.createElementAndText(collection, "SyncKey", syncKey);
		DOMUtils.createElementAndText(collection, "CollectionId", Integer.toString(inboxServerId));
		DOMUtils.createElement(collection, "GetChanges");
		DOMUtils.createElementAndText(collection, "WindowSize", "100");

		ResponseObject response = runEndpoint(syncInbox);
		assertEquals(200, response.getStatusCode());

	}

	public void testInvalidSyncReq() {
		Document syncInbox = DOMUtils.createDoc("AirSync", "Sync");
		// this is invalid, wrong NS
		Element sk = syncInbox.createElementNS("FolderHierarchy", "SyncKey");
		sk.setTextContent("123456");
		syncInbox.getDocumentElement().appendChild(sk);
		ResponseObject response = runEndpoint(syncInbox);
		// assertEquals(200, response.getStatusCode());
		assertEquals(400, response.getStatusCode());
		assertTrue("Validator should complain about the sync key element",
				response.getStatusMessage().contains("Invalid content was found starting with element 'SyncKey'"));
	}

	private Document runInboxSyncEndpoint(String syncKey) throws IOException {
		Document syncInbox = DOMUtils.createDoc("AirSync", "Sync");
		Element root = syncInbox.getDocumentElement();
		Element collections = DOMUtils.createElement(root, "Collections");
		Element collection = DOMUtils.createElement(collections, "Collection");
		DOMUtils.createElementAndText(collection, "SyncKey", syncKey);
		DOMUtils.createElementAndText(collection, "CollectionId", Integer.toString(inboxServerId));
		if (!syncKey.equals("0")) {
			// check we fixed the schema
			DOMUtils.createElement(collection, "GetChanges");
		}
		ResponseObject response = runEndpoint(syncInbox);
		assertEquals(200, response.getStatusCode());
		Buffer content = response.content;
		Document syncResponse = WBXMLTools.toXml(content.getBytes());
		return syncResponse;
	}

	private String syncKey(Document doc) {
		return DOMUtils.getUniqueElement(doc.getDocumentElement(), "SyncKey").getTextContent();
	}

	public void testSyncInboxZeroKey() throws IOException {
		Document syncResponse = runInboxSyncEndpoint("0");
		String syncKey = syncKey(syncResponse);
		assertNotNull(syncKey);
		assertFalse(syncKey.isEmpty());
		System.out.println("SyncKey is " + syncKey);
	}

	public void testSyncNotExistingCollectionZeroKey() throws IOException {
		Document syncResponse = runSyncEndpoint(Integer.MAX_VALUE, "0");
		String syncKey = syncKey(syncResponse);
		assertNotNull(syncKey);
		assertFalse(syncKey.isEmpty());
		System.out.println("SyncKey is " + syncKey);
	}

	public void testSyncInboxWithContent() throws IOException, TransformerException {
		Document initSyncKeyChain = runInboxSyncEndpoint("0");
		String syncKey = syncKey(initSyncKeyChain);
		assertNotNull(syncKey);
		Document folderContent = runInboxSyncEndpoint(syncKey);
		assertNotNull(folderContent);
		DOMUtils.logDom(folderContent);
	}

	public void testSyncWithWaitAndDelivery() throws Exception {
		Document initSyncKeyChain = runInboxSyncEndpoint("0");
		String syncKey = syncKey(initSyncKeyChain);
		assertNotNull(syncKey);
		Document folderContent = runInboxSyncEndpoint(syncKey);
		assertNotNull(folderContent);

		String nextSyncKey = syncKey(folderContent);
		System.out.println("***** next sync key will be: " + nextSyncKey);

		Document waitSync = DOMUtils.createDoc("AirSync", "Sync");
		Element root = waitSync.getDocumentElement();
		Element collections = DOMUtils.createElement(root, "Collections");
		Element collection = DOMUtils.createElement(collections, "Collection");
		DOMUtils.createElementAndText(collection, "SyncKey", syncKey);
		DOMUtils.createElementAndText(collection, "CollectionId", Integer.toString(inboxServerId));
		DOMUtils.createElementAndText(root, "Wait", "5");
		try {
			runEndpoint(waitSync);
			fail("Nothing happens, this should timeout");
		} catch (TimeoutException to) {
		}

		emitNewMailNotification();

		try {
			Buffer content = lastAsyncResponse.waitForIt(20, TimeUnit.SECONDS);
			assertNotNull(content);
			assertEquals(200, lastAsyncResponse.getStatusCode());
			Document syncResponse = WBXMLTools.toXml(content.getBytes());
			assertNotNull(syncResponse);
			String status = DOMUtils.getUniqueElement(syncResponse.getDocumentElement(), "Status").getTextContent();
			assertEquals("Status should be OK", SyncStatus.OK.asXmlValue(), status);
		} catch (TimeoutException e) {
			fail("A notification was sent, lastAsyncResponse should be unlocked.");
		}
	}

	public void testSyncHeartbeatIntervalAndDelivery() throws Exception {
		Document initSyncKeyChain = runInboxSyncEndpoint("0");
		String syncKey = syncKey(initSyncKeyChain);
		assertNotNull(syncKey);
		Document folderContent = runInboxSyncEndpoint(syncKey);
		assertNotNull(folderContent);

		String nextSyncKey = syncKey(folderContent);
		System.out.println("***** next sync key will be: " + nextSyncKey);

		Document waitSync = DOMUtils.createDoc("AirSync", "Sync");
		Element root = waitSync.getDocumentElement();
		Element collections = DOMUtils.createElement(root, "Collections");
		Element collection = DOMUtils.createElement(collections, "Collection");
		DOMUtils.createElementAndText(collection, "SyncKey", syncKey);
		DOMUtils.createElementAndText(collection, "CollectionId", Integer.toString(inboxServerId));
		DOMUtils.createElementAndText(root, "HeartbeatInterval", "900");
		try {
			runEndpoint(waitSync);
			fail("Nothing happens, this should timeout");
		} catch (TimeoutException to) {
		}

		emitNewMailNotification();

		try {
			Buffer content = lastAsyncResponse.waitForIt(20, TimeUnit.SECONDS);
			assertNotNull(content);
			assertEquals(200, lastAsyncResponse.getStatusCode());
			Document syncResponse = WBXMLTools.toXml(content.getBytes());
			assertNotNull(syncResponse);
			String status = DOMUtils.getUniqueElement(syncResponse.getDocumentElement(), "Status").getTextContent();
			assertEquals("Status should be OK", SyncStatus.OK.asXmlValue(), status);
		} catch (TimeoutException e) {
			fail("A notification was sent, lastAsyncResponse should be unlocked.");
		}
	}

	public void testCalendarSync() throws Exception {
		IAuthentication authService = ClientSideServiceProvider.getProvider(testDevice.coreUrl, null)
				.instance(IAuthentication.class);
		LoginResponse token = authService.login("admin0@global.virt", Token.admin0(), "sync-endpoint-calendar-tests");
		IContainers ic = ClientSideServiceProvider.getProvider(testDevice.coreUrl, token.authKey)
				.instance(IContainers.class, domainUid);
		String calendarUid = UUID.randomUUID().toString();
		ContainerDescriptor calendarDescriptor = ContainerDescriptor.create(calendarUid, "vacances",
				SecurityContext.SYSTEM.getSubject(), "calendar", domainUid, true);
		ic.create(calendarUid, calendarDescriptor);

		IContainerManagement contactBookContainerManagement = ClientSideServiceProvider
				.getProvider(testDevice.coreUrl, token.authKey).instance(IContainerManagement.class, calendarUid);
		contactBookContainerManagement
				.setAccessControlList(Arrays.asList(AccessControlEntry.create(testDevice.owner.uid, Verb.Write)));

		IUserSubscription userSubService = ClientSideServiceProvider.getProvider(testDevice.coreUrl, token.authKey)
				.instance(IUserSubscription.class, domainUid);
		userSubService.subscribe(owner.uid, Arrays.asList(ContainerSubscription.create(calendarUid, false)));

		contactBookContainerManagement.allowOfflineSync(owner.uid);

		// Sync mother fucker
		Integer calendarId = null;
		FolderSyncEndpoint fse = new FolderSyncEndpoint();
		Document document = DOMUtils.createDoc("FolderHierarchy", "FolderSync");
		Element root = document.getDocumentElement();
		Element sk = document.createElementNS("FolderHierarchy", "SyncKey");
		sk.setTextContent("0");
		root.appendChild(sk);
		ResponseObject folderSyncResponse = runEndpoint(fse, document);
		Buffer content = folderSyncResponse.content;
		Document folderSync = WBXMLTools.toXml(content.getBytes());
		assertNotNull(folderSync);
		NodeList added = folderSync.getDocumentElement().getElementsByTagNameNS("FolderHierarchy", "Add");
		for (int i = 0; i < added.getLength(); i++) {
			Element el = (Element) added.item(i);
			String folderType = DOMUtils.getUniqueElement(el, "Type").getTextContent();
			String displayName = DOMUtils.getUniqueElement(el, "DisplayName").getTextContent();
			if ("13".equals(folderType) && calendarDescriptor.name.equals(displayName)) { // USER_CREATED_CALENDAR
				calendarId = Integer.parseInt(DOMUtils.getUniqueElement(el, "ServerId").getTextContent());
			}
		}
		assertNotNull(calendarId);

		token = authService.login(testDevice.loginAtDomain, testDevice.password, "sync-endpoint-calendar-tests");
		ICalendar cc = ClientSideServiceProvider.getProvider(testDevice.coreUrl, token.authKey)
				.instance(ICalendar.class, calendarUid);

		// Create 12 events
		for (int i = 0; i < PAGE_SIZE + (PAGE_SIZE / 2); i++) {
			VEvent event = new VEvent();
			event.dtstart = BmDateTimeWrapper.create(ZonedDateTime.now(), Precision.DateTime);
			event.summary = "Piscine";
			event.description = "w00t";
			event.transparency = VEvent.Transparency.Opaque;
			event.classification = VEvent.Classification.Public;
			event.status = VEvent.Status.Confirmed;
			event.priority = 3;

			event.organizer = new VEvent.Organizer(testDevice.owner.displayName,
					testDevice.owner.value.defaultEmail().address);

			List<VEvent.Attendee> attendees = new ArrayList<>(1);
			VEvent.Attendee me = VEvent.Attendee.create(VEvent.CUType.Individual, "", VEvent.Role.Chair,
					VEvent.ParticipationStatus.Accepted, true, "", "", "", calendarDescriptor.name, "", "",
					calendarDescriptor.uid, null);
			attendees.add(me);

			event.attendees = attendees;
			cc.create(UUID.randomUUID().toString(), VEventSeries.create(event), false);
		}

		// Decline 2 events
		for (int i = 0; i < 2; i++) {
			VEvent event = new VEvent();
			event.dtstart = BmDateTimeWrapper.create(ZonedDateTime.now(), Precision.DateTime);
			event.summary = "Piscine";
			event.description = "w00t";
			event.transparency = VEvent.Transparency.Opaque;
			event.classification = VEvent.Classification.Public;
			event.status = VEvent.Status.Confirmed;
			event.priority = 3;

			event.organizer = new VEvent.Organizer(testDevice.owner.displayName,
					testDevice.owner.value.defaultEmail().address);

			List<VEvent.Attendee> attendees = new ArrayList<>(1);
			VEvent.Attendee me = VEvent.Attendee.create(VEvent.CUType.Individual, "", VEvent.Role.Chair,
					VEvent.ParticipationStatus.Declined, true, "", "", "", calendarDescriptor.name, "", "",
					calendarDescriptor.uid, null);
			attendees.add(me);

			event.attendees = attendees;
			cc.create(UUID.randomUUID().toString(), VEventSeries.create(event), false);
		}

		// Delete 2 events
		for (int i = 0; i < 2; i++) {
			VEvent event = new VEvent();
			event.dtstart = BmDateTimeWrapper.create(ZonedDateTime.now(), Precision.DateTime);
			event.summary = "Piscine";
			event.description = "w00t";
			event.transparency = VEvent.Transparency.Opaque;
			event.classification = VEvent.Classification.Public;
			event.status = VEvent.Status.Confirmed;
			event.priority = 3;

			event.organizer = new VEvent.Organizer(testDevice.owner.displayName,
					testDevice.owner.value.defaultEmail().address);

			List<VEvent.Attendee> attendees = new ArrayList<>(1);
			VEvent.Attendee me = VEvent.Attendee.create(VEvent.CUType.Individual, "", VEvent.Role.Chair,
					VEvent.ParticipationStatus.Accepted, true, "", "", "", calendarDescriptor.name, "", "",
					calendarDescriptor.uid, null);
			attendees.add(me);

			event.attendees = attendees;
			String uid = UUID.randomUUID().toString();
			cc.create(uid, VEventSeries.create(event), false);
			cc.delete(uid, true);
		}

		// sk 0
		Document sync = DOMUtils.createDoc("AirSync", "Sync");
		root = sync.getDocumentElement();
		Element collections = DOMUtils.createElement(root, "Collections");
		Element collection = DOMUtils.createElement(collections, "Collection");
		DOMUtils.createElementAndText(collection, "SyncKey", "0");
		DOMUtils.createElementAndText(collection, "CollectionId", Integer.toString(calendarId));

		ResponseObject response = runEndpoint(sync);
		assertEquals(200, response.getStatusCode());
		content = response.content;
		Document resp = WBXMLTools.toXml(content.getBytes());
		String syncKey = syncKey(resp);
		assertNotNull(syncKey);
		String collectionId = DOMUtils.getUniqueElement(resp.getDocumentElement(), "CollectionId").getTextContent();
		assertEquals(calendarId + "", collectionId);
		String status = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Status").getTextContent();
		assertEquals("1", status);
		Element moreAvailable = DOMUtils.getUniqueElement(resp.getDocumentElement(), "MoreAvailable");
		assertNull(moreAvailable);
		Element commands = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Commands");
		assertNull(commands);

		// 1st page (8 items)
		resp = runSyncEndpoint(calendarId, syncKey);
		syncKey = syncKey(resp);
		moreAvailable = DOMUtils.getUniqueElement(resp.getDocumentElement(), "MoreAvailable");
		assertNotNull(moreAvailable);
		commands = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Commands");
		assertNotNull(commands);
		assertEquals(PAGE_SIZE, commands.getChildNodes().getLength());
		for (int i = 0; i < commands.getChildNodes().getLength(); i++) {
			Element item = (Element) commands.getChildNodes().item(i);
			assertEquals("Add", item.getNodeName());
			Element appData = DOMUtils.getUniqueElement(item, "ApplicationData");
			assertNotNull(appData);
			assertNotNull(DOMUtils.getUniqueElement(appData, "Timezone"));
			assertNotNull(DOMUtils.getUniqueElement(appData, "AllDayEvent"));
			assertNotNull(DOMUtils.getUniqueElement(appData, "BusyStatus"));
			assertNotNull(DOMUtils.getUniqueElement(appData, "OrganizerName"));
			assertNotNull(DOMUtils.getUniqueElement(appData, "OrganizerEmail"));
			assertNotNull(DOMUtils.getUniqueElement(appData, "DtStamp"));
			assertNotNull(DOMUtils.getUniqueElement(appData, "EndTime"));
			assertNotNull(DOMUtils.getUniqueElement(appData, "Sensitivity"));
			assertNotNull(DOMUtils.getUniqueElement(appData, "Subject"));
			assertNotNull(DOMUtils.getUniqueElement(appData, "StartTime"));
			assertNotNull(DOMUtils.getUniqueElement(appData, "UID"));

			Element body = DOMUtils.getUniqueElement(appData, "Body");
			assertNotNull(body);
			assertNotNull(DOMUtils.getUniqueElement(body, "Type"));
			assertNotNull(DOMUtils.getUniqueElement(body, "EstimatedDataSize"));
			Element dataElem = DOMUtils.getUniqueElement(body, "Data");
			assertNotNull(dataElem);
			System.out.println(dataElem.getTextContent());

			assertNull(DOMUtils.getUniqueElement(appData, "Location"));
			assertNull(DOMUtils.getUniqueElement(appData, "Recurrence"));
		}

		// 2nd page (6 items)
		resp = runSyncEndpoint(calendarId, syncKey);
		syncKey = syncKey(resp);
		moreAvailable = DOMUtils.getUniqueElement(resp.getDocumentElement(), "MoreAvailable");
		assertNull(moreAvailable);
		commands = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Commands");
		assertNotNull(commands);
		assertEquals(6, commands.getChildNodes().getLength());
		for (int i = 0; i < commands.getChildNodes().getLength(); i++) {
			Element item = (Element) commands.getChildNodes().item(i);
			assertEquals("Add", item.getNodeName());
			Element appData = DOMUtils.getUniqueElement(item, "ApplicationData");
			assertNotNull(appData);
			assertNotNull(DOMUtils.getUniqueElement(appData, "Timezone"));
			assertNotNull(DOMUtils.getUniqueElement(appData, "AllDayEvent"));
			assertNotNull(DOMUtils.getUniqueElement(appData, "BusyStatus"));
			assertNotNull(DOMUtils.getUniqueElement(appData, "OrganizerName"));
			assertNotNull(DOMUtils.getUniqueElement(appData, "OrganizerEmail"));
			assertNotNull(DOMUtils.getUniqueElement(appData, "DtStamp"));
			assertNotNull(DOMUtils.getUniqueElement(appData, "EndTime"));
			assertNotNull(DOMUtils.getUniqueElement(appData, "Sensitivity"));
			assertNotNull(DOMUtils.getUniqueElement(appData, "Subject"));
			assertNotNull(DOMUtils.getUniqueElement(appData, "StartTime"));
			assertNotNull(DOMUtils.getUniqueElement(appData, "UID"));
			assertNull(DOMUtils.getUniqueElement(appData, "Location"));
			assertNull(DOMUtils.getUniqueElement(appData, "Recurrence"));
		}

		// next sync (0 item)
		resp = runSyncEndpoint(calendarId, syncKey);
		syncKey = syncKey(resp);
		commands = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Commands");
		assertNull(commands);
	}

	public void testCreateRecEvent() throws Exception {
		IAuthentication authService = ClientSideServiceProvider.getProvider(testDevice.coreUrl, null)
				.instance(IAuthentication.class);
		LoginResponse token = authService.login("admin0@global.virt", Token.admin0(), "sync-endpoint-calendar-tests");
		IContainers ic = ClientSideServiceProvider.getProvider(testDevice.coreUrl, token.authKey)
				.instance(IContainers.class, domainUid);
		String calendarUid = UUID.randomUUID().toString();
		ContainerDescriptor calendarDescriptor = ContainerDescriptor.create(calendarUid, "vacances",
				SecurityContext.SYSTEM.getSubject(), "calendar", domainUid, true);
		ic.create(calendarUid, calendarDescriptor);

		IContainerManagement contactBookContainerManagement = ClientSideServiceProvider
				.getProvider(testDevice.coreUrl, token.authKey).instance(IContainerManagement.class, calendarUid);
		contactBookContainerManagement
				.setAccessControlList(Arrays.asList(AccessControlEntry.create(testDevice.owner.uid, Verb.Write)));

		IUserSubscription userSubService = ClientSideServiceProvider.getProvider(testDevice.coreUrl, token.authKey)
				.instance(IUserSubscription.class, domainUid);
		userSubService.subscribe(owner.uid, Arrays.asList(ContainerSubscription.create(calendarUid, false)));

		contactBookContainerManagement.allowOfflineSync(owner.uid);

		// Sync mother fucker
		Integer calendarId = null;
		FolderSyncEndpoint fse = new FolderSyncEndpoint();
		Document document = DOMUtils.createDoc("FolderHierarchy", "FolderSync");
		Element root = document.getDocumentElement();
		Element sk = document.createElementNS("FolderHierarchy", "SyncKey");
		sk.setTextContent("0");
		root.appendChild(sk);
		ResponseObject folderSyncResponse = runEndpoint(fse, document);
		Buffer content = folderSyncResponse.content;
		Document folderSync = WBXMLTools.toXml(content.getBytes());
		assertNotNull(folderSync);
		NodeList added = folderSync.getDocumentElement().getElementsByTagNameNS("FolderHierarchy", "Add");
		for (int i = 0; i < added.getLength(); i++) {
			Element el = (Element) added.item(i);
			String folderType = DOMUtils.getUniqueElement(el, "Type").getTextContent();
			String displayName = DOMUtils.getUniqueElement(el, "DisplayName").getTextContent();
			if ("13".equals(folderType) && calendarDescriptor.name.equals(displayName)) { // USER_CREATED_CALENDAR
				calendarId = Integer.parseInt(DOMUtils.getUniqueElement(el, "ServerId").getTextContent());
			}
		}
		assertNotNull(calendarId);

		// Create an event
		token = authService.login(testDevice.loginAtDomain, testDevice.password, "sync-endpoint-calendar-tests");
		ICalendar cc = ClientSideServiceProvider.getProvider(testDevice.coreUrl, token.authKey)
				.instance(ICalendar.class, calendarUid);

		Calendar c = Calendar.getInstance();
		c.add(Calendar.SECOND, -3600);

		VEvent event = new VEvent();
		ZonedDateTime temp = ZonedDateTime.ofInstant(Instant.ofEpochMilli(c.getTime().getTime()),
				ZoneId.systemDefault());
		event.dtstart = BmDateTimeWrapper.create(temp, Precision.DateTime);
		event.summary = "Piscine";
		event.location = "Toulouse";
		event.description = "Lorem ipsum";
		event.transparency = VEvent.Transparency.Opaque;
		event.classification = VEvent.Classification.Public;
		event.status = VEvent.Status.Confirmed;
		event.priority = 3;

		event.organizer = new VEvent.Organizer(testDevice.owner.displayName,
				testDevice.owner.value.defaultEmail().address);

		List<VEvent.Attendee> attendees = new ArrayList<>(1);
		VEvent.Attendee me = VEvent.Attendee.create(VEvent.CUType.Individual, "", VEvent.Role.Chair,
				VEvent.ParticipationStatus.Accepted, true, "", "", "", calendarDescriptor.name, "", "",
				calendarDescriptor.uid, null);
		attendees.add(me);

		event.attendees = attendees;

		event.rrule = new RRule();
		c = Calendar.getInstance();
		c.set(2022, 1, 1, 7, 0, 0);
		ZonedDateTime tempDate = ZonedDateTime.ofInstant(Instant.ofEpochMilli(c.getTime().getTime()),
				ZoneId.systemDefault());
		event.rrule.until = BmDateTimeWrapper.create(tempDate, Precision.DateTime);
		event.rrule.byDay = Arrays.asList(WeekDay.TU, WeekDay.WE);
		event.rrule.frequency = Frequency.WEEKLY;
		event.rrule.interval = 1;

		cc.create(UUID.randomUUID().toString(), VEventSeries.create(event), false);

		Document sync = DOMUtils.createDoc("AirSync", "Sync");
		root = sync.getDocumentElement();
		Element collections = DOMUtils.createElement(root, "Collections");
		Element collection = DOMUtils.createElement(collections, "Collection");
		DOMUtils.createElementAndText(collection, "SyncKey", "0");
		DOMUtils.createElementAndText(collection, "CollectionId", Integer.toString(calendarId));

		ResponseObject response = runEndpoint(sync);
		assertEquals(200, response.getStatusCode());
		content = response.content;
		Document resp = WBXMLTools.toXml(content.getBytes());
		String syncKey = syncKey(resp);
		assertNotNull(syncKey);
		String collectionId = DOMUtils.getUniqueElement(resp.getDocumentElement(), "CollectionId").getTextContent();
		assertEquals(calendarId + "", collectionId);
		String status = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Status").getTextContent();
		assertEquals("1", status);
		Element moreAvailable = DOMUtils.getUniqueElement(resp.getDocumentElement(), "MoreAvailable");
		assertNull(moreAvailable);
		Element commands = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Commands");
		assertNull(commands);

		resp = runSyncEndpoint(calendarId, syncKey);
		syncKey = syncKey(resp);
		moreAvailable = DOMUtils.getUniqueElement(resp.getDocumentElement(), "MoreAvailable");
		assertNull(moreAvailable);
		commands = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Commands");
		assertNotNull(commands);
		assertEquals(1, commands.getChildNodes().getLength());
		Element item = (Element) commands.getChildNodes().item(0);
		assertEquals("Add", item.getNodeName());
		Element appData = DOMUtils.getUniqueElement(item, "ApplicationData");
		assertNotNull(appData);
		assertNotNull(DOMUtils.getUniqueElement(appData, "Timezone"));
		assertNotNull(DOMUtils.getUniqueElement(appData, "AllDayEvent"));
		assertNotNull(DOMUtils.getUniqueElement(appData, "BusyStatus"));
		assertNotNull(DOMUtils.getUniqueElement(appData, "OrganizerName"));
		assertNotNull(DOMUtils.getUniqueElement(appData, "OrganizerEmail"));
		assertNotNull(DOMUtils.getUniqueElement(appData, "DtStamp"));
		assertNotNull(DOMUtils.getUniqueElement(appData, "EndTime"));
		assertNotNull(DOMUtils.getUniqueElement(appData, "Sensitivity"));
		assertNotNull(DOMUtils.getUniqueElement(appData, "Subject"));
		assertNotNull(DOMUtils.getUniqueElement(appData, "StartTime"));
		assertNotNull(DOMUtils.getUniqueElement(appData, "UID"));
		assertNotNull(DOMUtils.getUniqueElement(appData, "Location"));

		Element rec = DOMUtils.getUniqueElement(appData, "Recurrence");
		assertNotNull(rec);
		assertEquals("1", DOMUtils.getUniqueElement(rec, "Type").getTextContent());
		assertEquals("1", DOMUtils.getUniqueElement(rec, "Interval").getTextContent());
		assertEquals("12", DOMUtils.getUniqueElement(rec, "DayOfWeek").getTextContent());

		assertEquals("20220201T060000Z", DOMUtils.getUniqueElement(rec, "Until").getTextContent());
	}

	public void testDeleteEvent() throws Exception {
		IAuthentication authService = ClientSideServiceProvider.getProvider(testDevice.coreUrl, null)
				.instance(IAuthentication.class);
		LoginResponse token = authService.login("admin0@global.virt", Token.admin0(), "sync-endpoint-calendar-tests");
		IContainers ic = ClientSideServiceProvider.getProvider(testDevice.coreUrl, token.authKey)
				.instance(IContainers.class, domainUid);
		String calendarUid = UUID.randomUUID().toString();
		ContainerDescriptor calendarDescriptor = ContainerDescriptor.create(calendarUid, "vacances",
				SecurityContext.SYSTEM.getSubject(), "calendar", domainUid, true);
		ic.create(calendarUid, calendarDescriptor);

		IContainerManagement contactBookContainerManagement = ClientSideServiceProvider
				.getProvider(testDevice.coreUrl, token.authKey).instance(IContainerManagement.class, calendarUid);
		contactBookContainerManagement
				.setAccessControlList(Arrays.asList(AccessControlEntry.create(testDevice.owner.uid, Verb.Write)));

		IUserSubscription userSubService = ClientSideServiceProvider.getProvider(testDevice.coreUrl, token.authKey)
				.instance(IUserSubscription.class, domainUid);
		userSubService.subscribe(owner.uid, Arrays.asList(ContainerSubscription.create(calendarUid, false)));

		contactBookContainerManagement.allowOfflineSync(owner.uid);

		// Sync mother fucker
		Integer calendarId = null;
		FolderSyncEndpoint fse = new FolderSyncEndpoint();
		Document document = DOMUtils.createDoc("FolderHierarchy", "FolderSync");
		Element root = document.getDocumentElement();
		Element sk = document.createElementNS("FolderHierarchy", "SyncKey");
		sk.setTextContent("0");
		root.appendChild(sk);
		ResponseObject folderSyncResponse = runEndpoint(fse, document);
		Buffer content = folderSyncResponse.content;
		Document folderSync = WBXMLTools.toXml(content.getBytes());
		assertNotNull(folderSync);
		NodeList added = folderSync.getDocumentElement().getElementsByTagNameNS("FolderHierarchy", "Add");
		for (int i = 0; i < added.getLength(); i++) {
			Element el = (Element) added.item(i);
			String folderType = DOMUtils.getUniqueElement(el, "Type").getTextContent();
			String displayName = DOMUtils.getUniqueElement(el, "DisplayName").getTextContent();
			if ("13".equals(folderType) && calendarDescriptor.name.equals(displayName)) { // USER_CREATED_CALENDAR
				calendarId = Integer.parseInt(DOMUtils.getUniqueElement(el, "ServerId").getTextContent());
			}
		}
		assertNotNull(calendarId);

		// Create an event
		ICalendar cc = ClientSideServiceProvider.getProvider(testDevice.coreUrl, token.authKey)
				.instance(ICalendar.class, calendarUid);

		Calendar c = Calendar.getInstance();
		c.add(Calendar.SECOND, -3600);

		VEvent event = new VEvent();
		ZonedDateTime temp = ZonedDateTime.ofInstant(Instant.ofEpochMilli(c.getTime().getTime()),
				ZoneId.systemDefault());
		event.dtstart = BmDateTimeWrapper.create(temp, Precision.DateTime);
		event.summary = "Piscine";
		event.location = "Toulouse";
		event.description = "Lorem ipsum";
		event.transparency = VEvent.Transparency.Opaque;
		event.classification = VEvent.Classification.Private;
		event.status = VEvent.Status.Confirmed;
		event.priority = 3;

		event.organizer = new VEvent.Organizer(testDevice.owner.value.defaultEmail().address);

		List<VEvent.Attendee> attendees = new ArrayList<>(1);
		VEvent.Attendee me = VEvent.Attendee.create(VEvent.CUType.Individual, "", VEvent.Role.Chair,
				VEvent.ParticipationStatus.Accepted, true, "", "", "", calendarDescriptor.name, "", "",
				calendarDescriptor.uid, null);
		attendees.add(me);

		event.attendees = attendees;
		String eventUid = UUID.randomUUID().toString();
		cc.create(eventUid, VEventSeries.create(event), false);

		Document sync = DOMUtils.createDoc("AirSync", "Sync");
		root = sync.getDocumentElement();
		Element collections = DOMUtils.createElement(root, "Collections");
		Element collection = DOMUtils.createElement(collections, "Collection");
		DOMUtils.createElementAndText(collection, "SyncKey", "0");
		DOMUtils.createElementAndText(collection, "CollectionId", Integer.toString(calendarId));

		ResponseObject response = runEndpoint(sync);
		assertEquals(200, response.getStatusCode());
		content = response.content;
		Document resp = WBXMLTools.toXml(content.getBytes());
		String syncKey = syncKey(resp);
		assertNotNull(syncKey);
		String collectionId = DOMUtils.getUniqueElement(resp.getDocumentElement(), "CollectionId").getTextContent();
		assertEquals(calendarId + "", collectionId);
		String status = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Status").getTextContent();
		assertEquals("1", status);
		Element moreAvailable = DOMUtils.getUniqueElement(resp.getDocumentElement(), "MoreAvailable");
		assertNull(moreAvailable);
		Element commands = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Commands");
		assertNull(commands);

		// 2nd page (1 item)
		resp = runSyncEndpoint(calendarId, syncKey);
		syncKey = syncKey(resp);
		moreAvailable = DOMUtils.getUniqueElement(resp.getDocumentElement(), "MoreAvailable");
		assertNull(moreAvailable);
		commands = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Commands");
		assertNotNull(commands);
		assertEquals(1, commands.getChildNodes().getLength());
		Element item = (Element) commands.getChildNodes().item(0);
		assertEquals("Add", item.getNodeName());

		// next sync (0 item)
		resp = runSyncEndpoint(calendarId, syncKey);
		syncKey = syncKey(resp);
		commands = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Commands");
		assertNull(commands);

		// delete one event
		cc.delete(eventUid, false);
		assertNull(cc.getComplete(calendarUid));

		// bang
		resp = runSyncEndpoint(calendarId, syncKey);
		syncKey = syncKey(resp);
		commands = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Commands");
		assertEquals(1, commands.getChildNodes().getLength());
		item = (Element) commands.getChildNodes().item(0);
		assertEquals("Delete", item.getNodeName());
	}

	public void testAddressbookSync() throws Exception {
		Integer addressbookId = null;
		FolderSyncEndpoint fse = new FolderSyncEndpoint();
		Document document = DOMUtils.createDoc("FolderHierarchy", "FolderSync");
		Element root = document.getDocumentElement();
		Element sk = document.createElementNS("FolderHierarchy", "SyncKey");
		sk.setTextContent("0");
		root.appendChild(sk);
		ResponseObject folderSyncResponse = runEndpoint(fse, document);
		Buffer content = folderSyncResponse.content;
		Document folderSync = WBXMLTools.toXml(content.getBytes());
		assertNotNull(folderSync);
		NodeList added = folderSync.getDocumentElement().getElementsByTagNameNS("FolderHierarchy", "Add");
		for (int i = 0; i < added.getLength(); i++) {
			Element el = (Element) added.item(i);
			String folderType = DOMUtils.getUniqueElement(el, "Type").getTextContent();
			if ("9".equals(folderType)) { // DEFAULT_CONTACTS_FOLDER
				addressbookId = Integer.parseInt(DOMUtils.getUniqueElement(el, "ServerId").getTextContent());
			}
		}
		assertNotNull(addressbookId);

		// sk 0
		Document sync = DOMUtils.createDoc("AirSync", "Sync");
		root = sync.getDocumentElement();
		Element collections = DOMUtils.createElement(root, "Collections");
		Element collection = DOMUtils.createElement(collections, "Collection");
		DOMUtils.createElementAndText(collection, "SyncKey", "0");
		DOMUtils.createElementAndText(collection, "CollectionId", Integer.toString(addressbookId));

		ResponseObject response = runEndpoint(sync);
		assertEquals(200, response.getStatusCode());
		content = response.content;
		Document resp = WBXMLTools.toXml(content.getBytes());
		String syncKey = syncKey(resp);
		assertNotNull(syncKey);
		String collectionId = DOMUtils.getUniqueElement(resp.getDocumentElement(), "CollectionId").getTextContent();
		assertEquals(addressbookId + "", collectionId);
		String status = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Status").getTextContent();
		assertEquals("1", status);
		Element moreAvailable = DOMUtils.getUniqueElement(resp.getDocumentElement(), "MoreAvailable");
		assertNull(moreAvailable);
		Element commands = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Commands");
		assertNull(commands);

		IAuthentication authService = ClientSideServiceProvider.getProvider(testDevice.coreUrl, null)
				.instance(IAuthentication.class);
		LoginResponse token = authService.login(login, password, "sync-endpoint-addressbook-tests");
		IAddressBook bc = ClientSideServiceProvider.getProvider(testDevice.coreUrl, token.authKey)
				.instance(IAddressBook.class, "book:Contacts_" + testDevice.owner.uid);

		// Create one
		VCard card = new VCard();

		card.identification = new VCard.Identification();
		card.identification.name.familyNames = "Bang";
		card.identification.name.givenNames = "John";

		String uid = UUID.randomUUID().toString();
		bc.create(uid, card);

		// Remove one
		// Ensure we have at leat one deleted item
		VCard card2 = new VCard();

		card2.identification = new VCard.Identification();
		card2.identification.name.familyNames = "Bang";
		card2.identification.name.givenNames = "John";

		String uid2 = UUID.randomUUID().toString();
		bc.create(uid2, card2);
		bc.delete(uid2);

		boolean done = false;
		while (!done) {
			resp = runSyncEndpoint(addressbookId, syncKey);
			syncKey = syncKey(resp);
			moreAvailable = DOMUtils.getUniqueElement(resp.getDocumentElement(), "MoreAvailable");
			done = (moreAvailable == null);
			if (!done) {
				commands = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Commands");
				assertNotNull(commands);
				assertTrue(commands.getChildNodes().getLength() <= 10);
				for (int i = 0; i < commands.getChildNodes().getLength(); i++) {
					Element item = (Element) commands.getChildNodes().item(i);
					assertEquals("Add", item.getNodeName());
				}
			}
		}

		// next sync (0 item)
		resp = runSyncEndpoint(addressbookId, syncKey);
		syncKey = syncKey(resp);
		commands = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Commands");
		assertNull(commands);

		VCard card3 = new VCard();

		card3.identification = new VCard.Identification();
		card3.identification.name.familyNames = "Bang";
		card3.identification.name.givenNames = "John";

		String uid3 = UUID.randomUUID().toString();
		bc.create(uid3, card3);

		// Add new item
		resp = runSyncEndpoint(addressbookId, syncKey);
		syncKey = syncKey(resp);
		commands = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Commands");
		assertEquals(1, commands.getChildNodes().getLength());
		Element item = (Element) commands.getChildNodes().item(0);
		assertEquals("Add", item.getNodeName());

		// Remove item
		bc.delete(uid3);
		resp = runSyncEndpoint(addressbookId, syncKey);
		syncKey = syncKey(resp);
		commands = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Commands");
		assertEquals(1, commands.getChildNodes().getLength());
		item = (Element) commands.getChildNodes().item(0);
		assertEquals("Delete", item.getNodeName());

		// next sync (0 item)
		resp = runSyncEndpoint(addressbookId, syncKey);
		syncKey = syncKey(resp);
		commands = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Commands");
		assertNull(commands);
	}

	/**
	 * Mail is added on server, incremental sync should return it
	 * 
	 * @throws Exception
	 */
	public void testInboxSyncMailAdded() throws Exception {
		// SK 0
		String syncKey = initSyncChain();

		// next sync (0 item)
		Document resp = runSyncEndpoint(inboxServerId, syncKey);
		syncKey = syncKey(resp);
		Element commands = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Commands");
		assertNull(commands);

		// append emails
		appendEmail("email1");
		appendEmail("email2");

		resp = runSyncEndpoint(inboxServerId, syncKey);
		syncKey = syncKey(resp);
		commands = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Commands");
		assertEquals("2 new mails added by test in inbox, we expect that", 2, commands.getChildNodes().getLength());
		for (int i = 0; i < commands.getChildNodes().getLength(); i++) {
			Element item = (Element) commands.getChildNodes().item(i);
			assertEquals("Add", item.getNodeName());
			Element appDataElem = DOMUtils.getUniqueElement(item, "ApplicationData");
			assertNotNull("ApplicationData must be present for an added email", appDataElem);
		}

		// next sync (0 item)
		resp = runSyncEndpoint(inboxServerId, syncKey);
		syncKey = syncKey(resp);
		commands = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Commands");
		assertNull(commands);
	}

	/**
	 * Mail is flagged on server, incremental sync should return an update for it
	 * 
	 * @throws Exception
	 */
	public void testInboxSyncMailUpdated() throws Exception {
		// SK 0
		TestMail toFlag = appendEmail("email1");
		String syncKey = initSyncChain();

		// flag the mail
		FlagsList fl = new FlagsList();
		fl.add(Flag.FLAGGED);
		fl.add(Flag.FORWARDED);
		fl.add(Flag.SEEN);
		updateMessage(toFlag, fl);

		Document resp = runSyncEndpoint(inboxServerId, syncKey);
		syncKey = syncKey(resp);
		Element commands = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Commands");
		assertEquals("One server-side change was expected", 1, commands.getChildNodes().getLength());
		for (int i = 0; i < commands.getChildNodes().getLength(); i++) {
			Element item = (Element) commands.getChildNodes().item(i);
			assertEquals("Change", item.getNodeName());
			NodeList lastVerb = item.getElementsByTagName("LastVerbExecuted");
			assertEquals("One LastVerbExecuted is expected as the mail was marked as forwarded", 1,
					lastVerb.getLength());
		}

		// next sync (0 item)
		resp = runSyncEndpoint(inboxServerId, syncKey);
		syncKey = syncKey(resp);
		commands = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Commands");
		assertNull(commands);
	}

	public void testInboxSyncMailDeleted() throws Exception {
		// SK 0
		TestMail toFlag = appendEmail("email1");
		String syncKey = initSyncChain();

		// flag the mail
		FlagsList fl = new FlagsList();
		fl.add(Flag.DELETED);
		updateMessage(toFlag, fl);

		Document resp = runSyncEndpoint(inboxServerId, syncKey);
		syncKey = syncKey(resp);
		Element commands = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Commands");
		assertEquals("One server-side change was expected", 1, commands.getChildNodes().getLength());
		Element item = (Element) commands.getChildNodes().item(0);
		assertEquals("Delete", item.getNodeName());

		// next sync (0 item)
		resp = runSyncEndpoint(inboxServerId, syncKey);
		syncKey = syncKey(resp);
		commands = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Commands");
		assertNull(commands);
	}

	/**
	 * Mail is read by client through a Sync command
	 * 
	 * @throws Exception
	 */
	public void testInboxSyncUpdateMail() throws Exception {
		TestMail toRead = appendEmail("to_flag");
		String syncKey = initSyncChain();
		final String serverId = String.format("%d:%d", inboxServerId, toRead.uid);
		Document resp = runSyncEndpoint(inboxServerId, syncKey, new IClientChangeProvider() {

			@Override
			public void setClientChanges(int collectionId, Element colElem) {
				Element commands = DOMUtils.createElement(colElem, "Commands");
				Element change = DOMUtils.createElement(commands, "Change");
				DOMUtils.createElementAndText(change, "ServerId", serverId);
				Element appData = DOMUtils.createElement(change, "AirSync:ApplicationData");
				DOMUtils.createElementAndText(appData, "Email:Read", "1");
			}
		});
		syncKey = syncKey(resp);
		Element responses = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Responses");
		assertNull(responses);

		// next sync (0 item)
		resp = runSyncEndpoint(inboxServerId, syncKey);
		syncKey = syncKey(resp);
		Element commands = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Commands");
		assertNull(commands);
		responses = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Responses");
		assertNull(responses);
	}

	private String initSyncChain() throws IOException {
		Document sync = DOMUtils.createDoc("AirSync", "Sync");
		Element root = sync.getDocumentElement();
		Element collections = DOMUtils.createElement(root, "Collections");
		Element collection = DOMUtils.createElement(collections, "Collection");
		DOMUtils.createElementAndText(collection, "SyncKey", "0");
		DOMUtils.createElementAndText(collection, "CollectionId", Integer.toString(inboxServerId));
		ResponseObject response = runEndpoint(sync);
		assertEquals(200, response.getStatusCode());
		Buffer content = response.content;
		Document resp = WBXMLTools.toXml(content.getBytes());
		String syncKey = syncKey(resp);
		assertNotNull(syncKey);
		String collectionId = DOMUtils.getUniqueElement(resp.getDocumentElement(), "CollectionId").getTextContent();
		assertEquals(inboxServerId + "", collectionId);
		String status = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Status").getTextContent();
		assertEquals("1", status);
		Element moreAvailable = DOMUtils.getUniqueElement(resp.getDocumentElement(), "MoreAvailable");
		assertNull(moreAvailable);
		Element commands = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Commands");
		assertNull(commands);

		// FULL SYNC
		boolean done = false;
		while (!done) {
			resp = runSyncEndpoint(inboxServerId, syncKey);
			syncKey = syncKey(resp);
			moreAvailable = DOMUtils.getUniqueElement(resp.getDocumentElement(), "MoreAvailable");
			done = (moreAvailable == null);
			commands = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Commands");
			assertNotNull(commands);
			assertTrue(commands.getChildNodes().getLength() <= PAGE_SIZE);
			for (int i = 0; i < commands.getChildNodes().getLength(); i++) {
				Element item = (Element) commands.getChildNodes().item(i);
				assertEquals("Add", item.getNodeName());
			}
		}
		return syncKey;
	}

	public void testSyncZeroTruncationSize() throws Exception {
		appendEmail("email1");

		Document sync = DOMUtils.createDoc("AirSync", "Sync");
		Element root = sync.getDocumentElement();
		Element collections = DOMUtils.createElement(root, "Collections");
		Element collection = DOMUtils.createElement(collections, "Collection");
		DOMUtils.createElementAndText(collection, "SyncKey", "0");
		DOMUtils.createElementAndText(collection, "CollectionId", Integer.toString(inboxServerId));
		DOMUtils.createElementAndText(collection, "DeletesAsMoves", "1");
		DOMUtils.createElement(collection, "GetChanges");
		DOMUtils.createElementAndText(collection, "WindowSize", Integer.toString(PAGE_SIZE));
		Element options = DOMUtils.createElement(collection, "Options");
		DOMUtils.createElementAndText(options, "Class", "Email");
		DOMUtils.createElementAndText(options, "FilterType", "1");
		DOMUtils.createElementAndText(options, "MIMESupport", "1");
		Element bodyPreference = DOMUtils.createElement(options, "AirSyncBase:BodyPreference");
		DOMUtils.createElementAndText(bodyPreference, "Type", "2");
		DOMUtils.createElementAndText(bodyPreference, "TruncationSize", "0");

		ResponseObject response = runEndpoint(sync);
		assertEquals(200, response.getStatusCode());
		Buffer content = response.content;
		Document resp = WBXMLTools.toXml(content.getBytes());
		Element commands = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Commands");
		// 10 mails append in setUp
		assertEquals(PAGE_SIZE, commands.getChildNodes().getLength());
		for (int i = 0; i < commands.getChildNodes().getLength(); i++) {
			Element item = (Element) commands.getChildNodes().item(i);
			assertEquals("Add", item.getNodeName());

			// Expected body
			// <Body xmlns="AirSyncBase">
			// <Type>2</Type>
			// <EstimatedDataSize>1515</EstimatedDataSize>
			// <Truncated>1</Truncated>
			// </Body>

			Element body = DOMUtils.getUniqueElement(item, "Body");
			assertNotNull(body);

			assertNotNull(DOMUtils.getUniqueElement(body, "Type"));
			assertNotNull(DOMUtils.getUniqueElement(body, "EstimatedDataSize"));
			assertNotNull(DOMUtils.getUniqueElement(body, "Truncated"));
			assertNull(DOMUtils.getUniqueElement(body, "Data"));
		}
	}

	public void testTaskSync() throws Exception {
		IAuthentication authService = ClientSideServiceProvider.getProvider(testDevice.coreUrl, null)
				.instance(IAuthentication.class);
		LoginResponse token = authService.login("admin0@global.virt", Token.admin0(), "sync-endpoint-calendar-tests");
		IContainers ic = ClientSideServiceProvider.getProvider(testDevice.coreUrl, token.authKey)
				.instance(IContainers.class, domainUid);
		String todolistUid = UUID.randomUUID().toString();
		ContainerDescriptor todoListDescriptor = ContainerDescriptor.create(todolistUid, "truc à faire",
				SecurityContext.SYSTEM.getSubject(), "todolist", domainUid, true);
		ic.create(todolistUid, todoListDescriptor);

		IContainerManagement contactBookContainerManagement = ClientSideServiceProvider
				.getProvider(testDevice.coreUrl, token.authKey).instance(IContainerManagement.class, todolistUid);
		contactBookContainerManagement
				.setAccessControlList(Arrays.asList(AccessControlEntry.create(testDevice.owner.uid, Verb.Write)));

		IUserSubscription userSubService = ClientSideServiceProvider.getProvider(testDevice.coreUrl, token.authKey)
				.instance(IUserSubscription.class, domainUid);
		userSubService.subscribe(owner.uid, Arrays.asList(ContainerSubscription.create(todolistUid, false)));

		contactBookContainerManagement.allowOfflineSync(owner.uid);

		Integer todolistId = null;
		FolderSyncEndpoint fse = new FolderSyncEndpoint();
		Document document = DOMUtils.createDoc("FolderHierarchy", "FolderSync");
		Element root = document.getDocumentElement();
		Element sk = document.createElementNS("FolderHierarchy", "SyncKey");
		sk.setTextContent("0");
		root.appendChild(sk);
		ResponseObject folderSyncResponse = runEndpoint(fse, document);
		Buffer content = folderSyncResponse.content;
		Document folderSync = WBXMLTools.toXml(content.getBytes());
		assertNotNull(folderSync);
		NodeList added = folderSync.getDocumentElement().getElementsByTagNameNS("FolderHierarchy", "Add");
		for (int i = 0; i < added.getLength(); i++) {
			Element el = (Element) added.item(i);
			String folderType = DOMUtils.getUniqueElement(el, "Type").getTextContent();
			String displayName = DOMUtils.getUniqueElement(el, "DisplayName").getTextContent();
			if ("15".equals(folderType) && todoListDescriptor.name.equals(displayName)) { // USER_CREATED_TASKS_FOLDER
				todolistId = Integer.parseInt(DOMUtils.getUniqueElement(el, "ServerId").getTextContent());
			}
		}
		assertNotNull(todolistId);

		token = authService.login(testDevice.loginAtDomain, testDevice.password, "sync-endpoint-calendar-tests");
		ITodoList service = ClientSideServiceProvider.getProvider(testDevice.coreUrl, token.authKey)
				.instance(ITodoList.class, todolistUid);

		VTodo todo = defaultVTodo(9); // priority low
		service.create(UUID.randomUUID().toString(), todo);

		// sk 0
		Document sync = DOMUtils.createDoc("AirSync", "Sync");
		root = sync.getDocumentElement();
		Element collections = DOMUtils.createElement(root, "Collections");
		Element collection = DOMUtils.createElement(collections, "Collection");
		DOMUtils.createElementAndText(collection, "SyncKey", "0");
		DOMUtils.createElementAndText(collection, "CollectionId", Integer.toString(todolistId));

		ResponseObject response = runEndpoint(sync);
		assertEquals(200, response.getStatusCode());
		content = response.content;
		Document resp = WBXMLTools.toXml(content.getBytes());
		String syncKey = syncKey(resp);
		assertNotNull(syncKey);
		String collectionId = DOMUtils.getUniqueElement(resp.getDocumentElement(), "CollectionId").getTextContent();
		assertEquals(todolistId + "", collectionId);
		String status = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Status").getTextContent();
		assertEquals("1", status);
		Element moreAvailable = DOMUtils.getUniqueElement(resp.getDocumentElement(), "MoreAvailable");
		assertNull(moreAvailable);
		Element commands = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Commands");
		assertNull(commands);

		// 1st page (1 item)
		resp = runSyncEndpoint(todolistId, syncKey);
		syncKey = syncKey(resp);
		moreAvailable = DOMUtils.getUniqueElement(resp.getDocumentElement(), "MoreAvailable");
		assertNull(moreAvailable);
		commands = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Commands");
		assertNotNull(commands);
		assertEquals(1, commands.getChildNodes().getLength());

		Element item = (Element) commands.getChildNodes().item(0);
		assertEquals("Add", item.getNodeName());
		Element appData = DOMUtils.getUniqueElement(item, "ApplicationData");
		assertNotNull(appData);
		assertNotNull(DOMUtils.getUniqueElement(appData, "Subject"));
		assertEquals(todo.summary, DOMUtils.getUniqueElement(appData, "Subject").getTextContent());

		// priority low
		assertEquals("0", DOMUtils.getUniqueElement(appData, "Importance").getTextContent());

		assertEquals("1970-01-02T00:00:00.000Z", DOMUtils.getUniqueElement(appData, "StartDate").getTextContent());
		assertEquals("1970-01-04T00:00:00.000Z", DOMUtils.getUniqueElement(appData, "DueDate").getTextContent());

		assertNotNull(DOMUtils.getUniqueElement(appData, "Body"));

		// new todo
		todo = defaultVTodo(5); // priority normal
		service.create(UUID.randomUUID().toString(), todo);

		// new sync
		resp = runSyncEndpoint(todolistId, syncKey);
		syncKey = syncKey(resp);
		moreAvailable = DOMUtils.getUniqueElement(resp.getDocumentElement(), "MoreAvailable");
		assertNull(moreAvailable);
		commands = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Commands");
		assertNotNull(commands);
		assertEquals(1, commands.getChildNodes().getLength());

		item = (Element) commands.getChildNodes().item(0);
		assertEquals("Add", item.getNodeName());
		appData = DOMUtils.getUniqueElement(item, "ApplicationData");
		assertNotNull(appData);
		assertEquals(todo.summary, DOMUtils.getUniqueElement(appData, "Subject").getTextContent());

		// priority normal
		assertEquals("1", DOMUtils.getUniqueElement(appData, "Importance").getTextContent());

		// new todo
		todo = defaultVTodo(1); // priority high
		service.create(UUID.randomUUID().toString(), todo);

		// new sync
		resp = runSyncEndpoint(todolistId, syncKey);
		syncKey = syncKey(resp);
		moreAvailable = DOMUtils.getUniqueElement(resp.getDocumentElement(), "MoreAvailable");
		assertNull(moreAvailable);
		commands = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Commands");
		assertNotNull(commands);
		assertEquals(1, commands.getChildNodes().getLength());

		item = (Element) commands.getChildNodes().item(0);
		assertEquals("Add", item.getNodeName());
		appData = DOMUtils.getUniqueElement(item, "ApplicationData");
		assertNotNull(appData);
		assertEquals(todo.summary, DOMUtils.getUniqueElement(appData, "Subject").getTextContent());

		// priority normal
		assertEquals("2", DOMUtils.getUniqueElement(appData, "Importance").getTextContent());

	}

	private VTodo defaultVTodo(int priority) {
		VTodo todo = new VTodo();
		ZonedDateTime temp = ZonedDateTime.ofInstant(Instant.ofEpochMilli(0), ZoneId.of("UTC"));
		todo.dtstart = BmDateTimeWrapper.create(temp.plusDays(1), Precision.Date);
		todo.due = BmDateTimeWrapper.create(temp.plusDays(3), Precision.Date);
		todo.summary = "todo-" + System.currentTimeMillis();
		todo.location = "Toulouse";
		todo.description = "Lorem ipsum";
		todo.classification = VTodo.Classification.Private;
		todo.status = Status.NeedsAction;
		todo.priority = priority;

		todo.organizer = new VTodo.Organizer(testDevice.loginAtDomain);

		todo.categories = new ArrayList<TagRef>(0);
		return todo;
	}

	private static interface IClientChangeProvider {
		void setClientChanges(int collectionId, Element collectionElement);
	}

	private Document runSyncEndpoint(Integer collectionId, String syncKey) throws IOException {
		return runSyncEndpoint(collectionId, syncKey, new IClientChangeProvider() {

			@Override
			public void setClientChanges(int collectionId, Element collectionElement) {
			}
		});
	}

	private Document runSyncEndpoint(Integer collectionId, String syncKey, IClientChangeProvider clientChanges)
			throws IOException {
		Document sync = DOMUtils.createDoc("AirSync", "Sync");
		Element root = sync.getDocumentElement();
		Element collections = DOMUtils.createElement(root, "Collections");
		Element collection = DOMUtils.createElement(collections, "Collection");
		DOMUtils.createElementAndText(collection, "SyncKey", syncKey);
		DOMUtils.createElementAndText(collection, "CollectionId", Integer.toString(collectionId));
		DOMUtils.createElementAndText(collection, "DeletesAsMoves", "1");
		DOMUtils.createElement(collection, "GetChanges");
		DOMUtils.createElementAndText(collection, "WindowSize", Integer.toString(PAGE_SIZE));
		Element options = DOMUtils.createElement(collection, "Options");
		DOMUtils.createElementAndText(options, "FilterType", "3");
		Element bodyPreference = DOMUtils.createElement(options, "AirSyncBase:BodyPreference");
		DOMUtils.createElementAndText(bodyPreference, "Type", "2");
		DOMUtils.createElementAndText(bodyPreference, "TruncationSize", "200000");
		clientChanges.setClientChanges(collectionId, collection);

		ResponseObject response = runEndpoint(sync);
		assertEquals(200, response.getStatusCode());
		Buffer content = response.content;
		Document resp = WBXMLTools.toXml(content.getBytes());
		return resp;
	}

}
