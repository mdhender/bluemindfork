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
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.slf4j.event.Level;

import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableSet;

import net.bluemind.calendar.api.CalendarView;
import net.bluemind.calendar.api.CalendarView.CalendarViewType;
import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.calendar.api.ICalendarViewUids;
import net.bluemind.calendar.api.IUserCalendarViews;
import net.bluemind.core.container.api.IContainerManagement;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.MaintenanceOperation;
import net.bluemind.directory.service.IDirEntryRepairSupport;
import net.bluemind.directory.service.RepairTaskMonitor;

public class CalendarViewRepairSupport implements IDirEntryRepairSupport {

	private final BmContext context;
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
			super(MAINTENANCE_OPERATION.identifier, null, "containers", 1);
			this.context = bmContext;
		}

		@SuppressWarnings("deprecation")
		@Override
		public void check(String domainUid, DirEntry entry, RepairTaskMonitor monitor) {
			Optional<ContainerDescriptor> viewContainer = checkForViewContainer(monitor, entry, () -> {
			});

			if (!viewContainer.isPresent()) {
				monitor.log("Skipping other calendarview checks");
				return;
			}

			IUserCalendarViews view = context.provider().instance(IUserCalendarViews.class, domainUid, entry.entryUid);
			List<ItemValue<CalendarView>> views = view.list().values;

			checkForDefaultView(monitor, entry, views, () -> {
			});

			processCalendarViews(monitor, views, viewData -> {
			});

			monitor.end();
		}

		@SuppressWarnings("deprecation")
		@Override
		public void repair(String domainUid, DirEntry entry, RepairTaskMonitor monitor) {
			IContainers containerService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
					.instance(IContainers.class);

			checkForViewContainer(monitor, entry, () -> {
				String containerUid = ICalendarViewUids.userCalendarView(entry.entryUid);
				ContainerDescriptor containerDescriptor = ContainerDescriptor.create(containerUid, entry.displayName,
						entry.entryUid, "calendarview", domainUid, true);
				containerService.create(containerUid, containerDescriptor);
				ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
						.instance(IContainerManagement.class, containerUid)
						.setAccessControlList(Arrays.asList(AccessControlEntry.create(entry.entryUid, Verb.All)));
			});

			IUserCalendarViews view = context.provider().instance(IUserCalendarViews.class, domainUid, entry.entryUid);
			List<ItemValue<CalendarView>> views = view.list().values;

			checkForDefaultView(monitor, entry, views, () -> {
				Optional<ItemValue<CalendarView>> existingDefaultView = view.list().values.stream()
						.filter(v -> v.uid.equals("default")).findFirst();
				if (!existingDefaultView.isPresent()) {
					monitor.log("Creating missing default calendar view", Level.WARN);
					monitor.notify("Missing default calendar view");
					CalendarView defaultView = new CalendarView();
					defaultView.label = "$$calendarhome$$";
					defaultView.type = CalendarViewType.WEEK;
					defaultView.calendars = Arrays.asList(ICalendarUids.defaultUserCalendar(entry.entryUid));
					view.create("default", defaultView);
				}
				view.setDefault("default");
			});

			views = view.list().values;

			processCalendarViews(monitor, views, viewData -> {
				viewData.view.value.calendars = viewData.existingCalendars;
				view.update(viewData.view.uid, viewData.view.value);
			});
		}

		private Optional<ContainerDescriptor> checkForViewContainer(RepairTaskMonitor monitor, DirEntry dirEntry,
				Runnable op) {
			String containerUid = ICalendarViewUids.userCalendarView(dirEntry.entryUid);

			IContainers containerService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
					.instance(IContainers.class);

			ContainerDescriptor container = containerService.getIfPresent(containerUid);
			if (container == null) {
				monitor.log("Calendarview container " + containerUid + " is missing", Level.WARN);
				op.run();
				return Optional.ofNullable(containerService.getIfPresent(containerUid));
			} else {
				return Optional.of(container);
			}
		}

		private void checkForDefaultView(RepairTaskMonitor monitor, DirEntry dirEntry,
				List<ItemValue<CalendarView>> views, Runnable op) {
			String userCalendarView = ICalendarViewUids.userCalendarView(dirEntry.entryUid);
			if (views.stream().noneMatch(view -> view.value != null && view.value.isDefault)) {
				monitor.log("Default calendarview " + userCalendarView + " is missing");
				op.run();
			}

		}

		private void processCalendarViews(RepairTaskMonitor monitor, List<ItemValue<CalendarView>> views,
				Consumer<ViewCalendarData> op) {
			IContainers containerService = context.provider().instance(IContainers.class);

			for (ItemValue<CalendarView> view : views) {
				List<String> existing = containerService.getContainersLight(view.value.calendars).stream()
						.map(c -> c.uid).collect(Collectors.toList());
				Collection<String> missing = Collections2.filter(view.value.calendars,
						Predicates.not(Predicates.in(existing)));
				if (!missing.isEmpty()) {
					monitor.log("Calendarview " + view.uid + ":" + view.displayName
							+ " contains inaccessible calendars " + Arrays.toString(missing.toArray()));
					monitor.notify("Calendarview " + view.uid + ":" + view.displayName
							+ " contains inaccessible calendars " + Arrays.toString(missing.toArray()));
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
