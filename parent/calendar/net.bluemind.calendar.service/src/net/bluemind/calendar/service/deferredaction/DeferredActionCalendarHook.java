/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2019
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
package net.bluemind.calendar.service.deferredaction;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.api.VEvent.Transparency;
import net.bluemind.calendar.api.VEventOccurrence;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.calendar.hook.ICalendarHook;
import net.bluemind.calendar.hook.internal.VEventMessage;
import net.bluemind.calendar.occurrence.OccurrenceHelper;
import net.bluemind.core.api.date.BmDateTime;
import net.bluemind.core.api.date.BmDateTime.Precision;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.cti.service.CTIDeferredAction;
import net.bluemind.deferredaction.api.DeferredAction;
import net.bluemind.deferredaction.api.IDeferredAction;
import net.bluemind.deferredaction.api.IDeferredActionContainerUids;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.icalendar.api.ICalendarElement.ParticipationStatus;
import net.bluemind.icalendar.api.ICalendarElement.VAlarm;
import net.bluemind.icalendar.api.ICalendarElement.VAlarm.Action;

public class DeferredActionCalendarHook implements ICalendarHook {

	@Override
	public void onEventCreated(VEventMessage message) {
		VEventSeries event = message.vevent;
		List<VEvent> occurrences = flatten(event);
		String ownerDirEntryPath = getOwnerDirEntryPath(message.container.domainUid, message.container.owner);
		for (VEvent occurrence : occurrences) {
			if (attends(occurrence, ownerDirEntryPath)) {
				handleOccurence(occurrence, message);
			}
		}
		updateCtiTrigger(message, occurrences);
	}

	private void handleOccurence(VEvent occurrence, VEventMessage message) {
		if (occurrence.hasAlarm()) {
			for (VAlarm valarm : occurrence.alarm) {
				addTrigger(valarm, occurrence, message);
			}

		}
	}

	private void addTrigger(VAlarm valarm, VEvent occurrence, VEventMessage message) {
		Optional<Date> trigger = calculateAlarmDate(valarm, occurrence.dtstart);
		if (trigger.isPresent()) {
			IDeferredAction service = getService(valarm, message);
			storeTrigger(EventDeferredAction.getReference(message.container.uid, message.itemUid),
					getConfig(message, occurrence, valarm.trigger), service, trigger.get());
		} else if (occurrence.rrule != null) {
			LocalDateTime now = LocalDateTime.now();
			LocalDateTime beginOfNextPeriod = now.plusSeconds(valarm.trigger);
			OccurrenceHelper
					.getNextOccurrence(BmDateTimeWrapper.create(beginOfNextPeriod, Precision.DateTime), occurrence)
					.ifPresent(nextOccurrence -> {
						nextOccurrence.recurid = null;
						addTrigger(valarm, nextOccurrence, message);
					});
		}
	}

	@Override
	public void onEventUpdated(VEventMessage message) {
		onEventDeleted(message);
		onEventCreated(message);
	}

	@Override
	public void onEventDeleted(VEventMessage message) {
		SecurityContext securityContext = SecurityContext.SYSTEM;
		List<IDeferredAction> services = Arrays.asList(
				ServerSideServiceProvider.getProvider(securityContext).instance(IDeferredAction.class,
						IDeferredActionContainerUids.uidForUser(message.container.owner)),
				ServerSideServiceProvider.getProvider(securityContext).instance(IDeferredAction.class,
						IDeferredActionContainerUids.uidForDomain(message.container.domainUid)));

		services.forEach(service -> service
				.getByReference(EventDeferredAction.getReference(message.container.uid, message.itemUid))
				.forEach(trigger -> {
					service.delete(trigger.uid);
				}));
	}

	private void updateCtiTrigger(VEventMessage message, List<VEvent> occurrences) {
		String container = IDeferredActionContainerUids.uidForDomain(message.container.domainUid);
		IDeferredAction service = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDeferredAction.class, container);

		List<ItemValue<DeferredAction>> ctiTriggers = service
				.getByReference(CTIDeferredAction.reference(message.container.owner));
		if (!ctiTriggers.isEmpty()) {
			ItemValue<DeferredAction> ctiTrigger = ctiTriggers.get(0);
			VEvent event = occurrences.get(0);
			LocalDateTime now = LocalDateTime.now();

			LocalDateTime triggerExecutionDate = ctiTrigger.value.executionDate.toInstant().atZone(ZoneId.of("UTC"))
					.toLocalDateTime();
			LocalDateTime eventDtStart = Instant
					.ofEpochMilli(BmDateTimeWrapper.toTimestamp(event.dtstart.iso8601, event.dtstart.timezone))
					.atZone(ZoneId.of("UTC")).toLocalDateTime();
			LocalDateTime eventDtEnd = Instant
					.ofEpochMilli(BmDateTimeWrapper.toTimestamp(event.dtend.iso8601, event.dtend.timezone))
					.atZone(ZoneId.of("UTC")).toLocalDateTime();
			if ((event.transparency == Transparency.Opaque && triggerExecutionDate.isAfter(now))
					&& (eventDtStart.isBefore(triggerExecutionDate) && eventDtEnd.isAfter(triggerExecutionDate))) {
				ctiTrigger.value.executionDate = new Date();
				service.update(ctiTrigger.uid, ctiTrigger.value);
			}
		}
	}

	private String getOwnerDirEntryPath(String domainUid, String owner) {
		DirEntry entry = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDirectory.class, domainUid).findByEntryUid(owner);
		return entry.path;
	}

	private boolean attends(VEvent occ, String dir) {
		final String owner = "bm://" + dir;
		return occ.attendees.isEmpty() || owner.equals(occ.organizer.dir) || occ.attendees.stream()
				.anyMatch(a -> a.partStatus != ParticipationStatus.Declined && owner.equals(a.dir));
	}

	private Map<String, String> getConfig(VEventMessage message, VEvent occurrence, Integer trigger) {
		@SuppressWarnings("serial")
		Map<String, String> config = new HashMap<String, String>() {
			{
				put("trigger", trigger + "");
				put("owner", message.container.owner);
			}
		};
		if (occurrence.exception()) {
			config.put("recurid", ((VEventOccurrence) occurrence).recurid.iso8601);
		}
		return config;
	}

	private Optional<Date> calculateAlarmDate(VAlarm valarm, BmDateTime eventDate) {
		ZonedDateTime event = new BmDateTimeWrapper(eventDate).toDateTime();
		ZonedDateTime alarm = event.plusSeconds(valarm.trigger);
		if (alarm.isBefore(ZonedDateTime.now())) {
			return Optional.empty();
		}
		return Optional.of(Date.from(alarm.toInstant()));
	}

	private void storeTrigger(String reference, Map<String, String> config, IDeferredAction service,
			Date triggerValue) {
		DeferredAction action = new DeferredAction();
		action.reference = reference;
		action.configuration = config;
		action.executionDate = triggerValue;
		action.actionId = EventDeferredAction.ACTION_ID;
		service.create(UUID.randomUUID().toString(), action);
	}

	private IDeferredAction getService(VAlarm valarm, VEventMessage message) {
		String containerUid = null;
		if (valarm.action != null && valarm.action == Action.Email) {
			containerUid = IDeferredActionContainerUids.uidForDomain(message.container.domainUid);
		} else {
			containerUid = IDeferredActionContainerUids.uidForUser(message.container.owner);
		}
		return ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IDeferredAction.class,
				containerUid);
	}

	private List<VEvent> flatten(VEventSeries event) {
		List<VEvent> evts = new ArrayList<>();
		Set<BmDateTime> exdate = new HashSet<>();
		event.occurrences.forEach(occurrence -> {
			evts.add(occurrence);
			exdate.add(occurrence.recurid);
		});
		if (event.main != null) {
			VEvent main = event.main.copy();
			if (main.exdate != null) {
				exdate.addAll(main.exdate);
			}
			main.exdate = exdate;
			evts.add(main);
		}
		return evts;
	}

	public static void init(Container container, ItemValue<VEventSeries> series) {
		VEventMessage message = new VEventMessage(series.value, series.uid, false, SecurityContext.SYSTEM, "",
				container);
		new DeferredActionCalendarHook().onEventCreated(message);
	}
}
