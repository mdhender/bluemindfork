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
package net.bluemind.eas.serdes.folderupdate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import net.bluemind.eas.dto.IPreviousRequestsKnowledge;
import net.bluemind.eas.dto.OptionalParams;
import net.bluemind.eas.dto.folderupdate.FolderUpdateRequest;
import net.bluemind.eas.serdes.IEasRequestParser;

public class FolderUpdateRequestParser implements IEasRequestParser<FolderUpdateRequest> {

	private static final Logger logger = LoggerFactory.getLogger(FolderUpdateRequestParser.class);

	@Override
	public FolderUpdateRequest parse(OptionalParams optParams, Document doc, IPreviousRequestsKnowledge past) {
		FolderUpdateRequest req = new FolderUpdateRequest();

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
			case "SyncKey":
				req.syncKey = child.getTextContent();
				break;
			case "ParentId":
				req.parentId = child.getTextContent();
				break;
			case "ServerId":
				req.serverId = child.getTextContent();
				break;
			case "DisplayName":
				req.displayName = child.getTextContent().trim();
				break;
			case "Type":
				req.type = Integer.parseInt(child.getTextContent());
				break;
			default:
				logger.warn("Not managed FolderCreate child {}", child);
				break;
			}
		}

		return req;
	}

}
