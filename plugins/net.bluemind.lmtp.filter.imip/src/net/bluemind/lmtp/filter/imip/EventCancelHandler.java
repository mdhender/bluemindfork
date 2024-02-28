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

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.calendar.api.ICalendar;
import net.bluemind.calendar.api.VEventOccurrence;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.delivery.lmtp.common.LmtpAddress;
import net.bluemind.delivery.lmtp.common.ResolvedBox;
import net.bluemind.delivery.lmtp.filters.PermissionDeniedException.MailboxInvitationDeniedException;
import net.bluemind.domain.api.Domain;
import net.bluemind.icalendar.api.ICalendarElement;
import net.bluemind.icalendar.api.ICalendarElement.Classification;
import net.bluemind.imip.parser.IMIPInfos;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

/**
 * Handles cancellations of meetings
 * 
 * 
 */
public class EventCancelHandler extends CancelHandler implements IIMIPHandler {

	public EventCancelHandler(ResolvedBox recipient, LmtpAddress sender) {
		super(recipient, sender);
	}

	private static final Logger logger = LoggerFactory.getLogger(EventCancelHandler.class);

	@Override
	public IMIPResponse handle(IMIPInfos imip, ResolvedBox recipient, ItemValue<Domain> domain,
			ItemValue<Mailbox> recipientMailbox) throws ServerFault {

		if (!super.validate(imip)) {
			return IMIPResponse.createEmptyResponse();
		}

		try {
			String calUid = getCalendarUid(recipientMailbox);

			// BM-2892 invitation right
			IUser userService = provider().instance(IUser.class, recipient.getDomainPart());
			ItemValue<User> sender = userService.byEmail(imip.organizerEmail);
			if (sender != null) {
				boolean canInvite = checkInvitationRight(recipient, calUid, sender);
				if (!canInvite) {
					ServerFault fault = new ServerFault(new MailboxInvitationDeniedException(recipientMailbox.uid));
					fault.setCode(ErrorCode.PERMISSION_DENIED);
					throw fault;
				}

			} // else external, don't care for now
			ICalendar cal = provider().instance(ICalendar.class, calUid);
			VEventSeries series = fromList(imip.properties, imip.iCalendarElements, imip.uid);
			List<ItemValue<VEventSeries>> currentSeries = cal.getByIcsUid(imip.uid);
			if (currentSeries.isEmpty()) {
				logger.warn("BM VEvent with event uid {} not found in calendar {}", imip.uid, calUid);
				return IMIPResponse.createEmptyResponse();
			}

			if (null == series.main) {
				List<VEventOccurrence> occurrences = series.occurrences;

				// real series
				if (currentSeries.size() == 1 && currentSeries.get(0).value.main != null) {
					ItemValue<VEventSeries> master = currentSeries.get(0);
					for (VEventOccurrence occurrence : occurrences) {
						logger.info("[{}] Cancelling event exception id {}, reccurid {}, in calendar {} ",
								imip.messageId, imip.uid, occurrence.recurid, calUid);
						VEventOccurrence occ = master.value.occurrence(occurrence.recurid);
						if (occ != null) {
							occ.status = ICalendarElement.Status.Cancelled;
							occ.sequence = occurrence.sequence;
						} else {
							if (master.value.occurrences.isEmpty()) {
								master.value.occurrences = Arrays.asList(occurrence);
							} else {
								master.value.occurrences.add(occurrence);
							}
						}
					}
					cal.update(master.uid, master.value, false);
				} else {
					// only occurence
					for (VEventOccurrence occurrence : occurrences) {
						for (ItemValue<VEventSeries> occurenceSerie : currentSeries) {
							VEventOccurrence oneOccurrence = occurenceSerie.value.occurrence(occurrence.recurid);
							if (occurenceSerie.value.occurrence(occurrence.recurid) != null) {
								oneOccurrence.status = ICalendarElement.Status.Cancelled;
								oneOccurrence.sequence = occurrence.sequence;
								cal.update(occurenceSerie.uid, occurenceSerie.value, false);
							}
						}
					}
				}
			} else {
				logger.info("[{}] Cancelling BM Event with ics uid {} in calendar {} ", imip.messageId, imip.uid,
						calUid);
				for (ItemValue<VEventSeries> oneOccurence : currentSeries) {
					oneOccurence.value.main.status = ICalendarElement.Status.Cancelled;
					oneOccurence.value.main.sequence = series.main.sequence;
					cal.update(oneOccurence.uid, oneOccurence.value, false);
				}
			}

			if (imipMessageContainsASingleException(series)) {
				VEventOccurrence vEventOccurrence = series.occurrences.get(0);
				return IMIPResponse.createCanceledExceptionResponse(imip.uid, vEventOccurrence.recurid.iso8601, calUid,
						vEventOccurrence.classification == Classification.Private);
			} else {
				return IMIPResponse.createCanceledResponse(imip.uid, calUid,
						series.flatten().get(0).classification == Classification.Private);
			}
		} catch (Exception e) {
			throw e;
		}
	}

	private boolean imipMessageContainsASingleException(VEventSeries series) {
		return series.occurrences.size() == 1 && series.main == null;
	}

}
