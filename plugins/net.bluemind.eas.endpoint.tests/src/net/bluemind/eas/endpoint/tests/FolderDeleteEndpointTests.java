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

import io.vertx.core.buffer.Buffer;
import net.bluemind.eas.command.folder.crud.FolderCreateEndpoint;
import net.bluemind.eas.command.folder.crud.FolderDeleteEndpoint;
import net.bluemind.eas.command.folder.sync.FolderSyncEndpoint;
import net.bluemind.eas.dto.folderdelete.FolderDeleteResponse;
import net.bluemind.eas.http.wbxml.WbxmlHandlerBase;
import net.bluemind.eas.testhelper.mock.ResponseObject;
import net.bluemind.eas.utils.DOMUtils;
import net.bluemind.eas.wbxml.WBXMLTools;

public class FolderDeleteEndpointTests extends AbstractEndpointTest {

	public void testDeleteFolder() throws IOException {

		String syncKey = initFolder();

		F f = createFolder(syncKey);

		Document document = DOMUtils.createDoc("FolderHierarchy", "FolderDelete");
		Element root = document.getDocumentElement();
		DOMUtils.createElementAndText(root, "SyncKey", f.syncKey);
		DOMUtils.createElementAndText(root, "ServerId", f.serverId);
		ResponseObject response = runEndpoint(document);
		assertEquals("Status should be 200", 200, response.getStatusCode());

		Buffer content = response.content;
		Document d = WBXMLTools.toXml(content.getBytes());

		Element status = DOMUtils.getUniqueElement(d.getDocumentElement(), "Status");
		assertEquals("1", status.getTextContent());

		Element sk = DOMUtils.getUniqueElement(d.getDocumentElement(), "SyncKey");
		assertNotNull(sk.getTextContent());

		assertFalse(sk.getTextContent().equals(f.syncKey));
	}

	public void testDeleteNonExistantFolder() throws IOException {

		Document document = DOMUtils.createDoc("FolderHierarchy", "FolderDelete");
		Element root = document.getDocumentElement();
		DOMUtils.createElementAndText(root, "SyncKey", "0");
		DOMUtils.createElementAndText(root, "ServerId", "913467788");
		ResponseObject response = runEndpoint(document);
		assertEquals("Status should be 200", 200, response.getStatusCode());

		Buffer content = response.content;
		Document d = WBXMLTools.toXml(content.getBytes());

		Element status = DOMUtils.getUniqueElement(d.getDocumentElement(), "Status");
		assertEquals(FolderDeleteResponse.Status.DoesNotExist.xmlValue(), status.getTextContent());

	}

	public void testDeleteNonExistantFolderWithServerIdAsString() throws IOException {

		Document document = DOMUtils.createDoc("FolderHierarchy", "FolderDelete");
		Element root = document.getDocumentElement();
		DOMUtils.createElementAndText(root, "SyncKey", "0");
		DOMUtils.createElementAndText(root, "ServerId", "PierrotLeFolder");
		ResponseObject response = runEndpoint(document);
		assertEquals("Status should be 200", 200, response.getStatusCode());

		Buffer content = response.content;
		Document d = WBXMLTools.toXml(content.getBytes());

		Element status = DOMUtils.getUniqueElement(d.getDocumentElement(), "Status");
		assertEquals(FolderDeleteResponse.Status.InvalidRequest.xmlValue(), status.getTextContent());
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

	public static final class F {
		public String syncKey;
		public String serverId;
	}

	@Override
	public WbxmlHandlerBase createEndpoint() {
		return new FolderDeleteEndpoint();
	}

}
