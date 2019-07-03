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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.joda.time.DateTime;
import org.vertx.java.core.buffer.Buffer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import net.bluemind.authentication.api.IAuthentication;
import net.bluemind.authentication.api.LoginResponse;
import net.bluemind.calendar.api.ICalendar;
import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.core.api.date.BmDateTime.Precision;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.http.ClientSideServiceProvider;
import net.bluemind.eas.command.folder.sync.FolderSyncEndpoint;
import net.bluemind.eas.command.sync.SyncEndpoint;
import net.bluemind.eas.dto.email.MessageClass;
import net.bluemind.eas.endpoint.tests.bodyoptions.ISyncOptionsProvider;
import net.bluemind.eas.http.wbxml.WbxmlHandlerBase;
import net.bluemind.eas.testhelper.mock.ResponseObject;
import net.bluemind.eas.utils.DOMUtils;
import net.bluemind.eas.wbxml.WBXMLTools;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.MQ.IMQConnectHandler;
import net.bluemind.icalendar.api.ICalendarElement.RRule;
import net.bluemind.icalendar.api.ICalendarElement.RRule.Frequency;
import net.bluemind.imap.FlagsList;

public class SyncEndpointMeetingRequestTests extends AbstractEndpointTest {

	private int inboxServerId;

	public void setUp() throws Exception {
		super.setUp();
		fetchFolderIds();
		final CountDownLatch mqLatch = new CountDownLatch(1);
		MQ.init(new IMQConnectHandler() {

			@Override
			public void connected() {
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
		assertTrue(inboxServerId > 0);
		System.out.println("Inbox has serverId:" + inboxServerId);
	}

	public void tearDown() throws Exception {
		super.tearDown();
	}

	public void testMeetingRequest() throws Exception {

		createEvent("c71ba9e6307521d92a6c2bc5ee0441bf897165cf", false);

		String syncKey = initSyncChain();

		appendEml("INBOX", "data/Sync/invitation.eml", new FlagsList());

		Document resp = runSyncEndpoint(inboxServerId, syncKey);
		syncKey = syncKey(resp);
		Element commands = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Commands");
		assertNotNull(commands);
		assertEquals(1, commands.getChildNodes().getLength());
		Element item = (Element) commands.getChildNodes().item(0);
		assertEquals("Add", item.getNodeName());

		assertEquals(MessageClass.ScheduleMeetingRequest.toString(14.1),
				DOMUtils.getUniqueElement(item, "MessageClass").getTextContent());
		assertEquals("urn:content-classes:calendarmessage",
				DOMUtils.getUniqueElement(item, "ContentClass").getTextContent());

		Element mr = DOMUtils.getUniqueElement(item, "MeetingRequest");
		assertNotNull(mr);
		assertNotNull(DOMUtils.getUniqueElement(mr, "AllDayEvent"));
		assertNotNull(DOMUtils.getUniqueElement(mr, "StartTime"));
		assertNotNull(DOMUtils.getUniqueElement(mr, "DtStamp"));
		assertNotNull(DOMUtils.getUniqueElement(mr, "EndTime"));
		assertNotNull(DOMUtils.getUniqueElement(mr, "InstanceType"));
		assertNotNull(DOMUtils.getUniqueElement(mr, "ResponseRequested"));
		assertNotNull(DOMUtils.getUniqueElement(mr, "Sensitivity"));
		assertNotNull(DOMUtils.getUniqueElement(mr, "TimeZone"));
		assertNotNull(DOMUtils.getUniqueElement(mr, "GlobalObjId"));
		assertNotNull(DOMUtils.getUniqueElement(mr, "DisallowNewTimeProposal"));
	}

	public void testRecurringMeetingRequest() throws Exception {

		createEvent("a0746e3224bbc66f386084eb7d854c75a9a99d4f", true);

		String syncKey = initSyncChain();
		appendEml("INBOX", "data/Sync/invitation_recurrence.eml", new FlagsList());
		Document resp = runSyncEndpoint(inboxServerId, syncKey);
		syncKey = syncKey(resp);
		Element commands = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Commands");
		assertNotNull(commands);
		assertEquals(1, commands.getChildNodes().getLength());
		Element item = (Element) commands.getChildNodes().item(0);
		assertEquals("Add", item.getNodeName());

		assertEquals(MessageClass.ScheduleMeetingRequest.toString(14.1),
				DOMUtils.getUniqueElement(item, "MessageClass").getTextContent());
		assertEquals("urn:content-classes:calendarmessage",
				DOMUtils.getUniqueElement(item, "ContentClass").getTextContent());

		Element mr = DOMUtils.getUniqueElement(item, "MeetingRequest");
		assertNotNull(mr);
		assertNotNull(DOMUtils.getUniqueElement(mr, "AllDayEvent"));
		assertNotNull(DOMUtils.getUniqueElement(mr, "StartTime"));
		assertNotNull(DOMUtils.getUniqueElement(mr, "DtStamp"));
		assertNotNull(DOMUtils.getUniqueElement(mr, "EndTime"));
		assertNotNull(DOMUtils.getUniqueElement(mr, "InstanceType"));
		assertNotNull(DOMUtils.getUniqueElement(mr, "ResponseRequested"));

		Element recurrences = DOMUtils.getUniqueElement(mr, "Recurrences");
		assertNotNull(recurrences);
		Element recurrence = DOMUtils.getUniqueElement(recurrences, "Recurrence");
		assertNotNull(recurrence);
		assertNotNull(DOMUtils.getUniqueElement(recurrence, "Type"));
		assertNotNull(DOMUtils.getUniqueElement(recurrence, "Interval"));
		assertNotNull(DOMUtils.getUniqueElement(recurrence, "DayOfWeek"));

		assertNotNull(DOMUtils.getUniqueElement(mr, "Sensitivity"));
		assertNotNull(DOMUtils.getUniqueElement(mr, "TimeZone"));
		assertNotNull(DOMUtils.getUniqueElement(mr, "GlobalObjId"));
		assertNotNull(DOMUtils.getUniqueElement(mr, "DisallowNewTimeProposal"));
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
		}
		return syncKey;
	}

	private String syncKey(Document doc) {
		return DOMUtils.getUniqueElement(doc.getDocumentElement(), "SyncKey").getTextContent();
	}

	private static interface IClientChangeProvider {
		void setClientChanges(int collectionId, Element collectionElement);
	}

	private Document runSyncEndpoint(Integer collectionId, String syncKey) throws IOException {
		return runSyncEndpoint(collectionId, syncKey, new IClientChangeProvider() {

			@Override
			public void setClientChanges(int collectionId, Element collectionElement) {
			}
		}, new ISyncOptionsProvider() {

			@Override
			public void setSyncOptions(Element options) {
				DOMUtils.createElementAndText(options, "FilterType", "3");
				Element bodyPreference = DOMUtils.createElement(options, "AirSyncBase:BodyPreference");
				DOMUtils.createElementAndText(bodyPreference, "Type", "2");
				DOMUtils.createElementAndText(bodyPreference, "TruncationSize", "200000");
			}
		});
	}

	private Document runSyncEndpoint(Integer collectionId, String syncKey, IClientChangeProvider clientChanges,
			ISyncOptionsProvider optionsProv) throws IOException {
		Document sync = DOMUtils.createDoc("AirSync", "Sync");
		Element root = sync.getDocumentElement();
		Element collections = DOMUtils.createElement(root, "Collections");
		Element collection = DOMUtils.createElement(collections, "Collection");
		DOMUtils.createElementAndText(collection, "SyncKey", syncKey);
		DOMUtils.createElementAndText(collection, "CollectionId", Integer.toString(collectionId));
		DOMUtils.createElementAndText(collection, "DeletesAsMoves", "1");
		DOMUtils.createElement(collection, "GetChanges");
		DOMUtils.createElementAndText(collection, "WindowSize", "256");
		Element options = DOMUtils.createElement(collection, "Options");
		optionsProv.setSyncOptions(options);
		clientChanges.setClientChanges(collectionId, collection);

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

	private void createEvent(String uid, boolean rrule) throws ServerFault {
		VEvent event = new VEvent();
		event.dtstart = BmDateTimeWrapper.create(new DateTime(), Precision.DateTime);
		event.summary = "Piscine";
		event.location = "Toulouse";
		event.description = "Lorem ipsum";
		event.transparency = VEvent.Transparency.Opaque;
		event.classification = VEvent.Classification.Private;
		event.status = VEvent.Status.Confirmed;
		event.priority = 3;

		if (rrule) {
			event.rrule = new RRule();
			event.rrule.frequency = Frequency.WEEKLY;
			event.rrule.count = 10;
			event.rrule.interval = 13;
			List<VEvent.RRule.WeekDay> weekDay = new ArrayList<VEvent.RRule.WeekDay>(4);
			weekDay.add(VEvent.RRule.WeekDay.MO);
			weekDay.add(VEvent.RRule.WeekDay.TU);
			weekDay.add(VEvent.RRule.WeekDay.TH);
			weekDay.add(VEvent.RRule.WeekDay.FR);
			event.rrule.byDay = weekDay;
		}

		event.organizer = new VEvent.Organizer(testDevice.owner.value.defaultEmail().address);

		List<VEvent.Attendee> attendees = new ArrayList<>(1);
		VEvent.Attendee me = VEvent.Attendee.create(VEvent.CUType.Individual, "", VEvent.Role.Chair,
				VEvent.ParticipationStatus.Accepted, true, "", "", "",
				testDevice.owner.value.contactInfos.identification.formatedName.value, "", "", null,
				testDevice.owner.value.defaultEmail().address);
		attendees.add(me);

		event.attendees = attendees;

		IAuthentication authService = ClientSideServiceProvider.getProvider(testDevice.coreUrl, null)
				.instance(IAuthentication.class);
		LoginResponse token = authService.login(login, password, "sync-endpoint-calendar-tests");
		ICalendar cc = ClientSideServiceProvider.getProvider(testDevice.coreUrl, token.authKey)
				.instance(ICalendar.class, "calendar:Default:" + testDevice.owner.uid);

		cc.create(uid, VEventSeries.create(event), false);
	}

}
