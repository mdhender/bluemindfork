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
package net.bluemind.eas.dto.email;

import java.util.Date;
import java.util.List;

import net.bluemind.eas.dto.calendar.CalendarResponse;
import net.bluemind.eas.dto.calendar.CalendarResponse.FirstDayOfWeek;
import net.bluemind.eas.dto.calendar.CalendarResponse.Recurrence.CalendarType;
import net.bluemind.eas.dto.tasks.TasksResponse;

public class EmailResponse {

	public static enum Importance {

		Low(0), Normal(1), High(2);

		private final String xmlValue;

		private Importance(int value) {
			xmlValue = Integer.toString(value);
		}

		public String xmlValue() {
			return xmlValue;
		}
	}

	// public static enum MeetingMessageType {
	//
	// SlientUpdate(0), InitialMeetingRequest(1), FullUpdate(2), //
	// InformationalUpdate(3), Outdated(4), IdentifyDelegatorsCopy(5),
	// hasBeenDelegatedAndNotResponded(6);
	//
	// private final String xmlValue;
	//
	// private MeetingMessageType(int value) {
	// xmlValue = Integer.toString(value);
	// }
	//
	// public String xmlValue() {
	// return xmlValue;
	// }
	// }

	public static enum LastVerbExecuted {

		Unknown(0), ReplyToSender(1), ReplyToAll(2), Forward(3);

		private final String xmlValue;

		private LastVerbExecuted(int value) {
			xmlValue = Integer.toString(value);
		}

		public String xmlValue() {
			return xmlValue;
		}
	}

	public static final class Flag {
		public static enum Status {

			Cleared(0), Complete(1), Active(2);

			private final String xmlValue;

			private Status(int value) {
				xmlValue = Integer.toString(value);
			}

			public String xmlValue() {
				return xmlValue;
			}
		}

		public TasksResponse tasks;
		public String flagType;
		public Status status;

	}

	public String to;
	public String cc;
	public String from;
	public String subject;
	public String replyTo;
	public Date dateReceived;
	public String displayTo;
	public String threadTopic;
	public Importance importance;
	public boolean read;
	public MessageClass messageClass = MessageClass.Note;
	public CalendarResponse meetingRequest;
	public String internetCPID;
	public Flag flag;
	public String contentClass;
	public List<String> categories;
	public String umCallerID;
	public String umUserNotes;
	public Integer umAttDuration;
	public Integer umAttOrder;
	public String conversationId;
	public String conversationIndex;
	public LastVerbExecuted lastVerbExecuted;
	public Date lastVerbExecutionTime;
	public Boolean receivedAsBcc;
	public String sender;
	public CalendarType calendarType;
	public Boolean isLeapMonth;
	public String accountId;
	public FirstDayOfWeek firstDayOfWeek;
	// public MeetingMessageType meetingMessageType;

}
