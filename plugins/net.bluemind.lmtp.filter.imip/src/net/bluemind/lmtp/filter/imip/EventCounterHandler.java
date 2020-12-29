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
import java.util.List;
import java.util.Optional;

import net.bluemind.calendar.api.ICalendar;
import net.bluemind.calendar.api.VEventCounter;
import net.bluemind.calendar.api.VEventOccurrence;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.core.api.date.BmDateTime;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.icalendar.api.ICalendarElement.Attendee;
import net.bluemind.imip.parser.IMIPInfos;
import net.bluemind.lmtp.backend.LmtpAddress;
import net.bluemind.mailbox.api.Mailbox;

public class EventCounterHandler extends AbstractLmtpHandler implements IIMIPHandler {

	public EventCounterHandler(LmtpAddress recipient, LmtpAddress sender) {
		super(recipient, sender);
	}

	@Override
	public IMIPResponse handle(IMIPInfos imip, LmtpAddress recipient, ItemValue<Domain> domain,
			ItemValue<Mailbox> recipientMailbox) throws ServerFault {

		String calUid = getCalendarUid(recipientMailbox);
		ICalendar cal = provider().instance(ICalendar.class, calUid);
		List<ItemValue<VEventSeries>> items = getAndValidateExistingSeries(cal, imip);
		validateItemCount(imip, 1);
		ItemValue<VEventSeries> currentSeries = items.get(0);

		if (!currentSeries.value.acceptCounters) {
			throw new ServerFault(String.format("%s does not allow counter propositions", imip.uid));
		}

		VEventSeries propositionSeries = fromList(imip.properties, imip.iCalendarElements, imip.uid);
		VEventOccurrence counterEvent = null;
		if (propositionSeries.main != null) {
			counterEvent = VEventOccurrence.fromEvent(propositionSeries.main, null);
			for (Attendee att : currentSeries.value.main.attendees) {
				if (counterEvent.attendees.get(0).mailto.equals(att.mailto)) {
					att.partStatus = counterEvent.attendees.get(0).partStatus;
				}
			}
		} else {
			counterEvent = propositionSeries.occurrences.get(0);
			if (currentSeries.value.occurrence(counterEvent.recurid) == null) {
				// counter on non-existing exception
				List<VEventOccurrence> occurrences = new ArrayList<>(currentSeries.value.occurrences);
				VEventOccurrence newOccurrence = VEventOccurrence.fromEvent(counterEvent.copy(), counterEvent.recurid);

				for (Attendee att : currentSeries.value.main.attendees) {
					if (!counterEvent.attendees.get(0).mailto.equals(att.mailto)) {
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
				occurrences.add(newOccurrence);
				currentSeries.value.occurrences = occurrences;
			} else {
				for (Attendee att : currentSeries.value.occurrence(counterEvent.recurid).attendees) {
					if (counterEvent.attendees.get(0).mailto.equals(att.mailto)) {
						att.partStatus = counterEvent.attendees.get(0).partStatus;
					}
				}
			}
		}

		Attendee originator = counterEvent.attendees.get(0);

		String commonName = originator.commonName;
		String email = originator.mailto;
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

		return IMIPResponse.createCounterResponse(imip.uid, email, counterEvent);
	}

}
