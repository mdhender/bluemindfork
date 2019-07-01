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
package net.bluemind.dav.server.proto.report.caldav;

import java.util.List;

import javax.xml.namespace.QName;

import net.bluemind.calendar.api.VEventQuery;
import net.bluemind.core.api.date.BmDateTime;
import net.bluemind.dav.server.proto.report.ReportQuery;
import net.bluemind.dav.server.store.DavResource;

public class CalendarQueryQuery extends ReportQuery {

	public static interface Filter {

		void update(VEventQuery vq);

	}

	public static class CompFilter implements Filter {
		String name;

		@Override
		public void update(VEventQuery vq) {
		}
	}

	public static class TimeRangeFilter implements Filter {

		BmDateTime start;
		BmDateTime end;

		@Override
		public void update(VEventQuery vq) {
			vq.dateMin = start;
			vq.dateMax = end;
		}

	}

	private List<QName> props;
	private List<Filter> toMatch;

	protected CalendarQueryQuery(DavResource dr, QName root, List<QName> props, List<Filter> toMatch) {
		super(dr, root);
		this.props = props;
		this.toMatch = toMatch;
	}

	public List<QName> getProps() {
		return props;
	}

	public List<Filter> getToMatch() {
		return toMatch;
	}

}
