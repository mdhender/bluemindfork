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
package net.bluemind.eas.serdes.ping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import net.bluemind.eas.dto.IPreviousRequestsKnowledge;
import net.bluemind.eas.dto.OptionalParams;
import net.bluemind.eas.dto.ping.PingRequest;
import net.bluemind.eas.dto.ping.PingRequest.Folders;
import net.bluemind.eas.dto.ping.PingRequest.Folders.Folder;
import net.bluemind.eas.serdes.IEasRequestParser;

public class PingRequestParser implements IEasRequestParser<PingRequest> {
	private static final Logger logger = LoggerFactory.getLogger(PingRequestParser.class);

	@Override
	public PingRequest parse(OptionalParams optParams, Document doc, IPreviousRequestsKnowledge past) {
		if (doc != null) {
			PingRequest pr = new PingRequest();
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
				case "HeartbeatInterval":
					pr.heartbeatInterval = Integer.parseInt(child.getTextContent());
					break;
				case "Folders":
					pr.folders = parseFolders(child);
					break;
				default:
					logger.warn("Not managed Ping child {}", child);
					break;
				}
			}

			return pr;
		}

		// empty ping request
		return null;
	}

	private Folders parseFolders(Element el) {
		PingRequest.Folders folders = new PingRequest.Folders();

		NodeList children = el.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node n = children.item(i);
			if (n.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}
			Element child = (Element) n;
			String childName = child.getNodeName();

			switch (childName) {
			case "Folder":
				folders.folders.add(parseFolder(child));
				break;
			default:
				logger.warn("Not managed Ping.Folders child {}", child);
				break;
			}
		}
		return folders;
	}

	private Folder parseFolder(Element el) {
		PingRequest.Folders.Folder folder = new PingRequest.Folders.Folder();

		NodeList children = el.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node n = children.item(i);
			if (n.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}
			Element child = (Element) n;
			String childName = child.getNodeName();

			switch (childName) {
			case "Id":
				folder.id = child.getTextContent();
				break;
			case "Class":
				folder.clazz = PingRequest.Folders.Folder.Class.valueOf(child.getTextContent());
				break;
			default:
				logger.warn("Not managed Ping.Folders.Folder child {}", child);
				break;
			}
		}

		return folder;
	}

}
