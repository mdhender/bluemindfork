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

import org.vertx.java.core.buffer.Buffer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import net.bluemind.eas.command.folder.crud.FolderCreateEndpoint;
import net.bluemind.eas.command.folder.crud.FolderUpdateEndpoint;
import net.bluemind.eas.command.folder.sync.FolderSyncEndpoint;
import net.bluemind.eas.dto.folderupdate.FolderUpdateResponse;
import net.bluemind.eas.http.wbxml.WbxmlHandlerBase;
import net.bluemind.eas.testhelper.mock.ResponseObject;
import net.bluemind.eas.utils.DOMUtils;
import net.bluemind.eas.wbxml.WBXMLTools;

public class FolderUpdateEndpointTests extends AbstractEndpointTest {

	public void testUpdateFolder() throws IOException {

		Document doc = initFolder();
		Element sk = DOMUtils.getUniqueElement(doc.getDocumentElement(), "SyncKey");
		String syncKey = sk.getTextContent();
		assertNotNull(syncKey);

		F f = createFolder(syncKey);

		String updated = "updated" + System.currentTimeMillis();

		Document document = DOMUtils.createDoc("FolderHierarchy", "FolderUpdate");
		Element root = document.getDocumentElement();
		DOMUtils.createElementAndText(root, "SyncKey", f.syncKey);
		DOMUtils.createElementAndText(root, "ServerId", f.serverId);
		DOMUtils.createElementAndText(root, "ParentId", "0");
		DOMUtils.createElementAndText(root, "DisplayName", updated);
		ResponseObject response = runEndpoint(document);
		assertEquals("Status should be 200", 200, response.getStatusCode());

		Buffer content = response.content;
		Document d = WBXMLTools.toXml(content.getBytes());

		Element status = DOMUtils.getUniqueElement(d.getDocumentElement(), "Status");
		assertEquals("1", status.getTextContent());

		sk = DOMUtils.getUniqueElement(d.getDocumentElement(), "SyncKey");
		assertNotNull(sk.getTextContent());

		assertFalse(sk.getTextContent().equals(f.syncKey));

		// Check check
		doc = initFolder();
		boolean found = false;
		NodeList added = doc.getDocumentElement().getElementsByTagNameNS("FolderHierarchy", "Add");
		for (int i = 0; i < added.getLength(); i++) {
			Element el = (Element) added.item(i);
			String folderType = DOMUtils.getUniqueElement(el, "Type").getTextContent();
			String dn = DOMUtils.getUniqueElement(el, "DisplayName").getTextContent();
			if ("12".equals(folderType) && updated.equals(dn)) { // USER_CREATED_EMAIL_FOLDER
				found = true;
			}
		}
		assertTrue(found);
	}

	public void testUpdateFolderNonExistantFolder() throws IOException {

		Document document = DOMUtils.createDoc("FolderHierarchy", "FolderUpdate");
		Element root = document.getDocumentElement();
		DOMUtils.createElementAndText(root, "SyncKey", "0");
		DOMUtils.createElementAndText(root, "ServerId", "666667");
		DOMUtils.createElementAndText(root, "ParentId", "0");
		DOMUtils.createElementAndText(root, "DisplayName", "updated" + System.currentTimeMillis());
		ResponseObject response = runEndpoint(document);
		assertEquals("Status should be 200", 200, response.getStatusCode());
		Buffer content = response.content;
		Document d = WBXMLTools.toXml(content.getBytes());

		Element status = DOMUtils.getUniqueElement(d.getDocumentElement(), "Status");
		assertEquals(FolderUpdateResponse.Status.DoesNotExist.xmlValue(), status.getTextContent());
	}

	public void testUpdateFolderBadServerId() throws IOException {

		Document document = DOMUtils.createDoc("FolderHierarchy", "FolderUpdate");
		Element root = document.getDocumentElement();
		DOMUtils.createElementAndText(root, "SyncKey", "0");
		DOMUtils.createElementAndText(root, "ServerId", "toto");
		DOMUtils.createElementAndText(root, "ParentId", "0");
		DOMUtils.createElementAndText(root, "DisplayName", "updated" + System.currentTimeMillis());
		ResponseObject response = runEndpoint(document);
		assertEquals("Status should be 200", 200, response.getStatusCode());
		Buffer content = response.content;
		Document d = WBXMLTools.toXml(content.getBytes());

		Element status = DOMUtils.getUniqueElement(d.getDocumentElement(), "Status");
		assertEquals(FolderUpdateResponse.Status.InvalidRequest.xmlValue(), status.getTextContent());
	}

	public void testUpdateFolderBadParentId() throws IOException {

		Document document = DOMUtils.createDoc("FolderHierarchy", "FolderUpdate");
		Element root = document.getDocumentElement();
		DOMUtils.createElementAndText(root, "SyncKey", "0");
		DOMUtils.createElementAndText(root, "ServerId", "1");
		DOMUtils.createElementAndText(root, "ParentId", "coucou");
		DOMUtils.createElementAndText(root, "DisplayName", "updated" + System.currentTimeMillis());
		ResponseObject response = runEndpoint(document);
		assertEquals("Status should be 200", 200, response.getStatusCode());
		Buffer content = response.content;
		Document d = WBXMLTools.toXml(content.getBytes());

		Element status = DOMUtils.getUniqueElement(d.getDocumentElement(), "Status");
		assertEquals(FolderUpdateResponse.Status.InvalidRequest.xmlValue(), status.getTextContent());
	}

	public void testUpdateFolderParentDoesNotExists() throws IOException {
		Document doc = initFolder();
		Element sk = DOMUtils.getUniqueElement(doc.getDocumentElement(), "SyncKey");
		String syncKey = sk.getTextContent();
		assertNotNull(syncKey);

		F f = createFolder(syncKey);

		Document document = DOMUtils.createDoc("FolderHierarchy", "FolderUpdate");
		Element root = document.getDocumentElement();
		DOMUtils.createElementAndText(root, "SyncKey", "0");
		DOMUtils.createElementAndText(root, "ServerId", f.serverId);
		DOMUtils.createElementAndText(root, "ParentId", "33333");
		DOMUtils.createElementAndText(root, "DisplayName", "updated" + System.currentTimeMillis());
		ResponseObject response = runEndpoint(document);
		assertEquals("Status should be 200", 200, response.getStatusCode());
		Buffer content = response.content;
		Document d = WBXMLTools.toXml(content.getBytes());

		Element status = DOMUtils.getUniqueElement(d.getDocumentElement(), "Status");
		assertEquals(FolderUpdateResponse.Status.ParentFolderNotFound.xmlValue(), status.getTextContent());
	}

	private F createFolder(String syncKey) throws IOException {
		Document document = DOMUtils.createDoc("FolderHierarchy", "FolderCreate");
		Element root = document.getDocumentElement();
		DOMUtils.createElementAndText(root, "SyncKey", syncKey);
		DOMUtils.createElementAndText(root, "ParentId", "0");
		DOMUtils.createElementAndText(root, "DisplayName", "testCreateFolder" + System.currentTimeMillis());
		DOMUtils.createElementAndText(root, "Type", "12"); // USER_CREATED_EMAIL_FOLDER
		ResponseObject response = runEndpoint(new FolderCreateEndpoint(), document);
		assertEquals("Status should be 200", 200, response.getStatusCode());

		Buffer content = response.content;
		Document d = WBXMLTools.toXml(content.getBytes());

		Element status = DOMUtils.getUniqueElement(d.getDocumentElement(), "Status");
		assertEquals("1", status.getTextContent());
		Element sk = DOMUtils.getUniqueElement(d.getDocumentElement(), "SyncKey");
		assertNotNull(sk.getTextContent());
		Element serverId = DOMUtils.getUniqueElement(d.getDocumentElement(), "ServerId");
		assertNotNull(serverId.getTextContent());

		F f = new F();
		f.serverId = serverId.getTextContent();
		f.syncKey = sk.getTextContent();

		return f;
	}

	private Document initFolder() throws IOException {
		Document document = DOMUtils.createDoc("FolderHierarchy", "FolderSync");
		Element root = document.getDocumentElement();
		DOMUtils.createElementAndText(root, "SyncKey", "0");
		ResponseObject response = runEndpoint(new FolderSyncEndpoint(), document);
		assertEquals("Status should be 200", 200, response.getStatusCode());

		Buffer content = response.content;
		Document d = WBXMLTools.toXml(content.getBytes());

		return d;
	}

	public static final class F {
		public String syncKey;
		public String serverId;
	}

	@Override
	public WbxmlHandlerBase createEndpoint() {
		return new FolderUpdateEndpoint();
	}

}
