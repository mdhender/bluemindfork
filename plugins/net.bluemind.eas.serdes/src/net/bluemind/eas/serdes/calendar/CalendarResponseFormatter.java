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
package net.bluemind.eas.serdes.calendar;

import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import net.bluemind.eas.dto.NamespaceMapping;
import net.bluemind.eas.dto.base.Callback;
import net.bluemind.eas.dto.calendar.CalendarResponse;
import net.bluemind.eas.dto.calendar.CalendarResponse.Attendee;
import net.bluemind.eas.dto.calendar.CalendarResponse.EventException;
import net.bluemind.eas.dto.calendar.CalendarResponse.InstanceType;
import net.bluemind.eas.serdes.FastDateFormat;
import net.bluemind.eas.serdes.IEasFragmentFormatter;
import net.bluemind.eas.serdes.IResponseBuilder;
import net.bluemind.lib.globalid.ExtIdConverter;

public class CalendarResponseFormatter implements IEasFragmentFormatter<CalendarResponse> {

	@Override
	public void append(IResponseBuilder b, double protocolVersion, CalendarResponse calendar,
			Callback<IResponseBuilder> cb) {
		if (notEmpty(calendar.timezone)) {
			b.text(NamespaceMapping.CALENDAR, "Timezone", calendar.timezone);
		}
		if (calendar.dtStamp != null) {
			b.text(NamespaceMapping.CALENDAR, "DtStamp", FastDateFormat.format(calendar.dtStamp));
		}
		if (calendar.startTime != null) {
			b.text(NamespaceMapping.CALENDAR, "StartTime", FastDateFormat.format(calendar.startTime));
		}
		if (notEmpty(calendar.subject)) {
			b.text(NamespaceMapping.CALENDAR, "Subject", calendar.subject);
		}
		if (notEmpty(calendar.location)) {
			if (protocolVersion > 14.1) {
				b.container(NamespaceMapping.AIR_SYNC_BASE, "Location");
				b.text(NamespaceMapping.AIR_SYNC_BASE, "DisplayName", calendar.location);
				b.endContainer();
			} else {
				b.text(NamespaceMapping.CALENDAR, "Location", calendar.location);
			}
		}
		if (notEmpty(calendar.uid)) {
			b.text(NamespaceMapping.CALENDAR, "UID", calendar.uid);
		}
		if (notEmpty(calendar.organizerName)) {
			b.text(NamespaceMapping.CALENDAR, "OrganizerName", calendar.organizerName);
		}
		if (notEmpty(calendar.organizerEmail)) {
			b.text(NamespaceMapping.CALENDAR, "OrganizerEmail", calendar.organizerEmail);
		}
		if (calendar.endTime != null) {
			b.text(NamespaceMapping.CALENDAR, "EndTime", FastDateFormat.format(calendar.endTime));
		}
		if (calendar.sensitivity != null) {
			b.text(NamespaceMapping.CALENDAR, "Sensitivity", calendar.sensitivity.xmlValue());
		}
		if (calendar.busyStatus != null) {
			b.text(NamespaceMapping.CALENDAR, "BusyStatus", calendar.busyStatus.xmlValue());
		}

		if (calendar.allDayEvent != null) {
			b.text(NamespaceMapping.CALENDAR, "AllDayEvent", calendar.allDayEvent ? "1" : "0");
		}

		if (calendar.reminder != null) {
			b.text(NamespaceMapping.CALENDAR, "Reminder", calendar.reminder.toString());
		}

		if (calendar.meetingStatus != null) {
			b.text(NamespaceMapping.CALENDAR, "MeetingStatus", calendar.meetingStatus.xmlValue());
		}

		appendAttendees(b, calendar.attendees);

		if (calendar.categories != null) {
			b.container(NamespaceMapping.CALENDAR, "Categories");
			for (String c : calendar.categories) {
				b.text(NamespaceMapping.CALENDAR, "Category", c);
			}
			b.endContainer();
		}

		if (calendar.recurrence != null) {
			b.container(NamespaceMapping.CALENDAR, "Recurrence");
			if (calendar.recurrence.type != null) {
				b.text("Type", calendar.recurrence.type.xmlValue());
			}
			if (calendar.recurrence.occurrences != null) {
				b.text("Occurrences", calendar.recurrence.occurrences.toString());
			}
			if (calendar.recurrence.interval != null) {
				b.text("Interval", calendar.recurrence.interval.toString());
			}
			if (calendar.recurrence.weekOfMonth != null) {
				b.text("WeekOfMonth", calendar.recurrence.weekOfMonth.toString());
			}
			if (calendar.recurrence.dayOfWeek != null) {
				b.text("DayOfWeek", calendar.recurrence.dayOfWeek.xmlValue());
			}
			if (calendar.recurrence.monthOfYear != null) {
				b.text("MonthOfYear", calendar.recurrence.monthOfYear.toString());
			}
			if (calendar.recurrence.until != null) {
				Calendar c = Calendar.getInstance();
				c.setTime(calendar.recurrence.until);
				// year 2038 problem on Android
				if (c.get(Calendar.YEAR) < 2037) {
					b.text("Until", FastDateFormat.format(c.getTime()));
				}
			}
			if (calendar.recurrence.dayOfMonth != null) {
				b.text("DayOfMonth", calendar.recurrence.dayOfMonth.toString());
			}
			if (protocolVersion > 12.1) {
				if (calendar.recurrence.calendarType != null) {
					b.text("CalendarType", calendar.recurrence.calendarType.xmlValue());
				}
				if (calendar.recurrence.isLeapMonth != null) {
					b.text("IsLeapMonth", calendar.recurrence.isLeapMonth ? "1" : "0");
				}
				if (protocolVersion > 14.0 && calendar.recurrence.firstDayOfWeek != null) {
					b.text("FirstDayOfWeek", calendar.recurrence.firstDayOfWeek.xmlValue());
				}
			}
			b.endContainer();
		}

		if (calendar.exceptions != null && !calendar.exceptions.isEmpty()) {
			b.container(NamespaceMapping.CALENDAR, "Exceptions");
			for (EventException e : calendar.exceptions) {
				b.container("Exception");
				if (e.deleted != null) {
					b.text("Deleted", e.deleted ? "1" : "0");
				}
				if (e.exceptionStartTime != null) {
					b.text("ExceptionStartTime", FastDateFormat.format(e.exceptionStartTime));
				}
				if (notEmpty(e.calendar.subject)) {
					b.text("Subject", e.calendar.subject);
				}
				if (e.calendar.startTime != null) {
					b.text("StartTime", FastDateFormat.format(e.calendar.startTime));
				}
				if (e.calendar.endTime != null) {
					b.text("EndTime", FastDateFormat.format(e.calendar.endTime));
				}

				// TODO Airsync:Body ?
				if (notEmpty(e.location)) {
					b.text("Location", e.location);
				}
				if (e.calendar.categories != null) {
					b.container("Categories");
					for (String c : e.calendar.categories) {
						b.text("Category", c);
					}
					b.endContainer();
				}
				if (e.calendar.sensitivity != null) {
					b.text("Sensitivity", e.calendar.sensitivity.xmlValue());
				}
				if (e.calendar.busyStatus != null) {
					b.text("BusyStatus", e.calendar.busyStatus.xmlValue());
				}
				if (e.calendar.allDayEvent != null) {
					b.text("AllDayEvent", e.calendar.allDayEvent.booleanValue() ? "1" : "0");
				}
				if (e.calendar.reminder != null) {
					b.text("Reminder", e.calendar.reminder.toString());
				}
				if (e.calendar.dtStamp != null) {
					b.text("DtStamp", FastDateFormat.format(e.calendar.dtStamp));
				}
				if (protocolVersion > 14.0 && e.calendar.meetingStatus != null) {
					b.text("MeetingStatus", e.calendar.meetingStatus.xmlValue());
				}

				if (protocolVersion > 12.1) {
					appendAttendees(b, e.calendar.attendees);

					if (e.appointmentReplyTime != null) {
						b.text(NamespaceMapping.CALENDAR, "AppointmentReplyTime",
								FastDateFormat.format(e.appointmentReplyTime));
					}

					if (e.responseType != null) {
						b.text("ResponseType", e.responseType.xmlValue());
					}
				}

				if (protocolVersion > 14.0 && notEmpty(e.onlineMeetingConfLink)) {
					b.text("OnlineMeetingConfLink", e.onlineMeetingConfLink);
				}
				if (protocolVersion > 14.0 && notEmpty(e.onlineMeetingExternalLink)) {
					b.text("OnlineMeetingExternalLink", e.onlineMeetingExternalLink);
				}
				b.endContainer(); // Exception
			}
			b.endContainer(); // Exceptions
		}

		if (protocolVersion > 12.1) {
			if (calendar.responseRequested != null) {
				b.text(NamespaceMapping.CALENDAR, "ResponseRequested",
						calendar.responseRequested.booleanValue() ? "1" : "0");
			}

			if (calendar.appointmentReplyTime != null) {
				b.text(NamespaceMapping.CALENDAR, "AppointmentReplyTime",
						FastDateFormat.format(calendar.appointmentReplyTime));
			}
			if (calendar.responseType != null) {
				b.text(NamespaceMapping.CALENDAR, "ResponseType", calendar.responseType.xmlValue());
			}
			if (calendar.disallowNewTimeProposal != null) {
				b.text(NamespaceMapping.CALENDAR, "DisallowNewTimeProposal",
						calendar.disallowNewTimeProposal.booleanValue() ? "1" : "0");
			}
			if (protocolVersion > 14) {
				if (notEmpty(calendar.onlineMeetingConfLink)) {
					b.text(NamespaceMapping.CALENDAR, "OnlineMeetingConfLink", calendar.onlineMeetingConfLink);
				}
				if (notEmpty(calendar.onlineMeetingExternalLink)) {
					b.text(NamespaceMapping.CALENDAR, "OnlineMeetingExternalLink", calendar.onlineMeetingExternalLink);
				}
			}
		}

		if (protocolVersion > 14.1) {
			appendAttachments(b, calendar);
		}

		cb.onResult(b);
	}

	private void appendAttachments(IResponseBuilder responseBuilder, CalendarResponse calendar) {
		if (calendar.attachments != null && !calendar.attachments.isEmpty()) {
			responseBuilder.container(NamespaceMapping.AIR_SYNC_BASE, "Attachments");
			calendar.attachments.forEach(attachment -> {
				responseBuilder.container("Attachment");
				if (attachment.displayName != null) {
					responseBuilder.text("DisplayName", attachment.displayName);
				}
				if (attachment.fileReference != null) {
					responseBuilder.text("FileReference", attachment.fileReference);
				}
				if (attachment.method != null) {
					responseBuilder.text("AttMethod", attachment.method.xmlValue());
				}
				if (attachment.estimateDataSize != null) {
					responseBuilder.text("EstimatedDataSize", attachment.estimateDataSize.toString());
				}

				responseBuilder.endContainer(); // Attachment
			});
			responseBuilder.endContainer(); // Attachments
		}
	}

	public void appendCalendarMeetingRequestResponse(IResponseBuilder b, double protocolVersion,
			CalendarResponse calendar) {
		b.container(NamespaceMapping.EMAIL, "MeetingRequest");

		if (calendar.allDayEvent != null) {
			b.text(NamespaceMapping.EMAIL, "AllDayEvent", calendar.allDayEvent ? "1" : "0");
		}
		if (calendar.startTime != null) {
			b.text(NamespaceMapping.EMAIL, "StartTime", MeetingRequestFastDateFormat.format(calendar.startTime));
		}
		if (calendar.dtStamp != null) {
			b.text(NamespaceMapping.EMAIL, "DtStamp", MeetingRequestFastDateFormat.format(calendar.dtStamp));
		}
		if (calendar.endTime != null) {
			b.text(NamespaceMapping.EMAIL, "EndTime", MeetingRequestFastDateFormat.format(calendar.endTime));
		}

		if (calendar.instanceType != null) {
			b.text(NamespaceMapping.EMAIL, "InstanceType", calendar.instanceType.xmlValue());
		}

		if (calendar.instanceType == InstanceType.EXCEPTION_TO_RECURRING) {
			b.text(NamespaceMapping.EMAIL, "RecurrenceId", MeetingRequestFastDateFormat.format(calendar.recurrenceId));
		}

		if (notEmpty(calendar.location)) {
			if (protocolVersion > 14.1) {
				b.container(NamespaceMapping.AIR_SYNC_BASE, "Location");
				b.text(NamespaceMapping.AIR_SYNC_BASE, "DisplayName", calendar.location);
				b.endContainer();
			} else {
				b.text(NamespaceMapping.EMAIL, "Location", calendar.location);
			}
		}

		if (notEmpty(calendar.organizerName) && notEmpty(calendar.organizerEmail)) {
			b.text(NamespaceMapping.EMAIL, "Organizer", calendar.organizerName + " <" + calendar.organizerEmail + ">");
		} else if (notEmpty(calendar.organizerEmail)) {
			b.text(NamespaceMapping.EMAIL, "Organizer", calendar.organizerEmail + " <" + calendar.organizerEmail + ">");
		} else if (notEmpty(calendar.organizerName)) {
			b.text(NamespaceMapping.EMAIL, "Organizer", calendar.organizerName);
		}

		// TODO RecurrenceId

		if (calendar.reminder != null) {
			b.text(NamespaceMapping.EMAIL, "Reminder", calendar.reminder.toString());
		}

		boolean responseRequested = false;
		Date now = new Date();
		if (calendar.startTime != null && now.before(calendar.startTime)) {
			responseRequested = true;
		}
		if (calendar.recurrence != null) {
			if (calendar.recurrence.until != null && calendar.recurrence.until.before(now)) {
				responseRequested = false;
			} else {
				responseRequested = true;
			}
		}
		b.text(NamespaceMapping.EMAIL, "ResponseRequested", responseRequested ? "1" : "0");

		if (calendar.recurrence != null) {
			b.container(NamespaceMapping.EMAIL, "Recurrences").container("Recurrence");

			if (calendar.recurrence.type != null) {
				b.text(NamespaceMapping.EMAIL, "Type", calendar.recurrence.type.xmlValue());
			}
			if (calendar.recurrence.interval != null) {
				b.text(NamespaceMapping.EMAIL, "Interval", calendar.recurrence.interval.toString());
			}
			if (calendar.recurrence.until != null) {
				b.text(NamespaceMapping.EMAIL, "Until", FastDateFormat.format(calendar.recurrence.until));
			}
			if (calendar.recurrence.occurrences != null) {
				b.text(NamespaceMapping.EMAIL, "Occurrences", calendar.recurrence.occurrences.toString());
			}
			if (calendar.recurrence.weekOfMonth != null) {
				b.text(NamespaceMapping.EMAIL, "WeekOfMonth", calendar.recurrence.weekOfMonth.toString());
			}
			if (calendar.recurrence.dayOfMonth != null) {
				b.text(NamespaceMapping.EMAIL, "DayOfMonth", calendar.recurrence.dayOfMonth.toString());
			}
			if (calendar.recurrence.dayOfWeek != null) {
				b.text(NamespaceMapping.EMAIL, "DayOfWeek", calendar.recurrence.dayOfWeek.xmlValue());
			}
			if (calendar.recurrence.monthOfYear != null) {
				b.text(NamespaceMapping.EMAIL, "MonthOfYear", calendar.recurrence.monthOfYear.toString());
			}
			if (protocolVersion > 12.1) {
				if (calendar.recurrence.calendarType != null) {
					b.text(NamespaceMapping.EMAIL_2, "CalendarType", calendar.recurrence.calendarType.xmlValue());
				}
				if (calendar.recurrence.isLeapMonth != null) {
					b.text(NamespaceMapping.EMAIL_2, "IsLeapMonth",
							calendar.recurrence.isLeapMonth.booleanValue() ? "1" : "0");
				}
				if (protocolVersion > 14.0 && calendar.recurrence.firstDayOfWeek != null) {
					b.text(NamespaceMapping.EMAIL_2, "FirstDayOfWeek", calendar.recurrence.firstDayOfWeek.xmlValue());
				}
			}
			b.endContainer().endContainer(); // recurrences/recurrence
		}

		if (calendar.sensitivity != null) {
			b.text(NamespaceMapping.EMAIL, "Sensitivity", calendar.sensitivity.xmlValue());
		}
		if (calendar.busyStatus != null) {
			b.text(NamespaceMapping.EMAIL, "BusyStatus", calendar.busyStatus.xmlValue());
		}
		if (notEmpty(calendar.timezone)) {
			b.text(NamespaceMapping.EMAIL, "TimeZone", calendar.timezone);
		} else {
			b.text(NamespaceMapping.EMAIL, "TimeZone",
					"xP///wAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAoAAAAFAAMAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAMAAAAFAAIAAAAAAAAAxP///w==");
		}

		String globalObjId = ExtIdConverter.fromExtId(calendar.uid);
		if (protocolVersion < 16) {
			b.text(NamespaceMapping.EMAIL, "GlobalObjId", toB64(ExtIdConverter.fromHexString(globalObjId)));
		} else {
			b.text(NamespaceMapping.CALENDAR, "UID", calendar.uid);
			// it should not be necessary to add GlobalObjId with pv >=16, but IOS seems to
			// work better like this
			// The server will return the calendar:UID element ([MS-ASCAL] section 2.2.2.46)
			// instead of the
			// GlobalObjId element when protocol version 16.0 or 16.1 is used.
			b.text(NamespaceMapping.EMAIL, "GlobalObjId", toB64(ExtIdConverter.fromHexString(globalObjId)));
		}

		if (protocolVersion > 12.1) {
			b.text(NamespaceMapping.EMAIL, "DisallowNewTimeProposal", "1");
			if (protocolVersion > 14) {
				b.text(NamespaceMapping.EMAIL_2, "MeetingMessageType", "1");
			}
		}
		b.endContainer();
	}

	private static String toB64(byte[] bytes) {
		return Base64.getEncoder().encodeToString(bytes);
	}

	private void appendAttendees(IResponseBuilder b, List<Attendee> attendees) {
		if (attendees != null && !attendees.isEmpty()) {
			b.container(NamespaceMapping.CALENDAR, "Attendees");
			for (Attendee a : attendees) {
				b.container("Attendee");
				if (a.email != null) {
					b.text("Email", a.email);
				}
				if (a.name != null) {
					b.text("Name", a.name);
				}
				if (a.status != null) {
					b.text("AttendeeStatus", a.status.xmlValue());
				}
				b.text("AttendeeType", a.type.xmlValue());
				b.endContainer();
			}
			b.endContainer();
		}
	}

	private boolean notEmpty(String s) {
		return s != null && !s.trim().isEmpty();
	}

}
