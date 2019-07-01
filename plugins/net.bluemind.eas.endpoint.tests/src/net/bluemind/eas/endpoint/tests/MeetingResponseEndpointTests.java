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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.james.mime4j.codec.DecodeMonitor;
import org.apache.james.mime4j.dom.Header;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.MessageBuilder;
import org.apache.james.mime4j.dom.MessageServiceFactory;
import org.apache.james.mime4j.dom.MessageWriter;
import org.apache.james.mime4j.dom.Multipart;
import org.apache.james.mime4j.dom.TextBody;
import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.dom.field.UnstructuredField;
import org.apache.james.mime4j.field.Fields;
import org.apache.james.mime4j.field.UnstructuredFieldImpl;
import org.apache.james.mime4j.message.BasicBodyFactory;
import org.apache.james.mime4j.message.BodyPart;
import org.apache.james.mime4j.message.MessageImpl;
import org.apache.james.mime4j.message.MultipartImpl;
import org.apache.james.mime4j.stream.RawField;
import org.joda.time.DateTime;
import org.vertx.java.core.buffer.Buffer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import net.bluemind.authentication.api.IAuthentication;
import net.bluemind.authentication.api.LoginResponse;
import net.bluemind.calendar.api.ICalendar;
import net.bluemind.calendar.api.IVEvent;
import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.core.api.date.BmDateTime.Precision;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.http.ClientSideServiceProvider;
import net.bluemind.eas.command.folder.sync.FolderSyncEndpoint;
import net.bluemind.eas.command.meetingresponse.MeetingResponseEndpoint;
import net.bluemind.eas.command.meetingresponse.MeetingResponseStatus;
import net.bluemind.eas.http.wbxml.WbxmlHandlerBase;
import net.bluemind.eas.testhelper.mock.ResponseObject;
import net.bluemind.eas.utils.DOMUtils;
import net.bluemind.eas.wbxml.WBXMLTools;
import net.bluemind.imap.FlagsList;
import net.bluemind.imap.IMAPException;
import net.bluemind.imap.InternalDate;
import net.bluemind.imap.StoreClient;

public class MeetingResponseEndpointTests extends AbstractEndpointTest {

	public void testInvalidMeetingRequest() throws Exception {
		int inbox = initFolder();

		Document document = DOMUtils.createDoc("MeetingResponse", "MeetingResponse");
		Element req = DOMUtils.createElement(document.getDocumentElement(), "Request");
		DOMUtils.createElementAndText(req, "UserResponse", "1");// ACCEPTED
		DOMUtils.createElementAndText(req, "CollectionId", Integer.toString(inbox));
		DOMUtils.createElementAndText(req, "RequestId", inbox + ":123456789");
		ResponseObject response = runEndpoint(document);
		assertEquals("Status should be 200", 200, response.getStatusCode());

		Buffer content = response.content;
		Document d = WBXMLTools.toXml(content.getBytes());
		Element requestId = DOMUtils.getUniqueElement(d.getDocumentElement(), "RequestId");
		assertEquals(inbox + ":123456789", requestId.getTextContent());

		Element status = DOMUtils.getUniqueElement(d.getDocumentElement(), "Status");
		assertEquals(MeetingResponseStatus.INVALID_MEETING_REQUEST.asXmlValue(), status.getTextContent());
	}

	public void testMeetingRequestAccept() throws Exception {
		int inbox = initFolder();
		int mailId = initEvent();
		assertTrue(mailExists(mailId));

		Document document = DOMUtils.createDoc("MeetingResponse", "MeetingResponse");
		Element req = DOMUtils.createElement(document.getDocumentElement(), "Request");
		DOMUtils.createElementAndText(req, "UserResponse", "1"); // ACCEPTED
		DOMUtils.createElementAndText(req, "CollectionId", Integer.toString(inbox));
		DOMUtils.createElementAndText(req, "RequestId", inbox + ":" + mailId);
		ResponseObject response = runEndpoint(document);
		assertEquals("Status should be 200", 200, response.getStatusCode());

		Buffer content = response.content;
		Document d = WBXMLTools.toXml(content.getBytes());
		Element requestId = DOMUtils.getUniqueElement(d.getDocumentElement(), "RequestId");
		assertEquals(inbox + ":" + mailId, requestId.getTextContent());

		Element status = DOMUtils.getUniqueElement(d.getDocumentElement(), "Status");
		assertEquals("Meeting update me be successful", MeetingResponseStatus.SUCCESS.asXmlValue(),
				status.getTextContent());

		Element calendarId = DOMUtils.getUniqueElement(d.getDocumentElement(), "CalendarId");
		assertNotNull(calendarId);

		assertFalse(mailExists(mailId));

	}

	public void testMeetingRequestDecline() throws Exception {
		int inbox = initFolder();
		int mailId = initEvent();
		assertTrue(mailExists(mailId));

		Document document = DOMUtils.createDoc("MeetingResponse", "MeetingResponse");
		Element req = DOMUtils.createElement(document.getDocumentElement(), "Request");
		DOMUtils.createElementAndText(req, "UserResponse", "3"); // DECLINED
		DOMUtils.createElementAndText(req, "CollectionId", Integer.toString(inbox));
		DOMUtils.createElementAndText(req, "RequestId", inbox + ":" + mailId);
		ResponseObject response = runEndpoint(document);
		assertEquals("Status should be 200", 200, response.getStatusCode());

		Buffer content = response.content;
		Document d = WBXMLTools.toXml(content.getBytes());
		Element requestId = DOMUtils.getUniqueElement(d.getDocumentElement(), "RequestId");
		assertEquals(inbox + ":" + mailId, requestId.getTextContent());

		Element calendarId = DOMUtils.getUniqueElement(d.getDocumentElement(), "CalendarId");
		assertNull(calendarId);

		Element status = DOMUtils.getUniqueElement(d.getDocumentElement(), "Status");
		assertEquals(MeetingResponseStatus.SUCCESS.asXmlValue(), status.getTextContent());

		assertFalse(mailExists(mailId));

	}

	public void testMultipleMeetingRequest() throws Exception {
		int inbox = initFolder();
		int mailId = initEvent();
		assertTrue(mailExists(mailId));
		int mailId2 = initEvent();
		assertTrue(mailExists(mailId2));

		Document document = DOMUtils.createDoc("MeetingResponse", "MeetingResponse");

		Element req = DOMUtils.createElement(document.getDocumentElement(), "Request");
		DOMUtils.createElementAndText(req, "UserResponse", "3"); // DECLINED
		DOMUtils.createElementAndText(req, "CollectionId", Integer.toString(inbox));
		DOMUtils.createElementAndText(req, "RequestId", inbox + ":" + mailId);

		Element req2 = DOMUtils.createElement(document.getDocumentElement(), "Request");
		DOMUtils.createElementAndText(req2, "UserResponse", "1"); // ACCEPTED
		DOMUtils.createElementAndText(req2, "CollectionId", Integer.toString(inbox));
		DOMUtils.createElementAndText(req2, "RequestId", inbox + ":" + mailId2);

		ResponseObject response = runEndpoint(document);
		assertEquals("Status should be 200", 200, response.getStatusCode());

		Buffer content = response.content;
		Document d = WBXMLTools.toXml(content.getBytes());

		NodeList results = d.getElementsByTagName("Result");
		assertEquals(2, results.getLength());

		boolean el1Found = false;
		boolean el2Found = false;
		for (int i = 0; i < 2; i++) {
			Element res = (Element) results.item(i);
			Element requestId = DOMUtils.getUniqueElement(res, "RequestId");
			if (requestId.getTextContent().equals(inbox + ":" + mailId)) {
				el1Found = true;
				Element status = DOMUtils.getUniqueElement(res, "Status");
				assertEquals("1", status.getTextContent());
				Element calendarId = DOMUtils.getUniqueElement(res, "CalendarId");
				assertNull(calendarId);
			}
			if (requestId.getTextContent().equals(inbox + ":" + mailId2)) {
				el2Found = true;
				Element status = DOMUtils.getUniqueElement(res, "Status");
				assertEquals("1", status.getTextContent());
				Element calendarId = DOMUtils.getUniqueElement(res, "CalendarId");
				assertNotNull(calendarId);
			}
		}
		assertTrue(el1Found);
		assertTrue(el2Found);

	}

	private boolean mailExists(int uid) throws IMAPException {
		try (StoreClient sc = new StoreClient(vmHostname, 1143, login, password)) {
			assertTrue(sc.login());
			sc.select("INBOX");
			InternalDate[] size = sc.uidFetchInternalDate(Arrays.asList(uid));
			return size.length > 0;
		} catch (Exception e) {
			fail(e.getMessage());
		}
		return false;
	}

	private int initFolder() throws IOException {
		Document document = DOMUtils.createDoc("FolderHierarchy", "FolderSync");
		DOMUtils.createElementAndText(document.getDocumentElement(), "FolderHierarchy:SyncKey", "0");
		ResponseObject response = runEndpoint(new FolderSyncEndpoint(), document);
		assertEquals("Status should be 200", 200, response.getStatusCode());

		Buffer content = response.content;
		Document doc = WBXMLTools.toXml(content.getBytes());

		assertNotNull(doc);
		NodeList added = doc.getDocumentElement().getElementsByTagNameNS("FolderHierarchy", "Add");
		Integer inboxId = null;
		for (int i = 0; i < added.getLength(); i++) {
			Element el = (Element) added.item(i);
			String folderType = DOMUtils.getUniqueElement(el, "Type").getTextContent();
			if ("2".equals(folderType)) { // INBOX
				inboxId = Integer.parseInt(DOMUtils.getUniqueElement(el, "ServerId").getTextContent());
				break;
			}
		}
		assertNotNull(inboxId);
		assertTrue(inboxId > 0);
		return inboxId;
	}

	private int initEvent() throws Exception {
		IAuthentication authService = ClientSideServiceProvider.getProvider(testDevice.coreUrl, null)
				.instance(IAuthentication.class);
		LoginResponse token = authService.login(login, password, "meetingresponse-endpoint-tests");

		ICalendar calendarService = ClientSideServiceProvider.getProvider(testDevice.coreUrl, token.authKey)
				.instance(ICalendar.class, "calendar:Default:" + testDevice.owner.uid);

		VEvent event = new VEvent();
		event.dtstart = BmDateTimeWrapper.create(new DateTime(), Precision.DateTime);
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
				testDevice.owner.value.defaultEmail().address);
		attendees.add(me);

		event.attendees = attendees;
		String uid = UUID.randomUUID().toString();
		calendarService.create(uid, VEventSeries.create(event), false);

		try (StoreClient sc = new StoreClient(vmHostname, 1143, login, password)) {
			assertTrue(sc.login());
			sc.select("INBOX");

			ByteArrayOutputStream out = new ByteArrayOutputStream();
			MessageServiceFactory msf = MessageServiceFactory.newInstance();
			Message mm = getRandomMessage(msf, token.authKey, uid);
			MessageWriter writer = msf.newMessageWriter();
			writer.writeMessage(mm, out);
			ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());

			int id = sc.append("INBOX", in, new FlagsList());
			assertTrue(id > 0);
			sc.logout();

			return id;
		} catch (Exception e) {
			fail(e.getMessage());
		}
		return 0;
	}

	private Message getRandomMessage(MessageServiceFactory msf, String authKey, String uid)
			throws IOException, ServerFault {
		MessageBuilder builder = msf.newMessageBuilder();

		MessageImpl m = new MessageImpl();
		m.setDate(new Date());
		m.setSubject("on va à la piscine ou quoi?");
		m.setSender(new Mailbox("John Bang", "john.bang", "local.lan"));
		m.setFrom(new Mailbox("John Bang", "john.bang", "local.lan"));
		m.setTo(new Mailbox(owner.displayName, owner.value.login, domainUid));

		RawField rf = new RawField("X-BM-Event", uid);
		UnstructuredField bmExtId = UnstructuredFieldImpl.PARSER.parse(rf, DecodeMonitor.SILENT);
		m.getHeader().addField(bmExtId);

		BasicBodyFactory bbf = new BasicBodyFactory();
		TextBody text = bbf.textBody("<html><body>piscine?</body></html>", "UTF-8");
		BodyPart body = new BodyPart();
		body.setText(text);

		Header h = builder.newHeader();
		h.setField(Fields.contentType("text/html; charset=UTF-8;"));
		h.setField(Fields.contentTransferEncoding("quoted-printable"));
		body.setHeader(h);

		IVEvent veventService = ClientSideServiceProvider.getProvider(testDevice.coreUrl, authKey)
				.instance(IVEvent.class, "calendar:Default:" + testDevice.owner.uid);

		TextBody icsTextBody = bbf.textBody(veventService.exportIcs(uid));
		BodyPart icsBodyPart = new BodyPart();
		icsBodyPart.setText(icsTextBody);

		BodyPart attachment = new BodyPart();
		attachment.setBody(icsBodyPart.getBody());
		attachment.setFilename("event.ics");
		h = builder.newHeader();
		h.setField(Fields.contentType("application/ics; name=\"event.ics\""));
		h.setField(Fields.contentDisposition("attachment; filename=\"event.ics\""));
		h.setField(Fields.contentTransferEncoding("base64"));
		attachment.setHeader(h);

		BodyPart textCalendar = new BodyPart();
		textCalendar.setBody(icsBodyPart.getBody());
		textCalendar.setFilename("event.ics");
		h = builder.newHeader();
		h.setField(Fields.contentType("text/calendar; charset=UTF-8; method=REQUEST"));
		h.setField(Fields.contentTransferEncoding("8bit"));
		textCalendar.setHeader(h);

		Multipart alternative = new MultipartImpl("alternative");
		alternative.addBodyPart(body);
		if (textCalendar != null) {
			alternative.addBodyPart(textCalendar);
		}

		MessageImpl alternativeMessage = new MessageImpl();
		alternativeMessage.setMultipart(alternative);

		BodyPart alternativePart = new BodyPart();
		alternativePart.setMessage(alternativeMessage);

		Multipart mixed = new MultipartImpl("mixed");
		mixed.addBodyPart(alternativeMessage);
		if (attachment != null) {
			mixed.addBodyPart(attachment);
		}

		m.setMultipart(mixed);

		return m;
	}

	@Override
	public WbxmlHandlerBase createEndpoint() {
		return new MeetingResponseEndpoint();
	}

}
