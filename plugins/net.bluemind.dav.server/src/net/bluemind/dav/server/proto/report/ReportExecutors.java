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
package net.bluemind.dav.server.proto.report;

import java.util.concurrent.ConcurrentHashMap;

import javax.xml.namespace.QName;

import net.bluemind.dav.server.proto.report.caldav.CalendarMultigetExecutor;
import net.bluemind.dav.server.proto.report.caldav.CalendarQueryExecutor;
import net.bluemind.dav.server.proto.report.calendarserver.CalendarServerPrincipalSearchExecutor;
import net.bluemind.dav.server.proto.report.webdav.ExpandPropertyExecutor;
import net.bluemind.dav.server.proto.report.webdav.PrincipalPropertySearchExecutor;
import net.bluemind.dav.server.proto.report.webdav.PrincipalSearchPropertySetExecutor;
import net.bluemind.dav.server.proto.report.webdav.SyncCollectionExecutor;

public class ReportExecutors {

	private static final ConcurrentHashMap<QName, IReportExecutor> rexecs;

	static {
		rexecs = new ConcurrentHashMap<>();
		reg(new ExpandPropertyExecutor());
		reg(new PrincipalPropertySearchExecutor());
		reg(new PrincipalSearchPropertySetExecutor());
		reg(new SyncCollectionExecutor());
		reg(new CalendarMultigetExecutor());
		reg(new CalendarQueryExecutor());
		reg(new CalendarServerPrincipalSearchExecutor());
	}

	private static void reg(IReportExecutor re) {
		rexecs.put(re.getKind(), re);
	}

	public static final IReportExecutor get(QName reportRoot) {
		IReportExecutor re = rexecs.get(reportRoot);
		if (re == null) {
			throw new RuntimeException("No report implementation for " + reportRoot);
		}
		return re;
	}

}
