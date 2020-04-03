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
package net.bluemind.icalendar.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.bluemind.attachment.api.AttachedFile;
import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.date.BmDateTime;
import net.bluemind.tag.api.TagRef;

@BMApi(version = "3")
public class ICalendarElement {

	public BmDateTime dtstart;
	public String summary;
	public Classification classification;
	public String location;

	/**
	 * Contains HTML without the outer html & body markups.
	 */
	public String description;

	public Integer priority;
	public List<VAlarm> alarm;
	public Status status;
	public List<Attendee> attendees = new ArrayList<>();
	public Organizer organizer;
	public List<TagRef> categories = new ArrayList<>();
	public Set<BmDateTime> exdate;
	public Set<BmDateTime> rdate;
	public RRule rrule;
	public String url;
	public List<AttachedFile> attachments = new ArrayList<>();

	/**
	 * When a ICalendarElement is created, its sequence number is 0. It is
	 * monotonically incremented by the "Organizer's" each time the "Organizer"
	 * makes a significant revision to the calendar component.
	 */
	public Integer sequence = 0;

	/**
	 * Indicates whether invitations have been already sent at least once.
	 */
	public boolean draft;

	@BMApi(version = "3")
	public static class VAlarm {
		public Action action = Action.Display;

		/**
		 * Relative to ICalendarelement.dtstart. In SECOND
		 * 
		 * Either a positive or negative duration may be specified for the "TRIGGER"
		 * property. An alarm with a positive duration is triggered after the associated
		 * start or end of the event or to-do. An alarm with a negative duration is
		 * triggered before the associated start or end of the event or to-do.
		 */
		public Integer trigger;
		public String description;

		/**
		 * in SECOND
		 */
		public Integer duration;
		public Integer repeat;
		public String summary;
		// attendees and attach not implemented

		@BMApi(version = "3")
		public enum Action {
			Audio, //
			Display, //
			Email;
		}

		/**
		 * Simple alarm
		 * 
		 * @param action
		 * @param trigger
		 * @return
		 */
		public static VAlarm create(Integer trigger) {
			VAlarm ret = new VAlarm();
			ret.trigger = trigger;
			return ret;
		}

		/**
		 * Simple alarm with summary
		 * 
		 * @param trigger
		 * @param summary
		 * @return
		 */
		public static VAlarm create(Integer trigger, String summary) {
			VAlarm ret = new VAlarm();
			ret.trigger = trigger;
			ret.summary = summary;
			return ret;
		}

		/**
		 * @param action
		 * @param trigger
		 * @param description
		 * @param duration
		 * @param repeat
		 * @param summary
		 * @return
		 */
		public static VAlarm create(Action action, Integer trigger, String description, Integer duration,
				Integer repeat, String summary) {
			VAlarm ret = new VAlarm();
			ret.action = action;
			ret.trigger = trigger;
			ret.description = description;
			ret.duration = duration;
			ret.repeat = repeat;
			ret.summary = summary;
			return ret;
		}

		public VAlarm copy() {
			return VAlarm.create(this.action, this.trigger, this.description, this.duration, this.repeat, this.summary);
		}
	}

	@BMApi(version = "3")
	public static class Attendee {
		/**
		 * "CUTYPE", to indicate the type of calendar user
		 */
		public CUType cutype;

		/**
		 * "MEMBER", to indicate the groups that the attendee belongs to
		 **/
		public String member;
		/**
		 * "ROLE", for the intended role that the attendee will have in the calendar
		 * component;
		 */
		public Role role;
		/**
		 * "PARTSTAT", for the status of the attendee's participation;
		 */
		public ParticipationStatus partStatus;
		/**
		 * "RSVP", for indicating whether the favor of a reply is requested;
		 */
		public Boolean rsvp;
		/**
		 * "DELEGATED-TO", to indicate the calendar users that the original request was
		 * delegated to;
		 */
		public String delTo;

		/**
		 * "DELEGATED-FROM", to indicate whom the request was delegated from
		 */
		public String delFrom;
		public String sentBy;
		public String commonName;

		/**
		 * "DIR", to indicate the URI that points to the directory information
		 * corresponding to the attendee.
		 */
		public String dir;
		public String lang;
		public String mailto;
		public String uri; // link to another thing. Container, Item ...
		public boolean internal;
		/**
		 * Comment by attendee
		 */
		public String responseComment;

		public static Attendee create(CUType cuType, String member, Role role, ParticipationStatus partStatus,
				Boolean rsvp, String delTo, String delFrom, String sentBy, String commonName, String dir, String lang,
				String uri, String mailto) {
			return create(cuType, member, role, partStatus, rsvp, delTo, delFrom, sentBy, commonName, dir, lang, uri,
					mailto, null);
		}

		public static Attendee create(CUType cuType, String member, Role role, ParticipationStatus partStatus,
				Boolean rsvp, String delTo, String delFrom, String sentBy, String commonName, String dir, String lang,
				String uri, String mailto, String responseComment) {
			Attendee attendee = new Attendee();
			attendee.cutype = cuType;
			attendee.member = member;
			attendee.role = role;
			attendee.partStatus = partStatus;
			attendee.rsvp = rsvp;
			attendee.delTo = delTo;
			attendee.delFrom = delFrom;
			attendee.sentBy = sentBy;
			attendee.commonName = commonName;
			attendee.dir = dir;
			attendee.lang = lang;
			attendee.uri = uri;
			attendee.mailto = mailto;
			attendee.internal = true;
			attendee.responseComment = responseComment;
			return attendee;
		}

		@Override
		public String toString() {
			return "URI: " + uri + ", CN:" + commonName + ", MAILTO: " + mailto + ", PART: " + partStatus + ", DIR: "
					+ dir + ", RSVP: " + rsvp;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			if (dir != null && !dir.isEmpty()) {
				result = prime * result + dir.hashCode();
			} else if (mailto != null && !mailto.isEmpty()) {
				result = prime * result + mailto.hashCode();
			} else {
				result = prime * result + ((commonName == null) ? 0 : commonName.hashCode());
			}
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Attendee other = (Attendee) obj;

			if (dir != null && !dir.isEmpty()) {
				return dir.equals(other.dir);
			} else if (mailto != null && !mailto.isEmpty()) {
				return mailto.equals(other.mailto);
			} else if (commonName != null) {
				return commonName.equals(other.commonName);
			} else {
				return false;
			}
		}

		public boolean sameDirOrMailtoAs(Attendee a) {
			if (dir != null && !dir.isEmpty()) {
				return dir.equals(a.dir);
			} else if (a.dir != null && !a.dir.isEmpty()) {
				return false;
			}
			return mailto.equalsIgnoreCase(a.mailto);

		}

		public Attendee copy() {
			return Attendee.create(cutype, member, role, partStatus, rsvp, delTo, delFrom, sentBy, commonName, dir,
					lang, uri, mailto, responseComment);
		}

	}

	/**
	 * * Compare this list with the list <code>attendees</code> and return the
	 * {@link Attendee}s present in the current list but not contained in
	 * <code>attendees</code>
	 * 
	 * @param attendees
	 * @return
	 */
	public static List<Attendee> diff(List<Attendee> source, List<Attendee> attendees) {
		List<Attendee> diff = new ArrayList<>(source.size());

		for (Attendee attendee : source) {
			Attendee a = get(attendees, attendee);
			if (a == null) {
				diff.add(attendee);
			}
		}

		return diff;
	}

	public static Attendee get(List<Attendee> source, Attendee a) {
		for (Attendee attendee : source) {
			if (attendee.sameDirOrMailtoAs(a)) {
				return a;
			}
		}
		return null;
	}

	/**
	 * @param attendees
	 * @return
	 */
	public static List<Attendee> same(List<Attendee> source, List<Attendee> attendees) {
		List<Attendee> same = new ArrayList<>(source.size());

		for (Attendee attendee : source) {
			if (get(attendees, attendee) != null) {
				same.add(attendee);
			}
		}

		return same;
	}

	/**
	 *
	 */
	@BMApi(version = "3")
	public static class Organizer {

		public String uri;
		public String commonName;
		public String mailto;
		public String dir;

		public Organizer() {

		}

		/**
		 * @param mailto
		 */
		public Organizer(String mailto) {
			this.mailto = mailto;
		}

		/**
		 * @param commonName
		 * @param mailto
		 */
		public Organizer(String commonName, String mailto) {
			this.commonName = commonName;
			this.mailto = mailto;
		}

		@Override
		public boolean equals(Object obj) {
			return mailto.equalsIgnoreCase(((Organizer) obj).mailto);
		}

		@Override
		public String toString() {
			return "URI: " + uri + ", CN: " + commonName + ", MAILTO: " + mailto + ", DIR: " + dir;
		}

		public Organizer copy() {
			Organizer organizer = new Organizer(this.commonName, this.mailto);
			organizer.uri = this.uri;
			organizer.dir = this.dir;
			return organizer;
		}

	}

	@BMApi(version = "3")
	public enum Status {
		NeedsAction, Completed, InProcess, Cancelled, Confirmed, Tentative
	}

	@BMApi(version = "3")
	public enum Classification {
		Public, //
		Private, //
		Confidential;
	}

	/**
	 * 4.2.3 Calendar User Type
	 * 
	 * To specify the type of calendar user
	 * 
	 */
	@BMApi(version = "3")
	public enum CUType {
		/** An individual **/
		Individual, //
		/** A group of individual **/
		Group, //
		/** A physical Resource **/
		Resource, //
		/** A room resource **/
		Room, //
		/** Otherwise not known **/
		Unknown;

		public static ICalendarElement.CUType byName(String name) {
			if (Individual.name().equalsIgnoreCase(name)) {
				return ICalendarElement.CUType.Individual;
			} else if (Group.name().equalsIgnoreCase(name)) {
				return ICalendarElement.CUType.Group;
			} else if (Resource.name().equalsIgnoreCase(name)) {
				return ICalendarElement.CUType.Resource;
			} else if (Room.name().equalsIgnoreCase(name)) {
				return ICalendarElement.CUType.Room;
			} else {
				return ICalendarElement.CUType.Unknown;
			}
		}
	}

	// TODO: Forbidden status yay
	@BMApi(version = "3")
	public enum ParticipationStatus {
		/** To-do needs action **/
		NeedsAction, //
		/** To-do accepted **/
		Accepted, //
		/** To-do declined **/
		Declined, //
		/** To-do tentatively accepted **/
		Tentative, //
		/** To-do delegated **/
		Delegated, //
		/** To-do completed **/
		Completed //
	}

	@BMApi(version = "3")
	public enum Role {
		/** Indicates chair of the calendar entity **/
		Chair, //
		/** Indicates a participant whose participation is required **/
		RequiredParticipant, //
		/**
		 * Indicates a participant whose participation is optional
		 **/
		OptionalParticipant, //
		/**
		 * Indicates a participant who is copied for information purposes only
		 **/
		NonParticipant
	}

	@BMApi(version = "3")
	public static class RRule {

		public RRule() {
		}

		/** required **/
		public Frequency frequency;

		/** count and until must not occur in the same recur **/
		public Integer count;
		public BmDateTime until;

		public Integer interval;
		// 0 to 59
		public List<Integer> bySecond;
		// 0 to 59
		public List<Integer> byMinute;
		// 0 to 23
		public List<Integer> byHour;
		public List<WeekDay> byDay;
		// 0 to 31
		public List<Integer> byMonthDay;
		// 0 to 366
		public List<Integer> byYearDay;
		// 1 to 53
		// FIXME weekdaynum = [([plus] ordwk / minus ordwk)] weekday
		public List<Integer> byWeekNo;
		// 1 to 12
		public List<Integer> byMonth;

		// TODO bysetpos ,wkst
		@BMApi(version = "3")
		public enum Frequency {
			SECONDLY, MINUTELY, HOURLY, DAILY, WEEKLY, MONTHLY, YEARLY
		}

		@BMApi(version = "3")
		public static class WeekDay {

			public String day;
			public int offset;

			public static final WeekDay SU = new WeekDay("SU");
			public static final WeekDay MO = new WeekDay("MO");
			public static final WeekDay TU = new WeekDay("TU");
			public static final WeekDay WE = new WeekDay("WE");
			public static final WeekDay TH = new WeekDay("TH");
			public static final WeekDay FR = new WeekDay("FR");
			public static final WeekDay SA = new WeekDay("SA");

			public WeekDay() {
			}

			public WeekDay(String day) {
				if (day.length() == 2) {
					// SU, MO, TU, WE, TH, FR, SA
					this.day = day.toUpperCase();
					this.offset = 0;
				} else if (day.length() == 3) {
					// 1TH, 2MO, 4SA, ...
					this.day = day.substring(1, 3).toUpperCase();
					this.offset = Integer.parseInt(day.substring(0, 1));

				} else if (day.length() == 4) {
					// -1MO
					this.day = day.substring(2, 4).toUpperCase();
					this.offset = Integer.parseInt(day.substring(0, 2));
				} else {
					throw new IllegalArgumentException("Unsupported weekday " + day);
				}
			}

			public WeekDay(String day, int offset) {
				this.day = day;
				this.offset = offset;
			}

			@Override
			public String toString() {
				if (offset != 0) {
					return offset + "" + day;
				}
				return day;
			}

			@Override
			public int hashCode() {
				final int prime = 31;
				int result = 1;
				result = prime * result + ((day == null) ? 0 : day.hashCode());
				result = prime * result + offset;
				return result;
			}

			@Override
			public boolean equals(Object obj) {
				if (this == obj)
					return true;
				if (obj == null)
					return false;
				if (getClass() != obj.getClass())
					return false;
				WeekDay other = (WeekDay) obj;
				if (day == null) {
					if (other.day != null)
						return false;
				} else if (!day.equals(other.day))
					return false;
				if (offset != other.offset)
					return false;
				return true;
			}

		}

		public RRule copy() {
			RRule copy = new RRule();
			copy.count = this.count;
			copy.frequency = this.frequency;
			copy.interval = this.interval;
			if (null != this.until) {
				copy.until = new BmDateTime(this.until.iso8601, this.until.timezone, this.until.precision);
			}
			if (null != this.byDay) {
				copy.byDay = new ArrayList<>(this.byDay);
			}
			if (null != this.byHour) {
				copy.byHour = new ArrayList<>(this.byHour);
			}
			if (null != this.byMinute) {
				copy.byMinute = new ArrayList<>(this.byMinute);
			}
			if (null != this.byMonth) {
				copy.byMonth = new ArrayList<>(this.byMonth);
			}
			if (null != this.byMonthDay) {
				copy.byMonthDay = new ArrayList<>(this.byMonthDay);
			}
			if (null != this.bySecond) {
				copy.bySecond = new ArrayList<>(this.bySecond);
			}
			if (null != this.byWeekNo) {
				copy.byWeekNo = new ArrayList<>(this.byWeekNo);
			}
			if (null != this.byYearDay) {
				copy.byYearDay = new ArrayList<>(this.byYearDay);
			}
			return copy;
		}

	}

	/**
	 * @return timezone id
	 */
	public String timezone() {
		return dtstart.timezone;
	}

	/**
	 * @return
	 */
	public ICalendarElement copy() {
		return null;
	}

	public boolean hasAlarm() {
		return alarm != null && !alarm.isEmpty();
	}

	public boolean hasRecurrence() {
		return rrule != null && rrule.frequency != null;
	}

	public boolean meeting() {
		return
		// a meeting contains an organiser
		organizer != null && //
				( // no attenddee
				!attendees.isEmpty()
						// or organizer == first attendee and only one attendee
						// (caldav)
						|| !(attendees.size() == 1 && organizer != null
								&& attendees.get(0).mailto.equals(organizer.mailto)));
	}
}
