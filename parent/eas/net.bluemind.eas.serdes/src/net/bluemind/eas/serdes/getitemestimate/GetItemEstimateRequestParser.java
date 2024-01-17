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
package net.bluemind.eas.serdes.getitemestimate;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import net.bluemind.eas.dto.IPreviousRequestsKnowledge;
import net.bluemind.eas.dto.OptionalParams;
import net.bluemind.eas.dto.getitemestimate.GetItemEstimateRequest;
import net.bluemind.eas.dto.getitemestimate.GetItemEstimateRequest.Collection;
import net.bluemind.eas.dto.sync.CollectionId;
import net.bluemind.eas.serdes.IEasRequestParser;

public class GetItemEstimateRequestParser implements IEasRequestParser<GetItemEstimateRequest> {

	private static final Logger logger = LoggerFactory.getLogger(GetItemEstimateRequestParser.class);

	@Override
	public GetItemEstimateRequest parse(OptionalParams optParams, Document doc, IPreviousRequestsKnowledge past) {
		GetItemEstimateRequest req = new GetItemEstimateRequest();

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
			case "Collections":
				req.collections = parseCollections(child);
				break;
			default:
				logger.warn("Not managed GetItemEstimate child {}", child);
				break;
			}
		}

		return req;
	}

	private List<Collection> parseCollections(Element el) {
		List<Collection> collections = new ArrayList<Collection>();

		NodeList children = el.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node n = children.item(i);
			if (n.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}
			Element child = (Element) n;
			String childName = child.getNodeName();

			switch (childName) {
			case "Collection":
				collections.add(parseCollection(child));
				break;
			default:
				logger.warn("Not managed GetItemEstimate.Collections child {}", child);
				break;
			}
		}

		return collections;
	}

	private Collection parseCollection(Element el) {
		Collection c = new Collection();

		NodeList children = el.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node n = children.item(i);
			if (n.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}
			Element child = (Element) n;
			String childName = child.getNodeName();

			switch (childName) {
			case "SyncKey":
				c.syncKey = child.getTextContent();
				break;
			case "CollectionId":
				c.collectionId = CollectionId.of(child.getTextContent());
				break;
			default:
				logger.warn("Not managed GetItemEstimate.Collections.Collection child {}", child);
				break;
			}
		}

		return c;
	}

}
