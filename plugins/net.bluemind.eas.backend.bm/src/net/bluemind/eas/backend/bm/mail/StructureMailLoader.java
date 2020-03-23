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
package net.bluemind.eas.backend.bm.mail;

import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;
import java.util.stream.Collectors;

import com.google.common.base.Splitter;

import net.bluemind.backend.mail.api.IMailboxItems;
import net.bluemind.backend.mail.api.MailboxItem;
import net.bluemind.backend.mail.api.MessageBody.Header;
import net.bluemind.backend.mail.api.MessageBody.Recipient;
import net.bluemind.backend.mail.api.MessageBody.RecipientKind;
import net.bluemind.backend.mail.api.flags.MailboxItemFlag;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.core.api.date.BmDateTime;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.eas.backend.BackendSession;
import net.bluemind.eas.backend.MSEvent;
import net.bluemind.eas.backend.MailFolder;
import net.bluemind.eas.backend.bm.calendar.EventConverter;
import net.bluemind.eas.backend.bm.compat.OldFormats;
import net.bluemind.eas.backend.bm.impl.CoreConnect;
import net.bluemind.eas.backend.bm.mail.loader.EventProvider;
import net.bluemind.eas.dto.calendar.CalendarResponse;
import net.bluemind.eas.dto.calendar.CalendarResponse.InstanceType;
import net.bluemind.eas.dto.email.EmailResponse;
import net.bluemind.eas.dto.email.EmailResponse.Flag.Status;
import net.bluemind.eas.dto.email.EmailResponse.LastVerbExecuted;
import net.bluemind.eas.dto.email.MessageClass;

/**
 * Creates a {@link EmailResponse} from an imap uid.
 * 
 * 
 */
public class StructureMailLoader extends CoreConnect {

	private final MailFolder folder;
	private final BackendSession bs;

	/**
	 * @param bf    the body factory used to process the body parts
	 * @param bs
	 * @param mbox
	 * @param store must be in selected state
	 */
	public StructureMailLoader(BackendSession bs, MailFolder folder) {
		this.folder = folder;
		this.bs = bs;
	}

	public EmailResponse fetch(int id) {
		IMailboxItems service = getMailboxItemsService(bs, folder.uid);
		ItemValue<MailboxItem> item = service.getCompleteById(id);
		if (item == null) {
			return null;
		}
		return fromMailboxItem(item.value);
	}

	private String format(List<Recipient> recipients, RecipientKind kind) {

		StringBuilder sb = new StringBuilder();
		boolean first = true;

		List<Recipient> filtered = recipients.stream().filter(r -> r.kind == kind).collect(Collectors.toList());

		for (Recipient r : filtered) {
			if (!first) {
				sb.append(", ");
			}
			String dn = r.dn;
			if (dn != null && !dn.trim().isEmpty()) {
				sb.append('"').append(dn).append("\" ");
			}

			sb.append('<').append(r.address).append('>');
			first = false;

		}
		return sb.toString();
	}

	private EmailResponse fromMailboxItem(MailboxItem item) {
		EmailResponse ret = new EmailResponse();
		ret.subject = item.body.subject;
		ret.threadTopic = ret.subject;
		ret.importance = EmailResponse.Importance.Normal;
		ret.contentClass = "urn:content-classes:message";
		ret.internetCPID = "65001";

		ret.from = format(item.body.recipients, RecipientKind.Originator);
		ret.to = format(item.body.recipients, RecipientKind.Primary);
		ret.cc = format(item.body.recipients, RecipientKind.CarbonCopy);

		ret.dateReceived = item.body.date;

		ret.read = item.flags.contains(MailboxItemFlag.System.Seen.value());
		ret.flag = new EmailResponse.Flag();

		if (item.flags.contains(MailboxItemFlag.System.Flagged.value())) {
			ret.flag.flagType = "Flag for follow-up";
			ret.flag.status = Status.Active;
		} else {
			ret.flag.status = Status.Cleared;
		}

		if (item.flags.contains(MailboxItemFlag.System.Answered.value())) {
			ret.lastVerbExecuted = LastVerbExecuted.ReplyToSender;
		} else if (item.flags.contains(new MailboxItemFlag("$Forwarded"))) {
			ret.lastVerbExecuted = LastVerbExecuted.Forward;
		}

		if (!"INBOX".equals(folder.fullName)) {
			return ret;
		}

		Optional<Header> cancel = item.body.headers.stream().filter(h -> "x-bm-event-canceled".equalsIgnoreCase(h.name))
				.findFirst();
		if (cancel.isPresent()) {
			String uid = cancel.get().firstValue();
			ret.meetingRequest = new CalendarResponse();
			ret.meetingRequest.uid = uid;
			ret.messageClass = MessageClass.ScheduleMeetingCanceled;
			return ret;
		}

		Optional<Header> header = item.body.headers.stream().filter(h -> "x-bm-event".equalsIgnoreCase(h.name))
				.findFirst();
		if (!header.isPresent()) {
			return ret;
		}

		String h = header.get().firstValue();
		Iterator<String> it = Splitter.on(";").trimResults().split(h).iterator();
		String eventUid = it.next();
		BmDateTime recurId = null;
		while (it.hasNext()) {
			String n = it.next();
			if (n.startsWith("recurid")) {
				List<String> rec = Splitter.on("=").trimResults().splitToList(n);
				if (rec.size() == 2) {
					String recId = rec.get(1);
					// substring to remove quotes
					recId = recId.substring(1, recId.length() - 1);
					recurId = BmDateTimeWrapper.create(recId);

				}
			} else if (n.startsWith("rsvp")) {
				// TODO RSVP -> msm.meetingRequest.responseRequested
			} else {
				logger.error("Unknown value {} for X-BM-Event", n);
			}
		}

		ItemValue<VEventSeries> vevent = new EventProvider(bs).get(eventUid);
		if (vevent != null) {
			EventConverter converter = new EventConverter();
			MSEvent msEvent = converter.convert(bs.getUser(), vevent);
			// FIXME add meetingMessageType into MeetingRequest
			ret.meetingRequest = OldFormats.update(msEvent, bs.getUser());
			ret.meetingRequest.itemUid = vevent.uid;
			ret.contentClass = "urn:content-classes:calendarmessage";
			ret.messageClass = MessageClass.ScheduleMeetingRequest;
			// msm.meetingMessageType =
			// MeetingMessageType.InitialMeetingRequest;

			ret.meetingRequest.instanceType = InstanceType.singleAppointment;

			if (msEvent.getRecurrence() != null) {
				ret.meetingRequest.instanceType = InstanceType.recurringMaster;
			}

			if (recurId != null) {
				// specific occurrence of a recurring event
				ret.meetingRequest.instanceType = InstanceType.exceptionToRecurring;
				// tz is GMT Sign Hours : Minutes. ex GMT+02:00
				TimeZone tz = TimeZone.getTimeZone("GMT" + recurId.timezone);
				Calendar begin = Calendar.getInstance(tz);
				begin.setTimeInMillis(new BmDateTimeWrapper(recurId).toTimestamp(tz.getID()));
				ret.meetingRequest.recurrenceId = begin.getTime();
			}
			logger.info("Found meeting request with uid {}, subject: {}", eventUid, vevent.value.main.summary);
		} else {
			logger.error("Fail to find meeting request with uid {}", eventUid);
		}

		return ret;
	}

}
