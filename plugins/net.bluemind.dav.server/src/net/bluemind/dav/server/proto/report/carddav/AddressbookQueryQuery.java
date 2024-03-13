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

import net.bluemind.addressbook.api.VCardQuery;
import net.bluemind.dav.server.proto.report.ReportQuery;
import net.bluemind.dav.server.store.DavResource;

public class AddressbookQueryQuery extends ReportQuery {

	private final List<QName> props;
	private final List<Filter> filters;

	protected AddressbookQueryQuery(DavResource dr, QName kind, List<QName> props, List<Filter> filters) {
		super(dr, kind);
		this.props = props;
		this.filters = filters;
	}

	public List<QName> getProps() {
		return props;
	}

	public List<Filter> getFilters() {
		return filters;
	}

	public static interface Filter {

		void update(VCardQuery vq);

	}

	public static class PropFilter implements Filter {
		String name;
		String value;

		@Override
		public void update(VCardQuery vq) {
		}
	}

	public static class ParamFilter implements Filter {
		String name;
		String value;

		@Override
		public void update(VCardQuery vq) {
		}
	}

}
