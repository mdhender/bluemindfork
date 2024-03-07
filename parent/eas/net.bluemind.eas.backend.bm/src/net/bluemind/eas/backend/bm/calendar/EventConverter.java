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
package net.bluemind.eas.backend.bm.calendar;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.api.VEvent.Transparency;
import net.bluemind.calendar.api.VEventOccurrence;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.core.api.date.BmDateTime;
import net.bluemind.core.api.date.BmDateTime.Precision;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.eas.backend.BackendSession;
import net.bluemind.eas.backend.IApplicationData;
import net.bluemind.eas.backend.MSAttendee;
import net.bluemind.eas.backend.MSEvent;
import net.bluemind.eas.backend.bm.mail.AttachmentHelper;
import net.bluemind.eas.data.calendarenum.AttendeeStatus;
import net.bluemind.eas.data.calendarenum.AttendeeType;
import net.bluemind.eas.data.formatter.PlainBodyFormatter;
import net.bluemind.eas.dto.base.AirSyncBaseResponse.Attachment;
import net.bluemind.eas.dto.base.AirSyncBaseResponse.Attachment.Method;
import net.bluemind.eas.dto.calendar.CalendarResponse;
import net.bluemind.eas.dto.calendar.CalendarResponse.BusyStatus;
import net.bluemind.eas.dto.calendar.CalendarResponse.EventException;
import net.bluemind.eas.dto.calendar.CalendarResponse.Recurrence;
import net.bluemind.eas.dto.calendar.CalendarResponse.Recurrence.DayOfWeek;
import net.bluemind.eas.dto.calendar.CalendarResponse.Recurrence.DayOfWeek.Days;
import net.bluemind.eas.dto.calendar.CalendarResponse.Sensitivity;
import net.bluemind.eas.dto.user.MSUser;
import net.bluemind.icalendar.api.ICalendarElement;
import net.bluemind.icalendar.api.ICalendarElement.Attendee;
import net.bluemind.icalendar.api.ICalendarElement.Classification;
import net.bluemind.icalendar.api.ICalendarElement.Organizer;
import net.bluemind.icalendar.api.ICalendarElement.ParticipationStatus;
import net.bluemind.icalendar.api.ICalendarElement.RRule;
import net.bluemind.icalendar.api.ICalendarElement.RRule.WeekDay;
import net.bluemind.icalendar.api.ICalendarElement.Role;
import net.bluemind.icalendar.api.ICalendarElement.Status;
import net.bluemind.icalendar.api.ICalendarElement.VAlarm;
import net.bluemind.icalendar.api.ICalendarElement.VAlarm.Action;

/**
 * Convert events between BM object model & Microsoft object model
 * 
 * 
 */
public class EventConverter {

	private static final Logger logger = LoggerFactory.getLogger(EventConverter.class);

	public MSEvent convert(BackendSession bs, ItemValue<VEventSeries> seriesItem) {
		VEventSeries vevent = seriesItem.value;
		MSEvent mse = null;
		if (vevent.main == null) {
			// only one occurence in "orphan" event
			mse = convert(bs, seriesItem.uid, vevent.occurrences.get(0));
			return mse;
		}

		mse = convert(bs, seriesItem.uid, vevent.main);

		if (!vevent.occurrences.isEmpty()) {
			String timezone = vevent.main != null ? vevent.main.timezone() : null;
			if (timezone == null) {
				timezone = bs.getUser().getTimeZone();
			}

			TimeZone tz = TimeZone.getTimeZone(timezone);

			List<EventException> exceptions = null;
			if (mse.getExceptions() != null) {
				exceptions = new ArrayList<>(vevent.occurrences.size() + mse.getExceptions().size());
				exceptions.addAll(mse.getExceptions());
			} else {
				exceptions = new ArrayList<>(vevent.occurrences.size());
			}
			for (VEventOccurrence recurrence : vevent.occurrences) {
				convertOccurrence(bs.getUser(), tz, exceptions, recurrence);
			}
			mse.setExceptions(exceptions);

		}

		mse.setDisallowNewTimeProposal(!vevent.acceptCounters);

		return mse;

	}

	private void convertOccurrence(MSUser me, TimeZone tz, List<EventException> exceptions,
			VEventOccurrence recurrence) {
		EventException e = new EventException();
		e.deleted = false;

		e.exceptionStartTime = new Date(new BmDateTimeWrapper(recurrence.recurid).toTimestamp(tz.getID()));
		e.calendar = new CalendarResponse();

		Calendar begin = Calendar.getInstance(tz);
		begin.setTimeInMillis(new BmDateTimeWrapper(recurrence.dtstart).toTimestamp(tz.getID()));
		e.calendar.startTime = begin.getTime();

		Calendar end = Calendar.getInstance(tz);
		end.setTimeInMillis(new BmDateTimeWrapper(recurrence.dtend).toTimestamp(tz.getID()));
		e.calendar.endTime = end.getTime();

		e.calendar.dtStamp = new Date();
		e.calendar.subject = recurrence.summary;

		e.location = recurrence.location;

		// BM-9483
		e.calendar.attendees = new ArrayList<CalendarResponse.Attendee>();
		for (Attendee attendee : recurrence.attendees) {

			MSAttendee msa = convertAttendee(attendee);
			CalendarResponse.Attendee a = new CalendarResponse.Attendee();
			a.email = msa.getEmail();
			a.name = msa.getName();
			switch (attendee.partStatus) {
			case Accepted:
				a.status = CalendarResponse.Attendee.AttendeeStatus.ACCEPTED;
				break;
			case NeedsAction:
				a.status = CalendarResponse.Attendee.AttendeeStatus.TENTATIVE;
				break;
			case Declined:
				a.status = CalendarResponse.Attendee.AttendeeStatus.DECLINED;
				break;
			case Tentative:
				a.status = CalendarResponse.Attendee.AttendeeStatus.TENTATIVE;
				break;
			default:
				break;
			}
			switch (msa.getAttendeeType()) {
			case REQUIRED:
				a.type = CalendarResponse.Attendee.AttendeeType.REQUIRED;
				break;
			case RESOURCE:
				a.type = CalendarResponse.Attendee.AttendeeType.RESOURCE;
				break;
			case OPTIONAL:
			default:
				a.type = CalendarResponse.Attendee.AttendeeType.OPTIONAL;
			}

			if (me.getDefaultEmail().equals(attendee.mailto)) {
				switch (attendee.partStatus) {
				case Accepted:
					e.responseType = CalendarResponse.ResponseType.ACCEPTED;
					e.appointmentReplyTime = new Date();
					break;
				case NeedsAction:
					e.responseType = CalendarResponse.ResponseType.NOT_RESPONDED;
					break;
				case Declined:
					e.responseType = CalendarResponse.ResponseType.DECLINED;
					e.appointmentReplyTime = new Date();
					break;
				case Tentative:
					e.responseType = CalendarResponse.ResponseType.TENTATIVE;
					e.appointmentReplyTime = new Date();
					break;
				default:
					break;
				}
			}

			e.calendar.attendees.add(a);
		}

		e.onlineMeetingExternalLink = recurrence.conference;

		exceptions.add(e);
	}

	public MSEvent convert(BackendSession bs, String uid, VEvent vevent) {
		MSEvent mse = new MSEvent();
		MSUser me = bs.getUser();
		mse.setUID(uid);
		mse.setDtStamp(new Date());
		mse.setSubject(vevent.summary);
		if (vevent.status.equals(Status.Cancelled)) {
			ResourceBundle res = ResourceBundle.getBundle("lang/cancelled_event", new Locale(bs.getLang()));
			String cancelledField = res.getString("cancelled");
			mse.setSubject(cancelledField + vevent.summary);
		}
		PlainBodyFormatter pf = new PlainBodyFormatter();
		mse.setDescription(pf.convert(vevent.description));
		mse.setLocation(vevent.location);
		mse.setOnlineMeetingExternalLink(vevent.conference);

		String timezone = vevent.timezone();
		if (timezone == null) {
			timezone = me.getTimeZone();
		}

		TimeZone tz = TimeZone.getTimeZone(timezone);
		if (!vevent.allDay()) {
			mse.setTimeZone(tz);
		}

		Calendar begin = Calendar.getInstance(tz);
		begin.setTimeInMillis(new BmDateTimeWrapper(vevent.dtstart).toTimestamp(tz.getID()));
		mse.setStartTime(begin.getTime());

		Calendar end = Calendar.getInstance(tz);
		end.setTimeInMillis(new BmDateTimeWrapper(vevent.dtend).toTimestamp(tz.getID()));
		mse.setEndTime(end.getTime());

		if (vevent.attendees != null) {
			for (Attendee attendee : vevent.attendees) {
				mse.addAttendee(convertAttendee(attendee));
			}
		}

		if (vevent.organizer != null) {
			if (vevent.organizer.commonName != null) {
				mse.setOrganizerName(vevent.organizer.commonName);
			}
			if (vevent.organizer.mailto != null) {
				mse.setOrganizerEmail(vevent.organizer.mailto);
			}
		} else {
			mse.setOrganizerName(me.getDisplayName());
			mse.setOrganizerEmail(me.getDefaultEmail());

		}
		mse.setAllDayEvent(vevent.allDay());
		mse.setRecurrence(getRecurrence(vevent));
		mse.setExceptions(getException(vevent, tz));

		if (vevent.alarm != null && !vevent.alarm.isEmpty()) {
			Optional<VAlarm> alarm = vevent.alarm.stream().filter(a -> a.action == Action.Display).findFirst();
			if (alarm.isPresent()) {
				mse.setReminder(alarm.get().trigger.intValue() / 60);
			}
		}

		mse.setBusyStatus(busyStatus(vevent));
		mse.setSensitivity(getSensitivity(vevent));

		mse.setMeetingCancelled(vevent.status == Status.Cancelled);

		if (vevent.attachments != null) {
			convertAttachments(vevent, mse);
		}

		return mse;
	}

	private void convertAttachments(VEvent vevent, MSEvent mse) {
		List<Attachment> attachments = new ArrayList<>();
		vevent.attachments.forEach(attachment -> {
			Attachment calendarAttachment = new Attachment();
			calendarAttachment.displayName = attachment.name;
			calendarAttachment.method = Method.NORMAL;
			calendarAttachment.fileReference = AttachmentHelper
					.getEventFileHostingAttachmentFileReference(attachment.publicUrl);
//				calendarAttachment.estimateDataSize = null;
			attachments.add(calendarAttachment);
		});
		mse.setAttachments(attachments);
	}

	/**
	 * @param vevent
	 * @return
	 */
	private Sensitivity getSensitivity(VEvent vevent) {
		if (vevent.classification == null) {
			return Sensitivity.NORMAL;
		}
		switch (vevent.classification) {
		case Confidential:
			return Sensitivity.CONFIDENTIAL;
		case Private:
			return Sensitivity.PRIVATE;
		default:
			return Sensitivity.NORMAL;
		}
	}

	/**
	 * @param vevent
	 * @param tz
	 * @return
	 */
	private List<EventException> getException(VEvent vevent, TimeZone tz) {
		List<EventException> ret = new LinkedList<>();
		if (vevent.exdate != null) {
			for (BmDateTime exception : vevent.exdate) {
				EventException e = new EventException();
				e.deleted = true;
				Calendar begin = Calendar.getInstance(tz);
				begin.setTimeInMillis(new BmDateTimeWrapper(exception).toTimestamp(tz.getID()));
				e.exceptionStartTime = begin.getTime();
				e.calendar = new CalendarResponse();
				e.calendar.startTime = e.exceptionStartTime;
				e.calendar.dtStamp = new Date();
				ret.add(e);
			}
		}

		return ret;
	}

	private BusyStatus busyStatus(VEvent event) {
		if (event.transparency == Transparency.Transparent) {
			return BusyStatus.FREE;
		}
		return BusyStatus.BUSY;
	}

	/**
	 * @param msev
	 * @return
	 */
	private RRule getRecurrence(MSEvent msev) {
		Date startDate = msev.getStartTime();
		Recurrence pr = msev.getRecurrence();
		RRule rrule = new RRule();

		// eas 16.1, iOS sends empty Recurrence
		// <Recurrence xmlns="Calendar"/>
		if (pr.type == null) {
			return null;
		}

		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));

		switch (pr.type) {
		case DAILY:
			rrule.frequency = RRule.Frequency.DAILY;
			rrule.byDay = daysOfWeek(pr.dayOfWeek);
			break;
		case MONTHLY:
			rrule.frequency = RRule.Frequency.MONTHLY;
			break;
		case WEEKLY:
			rrule.frequency = RRule.Frequency.WEEKLY;
			rrule.byDay = daysOfWeek(pr.dayOfWeek);
			break;
		case YEARLY:
			rrule.frequency = RRule.Frequency.YEARLY;
			cal.setTimeInMillis(startDate.getTime());
			cal.set(Calendar.DAY_OF_MONTH, pr.dayOfMonth);
			cal.set(Calendar.MONTH, pr.monthOfYear - 1);
			msev.setStartTime(cal.getTime());
			break;
		case YEARLY_BY_DAY:
			rrule.frequency = RRule.Frequency.YEARLY;
			break;
		case MONTHLY_BY_DAY:
			rrule.frequency = RRule.Frequency.MONTHLY;
			List<WeekDay> dayOfWeek = daysOfWeek(pr.dayOfWeek);
			for (WeekDay weekDay : dayOfWeek) {
				weekDay.offset = pr.weekOfMonth;
			}
			rrule.byDay = dayOfWeek;
			break;
		default:
		}

		if (pr.interval != null) {
			rrule.interval = pr.interval;
		} else {
			rrule.interval = 1;
		}

		// occurence or end date
		if (pr.occurrences != null && pr.occurrences > 0) {
			rrule.count = pr.occurrences;
		} else {
			if (pr.until != null) {
				// RFC 5545
				// If the "DTSTART" property is specified as a date with UTC
				// time or a date with local time and time zone reference, then
				// the
				// UNTIL rule part MUST be specified as a date with UTC time
				String tz = (null == msev.getTimeZone()) ? null : "UTC";
				rrule.until = BmDateTimeWrapper.fromTimestamp(pr.until.getTime(), tz);
			}
		}

		return rrule;
	}

	/**
	 * @param dayOfWeek
	 * @return
	 */
	private List<RRule.WeekDay> daysOfWeek(DayOfWeek dayOfWeek) {
		if (dayOfWeek == null) {
			return null;
		}
		List<RRule.WeekDay> ret = new ArrayList<>(dayOfWeek.days.size());

		if (dayOfWeek.days.contains(Days.SUNDAY)) {
			ret.add(RRule.WeekDay.su());
		}
		if (dayOfWeek.days.contains(Days.MONDAY)) {
			ret.add(RRule.WeekDay.mo());
		}
		if (dayOfWeek.days.contains(Days.TUESDAY)) {
			ret.add(RRule.WeekDay.tu());
		}
		if (dayOfWeek.days.contains(Days.WEDNESDAY)) {
			ret.add(RRule.WeekDay.we());
		}
		if (dayOfWeek.days.contains(Days.THRUSDAY)) {
			ret.add(RRule.WeekDay.th());
		}
		if (dayOfWeek.days.contains(Days.FRIDAY)) {
			ret.add(RRule.WeekDay.fr());
		}
		if (dayOfWeek.days.contains(Days.SATURDAY)) {
			ret.add(RRule.WeekDay.sa());
		}
		return ret;
	}

	/**
	 * @param vevent
	 * @return
	 */
	private Recurrence getRecurrence(VEvent vevent) {
		if (vevent.rrule == null) {
			return null;
		}

		Calendar c = Calendar.getInstance();
		c.setTime(new BmDateTimeWrapper(vevent.dtstart).toDate());

		Recurrence r = new Recurrence();
		switch (vevent.rrule.frequency) {
		case DAILY:
			r.type = Recurrence.Type.DAILY;
			break;
		case MONTHLY:
			if (vevent.rrule.byDay == null) {
				r.type = Recurrence.Type.MONTHLY;
				r.dayOfMonth = c.get(Calendar.DAY_OF_MONTH);
			} else {
				r.type = Recurrence.Type.MONTHLY_BY_DAY;
				r.weekOfMonth = c.get(Calendar.DAY_OF_WEEK_IN_MONTH);
				r.dayOfWeek = getDayOfWeek(c.get(Calendar.DAY_OF_WEEK));
			}
			break;
		case WEEKLY:
			r.type = Recurrence.Type.WEEKLY;
			if (vevent.rrule.byDay != null) {
				r.dayOfWeek = getDayOfWeek(vevent.rrule.byDay);
			} else {
				// BM-8230
				r.dayOfWeek = getDayOfWeek(c.get(Calendar.DAY_OF_WEEK));
			}
			break;
		case YEARLY:
			r.type = Recurrence.Type.YEARLY;
			r.dayOfMonth = c.get(Calendar.DAY_OF_MONTH);
			r.monthOfYear = c.get(Calendar.MONTH) + 1;
			break;
		case HOURLY:
		case MINUTELY:
		case SECONDLY:
		default:
			break;
		}

		if (vevent.rrule.until != null) {
			r.until = new BmDateTimeWrapper(vevent.rrule.until).toDate();
		}

		if (vevent.rrule.count != null) {
			r.occurrences = vevent.rrule.count;
		}

		if (vevent.rrule.interval != null) {
			r.interval = vevent.rrule.interval;
		}

		return r;
	}

	private DayOfWeek getDayOfWeek(int dayOfWeek) {
		DayOfWeek dow = new DayOfWeek();
		switch (dayOfWeek) {
		case 1:
			dow.days = EnumSet.of(Days.SUNDAY);
			break;
		case 2:
			dow.days = EnumSet.of(Days.MONDAY);
			break;
		case 3:
			dow.days = EnumSet.of(Days.TUESDAY);
			break;
		case 4:
			dow.days = EnumSet.of(Days.WEDNESDAY);
			break;
		case 5:
			dow.days = EnumSet.of(Days.THRUSDAY);
			break;
		case 6:
			dow.days = EnumSet.of(Days.FRIDAY);
			break;
		case 7:
			dow.days = EnumSet.of(Days.SATURDAY);
			break;
		default:
			break;
		}

		return dow;
	}

	/**
	 * @param byDay
	 * @return
	 */
	private DayOfWeek getDayOfWeek(List<RRule.WeekDay> byDay) {
		DayOfWeek dow = new DayOfWeek();

		dow.days = new HashSet<Days>(byDay.size());

		if (byDay.contains(RRule.WeekDay.su())) {
			dow.days.add(Days.SUNDAY);
		}

		if (byDay.contains(RRule.WeekDay.mo())) {
			dow.days.add(Days.MONDAY);
		}

		if (byDay.contains(RRule.WeekDay.tu())) {
			dow.days.add(Days.TUESDAY);
		}

		if (byDay.contains(RRule.WeekDay.we())) {
			dow.days.add(Days.WEDNESDAY);
		}

		if (byDay.contains(RRule.WeekDay.th())) {
			dow.days.add(Days.THRUSDAY);
		}

		if (byDay.contains(RRule.WeekDay.fr())) {
			dow.days.add(Days.FRIDAY);
		}

		if (byDay.contains(RRule.WeekDay.sa())) {
			dow.days.add(Days.SATURDAY);
		}

		return dow;
	}

	private Attendee convertAttendee(net.bluemind.eas.dto.calendar.CalendarResponse.Attendee a) {
		Attendee ret = new Attendee();
		ret.mailto = a.email;
		ret.commonName = a.name;
		ret.role = Role.RequiredParticipant;
		ret.partStatus = getParticipationStatus(a.status);
		return ret;
	}

	/**
	 * @param attendee
	 * @return
	 */
	private MSAttendee convertAttendee(VEvent.Attendee attendee) {
		MSAttendee msa = new MSAttendee();

		msa.setAttendeeStatus(getAttendeeStatus(attendee.partStatus));
		msa.setEmail(attendee.mailto);
		msa.setName(attendee.commonName);
		// FIXME harcoded REQUIRED ? (david)
		msa.setAttendeeType(AttendeeType.REQUIRED);
		return msa;
	}

	private AttendeeStatus getAttendeeStatus(ParticipationStatus status) {
		if (status == null) {
			return AttendeeStatus.ACCEPT;
		}

		// FIXME (david)
		switch (status) {
		// case COMPLETED:
		// return AttendeeStatus.RESPONSE_UNKNOWN;
		// case INPROGRESS:
		// return AttendeeStatus.NOT_RESPONDED;
		case Declined:
			return AttendeeStatus.DECLINE;
		case Delegated:
			return AttendeeStatus.RESPONSE_UNKNOWN;
		case NeedsAction:
			return AttendeeStatus.NOT_RESPONDED;
		case Tentative:
			return AttendeeStatus.TENTATIVE;
		default:
		case Accepted:
			return AttendeeStatus.ACCEPT;

		}

	}

	public ConvertedVEvent convert(BackendSession bs, IApplicationData appliData) {
		return convert(bs, null, appliData);
	}

	public ConvertedVEvent convert(BackendSession bs, VEventSeries vevent, IApplicationData appliData) {

		if (vevent == null) {
			vevent = new VEventSeries();
		}
		ConvertedVEvent ret = new ConvertedVEvent();
		MSEvent data = (MSEvent) appliData;

		VEvent e = convertEventOne(vevent.main, data);
		ret.uid = data.getUID();

		if (data.getRecurrence() != null) {
			RRule rrule = getRecurrence(data);
			if (rrule != null) {
				e.rrule = rrule;
			}

			if (data.getExceptions() != null && !data.getExceptions().isEmpty()) {
				convertExceptions(bs, ret, data, e);
			}
		}

		ret.vevent.main = e;

		return ret;
	}

	private void convertExceptions(BackendSession bs, ConvertedVEvent ret, MSEvent data, VEvent e) {
		List<VEventOccurrence> exceptions = new ArrayList<>(data.getExceptions().size());
		Set<BmDateTime> exdate = new HashSet<>(data.getExceptions().size());

		for (EventException excep : data.getExceptions()) {
			Precision p = e.dtstart.precision;

			if (excep.deleted.booleanValue()) {
				exdate.add(BmDateTimeWrapper.fromTimestamp(excep.exceptionStartTime.getTime(),
						data.getTimeZone().getID(), p));
			} else {
				VEvent exception = e.copy();
				exception.rrule = null;
				exception.dtstart = BmDateTimeWrapper.fromTimestamp(excep.startTime.getTime(),
						data.getTimeZone().getID(), p);
				exception.dtend = BmDateTimeWrapper.fromTimestamp(excep.endTime.getTime(), data.getTimeZone().getID(),
						p);
				exception.summary = excep.subject;
				exception.location = excep.location;

				if (excep.attendees != null && !excep.attendees.isEmpty()) {
					exception.attendees = new ArrayList<Attendee>(excep.attendees.size());
					for (net.bluemind.eas.dto.calendar.CalendarResponse.Attendee a : excep.attendees) {
						exception.attendees.add(convertAttendee(a));
					}

					// Exception is a meeting, organizer is expected
					if (exception.organizer == null || exception.organizer.mailto == null
							|| exception.organizer.mailto.isEmpty()) {
						exception.organizer = new Organizer(bs.getUser().getDefaultEmail());
					}

				}

				VEventOccurrence occ = VEventOccurrence.fromEvent(exception, BmDateTimeWrapper
						.fromTimestamp(excep.exceptionStartTime.getTime(), data.getTimeZone().getID(), p));
				exceptions.add(occ);
			}
		}

		e.exdate = exdate;
		ret.vevent.occurrences = exceptions;
	}

	// Exceptions.Exception.Body (section 2.2.3.9): This element is optional.
	// Exceptions.Exception.Categories (section 2.2.3.8): This element is
	// optional.
	private VEvent convertEventOne(VEvent oldEvent, MSEvent data) {
		VEvent e = new VEvent();
		if (data.getSubject() != null && !data.getSubject().trim().isEmpty()) {
			e.summary = data.getSubject();
		} else {
			e.summary = "---";
		}
		e.description = Optional.ofNullable(data.getDescription())
				.map(desc -> "<pre>" + desc.replace("\r\n", "\n") + "</pre>").orElse(null);

		if (data.getLocation() != null) {
			e.location = data.getLocation();
		} else {
			e.location = "";
		}

		Precision p = data.getAllDayEvent().booleanValue() ? Precision.Date : Precision.DateTime;
		e.dtstart = BmDateTimeWrapper.fromTimestamp(data.getStartTime().getTime(), data.getTimeZone().getID(), p);
		e.dtend = BmDateTimeWrapper.fromTimestamp(data.getEndTime().getTime(), data.getTimeZone().getID(), p);
		if (data.getReminder() != null && data.getReminder() >= 0) {
			VAlarm alarm = ICalendarElement.VAlarm.create(-data.getReminder() * 60);
			alarm.action = Action.Display;
			e.alarm = new ArrayList<ICalendarElement.VAlarm>(1);
			e.alarm.add(alarm);
		}

		List<VEvent.Attendee> attendees = new ArrayList<>(data.getAttendees().size());

		for (MSAttendee at : data.getAttendees()) {
			logger.info(" * msAttendee {} => {}", at.getEmail(), at.getAttendeeStatus());
			attendees.add(convertAttendee(at));
		}
		e.attendees = attendees;

		if (data.getBusyStatus() != null) {
			e.transparency = getTransparency(data.getBusyStatus());
		}

		if (data.getSensitivity() != null) {
			e.classification = getClassification(oldEvent, data.getSensitivity());
		}

		e.organizer = new ICalendarElement.Organizer();
		String oem = data.getOrganizerEmail();
		if (oem != null && !oem.isEmpty()) {
			e.organizer.mailto = data.getOrganizerEmail();
		}

		String dn = data.getOrganizerName();
		if (dn != null && !dn.isEmpty()) {
			e.organizer.commonName = data.getOrganizerName();
		}

		e.sequence = oldEvent != null ? oldEvent.sequence : 0;

		logger.info("Event {} has {} attendees", e.summary, e.attendees.size());

		return e;
	}

	/**
	 * @param vevent
	 * @param sensitivity
	 * @return
	 */
	private Classification getClassification(VEvent vevent, Sensitivity sensitivity) {
		if (sensitivity == null) {
			return vevent != null ? vevent.classification : Classification.Public;
		}

		switch (sensitivity) {
		case CONFIDENTIAL:
			return Classification.Confidential;
		case PRIVATE:
			return Classification.Private;
		case NORMAL:
		case PERSONAL:
		default:
			return Classification.Public;
		}

	}

	/**
	 * @param busyStatus
	 * @return
	 */
	private Transparency getTransparency(BusyStatus busyStatus) {
		if (busyStatus == BusyStatus.FREE) {
			return Transparency.Transparent;
		}
		return Transparency.Opaque;
	}

	/**
	 * @param attendee
	 * @return
	 */
	private Attendee convertAttendee(MSAttendee attendee) {
		Attendee ret = new VEvent.Attendee();
		ret.mailto = attendee.getEmail();
		ret.role = Role.RequiredParticipant;
		ret.partStatus = getParticipationStatus(attendee.getAttendeeStatus());
		return ret;
	}

	/**
	 * @param busy
	 * @param attendeeStatus
	 * @return
	 */
	private ParticipationStatus getParticipationStatus(AttendeeStatus attendeeStatus) {

		ParticipationStatus state = null;

		if (attendeeStatus != null) {
			switch (attendeeStatus) {
			case ACCEPT:
				state = ParticipationStatus.Accepted;
				break;
			case DECLINE:
				state = ParticipationStatus.Declined;
				break;
			case NOT_RESPONDED:
			case RESPONSE_UNKNOWN:
			case TENTATIVE:
				state = ParticipationStatus.NeedsAction;
				break;
			}
		}

		return state;
	}

	private ParticipationStatus getParticipationStatus(
			net.bluemind.eas.dto.calendar.CalendarResponse.Attendee.AttendeeStatus status) {
		ParticipationStatus ret = null;
		if (status != null) {
			switch (status) {
			case ACCEPTED:
				ret = ParticipationStatus.Accepted;
				break;
			case DECLINED:
				ret = ParticipationStatus.Declined;
				break;
			case NOT_RESPONDED:
			case RESPONSE_UNKNOWN:
			case TENTATIVE:
				ret = ParticipationStatus.Tentative;
				break;
			default:
				ret = ParticipationStatus.NeedsAction;
				break;

			}
		} else {
			ret = ParticipationStatus.NeedsAction;
		}
		return ret;
	}

}
