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

import net.bluemind.eas.command.folder.crud.FolderCreateEndpoint;
import net.bluemind.eas.command.folder.sync.FolderSyncEndpoint;
import net.bluemind.eas.dto.foldercreate.FolderCreateResponse;
import net.bluemind.eas.http.wbxml.WbxmlHandlerBase;
import net.bluemind.eas.testhelper.mock.ResponseObject;
import net.bluemind.eas.utils.DOMUtils;
import net.bluemind.eas.wbxml.WBXMLTools;

public class FolderCreateEndpointTests extends AbstractEndpointTest {

	public void testCreateFolder() throws IOException {

		String syncKey = initFolder();

		Document document = DOMUtils.createDoc("FolderHierarchy", "FolderCreate");
		Element root = document.getDocumentElement();
		DOMUtils.createElementAndText(root, "SyncKey", syncKey);
		DOMUtils.createElementAndText(root, "ParentId", "0");
		DOMUtils.createElementAndText(root, "DisplayName", "testCreateFolder" + System.currentTimeMillis());
		DOMUtils.createElementAndText(root, "Type", "12"); // USER_CREATED_EMAIL_FOLDER
		ResponseObject response = runEndpoint(document);
		assertEquals("Status should be 200", 200, response.getStatusCode());

		Buffer content = response.content;
		Document d = WBXMLTools.toXml(content.getBytes());

		Element status = DOMUtils.getUniqueElement(d.getDocumentElement(), "Status");
		assertEquals("1", status.getTextContent());
		Element sk = DOMUtils.getUniqueElement(d.getDocumentElement(), "SyncKey");
		assertNotNull(sk.getTextContent());
		Element serverId = DOMUtils.getUniqueElement(d.getDocumentElement(), "ServerId");
		assertNotNull(serverId.getTextContent());

		assertFalse(sk.getTextContent().equals(syncKey));

	}

	public void testCreateFolderInvalidType() throws IOException {

		String syncKey = initFolder();

		Document document = DOMUtils.createDoc("FolderHierarchy", "FolderCreate");
		Element root = document.getDocumentElement();
		DOMUtils.createElementAndText(root, "SyncKey", syncKey);
		DOMUtils.createElementAndText(root, "ParentId", "0");
		DOMUtils.createElementAndText(root, "DisplayName", "testCreateFolder" + System.currentTimeMillis());
		DOMUtils.createElementAndText(root, "Type", "254");
		ResponseObject response = runEndpoint(document);
		assertEquals("Status should be 200", 200, response.getStatusCode());
		Buffer content = response.content;
		Document d = WBXMLTools.toXml(content.getBytes());

		Element status = DOMUtils.getUniqueElement(d.getDocumentElement(), "Status");
		assertEquals("10", status.getTextContent());
	}

	public void testCreateFolderNonExistantParent() throws IOException {

		String syncKey = initFolder();

		Document document = DOMUtils.createDoc("FolderHierarchy", "FolderCreate");
		Element root = document.getDocumentElement();
		DOMUtils.createElementAndText(root, "SyncKey", syncKey);
		DOMUtils.createElementAndText(root, "ParentId", "666667");
		DOMUtils.createElementAndText(root, "DisplayName", "testCreateFolder" + System.currentTimeMillis());
		DOMUtils.createElementAndText(root, "Type", "12"); // USER_CREATED_EMAIL_FOLDER
		ResponseObject response = runEndpoint(document);
		assertEquals("Status should be 200", 200, response.getStatusCode());
		Buffer content = response.content;
		Document d = WBXMLTools.toXml(content.getBytes());

		Element status = DOMUtils.getUniqueElement(d.getDocumentElement(), "Status");
		assertEquals(FolderCreateResponse.Status.ParentFolderNotFound.xmlValue(), status.getTextContent());
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

		return sk.getTextContent();

	}

	@Override
	public WbxmlHandlerBase createEndpoint() {
		return new FolderCreateEndpoint();
	}

}
