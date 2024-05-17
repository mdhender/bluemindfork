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
package net.bluemind.eas.client.commands;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import net.bluemind.eas.client.AccountInfos;
import net.bluemind.eas.client.Move;
import net.bluemind.eas.client.MoveItemsResponse;
import net.bluemind.eas.client.OPClient;
import net.bluemind.eas.utils.DOMUtils;

/**
 * Performs a MoveItems AS command for the given folders
 * 
 * 
 */
public class MoveItemsSync extends TemplateBasedCommand<MoveItemsResponse> {

	protected Move[] moves;

	public MoveItemsSync(Move... moves) {
		super(NS.Move, "MoveItems", "MoveItemsRequest.xml");
		this.moves = moves;
	}

	public MoveItemsSync(Document doc) {
		super(NS.Move, "MoveItems", doc);
	}

	@Override
	protected void customizeTemplate(AccountInfos ai, OPClient opc) {
		Element cols = DOMUtils.getUniqueElement(tpl.getDocumentElement(), "Move");
		for (Move folder : moves) {
			Element col = DOMUtils.createElement(cols, "Move");
			folder.setXml(col);
			DOMUtils.createElementAndText(col, "SrcMsgId", folder.getSrcMsgId());
			DOMUtils.createElementAndText(col, "SrcFldId", folder.getSrcFldId());
			DOMUtils.createElementAndText(col, "DstFldId", folder.getDstFldId());
		}
	}

	@Override
	protected MoveItemsResponse parseResponse(Element root) {
		List<Move> ret = new ArrayList<>();

		NodeList nl = root.getElementsByTagName("Move");
		for (int i = 0; i < nl.getLength(); i++) {
			Element e = (Element) nl.item(i);
			Move col = new Move();
			col.setXml(e);
			col.setSrcMsgId(DOMUtils.getElementText(e, "SrcMsgId"));
			col.setSrcFldId(DOMUtils.getElementText(e, "SrcFldId"));
			col.setDstFldId(DOMUtils.getElementText(e, "DstFldId"));
			ret.add(col);
		}

		MoveItemsResponse sr = new MoveItemsResponse(ret);
		sr.dom = root.getOwnerDocument();
		return sr;
	}

}
