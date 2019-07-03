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

import java.util.HashSet;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.calendar.api.ICalendar;
import net.bluemind.calendar.api.VEventOccurrence;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.core.api.date.BmDateTime;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.imip.parser.IMIPInfos;
import net.bluemind.lmtp.backend.LmtpAddress;
import net.bluemind.lmtp.backend.PermissionDeniedException.MailboxInvitationDeniedException;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

/**
 * Handles cancellations of meetings
 * 
 * @author tom
 * 
 */
public class EventCancelHandler extends CancelHandler implements IIMIPHandler {

	private static final Logger logger = LoggerFactory.getLogger(EventCancelHandler.class);

	@Override
	public IMIPResponse handle(IMIPInfos imip, LmtpAddress recipient, ItemValue<Domain> domain,
			ItemValue<Mailbox> recipientMailbox) throws ServerFault {

		if (!super.validate(imip)) {
			return new IMIPResponse();
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
			VEventSeries series = fromList(imip.iCalendarElements, imip.uid);
			List<ItemValue<VEventSeries>> currentSeries = cal.getByIcsUid(imip.uid);
			if (currentSeries.isEmpty()) {
				logger.warn("BM VEvent with event uid {} not found in calendar {}", imip.uid, calUid);
				return new IMIPResponse();
			}
			if (null == series.main) {
				List<VEventOccurrence> occurrences = series.occurrences;

				// real series
				if (currentSeries.size() == 1 && currentSeries.get(0).value.main != null) {
					ItemValue<VEventSeries> master = currentSeries.get(0);
					for (VEventOccurrence occurrence : occurrences) {
						// Add exdate
						// BM-10462
						logger.info("[{}] Remove event exception id {}, reccurid {}, in calendar {} ", imip.messageId,
								imip.uid, occurrence.recurid, calUid);
						if (master.value.main.exdate == null) {
							master.value.main.exdate = new HashSet<BmDateTime>(1);
						}
						master.value.main.exdate.add(occurrence.recurid);

						master.value.occurrences.remove(master.value.occurrence(occurrence.recurid));
					}
					if (master.value.main == null && master.value.occurrences.isEmpty()) {
						cal.delete(master.uid, false);
					} else {
						cal.update(master.uid, master.value, false);
					}
				} else {
					// only occurence
					for (VEventOccurrence occurrence : occurrences) {
						for (ItemValue<VEventSeries> oneOccurence : currentSeries) {
							if (oneOccurence.value.occurrence(occurrence.recurid) != null) {
								cal.delete(oneOccurence.uid, false);
							}
						}
					}
				}
			} else {
				logger.info("[{}] Deleting BM Event with ics uid {} in calendar {} ", imip.messageId, imip.uid, calUid);
				for (ItemValue<VEventSeries> oneOccurence : currentSeries) {
					cal.delete(oneOccurence.uid, false);
				}
			}
			return new IMIPResponse();
		} catch (Exception e) {
			throw e;
		}
	}

}
