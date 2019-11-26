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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.addressbook.service.internal.repair;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.sql.DataSource;

import com.google.common.collect.Maps;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.service.internal.VCardContainerStoreService;
import net.bluemind.addressbook.service.internal.VCardGroupSanitizer;
import net.bluemind.core.api.report.DiagnosticReport;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.DataSourceRouter;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.tag.service.TagsSanitizer;

public class ABRepairSupport {

	private final BmContext context;
	private final String parentReportId;

	public ABRepairSupport(BmContext context, String parentReportId) {
		this.context = context;
		this.parentReportId = parentReportId;
	}

	public void check(String abUid, DiagnosticReport report, IServerTaskMonitor monitor) {
		RepairContext ctx = RepairContext.create(context, abUid);

		if (streamErrors(ctx).anyMatch(e -> e.getKey() == true)) {
			report.ko(parentReportId, String.format("Addressbook %s need a repair", abUid));
		} else {
			report.ok(parentReportId, String.format("Addressbook %s is ok", abUid));
		}
	}

	public void repair(String abUid, DiagnosticReport report, IServerTaskMonitor monitor) {
		RepairContext ctx = RepairContext.create(context, abUid);
		long repairCount = streamErrors(ctx).filter(e -> e.getKey() == true).map(e -> e.getValue())
				.map(e -> ctx.vStore.update(e.uid, e.displayName, e.value)).count();

		if (repairCount > 0) {
			report.ok(parentReportId, String.format("Addressbook %s repaired", abUid));
		} else {
			report.ok(parentReportId, String.format("Addressbook %s is ok", abUid));
		}
	}

	private Stream<Map.Entry<Boolean, ItemValue<VCard>>> streamErrors(RepairContext ctx) {
		List<String> all = ctx.vStore.allUids();
		VCardGroupSanitizer groupSanitizer = new VCardGroupSanitizer(context.su());
		TagsSanitizer tagsSanitizer = new TagsSanitizer(context.su());

		return all.stream().map(uid -> ctx.vStore.get(uid, null)).map(gIv -> {
			boolean sanitized = groupSanitizer.sanitize(gIv.value)
					|| tagsSanitizer.sanitize(gIv.value.explanatory.categories);
			return Maps.immutableEntry(sanitized, gIv);
		});
	}

	private static class RepairContext {
		public final Container container;
		public final VCardContainerStoreService vStore;

		public RepairContext(Container container, VCardContainerStoreService vStore) {
			this.container = container;
			this.vStore = vStore;
		}

		public static RepairContext create(BmContext context, String abUid) {
			DataSource ds = DataSourceRouter.get(context, abUid);
			ContainerStore cStore = new ContainerStore(context, ds, context.getSecurityContext());
			Container container = ContainerStore.doOrFail(() -> cStore.get(abUid));
			VCardContainerStoreService vStore = new VCardContainerStoreService(context, ds,
					context.getSecurityContext(), container);
			RepairContext ctx = new RepairContext(container, vStore);

			return ctx;
		}

	}

}
