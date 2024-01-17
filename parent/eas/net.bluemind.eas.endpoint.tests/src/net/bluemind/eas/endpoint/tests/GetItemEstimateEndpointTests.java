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
import net.bluemind.eas.command.getitemestimate.GetItemEstimateEndpoint;
import net.bluemind.eas.http.wbxml.WbxmlHandlerBase;
import net.bluemind.eas.testhelper.mock.ResponseObject;
import net.bluemind.eas.utils.DOMUtils;
import net.bluemind.eas.wbxml.WBXMLTools;

public class GetItemEstimateEndpointTests extends AbstractEndpointTest {

	private Integer inbox;

	public void testGetItemEstimate() throws Exception {
		String syncKey = initFolder();

		Document document = DOMUtils.createDoc("GetItemEstimate", "GetItemEstimate");

		Element collections = DOMUtils.createElement(document.getDocumentElement(), "Collections");

		Element collection = DOMUtils.createElement(collections, "Collection");
		DOMUtils.createElementAndText(collection, "AirSync:SyncKey", syncKey);
		DOMUtils.createElementAndText(collection, "CollectionId", inbox.toString());

		Element options = DOMUtils.createElement(collection, "AirSync:Options");
		DOMUtils.createElementAndText(options, "AirSync:FilterType", "2");
		DOMUtils.createElementAndText(options, "AirSync:Class", "Email");

		ResponseObject response = runEndpoint(document);
		assertEquals("Status should be 200", 200, response.getStatusCode());

		Buffer content = response.content;
		Document d = WBXMLTools.toXml(content.getBytes());

		Element status = DOMUtils.getUniqueElement(d.getDocumentElement(), "Status");
		assertEquals("1", status.getTextContent());
		Element estimate = DOMUtils.getUniqueElement(d.getDocumentElement(), "Estimate");
		assertEquals("1", estimate.getTextContent());

	}

	public void testGetItemEstimateInvalidSyncKey() throws Exception {
		initFolder();

		Document document = DOMUtils.createDoc("GetItemEstimate", "GetItemEstimate");

		Element collections = DOMUtils.createElement(document.getDocumentElement(), "Collections");

		Element collection = DOMUtils.createElement(collections, "Collection");
		DOMUtils.createElementAndText(collection, "AirSync:SyncKey", "123");
		DOMUtils.createElementAndText(collection, "CollectionId", inbox.toString());

		Element options = DOMUtils.createElement(collection, "AirSync:Options");
		DOMUtils.createElementAndText(options, "AirSync:FilterType", "2");
		DOMUtils.createElementAndText(options, "AirSync:Class", "Email");

		ResponseObject response = runEndpoint(document);

		assertEquals("Status should be 200", 200, response.getStatusCode());

		Buffer content = response.content;
		Document d = WBXMLTools.toXml(content.getBytes());

		Element status = DOMUtils.getUniqueElement(d.getDocumentElement(), "Status");
		assertEquals("4", status.getTextContent());
		Element estimate = DOMUtils.getUniqueElement(d.getDocumentElement(), "Estimate");
		assertEquals("0", estimate.getTextContent());
	}

	public void testGetItemEstimateCollectionNotFound() throws Exception {
		String sk = initFolder();

		Document document = DOMUtils.createDoc("GetItemEstimate", "GetItemEstimate");

		Element collections = DOMUtils.createElement(document.getDocumentElement(), "Collections");

		Element collection = DOMUtils.createElement(collections, "Collection");
		DOMUtils.createElementAndText(collection, "AirSync:SyncKey", sk);
		DOMUtils.createElementAndText(collection, "CollectionId", Integer.MAX_VALUE + "");

		Element options = DOMUtils.createElement(collection, "AirSync:Options");
		DOMUtils.createElementAndText(options, "AirSync:FilterType", "2");
		DOMUtils.createElementAndText(options, "AirSync:Class", "Email");

		ResponseObject response = runEndpoint(document);

		assertEquals("Status should be 200", 200, response.getStatusCode());

		Buffer content = response.content;
		Document d = WBXMLTools.toXml(content.getBytes());

		Element status = DOMUtils.getUniqueElement(d.getDocumentElement(), "Status");
		assertEquals("2", status.getTextContent());
		Element estimate = DOMUtils.getUniqueElement(d.getDocumentElement(), "Estimate");
		assertEquals("0", estimate.getTextContent());

	}

	public void testMultipleGetItemEstimate() throws Exception {
		String syncKey = initFolder();

		Document document = DOMUtils.createDoc("GetItemEstimate", "GetItemEstimate");

		Element collections = DOMUtils.createElement(document.getDocumentElement(), "Collections");

		Element collection1 = DOMUtils.createElement(collections, "Collection");
		DOMUtils.createElementAndText(collection1, "AirSync:SyncKey", syncKey);
		DOMUtils.createElementAndText(collection1, "CollectionId", inbox.toString());
		Element options1 = DOMUtils.createElement(collection1, "AirSync:Options");
		DOMUtils.createElementAndText(options1, "AirSync:FilterType", "2");
		DOMUtils.createElementAndText(options1, "AirSync:Class", "Email");

		Element collection2 = DOMUtils.createElement(collections, "Collection");
		DOMUtils.createElementAndText(collection2, "AirSync:SyncKey", syncKey);
		DOMUtils.createElementAndText(collection2, "CollectionId", Integer.MAX_VALUE + "");
		Element options2 = DOMUtils.createElement(collection2, "AirSync:Options");
		DOMUtils.createElementAndText(options2, "AirSync:FilterType", "2");
		DOMUtils.createElementAndText(options2, "AirSync:Class", "Email");

		ResponseObject response = runEndpoint(document);
		assertEquals("Status should be 200", 200, response.getStatusCode());

		Buffer content = response.content;
		Document d = WBXMLTools.toXml(content.getBytes());

		NodeList responses = d.getElementsByTagName("Response");
		assertEquals(2, responses.getLength());

		boolean col1Found = false;
		boolean col2Found = false;
		for (int i = 0; i < 2; i++) {
			Element el = (Element) responses.item(i);
			int collectionId = Integer.parseInt(DOMUtils.getUniqueElement(el, "CollectionId").getTextContent());
			if (collectionId == inbox) {
				col1Found = true;
				Element status = DOMUtils.getUniqueElement(el, "Status");
				assertEquals("1", status.getTextContent());
				Element estimate = DOMUtils.getUniqueElement(el, "Estimate");
				assertEquals("1", estimate.getTextContent());
			} else if (collectionId == Integer.MAX_VALUE) {
				col2Found = true;
				Element status = DOMUtils.getUniqueElement(el, "Status");
				assertEquals("2", status.getTextContent());
				Element estimate = DOMUtils.getUniqueElement(el, "Estimate");
				assertEquals("0", estimate.getTextContent());
			}
		}
		assertTrue(col1Found);
		assertTrue(col2Found);
	}

	private String initFolder() throws IOException {
		Document document = DOMUtils.createDoc("FolderHierarchy", "FolderSync");
		Element root = document.getDocumentElement();
		DOMUtils.createElementAndText(root, "SyncKey", "0");
		ResponseObject response = runEndpoint(new FolderSyncEndpoint(), document);
		assertEquals("Status should be 200", 200, response.getStatusCode());

		Buffer content = response.content;
		Document d = WBXMLTools.toXml(content.getBytes());
		Element sk = DOMUtils.getUniqueElement(d.getDocumentElement(), "SyncKey");
		assertNotNull(sk.getTextContent());

		NodeList added = d.getDocumentElement().getElementsByTagNameNS("FolderHierarchy", "Add");
		for (int i = 0; i < added.getLength(); i++) {
			Element el = (Element) added.item(i);
			String folderType = DOMUtils.getUniqueElement(el, "Type").getTextContent();
			switch (folderType) {
			case "2":
				inbox = Integer.parseInt(DOMUtils.getUniqueElement(el, "ServerId").getTextContent());
				break;
			}
		}
		assertNotNull(inbox);
		assertTrue(inbox > 0);
		return sk.getTextContent();

	}

	@Override
	public WbxmlHandlerBase createEndpoint() {
		return new GetItemEstimateEndpoint();
	}

}
