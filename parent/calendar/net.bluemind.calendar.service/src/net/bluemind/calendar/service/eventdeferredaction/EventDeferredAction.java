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
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.calendar.service.eventdeferredaction;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.base.MoreObjects;

import net.bluemind.calendar.api.ICalendar;
import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.calendar.occurrence.OccurrenceHelper;
import net.bluemind.core.api.date.BmDateTime;
import net.bluemind.core.api.date.BmDateTime.Precision;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.deferredaction.api.DeferredAction;
import net.bluemind.icalendar.api.ICalendarElement.VAlarm;

public class EventDeferredAction extends DeferredAction {

	public static final String ACTION_ID = "EVENT";

	public final VEvent vevent;
	public final VAlarm valarm;
	public final String ownerUid;
	private final String containerUid;
	private final String eventUid;
	private final Optional<BmDateTime> recurid;

	public EventDeferredAction(DeferredAction deferredAction) {
		this.actionId = deferredAction.actionId;
		this.reference = deferredAction.reference;
		this.configuration = deferredAction.configuration;
		this.executionDate = deferredAction.executionDate;

		this.containerUid = EventDeferredAction.getContainerUid(deferredAction.reference);
		this.eventUid = EventDeferredAction.getItemUid(deferredAction.reference);
		this.ownerUid = deferredAction.configuration.get("owner");
		this.recurid = getRecurId(deferredAction.configuration);

		this.vevent = retrieveEvent(containerUid, eventUid, recurid);
		this.valarm = getAlarm(Integer.parseInt(configuration.get("trigger")));
	}

	public EventDeferredAction(VEvent vevent, int trigger) {
		this.recurid = Optional.empty();
		this.ownerUid = "";
		this.containerUid = "";
		this.eventUid = "";
		this.vevent = vevent;
		this.valarm = getAlarm(trigger);
	}

	private Optional<BmDateTime> getRecurId(Map<String, String> configuration) {
		if (!configuration.containsKey("recurid") || !configuration.containsKey("recurid_timezone")) {
			return Optional.empty();
		}

		return Optional.of(new BmDateTime(configuration.get("recurid"), configuration.get("recurid_timezone"),
				BmDateTime.Precision.valueOf(configuration.get("recurid_precision"))));
	}

	public static String getReference(String containerUid, String itemUid) {
		return containerUid + "#" + itemUid;
	}

	public static String getContainerUid(String reference) {
		return reference.substring(0, reference.indexOf('#'));
	}

	public static String getItemUid(String reference) {
		return reference.substring(reference.indexOf('#') + 1);
	}

	public boolean isNotOccurrenceException() {
		return !recurid.isPresent();
	}

	public Optional<ZonedDateTime> nextExecutionDate() {
		ZonedDateTime zonedExecutionDate = ZonedDateTime.ofInstant(executionDate.toInstant(), ZoneId.systemDefault());
		ZonedDateTime beginOfNextPeriod = zonedExecutionDate.minusSeconds(valarm.trigger);
		BmDateTime bmBeginOfNextPeriod = BmDateTimeWrapper.create(beginOfNextPeriod, Precision.DateTime);
		return OccurrenceHelper.getNextOccurrence(bmBeginOfNextPeriod, vevent)//
				.map(t -> new BmDateTimeWrapper(t.dtstart).toDateTime())//
				.map(this::getTriggerDate);
	}

	/**
	 * 
	 * @param dtstart
	 * @param trigger:
	 *                     a negative integer in minute to be added to the dtstart
	 *                     of the event.
	 * @return
	 */
	public ZonedDateTime getTriggerDate(ZonedDateTime dtstart) {
		return dtstart.plusSeconds(valarm.trigger);
	}

	public boolean isRecurringEvent() {
		return vevent.rrule != null;
	}

	private VAlarm getAlarm(int trigger) {
		if (vevent.hasAlarm()) {
			return vevent.alarm.stream().filter(alarm -> alarm.trigger == trigger).findFirst().orElse(null);
		}
		return null;
	}

	public static Set<BmDateTime> excludeKnownExceptions(VEventSeries event, Set<BmDateTime> knownExdate) {
		if (knownExdate == null) {
			knownExdate = new HashSet<>();
		}
		Set<BmDateTime> exdate = event.occurrences.stream().map(occurrence -> occurrence.recurid)
				.collect(Collectors.toSet());
		exdate.addAll(knownExdate);
		return Collections.unmodifiableSet(exdate);
	}

	private static VEvent retrieveEvent(String containerUid, String eventUid, Optional<BmDateTime> recurid) {
		ServerSideServiceProvider servicesProvider = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		ICalendar calendar = servicesProvider.instance(ICalendar.class, containerUid);
		VEventSeries event = calendar.getComplete(eventUid).value;
		if (recurid.isPresent()) {
			return event.occurrence(recurid.get());
		} else {
			if (event.main.rrule == null) {
				return event.main;
			}
			VEvent main = event.main.copy();
			main.exdate = excludeKnownExceptions(event, main.exdate);
			return main;
		}
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(EventDeferredAction.class)//
				.add("ownerUid", ownerUid)//
				.addValue(super.toString())//
				.toString();
	}
}
