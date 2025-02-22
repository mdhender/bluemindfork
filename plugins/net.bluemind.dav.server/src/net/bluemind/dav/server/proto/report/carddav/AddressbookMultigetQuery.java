/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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

import net.bluemind.dav.server.proto.report.ReportQuery;
import net.bluemind.dav.server.store.DavResource;

public class AddressbookMultigetQuery extends ReportQuery {

	private List<QName> props;
	private List<String> hrefs;

	protected AddressbookMultigetQuery(DavResource dr, QName root, List<QName> props, List<String> hrefs) {
		super(dr, root);
		this.props = props;
		this.hrefs = hrefs;
	}

	public List<QName> getProps() {
		return props;
	}

	public List<String> getHrefs() {
		return hrefs;
	}

}
