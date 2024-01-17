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
package net.bluemind.eas.serdes.moveitems;

import java.util.LinkedList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import net.bluemind.eas.dto.IPreviousRequestsKnowledge;
import net.bluemind.eas.dto.OptionalParams;
import net.bluemind.eas.dto.moveitems.MoveItemsRequest;
import net.bluemind.eas.dto.moveitems.MoveItemsRequest.Move;
import net.bluemind.eas.serdes.IEasRequestParser;

public class MoveItemsParser implements IEasRequestParser<MoveItemsRequest> {
	private static final Logger logger = LoggerFactory.getLogger(MoveItemsParser.class);

	@Override
	public MoveItemsRequest parse(OptionalParams optParams, Document doc, IPreviousRequestsKnowledge past) {
		MoveItemsRequest request = new MoveItemsRequest();
		request.moveItems = new LinkedList<>();

		Element elements = doc.getDocumentElement();
		NodeList moves = elements.getChildNodes();

		for (int i = 0; i < moves.getLength(); i++) {
			Node node = moves.item(i);
			if (node.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}

			Element mv = (Element) node;

			if (!mv.getNodeName().equals("Move")) {
				logger.warn("Not managed MoveItems child {}", mv);
				continue;
			}

			request.moveItems.add(parseMove(mv));
		}

		return request;
	}

	private Move parseMove(Element mv) {
		Move m = new MoveItemsRequest.Move();

		NodeList childs = mv.getChildNodes();
		for (int i = 0; i < childs.getLength(); i++) {
			Node node = childs.item(i);
			if (node.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}

			Element child = (Element) node;
			String childName = child.getNodeName();
			switch (childName) {
			case "SrcMsgId":
				m.srcMsgId = child.getTextContent();
				break;
			case "SrcFldId":
				m.srcFldId = child.getTextContent();
				break;
			case "DstFldId":
				m.dstFldId = child.getTextContent();
				break;
			default:
				logger.warn("Not managed of Move child: '{}'", child);
				break;
			}
		}
		return m;

	}

}
