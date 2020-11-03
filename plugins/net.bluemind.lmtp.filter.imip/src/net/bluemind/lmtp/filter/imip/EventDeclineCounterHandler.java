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
import java.util.stream.Collectors;

import net.bluemind.calendar.api.ICalendar;
import net.bluemind.calendar.api.VEventOccurrence;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.imip.parser.IMIPInfos;
import net.bluemind.lmtp.backend.LmtpAddress;
import net.bluemind.mailbox.api.Mailbox;

public class EventDeclineCounterHandler extends AbstractLmtpHandler implements IIMIPHandler {

	public EventDeclineCounterHandler(LmtpAddress recipient, LmtpAddress sender) {
		super(recipient, sender);
	}

	@Override
	public IMIPResponse handle(IMIPInfos imip, LmtpAddress recipient, ItemValue<Domain> domain,
			ItemValue<Mailbox> recipientMailbox) throws ServerFault {

		String calUid = getCalendarUid(recipientMailbox);
		ICalendar cal = provider().instance(ICalendar.class, calUid);
		List<ItemValue<VEventSeries>> items = getAndValidateExistingSeries(cal, imip);
		validateItemCount(imip, 1);

		VEventSeries propositionSeries = fromList(imip.iCalendarElements, imip.uid);
		VEventOccurrence counterEvent = null;
		if (propositionSeries.main != null) {
			counterEvent = VEventOccurrence.fromEvent(propositionSeries.main, null);
		} else {
			counterEvent = propositionSeries.occurrences.get(0);
		}

		ItemValue<VEventSeries> currentSeries = items.get(0);
		String recId = counterEvent.recurid == null ? "0" : counterEvent.recurid.iso8601;
		currentSeries.value.counters = currentSeries.value.counters.stream().filter(counter -> {
			String counterRecId = counter.counter.recurid == null ? "0" : counter.counter.recurid.iso8601;
			return !(recId.equals(counterRecId));
		}).collect(Collectors.toList());

		cal.update(currentSeries.uid, currentSeries.value, false);

		return new IMIPResponse();
	}

}
