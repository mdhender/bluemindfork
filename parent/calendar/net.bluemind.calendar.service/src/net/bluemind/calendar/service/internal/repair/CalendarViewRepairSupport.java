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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableSet;

import net.bluemind.calendar.api.CalendarView;
import net.bluemind.calendar.api.IUserCalendarViews;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.api.report.DiagnosticReport;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.MaintenanceOperation;
import net.bluemind.directory.service.IDirEntryRepairSupport;

public class CalendarViewRepairSupport implements IDirEntryRepairSupport {

	private final BmContext context;
	private static final Logger logger = LoggerFactory.getLogger(CalendarViewRepairSupport.class);
	private static final MaintenanceOperation MAINTENANCE_OPERATION = MaintenanceOperation.create("calendarview",
			"Calendarview reparation");

	public static class Factory implements IDirEntryRepairSupport.Factory {
		@Override
		public IDirEntryRepairSupport create(BmContext context) {
			return new CalendarViewRepairSupport(context);
		}
	}

	public CalendarViewRepairSupport(BmContext context) {
		this.context = context;
	}

	public static class CalendarViewMaintenance extends InternalMaintenanceOperation {
		private final BmContext context;

		public CalendarViewMaintenance(BmContext bmContext) {
			super(MAINTENANCE_OPERATION.identifier, null, null, 1);
			this.context = bmContext;
		}

		@SuppressWarnings("deprecation")
		@Override
		public void check(String domainUid, DirEntry entry, DiagnosticReport report, IServerTaskMonitor monitor) {
			IUserCalendarViews view = context.provider().instance(IUserCalendarViews.class, domainUid, entry.entryUid);

			processCalendarViews(view.list(), viewData -> {
				logger.info("Calendarview {}:{} contains inaccessible calendars {}", viewData.view.uid,
						viewData.view.displayName, Arrays.toString(viewData.missingCalendars.toArray()));
				monitor.log("Calendarview " + viewData.view.uid + ":" + viewData.view.displayName
						+ " contains inaccessible calendars " + Arrays.toString(viewData.missingCalendars.toArray()));
			});
		}

		@SuppressWarnings("deprecation")
		@Override
		public void repair(String domainUid, DirEntry entry, DiagnosticReport report, IServerTaskMonitor monitor) {
			IUserCalendarViews view = context.provider().instance(IUserCalendarViews.class, domainUid, entry.entryUid);

			processCalendarViews(view.list(), viewData -> {
				logger.info("Calendarview {}:{} contains inaccessible calendar {}", viewData.view.uid,
						viewData.view.displayName, viewData.existingCalendars);
				monitor.log("Calendarview " + viewData.view.uid + ":" + viewData.view.displayName
						+ " contains inaccessible calendar " + viewData.view.uid);

				viewData.view.value.calendars = viewData.existingCalendars;
				view.update(viewData.view.uid, viewData.view.value);
			});
		}

		private void processCalendarViews(ListResult<ItemValue<CalendarView>> views, Consumer<ViewCalendarData> op) {
			IContainers containerService = context.provider().instance(IContainers.class);

			for (ItemValue<CalendarView> view : views.values) {
				List<String> existing = containerService.getContainersLight(view.value.calendars).stream()
						.map(c -> c.uid).collect(Collectors.toList());
				Collection<String> missing = Collections2.filter(view.value.calendars,
						Predicates.not(Predicates.in(existing)));
				if (!missing.isEmpty()) {
					op.accept(new ViewCalendarData(view, existing, missing));
				}
			}
		}

		final class ViewCalendarData {
			final ItemValue<CalendarView> view;
			final List<String> existingCalendars;
			final Collection<String> missingCalendars;

			ViewCalendarData(ItemValue<CalendarView> view, List<String> existing, Collection<String> missing) {
				this.view = view;
				this.existingCalendars = existing;
				this.missingCalendars = missing;
			}

		}
	}

	@Override
	public Set<MaintenanceOperation> availableOperations(Kind kind) {
		if (kind == Kind.USER) {
			return ImmutableSet.of(MAINTENANCE_OPERATION);
		} else {
			return Collections.emptySet();
		}
	}

	@Override
	public Set<InternalMaintenanceOperation> ops(Kind kind) {
		if (kind == Kind.USER) {
			return ImmutableSet.of(new CalendarViewMaintenance(context));
		} else {
			return Collections.emptySet();
		}
	}
}
