/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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
package net.bluemind.lmtp.filter.imip;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import net.bluemind.calendar.api.ICalendar;
import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.api.VEventCounter;
import net.bluemind.calendar.api.VEventOccurrence;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.core.api.date.BmDateTime;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.delivery.lmtp.common.LmtpAddress;
import net.bluemind.delivery.lmtp.common.ResolvedBox;
import net.bluemind.delivery.lmtp.filters.PermissionDeniedException.CounterNotAllowedException;
import net.bluemind.domain.api.Domain;
import net.bluemind.icalendar.api.ICalendarElement.Attendee;
import net.bluemind.icalendar.api.ICalendarElement.ParticipationStatus;
import net.bluemind.icalendar.api.ICalendarElement.Role;
import net.bluemind.imip.parser.IMIPInfos;
import net.bluemind.mailbox.api.Mailbox;

public class EventCounterHandler extends AbstractLmtpHandler implements IIMIPHandler {

	public EventCounterHandler(ResolvedBox recipient, LmtpAddress sender) {
		super(recipient, sender);
	}

	@Override
	public IMIPResponse handle(IMIPInfos imip, ResolvedBox recipient, ItemValue<Domain> domain,
			ItemValue<Mailbox> recipientMailbox) throws ServerFault {

		String calUid = getCalendarUid(recipientMailbox);
		ICalendar cal = provider().instance(ICalendar.class, calUid);
		List<ItemValue<VEventSeries>> items = getAndValidateExistingSeries(cal, imip);
		validateItemCount(imip, 1);
		ItemValue<VEventSeries> currentSeries = items.get(0);

		VEventSeries propositionSeries = fromList(imip.properties, imip.iCalendarElements, imip.uid);
		VEventOccurrence counterEvent = null;
		List<Attendee> counterProposedAttendees;
		if (propositionSeries.main != null) {
			counterEvent = VEventOccurrence.fromEvent(propositionSeries.main, null);
			counterProposedAttendees = proposedattendees(counterEvent);
			Attendee originator = originator(counterEvent, counterProposedAttendees);
			updateOriginatorPartStat(originator, currentSeries.value.main);
			addNewAttendees(counterEvent, currentSeries.value.main, originator.mailto);
		} else {
			counterEvent = propositionSeries.occurrences.get(0);
			counterProposedAttendees = proposedattendees(counterEvent);
			Attendee originator = originator(counterEvent, counterProposedAttendees);
			if (currentSeries.value.occurrence(counterEvent.recurid) == null) {
				// counter on non-existing exception
				List<VEventOccurrence> occurrences = new ArrayList<>(currentSeries.value.occurrences);
				VEventOccurrence newOccurrence = VEventOccurrence.fromEvent(counterEvent.copy(), counterEvent.recurid);

				for (Attendee att : currentSeries.value.main.attendees) {
					if (!originator.mailto.equals(att.mailto)) {
						newOccurrence.attendees.add(att);
					}
				}
				newOccurrence.dtstart = newOccurrence.recurid;
				long duration = new BmDateTimeWrapper(currentSeries.value.main.dtend).toUTCTimestamp()
						- new BmDateTimeWrapper(currentSeries.value.main.dtstart).toUTCTimestamp();
				long timestamp = BmDateTimeWrapper.toTimestamp(newOccurrence.dtstart.iso8601,
						newOccurrence.dtstart.timezone);
				timestamp += duration;
				BmDateTime dtend = BmDateTimeWrapper.fromTimestamp(timestamp, newOccurrence.dtstart.timezone);
				newOccurrence.dtend = dtend;
				addNewAttendees(counterEvent, newOccurrence, originator.mailto);
				occurrences.add(newOccurrence);
				currentSeries.value.occurrences = occurrences;
			} else {
				VEventOccurrence existingException = currentSeries.value.occurrence(counterEvent.recurid);
				updateOriginatorPartStat(originator, existingException);
				addNewAttendees(counterEvent, existingException, originator.mailto);
			}
		}

		Attendee originator = originator(counterEvent, counterProposedAttendees);
		String commonName = originator.commonName;
		String email = originator.mailto;

		if (!counterProposedAttendees.isEmpty()) {
			autoAcceptAttendeeProposals(cal, currentSeries);
		} else {
			if (!currentSeries.value.acceptCounters) {
				ServerFault fault = new ServerFault(new CounterNotAllowedException(recipientMailbox.uid));
				fault.setCode(ErrorCode.EVENT_ACCEPTS_NO_COUNTERS);
				throw fault;
			}
			updateEventWithCounterValues(cal, currentSeries, counterEvent, commonName, email);
		}

		return IMIPResponse.createCounterResponse(imip.uid, email, counterEvent, counterProposedAttendees, calUid);
	}

	private <T extends VEvent> void updateOriginatorPartStat(Attendee originator, T currentEvent) {
		for (Attendee att : currentEvent.attendees) {
			if (originator.mailto.equals(att.mailto)) {
				att.partStatus = originator.partStatus;
			}
		}
	}

	private void updateEventWithCounterValues(ICalendar cal, ItemValue<VEventSeries> currentSeries,
			VEventOccurrence counterEvent, String commonName, String email) {
		VEventCounter.CounterOriginator counterOriginator = VEventCounter.CounterOriginator.from(commonName, email);
		Optional<VEventCounter> counter = getExistingCounter(currentSeries.value.counters, counterEvent,
				counterOriginator);
		VEventCounter newCounter = null;
		if (counter.isPresent()) {
			newCounter = counter.get();
			newCounter.counter = counterEvent;
		} else {
			newCounter = counter.orElse(new VEventCounter());
			newCounter.originator = counterOriginator;
			newCounter.counter = counterEvent;
			currentSeries.value.counters.add(newCounter);
		}

		cal.update(currentSeries.uid, currentSeries.value, false);
	}

	private void autoAcceptAttendeeProposals(ICalendar cal, ItemValue<VEventSeries> currentSeries) {
		currentSeries.value.counters = Collections.emptyList();
		cal.update(currentSeries.uid, currentSeries.value, false);
	}

	private void addNewAttendees(VEventOccurrence counterEvent, VEvent existingEvent, String originator) {
		existingEvent.attendees.addAll(proposedattendees(counterEvent).stream().map(att -> {
			att.role = Role.OptionalParticipant;
			att.partStatus = ParticipationStatus.NeedsAction;
			att.sentBy = originator;
			return att;
		}).toList());
	}

	private Attendee originator(VEventOccurrence counter, List<Attendee> counterProposedAttendees) {
		return counter.attendees.stream().filter(att -> !counterProposedAttendees.contains(att)).findAny().get();
	}

	private List<Attendee> proposedattendees(VEventOccurrence counter) {
		return counter.attendees.stream().filter(att -> att.role == Role.NonParticipant).toList();
	}

}
