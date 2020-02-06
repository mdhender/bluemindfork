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
import net.bluemind.addressbook.api.IAddressBook;
import net.bluemind.addressbook.api.VCard;
import net.bluemind.authentication.api.IAuthentication;
import net.bluemind.authentication.api.LoginResponse;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.http.ClientSideServiceProvider;
import net.bluemind.eas.command.folder.sync.FolderSyncEndpoint;
import net.bluemind.eas.command.sync.SyncEndpoint;
import net.bluemind.eas.http.wbxml.WbxmlHandlerBase;
import net.bluemind.eas.testhelper.mock.ResponseObject;
import net.bluemind.eas.utils.DOMUtils;
import net.bluemind.eas.wbxml.WBXMLTools;

public class SyncEndpointAddressbookTests extends AbstractEndpointTest {

	private int bookId;
	private String syncKey;
	private IAddressBook bc;

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
			if ("9".equals(folderType)) { // DEFAULT_CONTACTS_FOLDER
				bookId = Integer.parseInt(DOMUtils.getUniqueElement(el, "ServerId").getTextContent());
			}
		}
		assertNotNull(bookId);
		assertTrue(bookId > 0);

		Document sync = DOMUtils.createDoc("AirSync", "Sync");
		root = sync.getDocumentElement();
		Element collections = DOMUtils.createElement(root, "Collections");
		Element collection = DOMUtils.createElement(collections, "Collection");
		DOMUtils.createElementAndText(collection, "SyncKey", "0");
		DOMUtils.createElementAndText(collection, "CollectionId", Integer.toString(bookId));

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
		LoginResponse token = authService.login(login, password, "sync-endpoint-addressbook-tests");
		bc = ClientSideServiceProvider.getProvider(testDevice.coreUrl, token.authKey).instance(IAddressBook.class,
				"book:Contacts_" + testDevice.owner.uid);

	}

	public void testCreateContact() throws Exception {
		// next sync (0 item)
		Document resp = sync();
		syncKey = DOMUtils.getUniqueElement(resp.getDocumentElement(), "SyncKey").getTextContent();
		Element commands = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Commands");
		assertNull(commands);

		// new event
		String clientId = System.currentTimeMillis() + "";
		Document sync = DOMUtils.createDoc("AirSync", "Sync");
		Element root = sync.getDocumentElement();
		Element collections = DOMUtils.createElement(root, "Collections");
		Element collection = DOMUtils.createElement(collections, "Collection");
		DOMUtils.createElementAndText(collection, "SyncKey", syncKey);
		DOMUtils.createElementAndText(collection, "CollectionId", Integer.toString(bookId));
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
		DOMUtils.createElementAndText(appData, "Contacts:CompanyName", "BlueMind");
		DOMUtils.createElementAndText(appData, "Contacts:Email1Address", "big.boss@bluemind.lan");
		DOMUtils.createElementAndText(appData, "Contacts:FirstName", "big");
		DOMUtils.createElementAndText(appData, "Contacts:LastName", "boss");
		DOMUtils.createElementAndText(appData, "Contacts:MobilePhoneNumber", "0600000000");

		ResponseObject response = runEndpoint(sync);
		assertEquals(200, response.getStatusCode());
		Buffer content = response.content;

		resp = WBXMLTools.toXml(content.getBytes());
		syncKey = DOMUtils.getUniqueElement(resp.getDocumentElement(), "SyncKey").getTextContent();
		String collectionId = DOMUtils.getUniqueElement(resp.getDocumentElement(), "CollectionId").getTextContent();
		assertEquals(bookId + "", collectionId);
		commands = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Commands");
		assertNull(commands);
		Element responses = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Responses");
		assertNull(responses);
	}

	public void testDeleteContact() throws Exception {
		String uid = createContact();

		assertNotNull(bc.getComplete(uid));

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
		assertEquals(bookId + ":" + uid, serverId);

		// delete
		Document sync = DOMUtils.createDoc("AirSync", "Sync");
		Element root = sync.getDocumentElement();
		Element collections = DOMUtils.createElement(root, "Collections");
		Element collection = DOMUtils.createElement(collections, "Collection");
		DOMUtils.createElementAndText(collection, "SyncKey", syncKey);
		DOMUtils.createElementAndText(collection, "CollectionId", Integer.toString(bookId));
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
		assertEquals(bookId + "", collectionId);
		String status = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Status").getTextContent();
		assertEquals("1", status);

		assertNull(bc.getComplete(uid));
	}

	public void testUpdateContact() throws Exception {
		String uid = createContact();
		assertNotNull(bc.getComplete(uid));

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
		assertEquals(bookId + ":" + uid, serverId);

		// update
		Document sync = DOMUtils.createDoc("AirSync", "Sync");
		Element root = sync.getDocumentElement();
		Element collections = DOMUtils.createElement(root, "Collections");
		Element collection = DOMUtils.createElement(collections, "Collection");
		DOMUtils.createElementAndText(collection, "SyncKey", syncKey);
		DOMUtils.createElementAndText(collection, "CollectionId", Integer.toString(bookId));
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
		DOMUtils.createElementAndText(appData, "Contacts:CompanyName", "BlueMind");
		DOMUtils.createElementAndText(appData, "Contacts:Email1Address", "big.boss@bluemind.lan");
		DOMUtils.createElementAndText(appData, "Contacts:FirstName", "big");
		DOMUtils.createElementAndText(appData, "Contacts:LastName", "boss");
		DOMUtils.createElementAndText(appData, "Contacts:MobilePhoneNumber", "0600000002");

		ResponseObject response = runEndpoint(sync);
		assertEquals(200, response.getStatusCode());
		Buffer content = response.content;
		resp = WBXMLTools.toXml(content.getBytes());
		syncKey = DOMUtils.getUniqueElement(resp.getDocumentElement(), "SyncKey").getTextContent();
		String collectionId = DOMUtils.getUniqueElement(resp.getDocumentElement(), "CollectionId").getTextContent();
		assertEquals(bookId + "", collectionId);
		String status = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Status").getTextContent();
		assertEquals("1", status);

		ItemValue<VCard> c = bc.getComplete(uid);
		assertNotNull(c);
		assertEquals("boss", c.value.identification.name.familyNames);
		assertEquals("big", c.value.identification.name.givenNames);

	}

	private static interface IClientChangeProvider {
		void setClientChanges(int collectionId, Element collectionElement);
	}

	private Document sync() throws IOException {
		return runSyncEndpoint(new IClientChangeProvider() {

			@Override
			public void setClientChanges(int collectionId, Element collectionElement) {
			}
		});
	}

	private String createContact() throws ServerFault {

		VCard card = new VCard();

		card.identification = new VCard.Identification();
		card.identification.name.familyNames = "Bang";
		card.identification.name.givenNames = "John";

		String uid = UUID.randomUUID().toString();
		bc.create(uid, card);

		return uid;
	}

	private Document runSyncEndpoint(IClientChangeProvider clientChanges) throws IOException {
		Document sync = DOMUtils.createDoc("AirSync", "Sync");
		Element root = sync.getDocumentElement();
		Element collections = DOMUtils.createElement(root, "Collections");
		Element collection = DOMUtils.createElement(collections, "Collection");
		DOMUtils.createElementAndText(collection, "SyncKey", syncKey);
		DOMUtils.createElementAndText(collection, "CollectionId", Integer.toString(bookId));
		DOMUtils.createElement(collection, "GetChanges");
		DOMUtils.createElementAndText(collection, "WindowSize", WINDOW_SIZE + "");
		Element options = DOMUtils.createElement(collection, "Options");
		DOMUtils.createElementAndText(options, "FilterType", "3");
		Element bodyPreference = DOMUtils.createElement(options, "AirSyncBase:BodyPreference");
		DOMUtils.createElementAndText(bodyPreference, "Type", "2");
		DOMUtils.createElementAndText(bodyPreference, "TruncationSize", "200000");
		clientChanges.setClientChanges(bookId, collection);
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
