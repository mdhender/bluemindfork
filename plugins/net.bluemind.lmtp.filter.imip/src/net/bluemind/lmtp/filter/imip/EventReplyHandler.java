/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.calendar.api.ICalendar;
import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.api.VEventOccurrence;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.calendar.occurrence.OccurrenceHelper;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.icalendar.api.ICalendarElement;
import net.bluemind.icalendar.api.ICalendarElement.Attendee;
import net.bluemind.imip.parser.IMIPInfos;
import net.bluemind.lmtp.backend.LmtpAddress;
import net.bluemind.mailbox.api.Mailbox;

/**
 * Handles external replies : user (organizer) is in bm domain. Process the
 * REPLY email from the external contact.
 * 
 * @author tom
 * 
 */
public class EventReplyHandler extends ReplyHandler implements IIMIPHandler {

	public EventReplyHandler(LmtpAddress recipient, LmtpAddress sender) {
		super(recipient, sender);
	}

	private static final Logger logger = LoggerFactory.getLogger(EventReplyHandler.class);

	@Override
	public IMIPResponse handle(IMIPInfos imip, LmtpAddress recipient, ItemValue<Domain> domain,
			ItemValue<Mailbox> recipientMailbox) throws ServerFault {
		String calUid = getCalendarUid(recipientMailbox);
		ICalendar cal = provider().instance(ICalendar.class, calUid);
		List<ItemValue<VEventSeries>> items = getAndValidateExistingSeries(cal, imip);
		ItemValue<VEventSeries> series = items.get(0);
		for (ICalendarElement element : imip.iCalendarElements) {
			VEvent vevent = (VEvent) element;
			List<VEvent.Attendee> atts = vevent.attendees;

			if (!super.validate(imip, atts)) {
				return new IMIPResponse();
			}

			VEvent ref = null;
			if (vevent.exception()) {
				VEventOccurrence occ = (VEventOccurrence) vevent;

				// sanitize recurid to match master dtstart timezone
				occ.recurid = BmDateTimeWrapper.create(occ.recurid.iso8601, series.value.main.dtstart.timezone,
						series.value.main.dtstart.precision);
				Optional<VEventOccurrence> result = OccurrenceHelper.getOccurrenceByRecurId(series, occ.recurid);
				if (!result.isPresent()) {
					logger.warn("Occurrence {} from series {} does not exist. Skipping.", occ.recurid, series.uid);
					continue;
				}
				if (series.value.occurrence(result.get().recurid) == null) {
					List<VEventOccurrence> occurrences = new ArrayList<>(series.value.occurrences);
					occurrences.add(result.get());
					series.value.occurrences = occurrences;
				}
				ref = result.get();
			} else {
				ref = series.value.main;
			}
			for (VEvent.Attendee attendee : atts) {
				mergeAttendeesPartStatus(ref, attendee);
			}

		}
		logger.info("Updating event series {}", series.uid);
		cal.update(series.uid, series.value, false);
		return new IMIPResponse();
	}

	private void mergeAttendeesPartStatus(VEvent event, Attendee attendee) {
		for (Attendee a : event.attendees) {
			if (a.mailto.equals(attendee.mailto)) {
				if (a.partStatus != attendee.partStatus) {
					logger.info("[{}] Update participation of {}: {} => {}", event.summary, attendee.mailto,
							a.partStatus, attendee.partStatus);
				}
				a.partStatus = attendee.partStatus;
				a.responseComment = attendee.responseComment;
				a.rsvp = false;
				return;
			}
		}
		event.attendees = new ArrayList<>(event.attendees);
		event.attendees.add(attendee);
	}

}
