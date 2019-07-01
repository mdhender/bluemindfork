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

import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.vertx.java.core.buffer.Buffer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import net.bluemind.eas.command.resolverecipients.ResolveRecipientsEndpoint;
import net.bluemind.eas.http.wbxml.WbxmlHandlerBase;
import net.bluemind.eas.testhelper.mock.ResponseObject;
import net.bluemind.eas.utils.DOMUtils;
import net.bluemind.eas.wbxml.WBXMLTools;

public class ResolveRecipientsEndpointTests extends AbstractEndpointTest {

	public void testResolveRecipients() throws Exception {
		Document document = DOMUtils.createDoc("ResolveRecipients", "ResolveRecipients");
		Element root = document.getDocumentElement();

		DOMUtils.createElementAndText(root, "To", owner.value.login + "@" + domainUid);

		Element options = DOMUtils.createElement(root, "Options");

		Element availability = DOMUtils.createElement(options, "Availability");

		// datetime YYYY-MM-DDTHH:MM:SS.MSSZ
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(0);
		DOMUtils.createElementAndText(availability, "StartTime", sdf.format(c.getTime()));
		c.add(Calendar.HOUR, 1);
		DOMUtils.createElementAndText(availability, "EndTime", sdf.format(c.getTime()));
		// Should be FREE for an hour

		ResponseObject response = runEndpoint(document);
		assertEquals("Status should be 200", 200, response.getStatusCode());

		Buffer content = response.content;
		Document d = WBXMLTools.toXml(content.getBytes());

		Element status = DOMUtils.getUniqueElement(d.getDocumentElement(), "Status");
		assertEquals("1", status.getTextContent());

		NodeList nl = d.getElementsByTagName("Response");
		assertEquals(1, nl.getLength());

		Element resp = (Element) nl.item(0);
		Element to = DOMUtils.getUniqueElement(resp, "To");
		assertEquals(owner.value.login + "@" + domainUid, to.getTextContent());

		Element responseStatus = DOMUtils.getUniqueElement(resp, "Status");
		assertEquals("1", responseStatus.getTextContent());

		Element recipCount = DOMUtils.getUniqueElement(resp, "RecipientCount");
		assertEquals("1", recipCount.getTextContent());

		nl = resp.getElementsByTagName("Recipient");
		assertEquals(1, nl.getLength());

		Element recip = (Element) nl.item(0);

		Element type = DOMUtils.getUniqueElement(recip, "Type");
		assertEquals("1", type.getTextContent());

		Element displayName = DOMUtils.getUniqueElement(recip, "DisplayName");
		assertEquals(owner.displayName, displayName.getTextContent());

		Element emailAddress = DOMUtils.getUniqueElement(recip, "EmailAddress");
		assertEquals(owner.value.login + "@" + domainUid, emailAddress.getTextContent());

		Element recipStatus = DOMUtils.getUniqueElement(recip, "Status");
		assertEquals("1", recipStatus.getTextContent());

		Element mergedFreeBusy = DOMUtils.getUniqueElement(recip, "MergedFreeBusy");
		assertEquals("00", mergedFreeBusy.getTextContent()); // free,free
	}

	public void testMultipleResolveRecipients() throws Exception {
		Document document = DOMUtils.createDoc("ResolveRecipients", "ResolveRecipients");
		Element root = document.getDocumentElement();

		DOMUtils.createElementAndText(root, "To", owner.value.login + "@" + domainUid);
		DOMUtils.createElementAndText(root, "To", owner.value.login + "@" + domainUid);

		Element options = DOMUtils.createElement(root, "Options");

		Element availability = DOMUtils.createElement(options, "Availability");

		// datetime YYYY-MM-DDTHH:MM:SS.MSSZ
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(0);
		DOMUtils.createElementAndText(availability, "StartTime", sdf.format(c.getTime()));
		c.add(Calendar.HOUR, 1);
		DOMUtils.createElementAndText(availability, "EndTime", sdf.format(c.getTime()));
		// Should be FREE for an hour

		ResponseObject response = runEndpoint(document);
		assertEquals("Status should be 200", 200, response.getStatusCode());

		Buffer content = response.content;
		Document d = WBXMLTools.toXml(content.getBytes());

		Element status = DOMUtils.getUniqueElement(d.getDocumentElement(), "Status");
		assertEquals("1", status.getTextContent());

		NodeList nl = d.getElementsByTagName("Response");
		assertEquals(2, nl.getLength());

		for (int i = 0; i < 2; i++) {
			Element resp = (Element) nl.item(i);
			Element to = DOMUtils.getUniqueElement(resp, "To");
			assertEquals(owner.value.login + "@" + domainUid, to.getTextContent());

			Element responseStatus = DOMUtils.getUniqueElement(resp, "Status");
			assertEquals("1", responseStatus.getTextContent());

			Element recipCount = DOMUtils.getUniqueElement(resp, "RecipientCount");
			assertEquals("1", recipCount.getTextContent());

			NodeList recipientList = resp.getElementsByTagName("Recipient");
			assertEquals(1, recipientList.getLength());

			Element recip = (Element) recipientList.item(0);

			Element type = DOMUtils.getUniqueElement(recip, "Type");
			assertEquals("1", type.getTextContent());

			Element displayName = DOMUtils.getUniqueElement(recip, "DisplayName");
			assertEquals(owner.displayName, displayName.getTextContent());

			Element emailAddress = DOMUtils.getUniqueElement(recip, "EmailAddress");
			assertEquals(owner.value.login + "@" + domainUid, emailAddress.getTextContent());

			Element recipStatus = DOMUtils.getUniqueElement(recip, "Status");
			assertEquals("1", recipStatus.getTextContent());

			Element mergedFreeBusy = DOMUtils.getUniqueElement(recip, "MergedFreeBusy");
			assertEquals("00", mergedFreeBusy.getTextContent()); // free,free
		}
	}

	@Override
	public WbxmlHandlerBase createEndpoint() {
		return new ResolveRecipientsEndpoint();
	}

}
