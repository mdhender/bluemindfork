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
package net.bluemind.calendar.service.internal.repair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.sql.DataSource;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.calendar.persistence.VEventIndexStore;
import net.bluemind.calendar.service.internal.VEventContainerStoreService;
import net.bluemind.core.api.report.DiagnosticReport;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.DataSourceRouter;
import net.bluemind.core.container.service.ContainerSettings;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.lib.elasticsearch.ESearchActivator;
import net.bluemind.tag.service.TagsSanitizer;

public class CalendarRepairSupport {

	private final BmContext context;
	private final String parentReportId;
	private final TagsSanitizer tagsSanitizer;

	public CalendarRepairSupport(BmContext context, String parentReportId) {
		this.context = context;
		this.parentReportId = parentReportId;
		tagsSanitizer = new TagsSanitizer(context.su());

	}

	public void check(String calUid, DiagnosticReport report, IServerTaskMonitor monitor) {
		RepairContext ctx = RepairContext.create(context, calUid);

		boolean settingsOk = checkAndRepairSettings(ctx, false);

		if (streamErrors(ctx).anyMatch(e -> e.getKey() == true) || !settingsOk) {
			report.ko(parentReportId, String.format("Calendar %s need a repair", calUid));
		} else {
			report.ok(parentReportId, String.format("Calendar %s is ok", calUid));
		}
	}

	public void repair(String calUid, DiagnosticReport report, IServerTaskMonitor monitor) {
		RepairContext ctx = RepairContext.create(context, calUid);
		long repairCount = streamErrors(ctx).filter(e -> e.getKey() == true).map(e -> e.getValue())
				.map(e -> ctx.vStore.update(e.uid, e.displayName, e.value)).count();

		boolean settingsOk = checkAndRepairSettings(ctx, true);

		if (repairCount > 0 || !settingsOk) {
			report.ok(parentReportId, String.format("Calendar %s repaired", calUid));
		} else {
			report.ok(parentReportId, String.format("Calendar %s is ok", calUid));
		}

		reindex(ctx);
		report.ok(parentReportId, String.format("Calendar %s reindexed", calUid));
	}

	private boolean checkAndRepairSettings(RepairContext ctx, boolean repair) {

		boolean ok = true;

		ContainerSettings service = new ContainerSettings(context, ctx.container);
		Map<String, String> settings = service.get();

		if (settings == null) {
			settings = new HashMap<String, String>();
		}

		if (!settings.containsKey("calendar.workingDays")) {
			settings.put("calendar.workingDays", "MO,TU,WE,TH,FR");
			ok = false;
		}

		if (!settings.containsKey("calendar.minDuration")) {
			settings.put("calendar.minDuration", "60");
			ok = false;
		}

		if (!settings.containsKey("calendar.dayStart")) {
			settings.put("calendar.dayStart", "08:00");
			ok = false;
		}

		if (!settings.containsKey("calendar.dayEnd")) {
			settings.put("calendar.dayEnd", "18:00");
			ok = false;
		}

		if (!settings.containsKey("calendar.timezone")) {
			settings.put("calendar.timezone", "Europe/Paris");
			ok = false;
		}

		if (repair && !ok) {
			service.set(settings);
		}

		return ok;
	}

	private void reindex(RepairContext ctx) {
		List<String> all = ctx.vStore.allUids();
		ctx.indexStore.deleteAll();
		Lists.partition(all, 500).forEach(uids -> {
			List<ItemValue<VEventSeries>> values = ctx.vStore.getMultiple(uids);
			ctx.indexStore.updates(values);
		});
	}

	private Stream<Map.Entry<Boolean, ItemValue<VEventSeries>>> streamErrors(RepairContext ctx) {
		List<String> all = ctx.vStore.allUids();

		return all.stream().map(uid -> ctx.vStore.get(uid, null)).map(eIv -> {
			boolean sanitized = sanitize(eIv);
			return Maps.immutableEntry(sanitized, eIv);
		});
	}

	private boolean sanitize(ItemValue<VEventSeries> series) {
		// FIXME sanitize attendee/organiser ?
		boolean modified = false;
		if (series.value.main != null) {
			modified |= tagsSanitizer.sanitize(series.value.main.categories);
		}

		if (series.value.occurrences != null) {
			modified |= series.value.occurrences.stream().map(occ -> tagsSanitizer.sanitize(occ.categories))
					.reduce(false, (a, b) -> a || b);
		}

		return modified;
	}

	private static class RepairContext {
		public final Container container;
		public final VEventContainerStoreService vStore;
		public final VEventIndexStore indexStore;

		public RepairContext(Container container, VEventContainerStoreService vStore) {
			this.container = container;
			this.vStore = vStore;
			this.indexStore = new VEventIndexStore(ESearchActivator.getClient(), container);
		}

		public static RepairContext create(BmContext context, String calUid) {
			DataSource ds = DataSourceRouter.get(context, calUid);
			ContainerStore cStore = new ContainerStore(context, ds, context.getSecurityContext());
			Container container = cStore.doOrFail(() -> cStore.get(calUid));
			VEventContainerStoreService vStore = new VEventContainerStoreService(context, ds,
					context.getSecurityContext(), container);
			RepairContext ctx = new RepairContext(container, vStore);

			return ctx;
		}

	}

}
