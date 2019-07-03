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

import javax.xml.namespace.QName;

import net.bluemind.dav.server.proto.NS;
import net.bluemind.dav.server.proto.QN;

public final class CDReports {

	public static final QName CALENDAR_QUERY = QN.qn(NS.CALDAV, "calendar-query");

	public static final QName CALENDAR_MULTIGET = QN.qn(NS.CALDAV, "calendar-multiget");

	public static final QName FREE_BUSY_QUERY = QN.qn(NS.CALDAV, "free-busy-query");

}
