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

import java.nio.charset.Charset;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;

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
			b.text(NamespaceMapping.Calendar, "Timezone", calendar.timezone);
		}
		if (calendar.dtStamp != null) {
			b.text(NamespaceMapping.Calendar, "DtStamp", FastDateFormat.format(calendar.dtStamp));
		}
		if (calendar.startTime != null) {
			b.text(NamespaceMapping.Calendar, "StartTime", FastDateFormat.format(calendar.startTime));
		}
		if (notEmpty(calendar.subject)) {
			b.text(NamespaceMapping.Calendar, "Subject", calendar.subject);
		}
		if (notEmpty(calendar.location)) {
			b.text(NamespaceMapping.Calendar, "Location", calendar.location);
		}
		if (notEmpty(calendar.uid)) {
			b.text(NamespaceMapping.Calendar, "UID", calendar.uid);
		}
		if (notEmpty(calendar.organizerName)) {
			b.text(NamespaceMapping.Calendar, "OrganizerName", calendar.organizerName);
		}
		if (notEmpty(calendar.organizerEmail)) {
			b.text(NamespaceMapping.Calendar, "OrganizerEmail", calendar.organizerEmail);
		}
		if (calendar.endTime != null) {
			b.text(NamespaceMapping.Calendar, "EndTime", FastDateFormat.format(calendar.endTime));
		}
		if (calendar.sensitivity != null) {
			b.text(NamespaceMapping.Calendar, "Sensitivity", calendar.sensitivity.xmlValue());
		}
		if (calendar.busyStatus != null) {
			b.text(NamespaceMapping.Calendar, "BusyStatus", calendar.busyStatus.xmlValue());
		}

		if (calendar.allDayEvent != null) {
			b.text(NamespaceMapping.Calendar, "AllDayEvent", calendar.allDayEvent ? "1" : "0");
		}

		if (calendar.reminder != null) {
			b.text(NamespaceMapping.Calendar, "Reminder", calendar.reminder.toString());
		}

		if (calendar.meetingStatus != null) {
			b.text(NamespaceMapping.Calendar, "MeetingStatus", calendar.meetingStatus.xmlValue());
		}

		appendAttendees(b, calendar.attendees);

		if (calendar.categories != null) {
			b.container(NamespaceMapping.Calendar, "Categories");
			for (String c : calendar.categories) {
				b.text(NamespaceMapping.Calendar, "Category", c);
			}
			b.endContainer();
		}

		if (calendar.recurrence != null) {
			b.container(NamespaceMapping.Calendar, "Recurrence");
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
			b.container(NamespaceMapping.Calendar, "Exceptions");
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
				if (notEmpty(e.calendar.location)) {
					b.text("Location", e.calendar.location);
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
					b.text("AllDayEvent", e.calendar.allDayEvent ? "1" : "0");
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
						b.text(NamespaceMapping.Calendar, "AppointmentReplyTime",
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
				b.text(NamespaceMapping.Calendar, "ResponseRequested", calendar.responseRequested ? "1" : "0");
			}

			if (calendar.appointmentReplyTime != null) {
				b.text(NamespaceMapping.Calendar, "AppointmentReplyTime",
						FastDateFormat.format(calendar.appointmentReplyTime));
			}
			if (calendar.responseType != null) {
				b.text(NamespaceMapping.Calendar, "ResponseType", calendar.responseType.xmlValue());
			}
			if (calendar.disallowNewTimeProposal != null) {
				b.text(NamespaceMapping.Calendar, "DisallowNewTimeProposal",
						calendar.disallowNewTimeProposal ? "1" : "0");
			}
			if (protocolVersion > 14) {
				if (notEmpty(calendar.onlineMeetingConfLink)) {
					b.text(NamespaceMapping.Calendar, "OnlineMeetingConfLink", calendar.onlineMeetingConfLink);
				}
				if (notEmpty(calendar.onlineMeetingExternalLink)) {
					b.text(NamespaceMapping.Calendar, "OnlineMeetingExternalLink", calendar.onlineMeetingExternalLink);
				}
			}
		}

		cb.onResult(b);
	}

	public void appendCalendarMeetingRequestResponse(IResponseBuilder b, double protocolVersion,
			CalendarResponse calendar) {
		b.container(NamespaceMapping.Email, "MeetingRequest");

		if (calendar.allDayEvent != null) {
			b.text(NamespaceMapping.Email, "AllDayEvent", calendar.allDayEvent ? "1" : "0");
		}
		if (calendar.startTime != null) {
			b.text(NamespaceMapping.Email, "StartTime", MeetingRequestFastDateFormat.format(calendar.startTime));
		}
		if (calendar.dtStamp != null) {
			b.text(NamespaceMapping.Email, "DtStamp", MeetingRequestFastDateFormat.format(calendar.dtStamp));
		}
		if (calendar.endTime != null) {
			b.text(NamespaceMapping.Email, "EndTime", MeetingRequestFastDateFormat.format(calendar.endTime));
		}

		b.text(NamespaceMapping.Email, "InstanceType", calendar.instanceType.xmlValue());
		if (calendar.instanceType == InstanceType.exceptionToRecurring) {
			b.text(NamespaceMapping.Email, "RecurrenceId", MeetingRequestFastDateFormat.format(calendar.recurrenceId));
		}

		if (notEmpty(calendar.location)) {
			if (protocolVersion < 16) {
				b.text(NamespaceMapping.Email, "Location", calendar.location);
			} else {
				b.text(NamespaceMapping.AirSyncBase, "Location", calendar.location);
			}
		}

		if (notEmpty(calendar.organizerName) && notEmpty(calendar.organizerEmail)) {
			b.text(NamespaceMapping.Email, "Organizer", calendar.organizerName + " <" + calendar.organizerEmail + ">");
		} else if (notEmpty(calendar.organizerEmail)) {
			b.text(NamespaceMapping.Email, "Organizer", calendar.organizerEmail + " <" + calendar.organizerEmail + ">");
		} else if (notEmpty(calendar.organizerName)) {
			b.text(NamespaceMapping.Email, "Organizer", calendar.organizerName);
		}

		// TODO RecurrenceId

		if (calendar.reminder != null) {
			b.text(NamespaceMapping.Email, "Reminder", calendar.reminder.toString());
		}

		boolean responseRequested = false;
		Date now = new Date();
		if (now.before(calendar.startTime)) {
			responseRequested = true;
		}
		if (calendar.recurrence != null) {
			if (calendar.recurrence.until != null && calendar.recurrence.until.before(now)) {
				responseRequested = false;
			}
			responseRequested = true;
		}
		b.text(NamespaceMapping.Email, "ResponseRequested", responseRequested ? "1" : "0");

		if (calendar.recurrence != null) {
			b.container(NamespaceMapping.Email, "Recurrences").container("Recurrence");

			if (calendar.recurrence.type != null) {
				b.text(NamespaceMapping.Email, "Type", calendar.recurrence.type.xmlValue());
			}
			if (calendar.recurrence.interval != null) {
				b.text(NamespaceMapping.Email, "Interval", calendar.recurrence.interval.toString());
			}
			if (calendar.recurrence.until != null) {
				b.text(NamespaceMapping.Email, "Until", FastDateFormat.format(calendar.recurrence.until));
			}
			if (calendar.recurrence.occurrences != null) {
				b.text(NamespaceMapping.Email, "Occurrences", calendar.recurrence.occurrences.toString());
			}
			if (calendar.recurrence.weekOfMonth != null) {
				b.text(NamespaceMapping.Email, "WeekOfMonth", calendar.recurrence.weekOfMonth.toString());
			}
			if (calendar.recurrence.dayOfMonth != null) {
				b.text(NamespaceMapping.Email, "DayOfMonth", calendar.recurrence.dayOfMonth.toString());
			}
			if (calendar.recurrence.dayOfWeek != null) {
				b.text(NamespaceMapping.Email, "DayOfWeek", calendar.recurrence.dayOfWeek.xmlValue());
			}
			if (calendar.recurrence.monthOfYear != null) {
				b.text(NamespaceMapping.Email, "MonthOfYear", calendar.recurrence.monthOfYear.toString());
			}
			if (protocolVersion > 12.1) {
				if (calendar.recurrence.calendarType != null) {
					b.text(NamespaceMapping.Email2, "CalendarType", calendar.recurrence.calendarType.xmlValue());
				}
				if (calendar.recurrence.isLeapMonth != null) {
					b.text(NamespaceMapping.Email2, "IsLeapMonth", calendar.recurrence.isLeapMonth ? "1" : "0");
				}
				if (protocolVersion > 14.0 && calendar.recurrence.firstDayOfWeek != null) {
					b.text(NamespaceMapping.Email2, "FirstDayOfWeek", calendar.recurrence.firstDayOfWeek.xmlValue());
				}
			}
			b.endContainer().endContainer(); // recurrences/recurrence
		}

		if (calendar.sensitivity != null) {
			b.text(NamespaceMapping.Email, "Sensitivity", calendar.sensitivity.xmlValue());
		}
		if (calendar.busyStatus != null) {
			b.text(NamespaceMapping.Email, "BusyStatus", calendar.busyStatus.xmlValue());
		}
		if (notEmpty(calendar.timezone)) {
			b.text(NamespaceMapping.Email, "TimeZone", calendar.timezone);
		} else {
			b.text(NamespaceMapping.Email, "TimeZone",
					"xP///wAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAoAAAAFAAMAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAMAAAAFAAIAAAAAAAAAxP///w==");
		}

		if (protocolVersion < 16) {
			String globalObjId = ExtIdConverter.fromExtId(calendar.uid);
			b.text(NamespaceMapping.Email, "GlobalObjId", toB64(ExtIdConverter.fromHexString(globalObjId)));
		} else {
			b.text(NamespaceMapping.Calendar, "UID", calendar.uid);
		}

		if (protocolVersion > 12.1) {
			b.text(NamespaceMapping.Email, "DisallowNewTimeProposal", "1");
			if (protocolVersion > 14) {
				b.text(NamespaceMapping.Email2, "MeetingMessageType", "1");
			}
		}
		b.endContainer();
	}

	private static String toB64(byte[] bytes) {
		ChannelBuffer src = ChannelBuffers.wrappedBuffer(bytes);
		ChannelBuffer result = org.jboss.netty.handler.codec.base64.Base64.encode(src, false);
		return result.toString(Charset.defaultCharset());
	}

	private void appendAttendees(IResponseBuilder b, List<Attendee> attendees) {
		if (attendees != null && !attendees.isEmpty()) {
			b.container(NamespaceMapping.Calendar, "Attendees");
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
