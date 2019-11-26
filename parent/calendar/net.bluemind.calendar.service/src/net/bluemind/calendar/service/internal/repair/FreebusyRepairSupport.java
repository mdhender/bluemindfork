/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.calendar.service.internal.repair;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.sql.DataSource;

import com.google.common.collect.Maps;

import net.bluemind.calendar.persistence.FreebusyStore;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.api.report.DiagnosticReport;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.DataSourceRouter;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.task.service.IServerTaskMonitor;

public class FreebusyRepairSupport {

	private final BmContext context;
	private final String parentReportId;

	public FreebusyRepairSupport(BmContext context, String parentReportId) {
		this.context = context;
		this.parentReportId = parentReportId;
	}

	public void check(String containerUid, DiagnosticReport report, IServerTaskMonitor monitor) throws SQLException {
		RepairContext ctx = RepairContext.create(context, containerUid);

		if (streamErrors(ctx).anyMatch(e -> e.getKey() == true)) {
			report.ko(parentReportId, String.format("Freebusy %s need a repair", containerUid));
		} else {
			report.ok(parentReportId, String.format("Freebusy %s is ok", containerUid));
		}
	}

	public void repair(String containerUid, DiagnosticReport report, IServerTaskMonitor monitor) throws SQLException {
		RepairContext ctx = RepairContext.create(context, containerUid);

		long repairCount = streamErrors(ctx).filter(e -> e.getKey() == true).map(e -> e.getValue()).map(e -> {
			try {
				ctx.store.remove(e);
			} catch (SQLException e1) {
				throw ServerFault.sqlFault(e1);
			}
			return true;
		}).count();

		if (repairCount > 0) {
			report.ok(parentReportId, String.format("Freebusy %s repaired", containerUid));
		} else {
			report.ok(parentReportId, String.format("Freebusy %s is ok", containerUid));
		}
	}

	private Stream<Map.Entry<Boolean, String>> streamErrors(RepairContext ctx) throws SQLException {
		List<String> all = ctx.store.get();

		ContainerStore cs = new ContainerStore(context, context.getDataSource(), context.getSecurityContext());
		return all.stream().map(uid -> {
			Container cal;
			try {
				cal = cs.get(uid);
			} catch (SQLException e) {
				throw ServerFault.sqlFault(e);
			}

			if (cal == null) {
				return Maps.immutableEntry(true, uid);
			}

			return Maps.immutableEntry(false, uid);
		});
	}

	private static class RepairContext {
		public final FreebusyStore store;

		public RepairContext(FreebusyStore store) {
			this.store = store;
		}

		public static RepairContext create(BmContext context, String containerUid) {
			DataSource ds = DataSourceRouter.get(context, containerUid);
			ContainerStore cStore = new ContainerStore(context, ds, context.getSecurityContext());
			Container container = cStore.doOrFail(() -> cStore.get(containerUid));
			FreebusyStore store = new FreebusyStore(ds, container);

			RepairContext ctx = new RepairContext(store);

			return ctx;
		}

	}
}
