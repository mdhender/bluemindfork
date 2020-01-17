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
import java.util.UUID;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import io.vertx.core.buffer.Buffer;
import net.bluemind.eas.command.folder.sync.FolderSyncEndpoint;
import net.bluemind.eas.command.sync.SyncEndpoint;
import net.bluemind.eas.endpoint.tests.helpers.TestMail;
import net.bluemind.eas.http.wbxml.WbxmlHandlerBase;
import net.bluemind.eas.testhelper.mock.ResponseObject;
import net.bluemind.eas.utils.DOMUtils;
import net.bluemind.eas.wbxml.WBXMLTools;

public class SyncEndpointMailTests extends AbstractEndpointTest {

	private int inboxId;
	private int trashId;
	private String syncKey;

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
			if ("2".equals(folderType)) { // DEFAULT_INBOX_FOLDER
				inboxId = Integer.parseInt(DOMUtils.getUniqueElement(el, "ServerId").getTextContent());
			} else if ("4".equals(folderType)) { // DEFAULT_DELETED_ITEMS_FOLDERS
				trashId = Integer.parseInt(DOMUtils.getUniqueElement(el, "ServerId").getTextContent());
			}
		}
		assertNotNull(inboxId);
		assertNotNull(trashId);
		assertTrue(inboxId > 0);
		assertTrue(trashId > 0);

		Document sync = DOMUtils.createDoc("AirSync", "Sync");
		root = sync.getDocumentElement();
		Element collections = DOMUtils.createElement(root, "Collections");
		Element collection = DOMUtils.createElement(collections, "Collection");
		DOMUtils.createElementAndText(collection, "SyncKey", "0");
		DOMUtils.createElementAndText(collection, "CollectionId", Integer.toString(inboxId));

		ResponseObject response = runEndpoint(sync);
		assertEquals(200, response.getStatusCode());
		content = response.content;
		Document resp = WBXMLTools.toXml(content.getBytes());
		syncKey = DOMUtils.getUniqueElement(resp.getDocumentElement(), "SyncKey").getTextContent();
		assertNotNull(syncKey);

		// FULL SYNC first
		boolean done = false;
		while (!done) {
			resp = sync(inboxId);
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

		done = false;
		while (!done) {
			resp = sync(trashId);
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

	}

	public void testDeleteMail() throws Exception {
		// append mail
		String subject = UUID.randomUUID().toString();
		TestMail tm = appendEmail(subject);

		assertTrue(fetchEmail("INBOX", tm.uid));

		// sync (1 new item)
		Document resp = sync(inboxId);
		syncKey = DOMUtils.getUniqueElement(resp.getDocumentElement(), "SyncKey").getTextContent();
		Element commands = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Commands");
		assertNotNull(commands);
		assertEquals(1, commands.getChildNodes().getLength());
		Element cmd = (Element) commands.getChildNodes().item(0);
		assertEquals("Add", cmd.getNodeName());
		String serverId = DOMUtils.getUniqueElement(resp.getDocumentElement(), "ServerId").getTextContent();
		assertNotNull(serverId);
		assertEquals(inboxId + ":" + tm.uid, serverId);

		// delete email
		Document sync = DOMUtils.createDoc("AirSync", "Sync");
		Element root = sync.getDocumentElement();
		Element collections = DOMUtils.createElement(root, "Collections");
		Element collection = DOMUtils.createElement(collections, "Collection");
		DOMUtils.createElementAndText(collection, "SyncKey", syncKey);
		DOMUtils.createElementAndText(collection, "CollectionId", Integer.toString(inboxId));

		// Delete permanently
		DOMUtils.createElementAndText(collection, "DeletesAsMoves", "0");

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
		assertEquals(inboxId + "", collectionId);
		String status = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Status").getTextContent();
		assertEquals("1", status);
		commands = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Commands");
		assertNull(commands);
		Element responses = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Responses");
		assertNull(responses);

		// check if email is not present in INBOX or in Trash
		assertFalse(fetchEmail("INBOX", tm.uid));

		// Nothing new in Trash
		resp = sync(trashId);
		syncKey = DOMUtils.getUniqueElement(resp.getDocumentElement(), "SyncKey").getTextContent();
		commands = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Commands");
		assertNull(commands);

		assertFalse(fetchEmail("Trash", tm.uid));
	}

	public void testDeletesAsMoveMail() throws Exception {
		// append mail
		String subject = UUID.randomUUID().toString();
		TestMail tm = appendEmail(subject);

		assertTrue(fetchEmail("INBOX", tm.uid));

		// sync (1 new item)
		Document resp = sync(inboxId);
		syncKey = DOMUtils.getUniqueElement(resp.getDocumentElement(), "SyncKey").getTextContent();
		Element commands = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Commands");
		assertNotNull(commands);
		assertEquals(1, commands.getChildNodes().getLength());
		Element cmd = (Element) commands.getChildNodes().item(0);
		assertEquals("Add", cmd.getNodeName());
		String serverId = DOMUtils.getUniqueElement(resp.getDocumentElement(), "ServerId").getTextContent();
		assertNotNull(serverId);
		assertEquals(inboxId + ":" + tm.uid, serverId);

		// delete email
		Document sync = DOMUtils.createDoc("AirSync", "Sync");
		Element root = sync.getDocumentElement();
		Element collections = DOMUtils.createElement(root, "Collections");
		Element collection = DOMUtils.createElement(collections, "Collection");
		DOMUtils.createElementAndText(collection, "SyncKey", syncKey);
		DOMUtils.createElementAndText(collection, "CollectionId", Integer.toString(inboxId));

		// Move to deleted items folder
		DOMUtils.createElementAndText(collection, "DeletesAsMoves", "1");

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
		assertEquals(inboxId + "", collectionId);
		String status = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Status").getTextContent();
		assertEquals("1", status);

		// check if email is not present in INBOX
		assertFalse(fetchEmail("INBOX", tm.uid));

		// sync trash
		syncKey = "0";
		resp = sync(trashId);
		syncKey = DOMUtils.getUniqueElement(resp.getDocumentElement(), "SyncKey").getTextContent();
		commands = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Commands");
		assertNotNull(commands);
		assertEquals(1, commands.getChildNodes().getLength());
		cmd = (Element) commands.getChildNodes().item(0);
		assertEquals("Add", cmd.getNodeName());
		serverId = DOMUtils.getUniqueElement(resp.getDocumentElement(), "ServerId").getTextContent();
		assertNotNull(serverId);
		String[] s = serverId.split(":");
		assertEquals(trashId + "", s[0]);

		// Check if email is present in Trash
		assertTrue(fetchEmail("Trash", Integer.parseInt(s[1])));

	}

	private static interface IClientChangeProvider {
		void setClientChanges(int collectionId, Element collectionElement);
	}

	private Document sync(int folderId) throws IOException {
		return runSyncEndpoint(folderId, new IClientChangeProvider() {

			@Override
			public void setClientChanges(int collectionId, Element collectionElement) {
			}
		});
	}

	private Document runSyncEndpoint(int folderId, IClientChangeProvider clientChanges) throws IOException {
		Document sync = DOMUtils.createDoc("AirSync", "Sync");
		Element root = sync.getDocumentElement();
		Element collections = DOMUtils.createElement(root, "Collections");
		Element collection = DOMUtils.createElement(collections, "Collection");
		DOMUtils.createElementAndText(collection, "SyncKey", syncKey);
		DOMUtils.createElementAndText(collection, "CollectionId", Integer.toString(folderId));
		DOMUtils.createElement(collection, "GetChanges");
		DOMUtils.createElementAndText(collection, "WindowSize", WINDOW_SIZE + "");
		Element options = DOMUtils.createElement(collection, "Options");
		DOMUtils.createElementAndText(options, "FilterType", "3");
		Element bodyPreference = DOMUtils.createElement(options, "AirSyncBase:BodyPreference");
		DOMUtils.createElementAndText(bodyPreference, "Type", "2");
		DOMUtils.createElementAndText(bodyPreference, "TruncationSize", "200000");
		clientChanges.setClientChanges(folderId, collection);
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

}
