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
import java.util.Collection;
import java.util.UUID;

import org.vertx.java.core.buffer.Buffer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import net.bluemind.eas.command.folder.sync.FolderSyncEndpoint;
import net.bluemind.eas.command.mail.sendmail.SendMailEndpoint;
import net.bluemind.eas.command.mail.smartreply.SmartReplyEndpoint;
import net.bluemind.eas.dto.sendmail.SendMailResponse;
import net.bluemind.eas.http.wbxml.WbxmlHandlerBase;
import net.bluemind.eas.testhelper.mock.ResponseObject;
import net.bluemind.eas.utils.DOMUtils;
import net.bluemind.eas.wbxml.WBXMLTools;
import net.bluemind.imap.SearchQuery;
import net.bluemind.imap.StoreClient;

public class SmartReplyEndpointTests extends AbstractEndpointTest {

	public void testSmartReply() throws Exception {
		Integer inbox = fetchInboxId();
		Integer uid = sendMail();

		Document document = DOMUtils.createDoc("ComposeMail", "SmartReply");
		Element root = document.getDocumentElement();

		String clientId = UUID.randomUUID().toString();

		DOMUtils.createElementAndText(root, "ClientId", clientId);
		DOMUtils.createElement(root, "SaveInSentItems");
		DOMUtils.createElement(root, "ReplaceMime");

		Element source = DOMUtils.createElement(root, "Source");
		DOMUtils.createElementAndText(source, "FolderId", inbox.toString());
		DOMUtils.createElementAndText(source, "ItemId", inbox.toString() + ":" + uid.toString());

		StringBuilder sb = new StringBuilder();
		sb.append("Content-Type: text/plain;\n");
		sb.append("	charset=utf8\n");
		sb.append("Content-Transfer-Encoding: base64\n");
		sb.append("Subject: Re: xxx\n");
		sb.append("References: <cmu-lmtpd-7807-1444651372-0@trusty>\n"); // osef?
		sb.append("From: " + owner.value.defaultEmail().address + "\n");
		sb.append("In-Reply-To: <cmu-lmtpd-7807-1444651372-0@trusty>\n");// osef?
		sb.append("Message-Id <" + clientId + "@bm.lan>\n");
		sb.append("Date: Mon, 12 Oct 2015 12:01:56 +0200\n");
		sb.append("To: " + owner.value.defaultEmail().address + "\n");
		sb.append("Mime-Version: 1.0 (1.0)\n");
		sb.append("\n");
		sb.append("QmFyDQoNCkVudm95w6kgZGUgbW9uIGlQYWQ=");

		DOMUtils.createElementAndText(root, "Mime",
				java.util.Base64.getEncoder().encodeToString(sb.toString().getBytes()));

		ResponseObject response = runEndpoint(document);
		assertEquals("Status should be 200", 200, response.getStatusCode());
		// success = empty response
		assertEquals(0, response.content.getBytes().length);

		response = runEndpoint(document);
		assertEquals("Status should be 200", 200, response.getStatusCode());
		Buffer content = response.content;
		Document d = WBXMLTools.toXml(content.getBytes());

		assertEquals("SmartReply", d.getDocumentElement().getNodeName());
		Element status = DOMUtils.getUniqueElement(d.getDocumentElement(), "Status");
		assertEquals(SendMailResponse.Status.PreviouslySent.xmlValue(), status.getTextContent());

	}

	private Integer fetchInboxId() throws IOException {
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
		for (int i = 0; i < added.getLength(); i++) {
			Element el = (Element) added.item(i);
			String folderType = DOMUtils.getUniqueElement(el, "Type").getTextContent();
			switch (folderType) {
			case "2":
				inboxId = Integer.parseInt(DOMUtils.getUniqueElement(el, "ServerId").getTextContent());
				break;
			}
		}
		assertNotNull(inboxId);
		assertTrue(inboxId > 0);
		return inboxId;
	}

	private Integer sendMail() throws Exception {
		Document document = DOMUtils.createDoc("ComposeMail", "SendMail");
		Element root = document.getDocumentElement();

		String clientId = UUID.randomUUID().toString();
		String subject = "mail" + System.currentTimeMillis();

		DOMUtils.createElementAndText(root, "ClientId", clientId);
		DOMUtils.createElement(root, "SaveInSentItems");

		StringBuilder sb = new StringBuilder();
		sb.append("Content-Type: text/plain;\n");
		sb.append("	charset=utf8\n");
		sb.append("Content-Transfer-Encoding: base64\n");
		sb.append("Subject: " + subject + "\n");
		sb.append("From: " + owner.value.defaultEmail().address + "\n");
		sb.append("Message-Id <" + clientId + "@bm.lan>\n");
		sb.append("Date: Mon, 12 Oct 2015 12:01:56 +0200\n");
		sb.append("To: " + owner.value.defaultEmail().address + "\n");
		sb.append("Mime-Version: 1.0 (1.0)\n");
		sb.append("\n");
		sb.append("QmFyDQoNCkVudm95w6kgZGUgbW9uIGlQYWQ=");

		DOMUtils.createElementAndText(root, "Mime",
				java.util.Base64.getEncoder().encodeToString(sb.toString().getBytes()));
		ResponseObject response = runEndpoint(new SendMailEndpoint(), document);
		assertEquals("Status should be 200", 200, response.getStatusCode());
		assertEquals(0, response.content.getBytes().length);

		Thread.sleep(1000);

		try (StoreClient sc = new StoreClient(vmHostname, 1143, login, password)) {
			assertTrue(sc.login());
			sc.select("INBOX");
			SearchQuery sq = new SearchQuery();
			sq.setSubject(subject);
			Collection<Integer> res = sc.uidSearch(sq);
			assertEquals(1, res.size());
			return (Integer) res.iterator().next();
		} catch (Exception e) {
			fail(e.getMessage());
		}
		return null;
	}

	@Override
	public WbxmlHandlerBase createEndpoint() {
		return new SmartReplyEndpoint();
	}

}
