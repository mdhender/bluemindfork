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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import io.vertx.core.buffer.Buffer;
import net.bluemind.eas.command.folder.sync.FolderSyncEndpoint;
import net.bluemind.eas.command.sync.SyncEndpoint;
import net.bluemind.eas.http.wbxml.WbxmlHandlerBase;
import net.bluemind.eas.testhelper.mock.ResponseObject;
import net.bluemind.eas.utils.DOMUtils;
import net.bluemind.eas.wbxml.WBXMLTools;

public class SyncEndpointSMSTests extends AbstractEndpointTest {

	public int inboxId;
	public String syncKey;
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
			if ("2".equals(folderType)) { // INBOX
				inboxId = Integer.parseInt(DOMUtils.getUniqueElement(el, "ServerId").getTextContent());
			}
		}
		assertNotNull(inboxId);

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

	}

	// <?xml version="1.0" encoding="UTF-8"?><Sync xmlns="AirSync">
	// <Collections>
	// <Collection>
	// <SyncKey>cd1a38cb-5b3b-420e-b5c9-15ede8262a1a</SyncKey>
	// <CollectionId>14781</CollectionId>
	// <DeletesAsMoves>1</DeletesAsMoves>
	// <GetChanges/>
	// <WindowSize>50</WindowSize>
	// <Options>
	// <FilterType>0</FilterType>
	// <RightsManagementSupport
	// xmlns="RightsManagement">1</RightsManagementSupport>
	// <BodyPreference xmlns="AirSyncBase">
	// <Type>2</Type>
	// <TruncationSize>102400</TruncationSize>
	// </BodyPreference>
	// <BodyPreference xmlns="AirSyncBase">
	// <Type>4</Type>
	// </BodyPreference>
	// </Options>
	// <Options>
	// <Class>SMS</Class>
	// <FilterType>0</FilterType>
	// <BodyPreference xmlns="AirSyncBase">
	// <Type>1</Type>
	// </BodyPreference>
	// </Options>
	// <Commands>
	// <Add>
	// <Class>SMS</Class>
	// <ClientId>SMS_1453986460744</ClientId>
	// <ApplicationData>
	// <To xmlns="Email">"" [MOBILE:]</To>
	// <From xmlns="Email">"36612" [MOBILE:36612]</From>
	// <DateReceived xmlns="Email">2016-01-28T13:07:40.000Z</DateReceived>
	// <Importance xmlns="Email">1</Importance>
	// <Read xmlns="Email">0</Read>
	// <Body xmlns="AirSyncBase">
	// <Type>1</Type>
	// <EstimatedDataSize>160</EstimatedDataSize>
	// <Data>C'est la fête du slip avec ce SMS. Cond. en
	// restaurant. Code 1888395</Data>
	// </Body>
	// </ApplicationData>
	// </Add>
	// </Commands>
	// </Collection>
	// </Collections>
	// </Sync>

	public void testSyncSMS() throws Exception {
		Document sync = DOMUtils.createDoc("AirSync", "Sync");
		Element root = sync.getDocumentElement();
		Element collections = DOMUtils.createElement(root, "Collections");
		Element collection = DOMUtils.createElement(collections, "Collection");
		DOMUtils.createElementAndText(collection, "SyncKey", syncKey);
		DOMUtils.createElementAndText(collection, "CollectionId", Integer.toString(inboxId));
		DOMUtils.createElementAndText(collection, "DeletesAsMoves", "1");
		DOMUtils.createElementAndText(collection, "WindowSize", WINDOW_SIZE + "");

		Element options = DOMUtils.createElement(collection, "Options");
		DOMUtils.createElementAndText(options, "FilterType", "0");
		Element bodyPreference = DOMUtils.createElement(options, "AirSyncBase:BodyPreference");
		DOMUtils.createElementAndText(bodyPreference, "Type", "1");
		DOMUtils.createElementAndText(bodyPreference, "TruncationSize", "32768");

		options = DOMUtils.createElement(collection, "Options");
		DOMUtils.createElementAndText(options, "Class", "SMS");
		DOMUtils.createElementAndText(options, "FilterType", "0");
		bodyPreference = DOMUtils.createElement(options, "AirSyncBase:BodyPreference");
		DOMUtils.createElementAndText(bodyPreference, "Type", "1");

		Element commands = DOMUtils.createElement(collection, "Commands");
		Element add = DOMUtils.createElement(commands, "Add");

		DOMUtils.createElementAndText(add, "Class", "SMS");
		DOMUtils.createElementAndText(add, "ClientId", "SMS_42");
		// Element appData = DOMUtils.createElement(add, "ApplicationData");
		// DOMUtils.createElementAndText(appData, "To", "\"\" [MOBILE:]");
		// DOMUtils.createElementAndText(appData, "From", "36612
		// [MOBILE:26612]");
		// DOMUtils.createElementAndText(appData, "DateReceived",
		// "2016-01-28T13:07:40.000Z");
		// DOMUtils.createElementAndText(appData, "Importance", "1");
		// DOMUtils.createElementAndText(appData, "Read", "0");

		ResponseObject response = runEndpoint(sync);
		assertEquals(200, response.getStatusCode());
		Buffer content = response.content;
		Document resp = WBXMLTools.toXml(content.getBytes());
		syncKey = DOMUtils.getUniqueElement(resp.getDocumentElement(), "SyncKey").getTextContent();
		assertNotNull(syncKey);
		String status = DOMUtils.getUniqueElement(resp.getDocumentElement(), "Status").getTextContent();
		assertEquals("1", status);
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

	private Document runSyncEndpoint(IClientChangeProvider clientChanges) throws IOException {
		Document sync = DOMUtils.createDoc("AirSync", "Sync");
		Element root = sync.getDocumentElement();
		Element collections = DOMUtils.createElement(root, "Collections");
		Element collection = DOMUtils.createElement(collections, "Collection");
		DOMUtils.createElementAndText(collection, "SyncKey", syncKey);
		DOMUtils.createElementAndText(collection, "CollectionId", Integer.toString(inboxId));
		DOMUtils.createElement(collection, "GetChanges");
		DOMUtils.createElementAndText(collection, "WindowSize", WINDOW_SIZE + "");
		Element options = DOMUtils.createElement(collection, "Options");
		DOMUtils.createElementAndText(options, "FilterType", "3");
		Element bodyPreference = DOMUtils.createElement(options, "AirSyncBase:BodyPreference");
		DOMUtils.createElementAndText(bodyPreference, "Type", "2");
		DOMUtils.createElementAndText(bodyPreference, "TruncationSize", "200000");
		clientChanges.setClientChanges(inboxId, collection);
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
