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

import static net.bluemind.calendar.service.CalendarRecurrenceRepairOperation.IDENTIFIER;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.event.Level;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import net.bluemind.calendar.api.ICalendar;
import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.calendar.api.VEventOccurrence;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.core.api.date.BmDateTime;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.ContainerQuery;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.BaseContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.MaintenanceOperation;
import net.bluemind.directory.service.IDirEntryRepairSupport;
import net.bluemind.directory.service.RepairTaskMonitor;

public class CalendarRecurrenceRepair implements IDirEntryRepairSupport {

	private static final MaintenanceOperation MAINTENANCE_OPERATION = //
			MaintenanceOperation.create(IDENTIFIER, "Calendar recurrence reparation");

	private final BmContext context;

	public static class Factory implements IDirEntryRepairSupport.Factory {
		@Override
		public IDirEntryRepairSupport create(BmContext context) {
			return new CalendarRecurrenceRepair(context);
		}
	}

	public CalendarRecurrenceRepair(BmContext context) {
		this.context = context;
	}

	public static class CalendarRecurrenceMaintenance extends InternalMaintenanceOperation {
		private final BmContext context;

		public CalendarRecurrenceMaintenance(BmContext bmContext) {
			super(MAINTENANCE_OPERATION.identifier, null, null, 1);
			this.context = bmContext;
		}

		@Override
		public void check(String domainUid, DirEntry entry, RepairTaskMonitor monitor) {
			checkAndRepair(false, entry, monitor);
		}

		@Override
		public void repair(String domainUid, DirEntry entry, RepairTaskMonitor monitor) {
			checkAndRepair(true, entry, monitor);
		}

		private void checkAndRepair(boolean repair, DirEntry entry, RepairTaskMonitor monitor) {

			IContainers containersService = context.provider().instance(IContainers.class);
			ContainerQuery q = ContainerQuery.ownerAndType(entry.entryUid, ICalendarUids.TYPE);
			List<BaseContainerDescriptor> containers = containersService.allLight(q).stream() //
					.collect(Collectors.toList());

			int workLoad = (repair) ? 2 * containers.size() : containers.size();
			monitor.begin(workLoad, String.format("%s %d calendar(s) of user %s", repair ? "Repair" : "Check",
					containers.size(), entry.entryUid));
			containers.forEach(container -> checkAndRepairCalendar(repair, container, monitor));
			monitor.end();
		}

		private void checkAndRepairCalendar(boolean repair, BaseContainerDescriptor container,
				RepairTaskMonitor monitor) {
			ICalendar service = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(ICalendar.class,
					container.uid);
			List<ItemValue<VEventSeries>> repairedItems = findAndRepairItems(service, container, monitor);
			if (repair) {
				saveRepairedItems(service, container, repairedItems, monitor);
			}
		}

		private List<ItemValue<VEventSeries>> findAndRepairItems(ICalendar service, BaseContainerDescriptor container,
				RepairTaskMonitor monitor) {
			List<ItemValue<VEventSeries>> repairedItems = Lists.partition(service.all(), 50).stream() //
					.flatMap(List::stream) //
					.map(service::getComplete) //
					.map(item -> repairBrokenItem(item)) //
					.filter(Optional::isPresent) //
					.map(Optional::get) //
					.collect(Collectors.toList());
			monitor.progress(1, String.format("Checked calendar %s, %d events with duplicates", container.uid,
					repairedItems.size()));
			repairedItems.forEach(item -> monitor.log(" - Event {} of calendar {} has a duplicated recurid", Level.WARN,
					item.uid, container.uid));
			repairedItems.forEach(item -> monitor.notify("Event {} of calendar {} has a duplicated recurid", item.uid,
					container.uid));
			return repairedItems;
		}

		private Optional<ItemValue<VEventSeries>> repairBrokenItem(ItemValue<VEventSeries> item) {
			if (item == null || item.value == null) {
				return Optional.empty();
			}
			Map<BmDateTime, List<VEventOccurrence>> occurencesByRecurid = item.value.occurrences.stream()
					.collect(Collectors.groupingBy(occ -> occ.recurid));

			boolean hasDuplicatedRecurid = occurencesByRecurid.values().stream().anyMatch(occs -> occs.size() > 1);
			if (hasDuplicatedRecurid) {
				item.value.occurrences = occurencesByRecurid.values().stream() //
						.map(occurrences -> occurrences.get(0)) //
						.collect(Collectors.toList());
				return Optional.of(item);
			} else {
				return Optional.empty();
			}
		}

		private int saveRepairedItems(ICalendar service, BaseContainerDescriptor container,
				List<ItemValue<VEventSeries>> repairedItems, IServerTaskMonitor monitor) {
			if (repairedItems.isEmpty()) {
				return 0;
			}

			int errors = repairedItems.stream().mapToInt(item -> repairEvent(service, item) ? 0 : 1).sum();
			if (errors == 0) {
				monitor.progress(1, String.format("Repaired calendar %s", container.uid));
			} else {
				monitor.progress(1, String.format("Failed to repaired calendar %s", container.uid));
			}
			return errors;
		}

		private boolean repairEvent(ICalendar service, ItemValue<VEventSeries> item) {
			try {
				service.update(item.uid, item.value, false);
				return true;
			} catch (ServerFault sf) {
				return false;
			}
		}
	}

	@Override
	public Set<MaintenanceOperation> availableOperations(Kind kind) {
		if (kind == Kind.USER || kind == Kind.CALENDAR || kind == Kind.RESOURCE) {
			return ImmutableSet.of(MAINTENANCE_OPERATION);
		} else {
			return Collections.emptySet();
		}
	}

	@Override
	public Set<InternalMaintenanceOperation> ops(Kind kind) {
		if (kind == Kind.USER || kind == Kind.CALENDAR || kind == Kind.RESOURCE) {
			return ImmutableSet.of(new CalendarRecurrenceMaintenance(context));
		} else {
			return Collections.emptySet();
		}
	}
}
