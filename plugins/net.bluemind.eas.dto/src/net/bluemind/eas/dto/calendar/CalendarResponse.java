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

import net.bluemind.eas.dto.base.AirSyncBaseResponse.Attachment;

public class CalendarResponse {

	public enum BusyStatus {

		FREE(0), TENTATIVE(1), BUSY(2), OUT_OF_OFFICE(3);

		private final String xmlValue;

		private BusyStatus(int value) {
			xmlValue = Integer.toString(value);
		}

		public String xmlValue() {
			return xmlValue;
		}
	}

	public enum Sensitivity {

		NORMAL(0), PERSONAL(1), PRIVATE(2), CONFIDENTIAL(3);

		private final String xmlValue;

		private Sensitivity(int value) {
			xmlValue = Integer.toString(value);
		}

		public String xmlValue() {
			return xmlValue;
		}
	}

	public enum MeetingStatus {

		APPOINTMENT(0), MEETING_AND_USER_IS_ORGANIZER(1), MEETING_AND_USER_IS_NOT_ORGANIZER(3), //
		CANCELED_AND_USER_WAS_ORGANIZER(5), CANCEL_RECEIVED(7), CANCELED_AND_USER_WAS_NOT_ORGANIZER(9);

		private final String xmlValue;

		private MeetingStatus(int value) {
			xmlValue = Integer.toString(value);
		}

		public String xmlValue() {
			return xmlValue;
		}

	}

	public static final class Attendee {
		public enum AttendeeStatus {

			RESPONSE_UNKNOWN(0), TENTATIVE(2), ACCEPTED(3), //
			DECLINED(4), NOT_RESPONDED(5);

			private final String xmlValue;

			private AttendeeStatus(int value) {
				xmlValue = Integer.toString(value);
			}

			public String xmlValue() {
				return xmlValue;
			}
		}

		public enum AttendeeType {

			REQUIRED(1), OPTIONAL(2), RESOURCE(3);

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
		public enum Type {

			DAILY(0), WEEKLY(1), MONTHLY(2), MONTHLY_BY_DAY(3), YEARLY(5), YEARLY_BY_DAY(6);

			private final String xmlValue;

			private Type(int value) {
				xmlValue = Integer.toString(value);
			}

			public String xmlValue() {
				return xmlValue;
			}
		}

		public enum CalendarType {

			DEFAULT(0), GREGORIAN(1), GREGORIAN_US(2), //
			JAPANESE_EMPEROR_ERA(3), TAIWAN(4), KOREA_TANGUN_ERA(5), //
			HIJRI(6), THAI(7), HEBREW_LUNAR(8), GREGORIAN_MIDDLE_EAST_FRENCH(9), //
			GREGORIAN_ARABIC(10), GREGORIAN_TRANSLITERATED_ENGLISH(11), GREGORIAN_TRANSLITERATED_FRENCH(12), //
			JAPANESE_LUNAR(14), CHINESE_LUNAR(15), KOREA_LUNAR(20);

			private final String xmlValue;

			private CalendarType(int value) {
				xmlValue = Integer.toString(value);
			}

			public String xmlValue() {
				return xmlValue;
			}
		}

		public static final class DayOfWeek {

			public enum Days {
				SUNDAY(1), MONDAY(2), TUESDAY(4), //
				WEDNESDAY(8), THRUSDAY(16), FRIDAY(32), //
				WEEKDAYS(62), SATURDAY(64), WEEKEND_DAYS(65), LAST_DAY_OF_MONTH(127);

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
				dow.days = new HashSet<>();

				if ((i & Days.MONDAY.value()) == Days.MONDAY.value()) {
					dow.days.add(Days.MONDAY);
				}
				if ((i & Days.TUESDAY.value()) == Days.TUESDAY.value()) {
					dow.days.add(Days.TUESDAY);
				}
				if ((i & Days.WEDNESDAY.value()) == Days.WEDNESDAY.value()) {
					dow.days.add(Days.WEDNESDAY);
				}
				if ((i & Days.THRUSDAY.value()) == Days.THRUSDAY.value()) {
					dow.days.add(Days.THRUSDAY);
				}
				if ((i & Days.FRIDAY.value()) == Days.FRIDAY.value()) {
					dow.days.add(Days.FRIDAY);
				}
				if ((i & Days.SATURDAY.value()) == Days.SATURDAY.value()) {
					dow.days.add(Days.SATURDAY);
				}
				if ((i & Days.SUNDAY.value()) == Days.SUNDAY.value()) {
					dow.days.add(Days.SUNDAY);
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

	public enum FirstDayOfWeek {

		SUNDAY(0), MONDAY(1), TUESDAY(2), //
		WEDNESDAY(3), THRUSDAY(4), FRIDAY(5), //
		SATURDAY(6);

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
		public String location;
	}

	public enum ResponseType {

		NONE(0), ORGANIZER(1), TENTATIVE(2), ACCEPTED(3), //
		DECLINED(4), NOT_RESPONDED(5);

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
	public enum InstanceType {
		/**
		 * A single appointment.
		 */
		SINGLE_APPOINTMENT(0),

		/**
		 * A master recurring appointment.
		 */
		RECURRING_MASTER(1),

		/**
		 * A single instance of a recurring appointment.
		 */
		SINGLE_INSTANCE(2),

		/**
		 * An exception to a recurring appointment.
		 */
		EXCEPTION_TO_RECURRING(3),

		/**
		 * An orphan instance of a recurring appointment. The value 4 is not supported
		 * by protocol versions 2.5, 12.0, 12.1, 14.0 and 14.1.
		 */
		ORPHAN_INSTANCE(4);

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

	public List<Attachment> attachments;
	public String timezoneJava;
}
