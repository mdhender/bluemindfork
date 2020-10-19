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
package net.bluemind.eas.dto.calendar;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CalendarResponse {

	public static enum BusyStatus {

		Free(0), Tentative(1), Busy(2), OutOfOffice(3);

		private final String xmlValue;

		private BusyStatus(int value) {
			xmlValue = Integer.toString(value);
		}

		public String xmlValue() {
			return xmlValue;
		}
	}

	public static enum Sensitivity {

		Normal(0), Personal(1), Private(2), Confidential(3);

		private final String xmlValue;

		private Sensitivity(int value) {
			xmlValue = Integer.toString(value);
		}

		public String xmlValue() {
			return xmlValue;
		}
	}

	public static enum MeetingStatus {

		Appointment(0), MeetingAndUserIsOrganizer(1), MeetingAndUserIsNotOrganizer(3), //
		CanceledAndUserWasOrganizer(5), CancelReceived(7), CanceledAndUserWasNotOrganizer(9);

		private final String xmlValue;

		private MeetingStatus(int value) {
			xmlValue = Integer.toString(value);
		}

		public String xmlValue() {
			return xmlValue;
		}

	}

	public static final class Attendee {
		public static enum AttendeeStatus {

			ResponseUnknown(0), Tentative(2), Accepted(3), //
			Declined(4), NotResponded(5);

			private final String xmlValue;

			private AttendeeStatus(int value) {
				xmlValue = Integer.toString(value);
			}

			public String xmlValue() {
				return xmlValue;
			}
		}

		public static enum AttendeeType {

			Required(1), Optional(2), Resource(3);

			private final String xmlValue;

			private AttendeeType(int value) {
				xmlValue = Integer.toString(value);
			}

			public String xmlValue() {
				return xmlValue;
			}
		}

		public String email;
		public String name;
		public AttendeeStatus status;
		public AttendeeType type;
	}

	public static final class Recurrence {
		public static enum Type {

			Daily(0), Weekly(1), Monthly(2), MonthlyByDay(3), Yearly(5), YearlyByDay(6);

			private final String xmlValue;

			private Type(int value) {
				xmlValue = Integer.toString(value);
			}

			public String xmlValue() {
				return xmlValue;
			}
		}

		public static enum CalendarType {

			Default(0), Gregorian(1), GregorianUS(2), //
			JapaneseEmperorEra(3), Taiwan(4), KoreaTangunEra(5), //
			Hijri(6), Thai(7), HebrewLunar(8), GregorianMiddleEastFrench(9), //
			GregorianArabic(10), GregorianTransliteratedEnglish(11), GregorianTransliteratedFrench(12), //
			JapaneseLunar(14), ChineseLunar(15), KoreaLunar(20);

			private final String xmlValue;

			private CalendarType(int value) {
				xmlValue = Integer.toString(value);
			}

			public String xmlValue() {
				return xmlValue;
			}
		}

		public static final class DayOfWeek {

			public static enum Days {
				Sunday(1), Monday(2), Tuesday(4), //
				Wednesday(8), Thrusday(16), Friday(32), //
				Weekdays(62), Saturday(64), WeekendDays(65), LastDayOfMonth(127);

				private final int xmlValue;

				private Days(int value) {
					xmlValue = value;
				}

				public int value() {
					return xmlValue;
				}
			}

			public Set<Days> days;

			public static DayOfWeek fromInt(Integer i) {
				DayOfWeek dow = new DayOfWeek();
				dow.days = new HashSet<Days>();

				if ((i & Days.Monday.value()) == Days.Monday.value()) {
					dow.days.add(Days.Monday);
				}
				if ((i & Days.Tuesday.value()) == Days.Tuesday.value()) {
					dow.days.add(Days.Tuesday);
				}
				if ((i & Days.Wednesday.value()) == Days.Wednesday.value()) {
					dow.days.add(Days.Wednesday);
				}
				if ((i & Days.Thrusday.value()) == Days.Thrusday.value()) {
					dow.days.add(Days.Thrusday);
				}
				if ((i & Days.Friday.value()) == Days.Friday.value()) {
					dow.days.add(Days.Friday);
				}
				if ((i & Days.Saturday.value()) == Days.Saturday.value()) {
					dow.days.add(Days.Saturday);
				}
				if ((i & Days.Sunday.value()) == Days.Sunday.value()) {
					dow.days.add(Days.Sunday);
				}
				return dow;
			}

			public String xmlValue() {
				int v = 0;
				for (Days d : days) {
					v += d.value();
				}
				return Integer.toString(v);
			}
		}

		public Type type;
		public Integer occurrences;
		public Integer interval;
		public Integer weekOfMonth;
		public DayOfWeek dayOfWeek;
		public Integer monthOfYear;
		public Date until;
		public Integer dayOfMonth;
		public CalendarType calendarType;
		public Boolean isLeapMonth;
		public FirstDayOfWeek firstDayOfWeek;
	}

	public static enum FirstDayOfWeek {

		Sunday(0), Monday(1), Tuesday(2), //
		Wednesday(3), Thrusday(4), Friday(5), //
		Saturday(6);

		private final String xmlValue;

		private FirstDayOfWeek(int value) {
			xmlValue = Integer.toString(value);
		}

		public String xmlValue() {
			return xmlValue;
		}
	}

	public static final class EventException {
		public Boolean deleted;
		public Date exceptionStartTime;
		public Date startTime;
		public Date endTime;
		public String subject;
		public CalendarResponse calendar;
		public Date appointmentReplyTime;
		public ResponseType responseType;
		public String onlineMeetingConfLink;
		public String onlineMeetingExternalLink;
		public List<Attendee> attendees;
	}

	public static enum ResponseType {

		None(0), Organizer(1), Tentative(2), Accepted(3), //
		Declined(4), NotResponded(5);

		private final String xmlValue;

		private ResponseType(int value) {
			xmlValue = Integer.toString(value);
		}

		public String xmlValue() {
			return xmlValue;
		}
	}

	/**
	 * The InstanceType element is a required child element of the MeetingRequest
	 * element (section 2.2.2.48) that specifies whether the calendar item is a
	 * single or recurring appointment. It is defined as an element in the Email
	 * namespace.
	 *
	 */
	public static enum InstanceType {
		/**
		 * A single appointment.
		 */
		singleAppointment(0),

		/**
		 * A master recurring appointment.
		 */
		recurringMaster(1),

		/**
		 * A single instance of a recurring appointment.
		 */
		singleInstance(2),

		/**
		 * An exception to a recurring appointment.
		 */
		exceptionToRecurring(3),

		/**
		 * An orphan instance of a recurring appointment. The value 4 is not supported
		 * by protocol versions 2.5, 12.0, 12.1, 14.0 and 14.1.
		 */
		orphanInstance(4);

		private final int xmlValue;

		private InstanceType(int v) {
			this.xmlValue = v;
		}

		public String xmlValue() {
			return Integer.toString(xmlValue);
		}
	}

	public String timezone;
	public Boolean allDayEvent;
	public BusyStatus busyStatus;
	public String organizerName;
	public String organizerEmail;
	public Date dtStamp;
	public Date endTime;
	public String location;
	/**
	 * in minutes
	 */
	public Integer reminder;
	public Sensitivity sensitivity;

	public String subject;
	public Date startTime;
	public String uid;
	public MeetingStatus meetingStatus;
	public List<Attendee> attendees;
	public List<String> categories;
	public Recurrence recurrence;
	public List<EventException> exceptions;
	public Boolean responseRequested;
	public Date appointmentReplyTime;
	public ResponseType responseType;
	public Boolean disallowNewTimeProposal;
	public String onlineMeetingConfLink;
	public String onlineMeetingExternalLink;
	public long itemUid;

	public InstanceType instanceType;
	public Date recurrenceId;
}
