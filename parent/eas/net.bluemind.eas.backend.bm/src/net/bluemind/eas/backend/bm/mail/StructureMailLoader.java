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

import java.io.ByteArrayInputStream;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import net.bluemind.backend.mail.api.IMailboxItems;
import net.bluemind.backend.mail.api.MailboxItem;
import net.bluemind.backend.mail.api.MessageBody.Header;
import net.bluemind.backend.mail.api.MessageBody.Part;
import net.bluemind.backend.mail.api.MessageBody.Recipient;
import net.bluemind.backend.mail.api.MessageBody.RecipientKind;
import net.bluemind.backend.mail.api.flags.MailboxItemFlag;
import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.calendar.api.ICalendarUids.UserCalendarType;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.calendar.helper.ical4j.VEventServiceHelper;
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
import net.bluemind.eas.backend.bm.mail.loader.MailAttachmentProvider;
import net.bluemind.eas.backend.bm.user.UserBackend;
import net.bluemind.eas.dto.calendar.CalendarResponse;
import net.bluemind.eas.dto.calendar.CalendarResponse.InstanceType;
import net.bluemind.eas.dto.email.EmailResponse;
import net.bluemind.eas.dto.email.EmailResponse.Flag.Status;
import net.bluemind.eas.dto.email.EmailResponse.LastVerbExecuted;
import net.bluemind.eas.dto.email.Importance;
import net.bluemind.eas.dto.email.MessageClass;
import net.bluemind.eas.utils.EasLogUser;
import net.bluemind.icalendar.api.ICalendarElement.Classification;
import net.bluemind.utils.HeaderUtil;

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

	public EmailResponse fetch(long id) {
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
		ret.importance = Importance.NORMAL;
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
			ret.flag.status = Status.ACTIVE;
		} else {
			ret.flag.status = Status.CLEARED;
		}

		if (item.flags.contains(MailboxItemFlag.System.Answered.value())) {
			ret.lastVerbExecuted = LastVerbExecuted.REPLY_TO_SENDER;
		} else if (item.flags.contains(new MailboxItemFlag("$Forwarded"))) {
			ret.lastVerbExecuted = LastVerbExecuted.FORWARD;
		}

		ret.isDraft = item.flags.contains(MailboxItemFlag.System.Draft.value()) || "Drafts".equals(folder.fullName);

		if (!"INBOX".equals(folder.fullName)) {
			return ret;
		}

		Optional<Header> cancel = item.body.headers.stream().filter(h -> "x-bm-event-canceled".equalsIgnoreCase(h.name))
				.findFirst();
		if (cancel.isPresent()) {
			HeaderUtil hUtil = new HeaderUtil(cancel.get().firstValue());
			Optional<Part> ics = item.body.structure.attachments().stream()
					.filter(part -> part.fileName.endsWith(".ics")).findFirst();
			ret.meetingRequest = ics.map(icsAttachment -> icsToMeetingRequest(item.imapUid, icsAttachment))
					.orElse(new CalendarResponse());
			ret.meetingRequest.uid = hUtil.getHeaderValue().map(HeaderUtil.Value::toString).orElseThrow();
			ret.messageClass = MessageClass.SCHEDULE_MEETING_CANCELED;
			return ret;
		}

		Optional<Header> header = item.body.headers.stream().filter(h -> "x-bm-event".equalsIgnoreCase(h.name))
				.findFirst();
		if (!header.isPresent()) {
			return ret;
		}

		HeaderUtil hUtil = new HeaderUtil(header.get().firstValue());
		String eventUid = hUtil.getHeaderValue().map(HeaderUtil.Value::toString).orElseThrow();

		// For delegation case, must get the calendar of the person who hand the
		// delegation over
		Optional<Header> headerXBmCalendar = item.body.headers.stream()
				.filter(h -> "x-bm-calendar".equalsIgnoreCase(h.name)).findFirst();

		String calendarUid = getCalendarUid(headerXBmCalendar);
		ret.calendarUid = calendarUid;

		ItemValue<VEventSeries> vevent = new EventProvider(bs).get(calendarUid, eventUid);
		if (vevent != null) {
			boolean isInvitationASimpleMessage = userCanRespond(item, vevent, ret, calendarUid);
			EventConverter converter = new EventConverter();
			MSEvent msEvent = converter.convert(bs, vevent);
			// FIXME add meetingMessageType into MeetingRequest
			ret.meetingRequest = OldFormats.update(msEvent, bs.getUser());
			ret.meetingRequest.itemUid = vevent.internalId;
			ret.contentClass = "urn:content-classes:calendarmessage";
			ret.messageClass = MessageClass.SCHEDULE_MEETING_REQUEST;
			// msm.meetingMessageType =
			// MeetingMessageType.InitialMeetingRequest;

			ret.meetingRequest.instanceType = InstanceType.SINGLE_APPOINTMENT;

			if (msEvent.getRecurrence() != null) {
				ret.meetingRequest.instanceType = InstanceType.RECURRING_MASTER;
			}

			BmDateTime recurId = hUtil.getHeaderAttribute("recurid").map(HeaderUtil.Value::toDate).orElse(null);
			if (recurId != null) {
				// specific occurrence of a recurring event
				ret.meetingRequest.instanceType = InstanceType.EXCEPTION_TO_RECURRING;
				// tz is GMT Sign Hours : Minutes. ex GMT+02:00
				TimeZone tz = TimeZone.getTimeZone("GMT" + recurId.timezone);
				Calendar begin = Calendar.getInstance(tz);
				begin.setTimeInMillis(new BmDateTimeWrapper(recurId).toTimestamp(tz.getID()));
				ret.meetingRequest.recurrenceId = begin.getTime();
			}
			if (isInvitationASimpleMessage) {
				transformReadOnlyRequestToSimpleEmail(ret);
			}
			EasLogUser.logInfoAsUser(bs.getLoginAtDomain(), logger, "Found meeting request with uid {}, subject: {}",
					eventUid, vevent.value.main.summary);
		} else {
			EasLogUser.logErrorAsUser(bs.getLoginAtDomain(), logger, "Fail to find meeting request with uid {}",
					eventUid);
		}

		return ret;
	}

	private CalendarResponse icsToMeetingRequest(long imapUid, Part ics) {
		CalendarResponse calResponse;
		try {
			byte[] fetchedPart = new MailAttachmentProvider(bs).fetchPart(ics, imapUid, folder.uid);
			List<ItemValue<VEventSeries>> ret = new LinkedList<>();
			Consumer<ItemValue<VEventSeries>> consumer = ret::add;
			VEventServiceHelper.parseCalendar(new ByteArrayInputStream(fetchedPart), Optional.empty(),
					Collections.emptyList(), consumer);
			EventConverter converter = new EventConverter();
			MSEvent msEvent = converter.convert(bs, ret.get(0));
			calResponse = OldFormats.update(msEvent, bs.getUser());
		} catch (Exception e) {
			EasLogUser.logWarnAsUser(bs.getLoginAtDomain(), logger, "Cannot transform ics to CalendarResponse", e);
			return new CalendarResponse();
		}
		return calResponse;
	}

	private void transformReadOnlyRequestToSimpleEmail(EmailResponse ret) {
		ret.messageClass = MessageClass.NOTE;
		ret.meetingRequest = null;
	}

	private String getCalendarUid(Optional<Header> headerXBmCalendar) {
		if (headerXBmCalendar.isEmpty()) {
			return ICalendarUids.defaultUserCalendar(bs.getUser().getUid());
		}
		HeaderUtil hUtilXBmCalendar = new HeaderUtil(headerXBmCalendar.get().firstValue());
		return hUtilXBmCalendar.getHeaderValue().map(HeaderUtil.Value::toString).orElseThrow();
	}

	private boolean userCanRespond(MailboxItem item, ItemValue<VEventSeries> vevent, EmailResponse ret,
			String calendarUid) {
		boolean isHeaderEventReadOnly = item.flags.stream().anyMatch(f -> f.flag.equals("BmEventReadOnly"));
		String calendarOwnerUid = ret.calendarUid.replace(ICalendarUids.TYPE + ":" + UserCalendarType.Default + ":", "")
				.trim();
		boolean isBackendUserIsDelegator = bs.getUser().getUid().equals(calendarOwnerUid);

		UserBackend userBackend = new UserBackend();
		boolean canUserAnswerPrivateEvent = vevent.value.main.classification.equals(Classification.Public)
				|| (userBackend.userHasRoleReadExtended(bs, calendarUid)
						&& vevent.value.main.classification.equals(Classification.Private));

		if (isBackendUserIsDelegator) {
			return isHeaderEventReadOnly;
		}

		return !canUserAnswerPrivateEvent;

	}
}
