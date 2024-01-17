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
package net.bluemind.eas.serdes.sendmail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import net.bluemind.eas.dto.IPreviousRequestsKnowledge;
import net.bluemind.eas.dto.OptionalParams;
import net.bluemind.eas.dto.sendmail.SendMailRequest;
import net.bluemind.eas.serdes.IEasRequestParser;

public class SendMailRequestParser implements IEasRequestParser<SendMailRequest> {

	private static final Logger logger = LoggerFactory.getLogger(SendMailRequestParser.class);

	@Override
	public SendMailRequest parse(OptionalParams optParams, Document doc, IPreviousRequestsKnowledge past) {
		SendMailRequest req = new SendMailRequest();

		Element elements = doc.getDocumentElement();
		NodeList children = elements.getChildNodes();

		for (int i = 0; i < children.getLength(); i++) {
			Node n = children.item(i);
			if (n.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}

			Element child = (Element) n;
			String childName = child.getNodeName();
			switch (childName) {
			case "ClientId":
				req.clientId = child.getTextContent();
				break;
			case "AccountId":
				req.accountId = child.getTextContent();
				break;
			case "SaveInSentItems":
				// The SaveInSentItems element is an empty tag element, meaning
				// it has no value or data type. It is distinguished only by the
				// presence or absence of the <SaveInSentItems/> tag.
				req.saveInSentItems = true;
				break;
			case "Mime":
				req.mime = child.getTextContent();
				break;
			default:
				logger.warn("Not managed SendMail child {}", child);
				break;
			}
		}

		return req;
	}

}
