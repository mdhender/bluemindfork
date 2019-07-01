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
package net.bluemind.eas.serdes.smartreply;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import net.bluemind.eas.dto.IPreviousRequestsKnowledge;
import net.bluemind.eas.dto.OptionalParams;
import net.bluemind.eas.dto.smartreply.SmartReplyRequest;
import net.bluemind.eas.dto.smartreply.SmartReplyRequest.Source;
import net.bluemind.eas.serdes.IEasRequestParser;

public class SmartReplyRequestParser implements IEasRequestParser<SmartReplyRequest> {

	private static final Logger logger = LoggerFactory.getLogger(SmartReplyRequestParser.class);

	@Override
	public SmartReplyRequest parse(OptionalParams optParams, Document doc, IPreviousRequestsKnowledge past) {
		SmartReplyRequest req = new SmartReplyRequest();

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
			case "Source":
				req.source = parseSource(child);
				break;
			case "AccountId":
				req.accountId = child.getTextContent();
				break;
			case "SaveInSentItems":
				req.saveInSentItems = true;
				break;
			case "ReplaceMime":
				req.replaceMime = true;
				break;
			case "Mime":
				req.mime = child.getTextContent();
				break;
			default:
				logger.warn("Not managed SmartReply child {}", child);
				break;
			}
		}

		return req;
	}

	private Source parseSource(Element el) {
		Source source = new Source();

		NodeList children = el.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node n = children.item(i);
			if (n.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}
			Element child = (Element) n;
			String childName = child.getNodeName();

			switch (childName) {
			case "FolderId":
				source.folderId = child.getTextContent();
				break;
			case "ItemId":
				source.itemId = child.getTextContent();
				break;
			case "LongId":
				source.longId = child.getTextContent();
				break;
			case "InstanceId":
				source.instanceId = child.getTextContent();
				break;
			default:
				logger.warn("Not managed SmartReply.Source child {}", child);
				break;
			}
		}

		return source;
	}

}
