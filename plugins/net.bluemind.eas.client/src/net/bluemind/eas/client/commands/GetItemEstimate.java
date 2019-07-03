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

import org.w3c.dom.Element;

import net.bluemind.eas.client.AccountInfos;
import net.bluemind.eas.client.EstimateResponse;
import net.bluemind.eas.client.Folder;
import net.bluemind.eas.client.OPClient;
import net.bluemind.eas.utils.DOMUtils;

/**
 * Performs a GetEstimate on the given folder with 0 as syncKey
 * 
 * 
 */
public class GetItemEstimate extends TemplateBasedCommand<EstimateResponse> {

	protected Folder folder;

	public GetItemEstimate(Folder f) {
		super(NS.ItemEstimate, "GetItemEstimate",
				"GetItemEstimateOneColRequest.xml");
		this.folder = f;
	}

	@Override
	protected void customizeTemplate(AccountInfos ai, OPClient opc) {
		Element cols = DOMUtils.getUniqueElement(tpl.getDocumentElement(),
				"CollectionId");
		cols.setTextContent(folder.getServerId());
	}

	@Override
	protected EstimateResponse parseResponse(Element root) {

		return new EstimateResponse();
	}

}
