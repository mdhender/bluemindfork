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

import org.vertx.java.core.buffer.Buffer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import net.bluemind.eas.command.mail.sendmail.SendMailEndpoint;
import net.bluemind.eas.dto.sendmail.SendMailResponse;
import net.bluemind.eas.http.wbxml.WbxmlHandlerBase;
import net.bluemind.eas.testhelper.mock.ResponseObject;
import net.bluemind.eas.utils.DOMUtils;
import net.bluemind.eas.wbxml.WBXMLTools;

public class SendMailEndpointTests extends AbstractEndpointTest {

	public void testSendMail() throws IOException {
		Document document = DOMUtils.createDoc("ComposeMail", "SendMail");
		Element root = document.getDocumentElement();

		String clientId = UUID.randomUUID().toString();

		DOMUtils.createElementAndText(root, "ClientId", clientId);
		DOMUtils.createElement(root, "SaveInSentItems");

		StringBuilder sb = new StringBuilder();
		sb.append("Content-Type: text/plain;\n");
		sb.append("	charset=utf8\n");
		sb.append("Content-Transfer-Encoding: base64\n");
		sb.append("Subject: mail" + System.currentTimeMillis() + "\n");
		sb.append("From: " + owner.value.defaultEmail().address + "\n");
		sb.append("Message-Id <" + clientId + "@bm.lan>\n");
		sb.append("Date: Mon, 12 Oct 2015 12:01:56 +0200\n");
		sb.append("To: " + owner.value.defaultEmail().address + "\n");
		sb.append("Mime-Version: 1.0 (1.0)\n");
		sb.append("\n");
		sb.append("QmFyDQoNCkVudm95w6kgZGUgbW9uIGlQYWQ=");

		DOMUtils.createElementAndText(root, "Mime",
				new String(java.util.Base64.getEncoder().encode(sb.toString().getBytes())));
		ResponseObject response = runEndpoint(document);
		assertEquals("Status should be 200", 200, response.getStatusCode());
		// success = empty response
		assertEquals(0, response.content.getBytes().length);

		response = runEndpoint(document);
		assertEquals("Status should be 200", 200, response.getStatusCode());
		Buffer content = response.content;
		Document d = WBXMLTools.toXml(content.getBytes());

		Element status = DOMUtils.getUniqueElement(d.getDocumentElement(), "Status");
		assertEquals(SendMailResponse.Status.PreviouslySent.xmlValue(), status.getTextContent());

	}

	@Override
	public WbxmlHandlerBase createEndpoint() {
		return new SendMailEndpoint();
	}

}
