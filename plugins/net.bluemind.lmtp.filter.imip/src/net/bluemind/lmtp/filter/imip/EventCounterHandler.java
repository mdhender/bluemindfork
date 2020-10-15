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

import java.util.List;
import java.util.Optional;

import net.bluemind.calendar.api.ICalendar;
import net.bluemind.calendar.api.VEventCounter;
import net.bluemind.calendar.api.VEventOccurrence;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.icalendar.api.ICalendarElement.Attendee;
import net.bluemind.imip.parser.IMIPInfos;
import net.bluemind.lmtp.backend.LmtpAddress;
import net.bluemind.mailbox.api.Mailbox;

public class EventCounterHandler extends AbstractLmtpHandler implements IIMIPHandler {

	@Override
	public IMIPResponse handle(IMIPInfos imip, LmtpAddress recipient, ItemValue<Domain> domain,
			ItemValue<Mailbox> recipientMailbox) throws ServerFault {

		String calUid = getCalendarUid(recipientMailbox);
		ICalendar cal = provider().instance(ICalendar.class, calUid);
		List<ItemValue<VEventSeries>> items = getAndValidateExistingSeries(cal, imip);
		validateItemCount(imip, 1);
		ItemValue<VEventSeries> currentSeries = items.get(0);

		VEventSeries propositionSeries = fromList(imip.iCalendarElements, imip.uid);
		VEventOccurrence counterEvent = null;
		if (propositionSeries.main != null) {
			counterEvent = VEventOccurrence.fromEvent(propositionSeries.main, null);
		} else {
			counterEvent = propositionSeries.occurrences.get(0);
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

		return new IMIPResponse();
	}

}
