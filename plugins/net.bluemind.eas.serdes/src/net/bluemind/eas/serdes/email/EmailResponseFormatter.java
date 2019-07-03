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
package net.bluemind.eas.serdes.email;

import java.text.SimpleDateFormat;

import net.bluemind.eas.dto.NamespaceMapping;
import net.bluemind.eas.dto.base.Callback;
import net.bluemind.eas.dto.email.EmailResponse;
import net.bluemind.eas.serdes.IEasResponseFormatter;
import net.bluemind.eas.serdes.IResponseBuilder;
import net.bluemind.eas.serdes.calendar.CalendarResponseFormatter;

public class EmailResponseFormatter implements IEasResponseFormatter<EmailResponse> {

	public void append(IResponseBuilder b, double protocolVersion, EmailResponse email,
			Callback<IResponseBuilder> done) {

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy'-'MM'-'dd'T'HH':'mm':'ss.SSS'Z'");

		// Email
		if (notEmpty(email.to)) {
			b.text(NamespaceMapping.Email, "To", email.to);
		}
		if (notEmpty(email.cc)) {
			b.text(NamespaceMapping.Email, "Cc", email.cc);
		}
		if (notEmpty(email.from)) {
			b.text(NamespaceMapping.Email, "From", email.from);
		}
		if (notEmpty(email.subject)) {
			b.text(NamespaceMapping.Email, "Subject", email.subject);
		}
		if (notEmpty(email.replyTo)) {
			b.text(NamespaceMapping.Email, "ReplyTo", email.replyTo);
		}
		if (email.dateReceived != null) {
			b.text(NamespaceMapping.Email, "DateReceived", sdf.format(email.dateReceived));
		}
		if (notEmpty(email.displayTo)) {
			b.text(NamespaceMapping.Email, "DisplayTo", email.displayTo);
		}
		if (notEmpty(email.threadTopic)) {
			b.text(NamespaceMapping.Email, "ThreadTopic", email.threadTopic);
		}
		if (email.importance != null) {
			b.text(NamespaceMapping.Email, "Importance", email.importance.xmlValue());
		}

		b.text(NamespaceMapping.Email, "Read", email.read ? "1" : "0");

		if (email.messageClass != null) {
			b.text(NamespaceMapping.Email, "MessageClass", email.messageClass.toString(protocolVersion));
		}

		if (email.meetingRequest != null) {
			CalendarResponseFormatter calendarResponseFormatter = new CalendarResponseFormatter();
			calendarResponseFormatter.appendCalendarMeetingRequestResponse(b, protocolVersion, email.meetingRequest);
		}

		afterMeetingRequest(b, protocolVersion, email, sdf);

		done.onResult(b);
	}

	private void afterMeetingRequest(IResponseBuilder b, double protocolVersion, EmailResponse email,
			SimpleDateFormat sdf) {
		if (notEmpty(email.internetCPID)) {
			b.text(NamespaceMapping.Email, "InternetCPID", email.internetCPID);
		}

		if (email.flag != null) {
			b.container(NamespaceMapping.Email, "Flag");

			// FIXME not sure
			// TODO and not implemented in v3
			// TasksResponseFormatter tasksResponseFormatter = new
			// TasksResponseFormatter();
			// tasksResponseFormatter.appendTasksResponse(protocolVersion, flag,
			// email.flag.tasks);

			if (email.flag.status != null) {
				b.text(NamespaceMapping.Email, "Status", email.flag.status.xmlValue());
			}
			if (notEmpty(email.flag.flagType)) {
				b.text(NamespaceMapping.Email, "FlagType", email.flag.flagType);
			}
			b.endContainer();
		}

		if (notEmpty(email.contentClass)) {
			b.text(NamespaceMapping.Email, "ContentClass", email.contentClass);
		}

		if (email.categories != null) {
			b.container(NamespaceMapping.Email, "Categories");
			for (String c : email.categories) {
				b.text(NamespaceMapping.Email, "Category", c);
			}
			b.endContainer();
		}

		email2Namespace(b, protocolVersion, email, sdf);
	}

	private void email2Namespace(IResponseBuilder b, double protocolVersion, EmailResponse email,
			SimpleDateFormat sdf) {
		// Email2
		if (protocolVersion > 12.1) {
			if (notEmpty(email.umCallerID)) {
				b.text(NamespaceMapping.Email2, "UmCallerID", email.umCallerID);
			}
			if (notEmpty(email.umUserNotes)) {
				b.text(NamespaceMapping.Email2, "UmUserNotes", email.umUserNotes);
			}
			if (email.umAttOrder != null) {
				b.text(NamespaceMapping.Email2, "UmAttOrder", email.umAttOrder.toString());
			}
			if (notEmpty(email.conversationId)) {
				b.text(NamespaceMapping.Email2, "ConversationId", email.conversationId);
			}
			if (notEmpty(email.conversationIndex)) {
				b.text(NamespaceMapping.Email2, "ConversationIndex", email.conversationIndex);
			}
			if (email.lastVerbExecuted != null) {
				b.text(NamespaceMapping.Email2, "LastVerbExecuted", email.lastVerbExecuted.xmlValue());
			}
			if (email.lastVerbExecutionTime != null) {
				b.text(NamespaceMapping.Email2, "LastVerbExecutionTime", sdf.format(email.lastVerbExecutionTime));
			}
			if (email.receivedAsBcc != null) {
				b.text(NamespaceMapping.Email2, "ReceivedAsBcc", email.receivedAsBcc ? "1" : "0");
			}
			if (notEmpty(email.sender)) {
				b.text(NamespaceMapping.Email2, "Sender", email.sender);
			}
			if (email.calendarType != null) {
				b.text(NamespaceMapping.Email2, "CalendarType", email.calendarType.xmlValue());
			}
			if (email.isLeapMonth != null) {
				b.text(NamespaceMapping.Email2, "IsLeapMonth", email.isLeapMonth ? "1" : "0");
			}
			if (notEmpty(email.accountId)) {
				b.text(NamespaceMapping.Email2, "AccountId", email.accountId);
			}
			if (email.firstDayOfWeek != null) {
				b.text(NamespaceMapping.Email2, "FirstDayOfWeek", email.firstDayOfWeek.xmlValue());
			}
		}
	}

	private boolean notEmpty(String s) {
		return s != null && !s.trim().isEmpty();
	}

	@Override
	public void format(IResponseBuilder builder, double protocolVersion, EmailResponse response,
			Callback<Void> completion) {
		throw new RuntimeException("Not a full doc");

	}

}
