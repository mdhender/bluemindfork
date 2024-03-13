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
package net.bluemind.dav.server.proto.report.carddav;

import java.util.List;

import javax.xml.namespace.QName;

import net.bluemind.addressbook.api.IVCardService;
import net.bluemind.addressbook.api.VCardInfo;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.dav.server.proto.report.ReportResponse;

public class AddressbookQueryResponse extends ReportResponse {

	private List<ItemValue<VCardInfo>> cards;
	private List<QName> props;
	private IVCardService service;

	public AddressbookQueryResponse(IVCardService service, String href, QName kind, List<ItemValue<VCardInfo>> cards,
			List<QName> props) {
		super(href, kind);
		this.cards = cards;
		this.props = props;
		this.service = service;
	}

	public List<ItemValue<VCardInfo>> getCards() {
		return cards;
	}

	public List<QName> getProps() {
		return props;
	}

	public IVCardService getIvCardService() {
		return service;
	}

}
