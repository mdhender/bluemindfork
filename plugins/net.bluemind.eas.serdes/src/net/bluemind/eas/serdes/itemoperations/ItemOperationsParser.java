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
package net.bluemind.eas.serdes.itemoperations;

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
import net.bluemind.eas.dto.itemoperations.ItemOperationsRequest;
import net.bluemind.eas.dto.itemoperations.ItemOperationsRequest.ItemOperation;
import net.bluemind.eas.dto.itemoperations.ResponseStyle;
import net.bluemind.eas.serdes.IEasRequestParser;
import net.bluemind.eas.serdes.base.BodyOptionsParser;
import net.bluemind.eas.serdes.base.RangeParser;

public class ItemOperationsParser implements IEasRequestParser<ItemOperationsRequest> {
	private static final Logger logger = LoggerFactory.getLogger(ItemOperationsParser.class);

	private RangeParser rangeParser = new RangeParser();

	@Override
	public ItemOperationsRequest parse(OptionalParams optParams, Document doc, IPreviousRequestsKnowledge past) {
		ItemOperationsRequest request = new ItemOperationsRequest();
		if ("T".equals(optParams.acceptMultiPart())) {
			request.style = ResponseStyle.Multipart;
			if ("gzip".equals(optParams.acceptEncoding())) {
				request.gzip = true;
			}
		}

		Element elements = doc.getDocumentElement();
		NodeList ops = elements.getChildNodes();

		request.itemOperations = new ArrayList<>(ops.getLength());

		for (int i = 0; i < ops.getLength(); i++) {
			Node node = ops.item(i);
			if (node.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}

			Element op = (Element) node;
			switch (op.getNodeName()) {
			case "EmptyFolderContents":
				appendEmptyFolderContents(request.itemOperations, op);
				break;
			case "Fetch":
				appendFetch(request.itemOperations, op);
				break;
			case "Move":
				appendMove(request.itemOperations, op);
				break;
			default:
				logger.warn("Operation {} not supported ", op.getNodeName());
			}
		}

		return request;
	}

	private void appendEmptyFolderContents(List<ItemOperation> itemOperations, Element opElt) {

		ItemOperationsRequest.EmptyFolderContents op = new ItemOperationsRequest.EmptyFolderContents();
		NodeList childs = opElt.getChildNodes();
		for (int i = 0; i < childs.getLength(); i++) {
			Node node = childs.item(i);
			if (node.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}

			Element elt = (Element) node;
			switch (elt.getNodeName()) {
			case "CollectionId":
				op.collectionId = elt.getTextContent();
				break;
			case "Options":
				op.options = parseEmptyFolderContentsOptions(elt);
			default:
				logger.warn("EmptyFolderContents element {} not supported ", elt.getNodeName());
				break;
			}
		}

		if (op.collectionId != null) {
			itemOperations.add(op);
		}

	}

	private ItemOperationsRequest.EmptyFolderContents.Options parseEmptyFolderContentsOptions(Element optElt) {
		ItemOperationsRequest.EmptyFolderContents.Options ret = new ItemOperationsRequest.EmptyFolderContents.Options();

		NodeList childs = optElt.getChildNodes();
		for (int i = 0; i < childs.getLength(); i++) {
			Node node = childs.item(i);
			if (node.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}
			Element elt = (Element) node;

			switch (elt.getNodeName()) {
			case "DeleteSubFolders":
				ret.deleteSubFolders = true;
				break;

			default:
				logger.warn("EmptyFolderContents.Options element {} not supported ", elt.getNodeName());
				break;
			}
		}

		return ret;
	}

	private void appendFetch(List<ItemOperation> itemOperations, Element opElt) {
		ItemOperationsRequest.Fetch op = new ItemOperationsRequest.Fetch();

		NodeList childs = opElt.getChildNodes();
		for (int i = 0; i < childs.getLength(); i++) {
			Node node = childs.item(i);
			if (node.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}

			Element elt = (Element) node;
			switch (elt.getNodeName()) {
			case "Store":
				op.store = elt.getTextContent().toLowerCase();
				break;
			case "ServerId":
				op.serverId = elt.getTextContent();
				break;
			case "CollectionId":
				op.collectionId = elt.getTextContent();
				break;
			case "LinkId":
				op.linkId = elt.getTextContent();
				break;
			case "LongId":
				op.longId = elt.getTextContent();
				break;
			case "FileReference":
				op.fileReference = elt.getTextContent();
				break;

			case "Options":
				op.options = parseFetchOptions(elt);
				break;
			default:
				logger.warn("EmptyFolderContents element {} not supported ", elt.getNodeName());
				break;
			}
		}

		itemOperations.add(op);
	}

	private ItemOperationsRequest.Fetch.Options parseFetchOptions(Element optionsElt) {
		ItemOperationsRequest.Fetch.Options ret = new ItemOperationsRequest.Fetch.Options();
		NodeList childs = optionsElt.getChildNodes();
		BodyOptionsParser bop = new BodyOptionsParser();
		ret.bodyOptions = bop.fromOptionsElement(optionsElt);
		for (int i = 0; i < childs.getLength(); i++) {
			Node node = childs.item(i);
			if (node.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}

			Element elt = (Element) node;
			switch (elt.getNodeName()) {
			case "Schema":
				ret.schema = elt;
				break;
			case "Range":
				ret.range = rangeParser.parse(elt);
				break;
			case "UserName":
				ret.userName = elt.getTextContent();
				break;
			case "Password":
				ret.password = elt.getTextContent();
				break;
			default:
				logger.warn("element {} not supported ", elt.getNodeName());
				break;
			}
		}

		return ret;
	}

	private void appendMove(List<ItemOperation> itemOperations, Element opElt) {
		ItemOperationsRequest.Move op = new ItemOperationsRequest.Move();

		NodeList childs = opElt.getChildNodes();
		for (int i = 0; i < childs.getLength(); i++) {
			Node node = childs.item(i);
			if (node.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}

			Element elt = (Element) node;
			switch (elt.getNodeName()) {
			case "ConversationId":
				op.conversationId = elt.getTextContent();
				break;
			case "DstFldId":
				op.dstFldId = elt.getTextContent();
				break;
			case "Options":
				op.options = parseMoveOptions(elt);
				break;
			default:
				logger.warn("Move element {} not supported ", elt.getNodeName());
				break;
			}
		}
		itemOperations.add(op);

	}

	private ItemOperationsRequest.Move.Options parseMoveOptions(Element optionsElt) {
		ItemOperationsRequest.Move.Options ret = new ItemOperationsRequest.Move.Options();

		NodeList childs = optionsElt.getChildNodes();
		for (int i = 0; i < childs.getLength(); i++) {
			Node node = childs.item(i);
			if (node.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}

			Element elt = (Element) node;
			switch (elt.getNodeName()) {
			case "MoveAlways":
				ret.moveAlways = true;
				break;

			default:
				logger.warn("Move.Optio,ns element {} not supported ", elt.getNodeName());
				break;
			}
		}
		return ret;
	}
}
