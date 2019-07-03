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
package net.bluemind.dav.server.proto;

import org.vertx.java.core.MultiMap;

public class DavHeaders {

	public static final String DAV_CAPS = "1, access-control, calendar-access, calendar-schedule, "
			+ "calendar-auto-schedule, calendar-availability, inbox-availability, calendar-proxy, "
			+ "calendarserver-private-events, calendarserver-private-comments, calendarserver-sharing, "
			+ "calendarserver-sharing-no-scheduling, calendar-query-extended, calendar-default-alarms, "
			// + "calendar-managed-attachments, "
			+ "calendarserver-partstat-changes, extended-mkcol, "
			+ "calendarserver-principal-property-search, calendarserver-principal-search, calendarserver-home-sync, addressbook";

	public static <T extends DavQuery> T parse(T query, MultiMap headers) {
		boolean brief = "t".equals(headers.get("Brief"));
		query.setBrief(brief);

		Depth depth = Depth.fromHeader(headers.get("Depth"));
		query.setDepth(depth);

		query.setPrefer(headers.get("Prefer"));

		return query;
	}

}
