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
package net.bluemind.calendar.service.eventdeferredaction;

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
import java.util.stream.Collectors;

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
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.cti.service.CTIDeferredAction;
import net.bluemind.deferredaction.api.DeferredAction;
import net.bluemind.deferredaction.api.IDeferredAction;
import net.bluemind.deferredaction.api.IDeferredActionContainerUids;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.icalendar.api.ICalendarElement.ParticipationStatus;
import net.bluemind.icalendar.api.ICalendarElement.VAlarm;
import net.bluemind.icalendar.api.ICalendarElement.VAlarm.Action;
import net.bluemind.user.api.IUserSettings;

public class EventDeferredActionHook implements ICalendarHook {

	@Override
	public void onEventCreated(VEventMessage message) {
		DirEntry dirEntry = getOwnerDirEntry(message.container.domainUid, message.container.owner);

		if (dirEntry.kind != Kind.USER) {
			return;
		}
		VEventSeries event = message.vevent;
		List<VEvent> occurrences = flatten(event);
		String ownerDirEntryPath = dirEntry.path;
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
		IUserSettings userSettingsService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IUserSettings.class, message.container.domainUid);
		Optional<String> userTimezone = Optional
				.ofNullable(userSettingsService.get(message.container.owner).get("timezone"));

		Optional<VEvent> currentOccurrence = getFirstOccurence(occurrence);
		while (currentOccurrence.isPresent()) {
			currentOccurrence = storeTrigger(valarm, currentOccurrence.get(), message, userTimezone);
		}
	}

	private Optional<VEvent> getFirstOccurence(VEvent occurrence) {
		if (occurrence.rrule == null || !occurrence.exdate.contains(occurrence.dtstart)) {
			return Optional.of(occurrence);
		} else {
			return OccurrenceHelper.getNextOccurrence(occurrence.dtstart, occurrence).map(occ -> (VEvent) occ);
		}
	}

	private Optional<VEvent> storeTrigger(VAlarm valarm, VEvent occurrence, VEventMessage message,
			Optional<String> userTimezone) {
		if (notInPast(occurrence.dtend)) {
			Date trigger = calculateAlarmDate(valarm, occurrence.dtstart, userTimezone);
			if (!trigger.before(new Date())) {
				IDeferredAction service = getService(valarm, message);
				String reference = EventDeferredAction.getReference(message.container.uid, message.itemUid);
				Map<String, String> config = getConfig(message, occurrence, valarm.trigger);
				storeTrigger(reference, config, service, trigger);
				return Optional.empty();
			}
		}

		return getNextOccurrence(valarm, occurrence).map(vEventOccurrence -> {
			vEventOccurrence.recurid = null;
			return vEventOccurrence;
		});
	}

	private Optional<VEventOccurrence> getNextOccurrence(VAlarm valarm, VEvent occurrence) {
		if (occurrence.rrule == null) {
			return Optional.empty();
		}

		BmDateTime beginOfNextPeriod = occurrence.dtend;
		// minusSeconds to make sure we handle positive trigger values (reminders after
		// the actual event)
		LocalDateTime now = LocalDateTime.now().minusSeconds(valarm.trigger);
		if (now.isAfter(new BmDateTimeWrapper(occurrence.dtend).toDateTime().toLocalDateTime())) {
			beginOfNextPeriod = BmDateTimeWrapper.create(now, Precision.DateTime);
		}

		return OccurrenceHelper.getNextOccurrence(beginOfNextPeriod, occurrence);
	}

	@Override
	public void onEventUpdated(VEventMessage message) {
		DirEntry dirEntry = getOwnerDirEntry(message.container.domainUid, message.container.owner);
		if (dirEntry.kind != Kind.USER) {
			return;
		}
		onEventDeleted(message);
		onEventCreated(message);
	}

	@Override
	public void onEventDeleted(VEventMessage message) {
		DirEntry dirEntry = getOwnerDirEntry(message.container.domainUid, message.container.owner);
		if (dirEntry.kind != Kind.USER) {
			return;
		}
		SecurityContext securityContext = SecurityContext.SYSTEM;
		List<IDeferredAction> services = Arrays.asList(
				ServerSideServiceProvider.getProvider(securityContext).instance(IDeferredAction.class,
						IDeferredActionContainerUids.uidForUser(message.container.owner)),
				ServerSideServiceProvider.getProvider(securityContext).instance(IDeferredAction.class,
						IDeferredActionContainerUids.uidForDomain(message.container.domainUid)));

		services.forEach(service -> service
				.getByReference(EventDeferredAction.getReference(message.container.uid, message.itemUid))
				.forEach(trigger -> service.delete(trigger.uid)));
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

	private DirEntry getOwnerDirEntry(String domainUid, String owner) {
		return ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IDirectory.class, domainUid)
				.findByEntryUid(owner);
	}

	private boolean attends(VEvent occ, String dir) {
		final String owner = "bm://" + dir;
		return occ.attendees.isEmpty() || owner.equals(occ.organizer.dir) || occ.attendees.stream()
				.anyMatch(a -> a.partStatus != ParticipationStatus.Declined && owner.equals(a.dir));
	}

	private Map<String, String> getConfig(VEventMessage message, VEvent occurrence, Integer trigger) {
		Map<String, String> config = new HashMap<>();
		config.put("trigger", Integer.toString(trigger));
		config.put("owner", message.container.owner);
		config.put("summary", occurrence.summary);
		config.put("location", occurrence.location);
		config.put("dtstart", JsonUtils.asString(occurrence.dtstart));
		config.put("dtend", JsonUtils.asString(occurrence.dtend));
		if (occurrence.rrule != null) {
			config.put("rrule", JsonUtils.asString(occurrence.rrule));
			config.put("exdates", JsonUtils.asString(occurrence.exdate.stream().filter(this::notInPast)
					.map(d -> d.iso8601).collect(Collectors.toList())));
		}
		if (occurrence.exception()) {
			config.put("recurid", ((VEventOccurrence) occurrence).recurid.iso8601);
			config.put("recurid_timezone", ((VEventOccurrence) occurrence).recurid.timezone);
			config.put("recurid_precision", ((VEventOccurrence) occurrence).recurid.precision.name());

		}
		return config;
	}

	private boolean notInPast(BmDateTime dt) {
		ZonedDateTime asZonedDt = new BmDateTimeWrapper(dt).toDateTime();
		return !asZonedDt.isBefore(ZonedDateTime.now());
	}

	private Date calculateAlarmDate(VAlarm valarm, BmDateTime eventDate, Optional<String> userTimezone) {
		ZonedDateTime event = eventDate.precision == Precision.Date && userTimezone.isPresent()
				? new BmDateTimeWrapper(eventDate).toDateTime(userTimezone.get())
				: new BmDateTimeWrapper(eventDate).toDateTime();
		ZonedDateTime alarm = event.plusSeconds(valarm.trigger);
		return Date.from(alarm.toInstant());
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
			occurrence.rrule = null; // Some exceptions seems to have a rrule
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
		new EventDeferredActionHook().onEventCreated(message);
	}
}
