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
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.vertx.java.core.buffer.Buffer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import net.bluemind.authentication.api.IAuthentication;
import net.bluemind.authentication.api.LoginResponse;
import net.bluemind.calendar.api.ICalendar;
import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.config.Token;
import net.bluemind.core.api.date.BmDateTime.Precision;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.ContainerSubscription;
import net.bluemind.core.container.api.IContainerManagement;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.http.ClientSideServiceProvider;
import net.bluemind.eas.command.folder.sync.FolderSyncEndpoint;
import net.bluemind.eas.command.sync.SyncEndpoint;
import net.bluemind.eas.http.wbxml.WbxmlHandlerBase;
import net.bluemind.eas.testhelper.mock.ResponseObject;
import net.bluemind.eas.utils.DOMUtils;
import net.bluemind.eas.wbxml.WBXMLTools;
import net.bluemind.user.api.IUserSubscription;

public class SyncEndpointCalendarTests extends AbstractEndpointTest {

	private int calendarId;
	private String syncKey;
	private ICalendar cc;

	private static final int WINDOW_SIZE = 25;

	public void setUp() throws Exception {
		super.setUp();

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
			if ("8".equals(folderType)) { // DEFAULT_CALENDAR_FOLDER
				calendarId = Integer.parseInt(DOMUtils.getUniqueElement(el, "ServerId").getTextContent());
			}
		}
		assertNotNull(calendarId);

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
		syncKey = DOMUtils.getUniqueElement(resp.getDocumentElement(), "SyncKey").getTextContent();
		assertNotNull(syncKey);

		// FULL SYNC first
		boolean done = false;
		while (!done) {
			resp = sync();
			syncKey = DOMUtils.getUniqueElement(resp.getDocumentElement(), "SyncKey").getTextContent();
			Element moreAvailable = DOMUtils.getUniqueElement(resp.getDocumentElement(), "MoreAvailable");
			done = (moreAvailable == null);
			if (!done) {
				Element commands = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Commands");
				assertNotNull(commands);
				assertTrue(commands.getChildNodes().getLength() <= WINDOW_SIZE);
				for (int i = 0; i < commands.getChildNodes().getLength(); i++) {
					Element item = (Element) commands.getChildNodes().item(i);
					assertEquals("Add", item.getNodeName());
				}
			}
		}

		IAuthentication authService = ClientSideServiceProvider.getProvider(testDevice.coreUrl, null)
				.instance(IAuthentication.class);
		LoginResponse token = authService.login(login, password, "sync-endpoint-calendar-tests");
		cc = ClientSideServiceProvider.getProvider(testDevice.coreUrl, token.authKey).instance(ICalendar.class,
				"calendar:Default:" + testDevice.owner.uid);

	}

	public void testCreateEvent() throws Exception {
		// next sync (0 item)
		Document resp = sync();
		syncKey = DOMUtils.getUniqueElement(resp.getDocumentElement(), "SyncKey").getTextContent();
		Element commands = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Commands");
		assertNull(commands);

		// new event
		String uid = UUID.randomUUID().toString();
		String clientId = System.currentTimeMillis() + "";
		String title = "brand new " + uid;
		Document sync = DOMUtils.createDoc("AirSync", "Sync");
		Element root = sync.getDocumentElement();
		Element collections = DOMUtils.createElement(root, "Collections");
		Element collection = DOMUtils.createElement(collections, "Collection");
		DOMUtils.createElementAndText(collection, "SyncKey", syncKey);
		DOMUtils.createElementAndText(collection, "CollectionId", Integer.toString(calendarId));
		DOMUtils.createElement(collection, "GetChanges");
		DOMUtils.createElementAndText(collection, "WindowSize", WINDOW_SIZE + "");

		Element options = DOMUtils.createElement(collection, "Options");
		DOMUtils.createElementAndText(options, "FilterType", "3");
		Element bodyPreference = DOMUtils.createElement(options, "AirSyncBase:BodyPreference");
		DOMUtils.createElementAndText(bodyPreference, "Type", "1");
		DOMUtils.createElementAndText(bodyPreference, "TruncationSize", "32768");

		commands = DOMUtils.createElement(collection, "Commands");
		Element add = DOMUtils.createElement(commands, "Add");
		DOMUtils.createElementAndText(add, "ClientId", clientId);

		Element appData = DOMUtils.createElement(add, "ApplicationData");

		Element body = DOMUtils.createElement(appData, "AirSyncBase:Body");
		DOMUtils.createElementAndText(body, "Type", "1");
		DOMUtils.createElementAndText(body, "Data", "desc !");

		DOMUtils.createElementAndText(appData, "Calendar:Timezone",
				"xP///wAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAoAAAAFAAMAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAMAAAAFAAIAAAAAAAAAxP///w==");
		DOMUtils.createElementAndText(appData, "Calendar:AllDayEvent", "0");
		DOMUtils.createElementAndText(appData, "Calendar:BusyStatus", "2");
		DOMUtils.createElementAndText(appData, "Calendar:DtStamp", "20151026T134937Z");
		DOMUtils.createElementAndText(appData, "Calendar:EndTime", "20151028T153000Z");
		DOMUtils.createElementAndText(appData, "Calendar:Location", "Toulouse");
		DOMUtils.createElementAndText(appData, "Calendar:Sensitivity", "0");
		DOMUtils.createElementAndText(appData, "Calendar:Subject", title);
		DOMUtils.createElementAndText(appData, "Calendar:StartTime", "20151028T143000Z");
		DOMUtils.createElementAndText(appData, "Calendar:UID", uid);
		DOMUtils.createElementAndText(appData, "Calendar:MeetingStatus", "0");

		Element rec = DOMUtils.createElement(appData, "Calendar:Recurrence");
		DOMUtils.createElementAndText(rec, "Type", "1");
		DOMUtils.createElementAndText(rec, "Interval", "1");
		DOMUtils.createElementAndText(rec, "DayOfWeek", "64");
		DOMUtils.createElementAndText(rec, "FirstDayOfWeek", "1");

		ResponseObject response = runEndpoint(sync);
		assertEquals(200, response.getStatusCode());
		Buffer content = response.content;

		// Expected
		// <?xml version="1.0" encoding="utf-8" ?>
		// <Sync xmlns="AirSync:">
		// <Collections>
		// <Collection>
		// <SyncKey>1657901952</SyncKey>
		// <CollectionId>1</CollectionId>
		// <Status>1</Status>
		// </Collection>
		// </Collections>
		// </Sync>

		resp = WBXMLTools.toXml(content.getBytes());
		syncKey = DOMUtils.getUniqueElement(resp.getDocumentElement(), "SyncKey").getTextContent();
		String collectionId = DOMUtils.getUniqueElement(resp.getDocumentElement(), "CollectionId").getTextContent();
		assertEquals(calendarId + "", collectionId);
		commands = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Commands");
		assertNull(commands);
		Element responses = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Responses");
		assertNull(responses);

		// next sync should be empty
		resp = sync();
		syncKey = DOMUtils.getUniqueElement(resp.getDocumentElement(), "SyncKey").getTextContent();
		commands = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Commands");
		assertNull(commands);
		responses = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Responses");
		assertNull(responses);
	}

	public void testDeleteEvent() throws Exception {
		String uid = createEvent();

		assertNotNull(cc.getComplete(uid));

		// next sync (1 item)
		Document resp = sync();
		syncKey = DOMUtils.getUniqueElement(resp.getDocumentElement(), "SyncKey").getTextContent();
		Element commands = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Commands");
		assertNotNull(commands);
		assertEquals(1, commands.getChildNodes().getLength());
		Element cmd = (Element) commands.getChildNodes().item(0);
		assertEquals("Add", cmd.getNodeName());
		String serverId = DOMUtils.getUniqueElement(resp.getDocumentElement(), "ServerId").getTextContent();
		assertNotNull(serverId);
		assertEquals(calendarId + ":" + uid, serverId);

		// delete
		Document sync = DOMUtils.createDoc("AirSync", "Sync");
		Element root = sync.getDocumentElement();
		Element collections = DOMUtils.createElement(root, "Collections");
		Element collection = DOMUtils.createElement(collections, "Collection");
		DOMUtils.createElementAndText(collection, "SyncKey", syncKey);
		DOMUtils.createElementAndText(collection, "CollectionId", Integer.toString(calendarId));
		DOMUtils.createElement(collection, "GetChanges");
		DOMUtils.createElementAndText(collection, "WindowSize", WINDOW_SIZE + "");

		Element options = DOMUtils.createElement(collection, "Options");
		DOMUtils.createElementAndText(options, "FilterType", "3");
		Element bodyPreference = DOMUtils.createElement(options, "AirSyncBase:BodyPreference");
		DOMUtils.createElementAndText(bodyPreference, "Type", "1");
		DOMUtils.createElementAndText(bodyPreference, "TruncationSize", "32768");

		commands = DOMUtils.createElement(collection, "Commands");
		Element delete = DOMUtils.createElement(commands, "Delete");
		DOMUtils.createElementAndText(delete, "ServerId", serverId);

		ResponseObject response = runEndpoint(sync);
		assertEquals(200, response.getStatusCode());
		Buffer content = response.content;
		resp = WBXMLTools.toXml(content.getBytes());
		syncKey = DOMUtils.getUniqueElement(resp.getDocumentElement(), "SyncKey").getTextContent();
		String collectionId = DOMUtils.getUniqueElement(resp.getDocumentElement(), "CollectionId").getTextContent();
		assertEquals(calendarId + "", collectionId);
		String status = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Status").getTextContent();
		assertEquals("1", status);

		assertNull(cc.getComplete(uid));
	}

	public void testUpdateEvent() throws Exception {
		String uid = createEvent();

		assertNotNull(cc.getComplete(uid));

		// next sync (1 item)
		Document resp = sync();
		syncKey = DOMUtils.getUniqueElement(resp.getDocumentElement(), "SyncKey").getTextContent();
		Element commands = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Commands");
		assertNotNull(commands);
		assertEquals(1, commands.getChildNodes().getLength());
		Element cmd = (Element) commands.getChildNodes().item(0);
		assertEquals("Add", cmd.getNodeName());
		String serverId = DOMUtils.getUniqueElement(resp.getDocumentElement(), "ServerId").getTextContent();
		assertNotNull(serverId);
		assertEquals(calendarId + ":" + uid, serverId);

		// update
		String title = "w00t w00t " + System.currentTimeMillis();
		Document sync = DOMUtils.createDoc("AirSync", "Sync");
		Element root = sync.getDocumentElement();
		Element collections = DOMUtils.createElement(root, "Collections");
		Element collection = DOMUtils.createElement(collections, "Collection");
		DOMUtils.createElementAndText(collection, "SyncKey", syncKey);
		DOMUtils.createElementAndText(collection, "CollectionId", Integer.toString(calendarId));
		DOMUtils.createElement(collection, "GetChanges");
		DOMUtils.createElementAndText(collection, "WindowSize", WINDOW_SIZE + "");

		Element options = DOMUtils.createElement(collection, "Options");
		DOMUtils.createElementAndText(options, "FilterType", "3");
		Element bodyPreference = DOMUtils.createElement(options, "AirSyncBase:BodyPreference");
		DOMUtils.createElementAndText(bodyPreference, "Type", "1");
		DOMUtils.createElementAndText(bodyPreference, "TruncationSize", "32768");

		commands = DOMUtils.createElement(collection, "Commands");
		Element change = DOMUtils.createElement(commands, "Change");
		DOMUtils.createElementAndText(change, "ServerId", serverId);

		Element appData = DOMUtils.createElement(change, "ApplicationData");
		DOMUtils.createElementAndText(appData, "Calendar:Timezone",
				"xP///wAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAoAAAAFAAMAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAMAAAAFAAIAAAAAAAAAxP///w==");
		DOMUtils.createElementAndText(appData, "Calendar:AllDayEvent", "0");
		DOMUtils.createElementAndText(appData, "Calendar:BusyStatus", "2");
		DOMUtils.createElementAndText(appData, "Calendar:DtStamp", "20151026T134937Z");
		DOMUtils.createElementAndText(appData, "Calendar:EndTime", "20151028T153000Z");
		DOMUtils.createElementAndText(appData, "Calendar:Location", "Toulouse");
		DOMUtils.createElementAndText(appData, "Calendar:Sensitivity", "0");
		DOMUtils.createElementAndText(appData, "Calendar:Subject", title);
		DOMUtils.createElementAndText(appData, "Calendar:StartTime", "20151028T143000Z");
		DOMUtils.createElementAndText(appData, "Calendar:UID", uid);
		DOMUtils.createElementAndText(appData, "Calendar:MeetingStatus", "0");

		ResponseObject response = runEndpoint(sync);
		assertEquals(200, response.getStatusCode());
		Buffer content = response.content;
		resp = WBXMLTools.toXml(content.getBytes());
		syncKey = DOMUtils.getUniqueElement(resp.getDocumentElement(), "SyncKey").getTextContent();
		String collectionId = DOMUtils.getUniqueElement(resp.getDocumentElement(), "CollectionId").getTextContent();
		assertEquals(calendarId + "", collectionId);
		String status = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Status").getTextContent();
		assertEquals("1", status);

		ItemValue<VEventSeries> e = cc.getComplete(uid);
		assertNotNull(e);
		assertEquals(title, e.value.main.summary);
	}

	public void testDeleteMultiCal() throws Exception {

		IAuthentication authService = ClientSideServiceProvider.getProvider(testDevice.coreUrl, null)
				.instance(IAuthentication.class);
		LoginResponse token = authService.login("admin0@global.virt", Token.admin0(), "sync-endpoint-calendar-tests");
		IContainers ic = ClientSideServiceProvider.getProvider(testDevice.coreUrl, token.authKey)
				.instance(IContainers.class, domainUid);
		String uid = UUID.randomUUID().toString();
		ContainerDescriptor calendarDescriptor = ContainerDescriptor.create(uid, "vacances",
				SecurityContext.SYSTEM.getSubject(), "calendar", domainUid, true);
		ic.create(uid, calendarDescriptor);

		IContainerManagement contactBookContainerManagement = ClientSideServiceProvider
				.getProvider(testDevice.coreUrl, token.authKey).instance(IContainerManagement.class, uid);
		contactBookContainerManagement
				.setAccessControlList(Arrays.asList(AccessControlEntry.create(testDevice.owner.uid, Verb.Write)));

		IUserSubscription userSubService = ClientSideServiceProvider.getProvider(testDevice.coreUrl, token.authKey)
				.instance(IUserSubscription.class, domainUid);
		userSubService.subscribe(owner.uid, Arrays.asList(ContainerSubscription.create(uid, false)));

		contactBookContainerManagement.allowOfflineSync(owner.uid);

		// Sync
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

		// Create an event into public calendar
		VEvent event = new VEvent();
		event.dtstart = BmDateTimeWrapper.create(ZonedDateTime.now(), Precision.DateTime);
		event.summary = "Piscine";
		event.location = "Toulouse";
		event.description = "Lorem ipsum";
		event.transparency = VEvent.Transparency.Opaque;
		event.classification = VEvent.Classification.Private;
		event.status = VEvent.Status.Confirmed;
		event.priority = 3;

		event.organizer = new VEvent.Organizer(testDevice.owner.value.defaultEmail().address);

		List<VEvent.Attendee> atts = new ArrayList<>(1);
		VEvent.Attendee me = VEvent.Attendee.create(VEvent.CUType.Individual, "", VEvent.Role.Chair,
				VEvent.ParticipationStatus.Accepted, true, "", "", "", calendarDescriptor.name, "", "", uid, "");
		atts.add(me);

		event.attendees = atts;
		String eventUid = UUID.randomUUID().toString();

		ICalendar cli = ClientSideServiceProvider.getProvider(testDevice.coreUrl, token.authKey)
				.instance(ICalendar.class, uid);
		cli.create(eventUid, VEventSeries.create(event), false);

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
		syncKey = DOMUtils.getUniqueElement(resp.getDocumentElement(), "SyncKey").getTextContent();
		assertNotNull(syncKey);
		String collectionId = DOMUtils.getUniqueElement(resp.getDocumentElement(), "CollectionId").getTextContent();
		assertEquals(calendarId + "", collectionId);
		String status = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Status").getTextContent();
		assertEquals("1", status);
		Element moreAvailable = DOMUtils.getUniqueElement(resp.getDocumentElement(), "MoreAvailable");
		assertNull(moreAvailable);
		Element commands = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Commands");
		assertNull(commands);

		// 1st page (1 item)
		resp = sync(calendarId);
		syncKey = DOMUtils.getUniqueElement(resp.getDocumentElement(), "SyncKey").getTextContent();
		moreAvailable = DOMUtils.getUniqueElement(resp.getDocumentElement(), "MoreAvailable");
		assertNull(moreAvailable);
		commands = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Commands");
		assertNotNull(commands);
		assertEquals(1, commands.getChildNodes().getLength());
		Element item = (Element) commands.getChildNodes().item(0);
		assertEquals("Add", item.getNodeName());
		Element appData = DOMUtils.getUniqueElement(item, "ApplicationData");
		assertNotNull(appData);

		// 1st page (1 item)
		resp = sync(calendarId);
		syncKey = DOMUtils.getUniqueElement(resp.getDocumentElement(), "SyncKey").getTextContent();
		moreAvailable = DOMUtils.getUniqueElement(resp.getDocumentElement(), "MoreAvailable");
		assertNull(moreAvailable);
		commands = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Commands");
		assertNull(commands);
		Element responses = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Responses");
		assertNull(responses);

		// Delete event
		sync = DOMUtils.createDoc("AirSync", "Sync");
		root = sync.getDocumentElement();
		collections = DOMUtils.createElement(root, "Collections");
		collection = DOMUtils.createElement(collections, "Collection");
		DOMUtils.createElementAndText(collection, "SyncKey", syncKey);
		DOMUtils.createElementAndText(collection, "CollectionId", Integer.toString(calendarId));
		DOMUtils.createElement(collection, "GetChanges");
		DOMUtils.createElementAndText(collection, "WindowSize", WINDOW_SIZE + "");
		Element options = DOMUtils.createElement(collection, "Options");
		DOMUtils.createElementAndText(options, "FilterType", "3");
		Element bodyPreference = DOMUtils.createElement(options, "AirSyncBase:BodyPreference");
		DOMUtils.createElementAndText(bodyPreference, "Type", "2");
		DOMUtils.createElementAndText(bodyPreference, "TruncationSize", "200000");
		commands = DOMUtils.createElement(collection, "Commands");
		Element delete = DOMUtils.createElement(commands, "Delete");
		DOMUtils.createElementAndText(delete, "ServerId", calendarId + ":" + eventUid);

		// bang
		// test event is re-add in Commands
		ResponseObject ro = runEndpoint(sync);
		assertEquals(200, ro.getStatusCode());
		resp = WBXMLTools.toXml(ro.content.getBytes());
		commands = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Commands");
		assertNotNull(commands);
		assertEquals(1, commands.getChildNodes().getLength());
		item = (Element) commands.getChildNodes().item(0);
		assertEquals("Add", item.getNodeName());

		responses = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Responses");
		assertNull(responses);
	}

	public void testUpdateMultical() throws Exception {
		IAuthentication authService = ClientSideServiceProvider.getProvider(testDevice.coreUrl, null)
				.instance(IAuthentication.class);
		LoginResponse token = authService.login("admin0@global.virt", Token.admin0(), "sync-endpoint-calendar-tests");
		IContainers ic = ClientSideServiceProvider.getProvider(testDevice.coreUrl, token.authKey)
				.instance(IContainers.class, domainUid);
		String uid = UUID.randomUUID().toString();
		ContainerDescriptor calendarDescriptor = ContainerDescriptor.create(uid, "vacances",
				SecurityContext.SYSTEM.getSubject(), "calendar", domainUid, true);
		ic.create(uid, calendarDescriptor);

		IContainerManagement contactBookContainerManagement = ClientSideServiceProvider
				.getProvider(testDevice.coreUrl, token.authKey).instance(IContainerManagement.class, uid);
		contactBookContainerManagement
				.setAccessControlList(Arrays.asList(AccessControlEntry.create(testDevice.owner.uid, Verb.Read)));

		IUserSubscription userSubService = ClientSideServiceProvider.getProvider(testDevice.coreUrl, token.authKey)
				.instance(IUserSubscription.class, domainUid);
		userSubService.subscribe(owner.uid, Arrays.asList(ContainerSubscription.create(uid, false)));

		contactBookContainerManagement.allowOfflineSync(owner.uid);

		// Sync
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

		// Create an event into public calendar
		VEvent event = new VEvent();
		event.dtstart = BmDateTimeWrapper.create(ZonedDateTime.now(), Precision.DateTime);
		event.summary = "Piscine";
		event.location = "Toulouse";
		event.description = "Lorem ipsum";
		event.transparency = VEvent.Transparency.Opaque;
		event.classification = VEvent.Classification.Public;
		event.status = VEvent.Status.Confirmed;
		event.priority = 3;

		event.organizer = new VEvent.Organizer(testDevice.owner.value.defaultEmail().address);

		List<VEvent.Attendee> atts = new ArrayList<>(1);
		VEvent.Attendee me = VEvent.Attendee.create(VEvent.CUType.Individual, "", VEvent.Role.Chair,
				VEvent.ParticipationStatus.Accepted, true, "", "", "", calendarDescriptor.name, "", "", uid, "");
		atts.add(me);

		event.attendees = atts;
		String eventUid = UUID.randomUUID().toString();
		ICalendar cli = ClientSideServiceProvider.getProvider(testDevice.coreUrl, token.authKey)
				.instance(ICalendar.class, uid);
		cli.create(eventUid, VEventSeries.create(event), false);

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
		syncKey = DOMUtils.getUniqueElement(resp.getDocumentElement(), "SyncKey").getTextContent();
		assertNotNull(syncKey);
		String collectionId = DOMUtils.getUniqueElement(resp.getDocumentElement(), "CollectionId").getTextContent();
		assertEquals(calendarId + "", collectionId);
		String status = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Status").getTextContent();
		assertEquals("1", status);
		Element moreAvailable = DOMUtils.getUniqueElement(resp.getDocumentElement(), "MoreAvailable");
		assertNull(moreAvailable);
		Element commands = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Commands");
		assertNull(commands);

		// 1st page (1 item)
		resp = sync(calendarId);
		syncKey = DOMUtils.getUniqueElement(resp.getDocumentElement(), "SyncKey").getTextContent();
		moreAvailable = DOMUtils.getUniqueElement(resp.getDocumentElement(), "MoreAvailable");
		assertNull(moreAvailable);
		commands = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Commands");
		assertNotNull(commands);
		assertEquals(1, commands.getChildNodes().getLength());
		Element item = (Element) commands.getChildNodes().item(0);
		assertEquals("Add", item.getNodeName());
		Element appData = DOMUtils.getUniqueElement(item, "ApplicationData");
		assertNotNull(appData);

		// 1st page (1 item)
		resp = sync(calendarId);
		syncKey = DOMUtils.getUniqueElement(resp.getDocumentElement(), "SyncKey").getTextContent();
		moreAvailable = DOMUtils.getUniqueElement(resp.getDocumentElement(), "MoreAvailable");
		assertNull(moreAvailable);
		commands = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Commands");
		assertNull(commands);
		Element responses = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Responses");
		assertNull(responses);

		// Update event
		sync = DOMUtils.createDoc("AirSync", "Sync");
		root = sync.getDocumentElement();
		collections = DOMUtils.createElement(root, "Collections");
		collection = DOMUtils.createElement(collections, "Collection");
		DOMUtils.createElementAndText(collection, "SyncKey", syncKey);
		DOMUtils.createElementAndText(collection, "CollectionId", Integer.toString(calendarId));
		DOMUtils.createElement(collection, "GetChanges");
		DOMUtils.createElementAndText(collection, "WindowSize", WINDOW_SIZE + "");
		Element options = DOMUtils.createElement(collection, "Options");
		DOMUtils.createElementAndText(options, "FilterType", "3");
		Element bodyPreference = DOMUtils.createElement(options, "AirSyncBase:BodyPreference");
		DOMUtils.createElementAndText(bodyPreference, "Type", "2");
		DOMUtils.createElementAndText(bodyPreference, "TruncationSize", "200000");
		commands = DOMUtils.createElement(collection, "Commands");
		Element change = DOMUtils.createElement(commands, "Change");
		DOMUtils.createElementAndText(change, "ServerId", calendarId + ":" + eventUid);
		appData = DOMUtils.createElement(change, "ApplicationData");
		DOMUtils.createElementAndText(appData, "Calendar:Timezone",
				"xP///wAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAoAAAAFAAMAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAMAAAAFAAIAAAAAAAAAxP///w==");
		DOMUtils.createElementAndText(appData, "Calendar:AllDayEvent", "0");
		DOMUtils.createElementAndText(appData, "Calendar:DtStamp", "20151116T161732Z");
		DOMUtils.createElementAndText(appData, "Calendar:EndTime", "20151106T143000Z");
		DOMUtils.createElementAndText(appData, "Calendar:Sensitivity", "0");
		DOMUtils.createElementAndText(appData, "Calendar:Subject", "updated subject");
		DOMUtils.createElementAndText(appData, "Calendar:StartTime", "20151106T103000Z");
		DOMUtils.createElementAndText(appData, "Calendar:MeetingStatus", "1");
		Element attendees = DOMUtils.createElement(appData, "Calendar:Attendees");
		Element attendee = DOMUtils.createElement(attendees, "Calendar:Attendee");
		DOMUtils.createElementAndText(attendee, "Name", calendarDescriptor.name);
		DOMUtils.createElementAndText(attendee, "Email", ""); // calendar
																// email??

		// bang
		// test event is re-sync with original data
		ResponseObject ro = runEndpoint(sync);
		assertEquals(200, ro.getStatusCode());
		resp = WBXMLTools.toXml(ro.content.getBytes());
		commands = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Commands");
		assertNotNull(commands);
		assertEquals(1, commands.getChildNodes().getLength());
		item = (Element) commands.getChildNodes().item(0);
		assertEquals("Change", item.getNodeName());
		appData = DOMUtils.getUniqueElement(item, "ApplicationData");
		assertNotNull(appData);

		Element subject = DOMUtils.getUniqueElement(item, "Subject");
		assertNotNull(subject);
		assertEquals(event.summary, subject.getTextContent());

		responses = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Responses");
		assertNull(responses);
	}

	public void testCreateEventWithoutDates() throws Exception {
		// next sync (0 item)
		Document resp = sync();
		syncKey = DOMUtils.getUniqueElement(resp.getDocumentElement(), "SyncKey").getTextContent();
		Element commands = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Commands");
		assertNull(commands);

		// new event
		String uid = UUID.randomUUID().toString();
		String clientId = System.currentTimeMillis() + "";
		String title = "brand new " + uid;
		Document sync = DOMUtils.createDoc("AirSync", "Sync");
		Element root = sync.getDocumentElement();
		Element collections = DOMUtils.createElement(root, "Collections");
		Element collection = DOMUtils.createElement(collections, "Collection");
		DOMUtils.createElementAndText(collection, "SyncKey", syncKey);
		DOMUtils.createElementAndText(collection, "CollectionId", Integer.toString(calendarId));
		DOMUtils.createElement(collection, "GetChanges");
		DOMUtils.createElementAndText(collection, "WindowSize", WINDOW_SIZE + "");

		Element options = DOMUtils.createElement(collection, "Options");
		DOMUtils.createElementAndText(options, "FilterType", "3");
		Element bodyPreference = DOMUtils.createElement(options, "AirSyncBase:BodyPreference");
		DOMUtils.createElementAndText(bodyPreference, "Type", "1");
		DOMUtils.createElementAndText(bodyPreference, "TruncationSize", "32768");

		commands = DOMUtils.createElement(collection, "Commands");
		Element add = DOMUtils.createElement(commands, "Add");
		DOMUtils.createElementAndText(add, "ClientId", clientId);

		Element appData = DOMUtils.createElement(add, "ApplicationData");

		Element body = DOMUtils.createElement(appData, "AirSyncBase:Body");
		DOMUtils.createElementAndText(body, "Type", "1");
		DOMUtils.createElementAndText(body, "Data", "desc !");

		DOMUtils.createElementAndText(appData, "Calendar:Timezone",
				"xP///wAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAoAAAAFAAMAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAMAAAAFAAIAAAAAAAAAxP///w==");
		DOMUtils.createElementAndText(appData, "Calendar:AllDayEvent", "0");
		DOMUtils.createElementAndText(appData, "Calendar:BusyStatus", "2");
		// DOMUtils.createElementAndText(appData, "Calendar:DtStamp",
		// "20151026T134937Z");
		// DOMUtils.createElementAndText(appData, "Calendar:EndTime",
		// "20151028T153000Z");
		DOMUtils.createElementAndText(appData, "Calendar:Location", "Toulouse");
		DOMUtils.createElementAndText(appData, "Calendar:Sensitivity", "0");
		DOMUtils.createElementAndText(appData, "Calendar:Subject", title);
		// DOMUtils.createElementAndText(appData, "Calendar:StartTime",
		// "20151028T143000Z");
		DOMUtils.createElementAndText(appData, "Calendar:UID", uid);
		DOMUtils.createElementAndText(appData, "Calendar:MeetingStatus", "0");

		Element rec = DOMUtils.createElement(appData, "Calendar:Recurrence");
		DOMUtils.createElementAndText(rec, "Type", "1");
		DOMUtils.createElementAndText(rec, "Interval", "1");
		DOMUtils.createElementAndText(rec, "DayOfWeek", "64");
		DOMUtils.createElementAndText(rec, "FirstDayOfWeek", "1");

		ResponseObject response = runEndpoint(sync);
		assertEquals(200, response.getStatusCode());
		Buffer content = response.content;

		// Expected
		// <?xml version="1.0" encoding="utf-8" ?>
		// <Sync xmlns="AirSync:">
		// <Collections>
		// <Collection>
		// <SyncKey>1657901952</SyncKey>
		// <CollectionId>1</CollectionId>
		// <Status>1</Status>
		// </Collection>
		// </Collections>
		// </Sync>

		resp = WBXMLTools.toXml(content.getBytes());
		syncKey = DOMUtils.getUniqueElement(resp.getDocumentElement(), "SyncKey").getTextContent();
		String collectionId = DOMUtils.getUniqueElement(resp.getDocumentElement(), "CollectionId").getTextContent();
		assertEquals(calendarId + "", collectionId);
		commands = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Commands");
		assertNull(commands);
		Element responses = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Responses");

		// next sync should be empty
		resp = sync();
		syncKey = DOMUtils.getUniqueElement(resp.getDocumentElement(), "SyncKey").getTextContent();
		commands = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Commands");
		assertNull(commands);
		responses = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Responses");
		assertNull(responses);
	}

	private Document sync(Integer collectionId) throws IOException {
		return runSyncEndpoint(collectionId, new IClientChangeProvider() {

			@Override
			public void setClientChanges(int collectionId, Element collectionElement) {
			}
		});
	}

	private static interface IClientChangeProvider {
		void setClientChanges(int collectionId, Element collectionElement);
	}

	private Document sync() throws IOException {
		return runSyncEndpoint(calendarId, new IClientChangeProvider() {

			@Override
			public void setClientChanges(int collectionId, Element collectionElement) {
			}
		});
	}

	private Document runSyncEndpoint(int collectionId, IClientChangeProvider clientChanges) throws IOException {
		Document sync = DOMUtils.createDoc("AirSync", "Sync");
		Element root = sync.getDocumentElement();
		Element collections = DOMUtils.createElement(root, "Collections");
		Element collection = DOMUtils.createElement(collections, "Collection");
		DOMUtils.createElementAndText(collection, "SyncKey", syncKey);
		DOMUtils.createElementAndText(collection, "CollectionId", Integer.toString(collectionId));
		DOMUtils.createElement(collection, "GetChanges");
		DOMUtils.createElementAndText(collection, "WindowSize", WINDOW_SIZE + "");
		Element options = DOMUtils.createElement(collection, "Options");
		DOMUtils.createElementAndText(options, "FilterType", "3");
		Element bodyPreference = DOMUtils.createElement(options, "AirSyncBase:BodyPreference");
		DOMUtils.createElementAndText(bodyPreference, "Type", "2");
		DOMUtils.createElementAndText(bodyPreference, "TruncationSize", "200000");
		clientChanges.setClientChanges(calendarId, collection);
		ResponseObject response = runEndpoint(sync);
		assertEquals(200, response.getStatusCode());
		Buffer content = response.content;
		Document resp = WBXMLTools.toXml(content.getBytes());
		return resp;
	}

	@Override
	public WbxmlHandlerBase createEndpoint() {
		return new SyncEndpoint();
	}

	private String createEvent() throws ServerFault {
		VEvent event = new VEvent();
		event.dtstart = BmDateTimeWrapper.create(ZonedDateTime.now(), Precision.DateTime);
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
				VEvent.ParticipationStatus.Accepted, true, "", "", "",
				testDevice.owner.value.contactInfos.identification.formatedName.value, "", "", null,
				testDevice.owner.value.defaultEmail().address + "@bm.lan");
		attendees.add(me);

		event.attendees = attendees;
		String uid = UUID.randomUUID().toString();
		cc.create(uid, VEventSeries.create(event), false);

		return uid;
	}
}
