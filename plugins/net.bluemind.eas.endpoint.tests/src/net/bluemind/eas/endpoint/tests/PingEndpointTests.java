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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.vertx.java.core.buffer.Buffer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import net.bluemind.eas.command.folder.sync.FolderSyncEndpoint;
import net.bluemind.eas.command.ping.PingEndpoint;
import net.bluemind.eas.dto.ping.PingResponse;
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

public class PingEndpointTests extends AbstractEndpointTest {

	private int inboxServerId;
	private int calendarServerId;
	private int addressbookServerId;
	private Producer mailNotification;

	public void setUp() throws Exception {
		super.setUp();
		fetchInboxId();
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
		return new PingEndpoint();
	}

	private void fetchInboxId() throws IOException {
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
		Integer inboxId = null;
		Integer calId = null;
		Integer bookId = null;
		for (int i = 0; i < added.getLength(); i++) {
			Element el = (Element) added.item(i);
			String folderType = DOMUtils.getUniqueElement(el, "Type").getTextContent();
			switch (folderType) {
			case "2":
				inboxId = Integer.parseInt(DOMUtils.getUniqueElement(el, "ServerId").getTextContent());
				break;
			case "8":
				calId = Integer.parseInt(DOMUtils.getUniqueElement(el, "ServerId").getTextContent());
				break;
			case "9":
				bookId = Integer.parseInt(DOMUtils.getUniqueElement(el, "ServerId").getTextContent());
				break;
			}
		}
		assertNotNull(inboxId);
		inboxServerId = inboxId;
		System.out.println("Inbox has serverId " + inboxServerId);
		assertNotNull(calId);
		calendarServerId = calId;
		System.out.println("Cal has serverId " + calendarServerId);
		assertNotNull(bookId);
		addressbookServerId = bookId;
		System.out.println("Addressbook has serverId " + addressbookServerId);

	}

	public void testSetupWorks() {
		assertTrue(inboxServerId > 0);
		assertTrue(calendarServerId > 0);
		assertTrue(addressbookServerId > 0);
	}

	public void testPingInvalidInterval() throws IOException {
		Document document = DOMUtils.createDoc("Ping", "Ping");
		ResponseObject response = runEndpoint(document);
		assertNotNull(response);
		assertEquals(200, response.getStatusCode());
		Buffer content = response.content;
		Document pingResponse = WBXMLTools.toXml(content.getBytes());
		Element interval = DOMUtils.getUniqueElement(pingResponse.getDocumentElement(), "HeartbeatInterval");
		assertNotNull(interval);
		int intervalValue = Integer.parseInt(interval.getTextContent());
		assertTrue(intervalValue > 0);
		Element status = DOMUtils.getUniqueElement(pingResponse.getDocumentElement(), "Status");
		assertNotNull(status);
		assertEquals(PingResponse.Status.InvalidHeartbeatInterval.xmlValue(), status.getTextContent());
	}

	/**
	 * In this case, we monitor everything.
	 * 
	 * tom : I haven't found anything in the spec saying we should do that.
	 * 
	 * @throws IOException
	 */
	public void testPingDoesNotRespond() throws IOException {
		Document document = DOMUtils.createDoc("Ping", "Ping");
		Element root = document.getDocumentElement();
		DOMUtils.createElementAndText(root, "HeartbeatInterval", "120");
		try {
			runEndpoint(document);
			fail("We should not receive a response and Ping should go into 'push' mode");
		} catch (TimeoutException e) {
			// this is what we except
			System.out.println(e.getMessage());
		}
	}

	public void testEmptyPingFirst() throws IOException {
		try {
			ResponseObject response = runEndpointNoBody("Ping");
			assertNotNull(response);
			assertEquals(200, response.getStatusCode());
			Buffer content = response.content;
			Document pingResponse = WBXMLTools.toXml(content.getBytes());
			assertNotNull(pingResponse);
			Element status = DOMUtils.getUniqueElement(pingResponse.getDocumentElement(), "Status");
			assertNotNull(status);
			assertEquals(PingResponse.Status.MissingParameter.xmlValue(), status.getTextContent());
		} catch (TimeoutException e) {
			e.printStackTrace();
			fail("This should not timeout as ping does not know what to monitor");
		}
	}

	public void testPingInboxBlocks() throws IOException {
		Document document = DOMUtils.createDoc("Ping", "Ping");
		Element root = document.getDocumentElement();
		DOMUtils.createElementAndText(root, "HeartbeatInterval", "120");
		Element folders = DOMUtils.createElement(root, "Folders");
		Element inboxElem = DOMUtils.createElement(folders, "Folder");
		DOMUtils.createElementAndText(inboxElem, "Id", Integer.toString(inboxServerId));
		DOMUtils.createElementAndText(inboxElem, "Class", "Email");
		try {
			runEndpoint(document);
			fail("We should not receive a response and Ping should go into 'push' mode");
		} catch (TimeoutException e) {
			// this is what we except
			System.out.println(e.getMessage());
		}
	}

	public void testPingInboxUnlockOnMailReceived() throws IOException {
		Document document = DOMUtils.createDoc("Ping", "Ping");
		Element root = document.getDocumentElement();
		DOMUtils.createElementAndText(root, "HeartbeatInterval", "120");
		Element folders = DOMUtils.createElement(root, "Folders");
		Element inboxElem = DOMUtils.createElement(folders, "Folder");
		DOMUtils.createElementAndText(inboxElem, "Id", Integer.toString(inboxServerId));
		DOMUtils.createElementAndText(inboxElem, "Class", "Email");
		try {
			runEndpoint(document);
			fail("We should not receive a response and Ping should go into 'push' mode");
		} catch (TimeoutException e) {
			// this is what we except
		}

		emitNewMailNotification();

		try {
			Buffer content = lastAsyncResponse.waitForIt(20, TimeUnit.SECONDS);
			assertNotNull(content);
			assertEquals(200, lastAsyncResponse.getStatusCode());
			Document pingResponse = WBXMLTools.toXml(content.getBytes());
			assertEquals(PingResponse.Status.ChangesOccurred.xmlValue(),
					DOMUtils.getUniqueElement(pingResponse.getDocumentElement(), "Status").getTextContent());
			assertEquals(Integer.toString(inboxServerId),
					DOMUtils.getUniqueElement(pingResponse.getDocumentElement(), "Folder").getTextContent());
		} catch (TimeoutException e) {
			fail("A notification was sent, lastAsyncResponse should be unlocked.");
		}
	}

}
