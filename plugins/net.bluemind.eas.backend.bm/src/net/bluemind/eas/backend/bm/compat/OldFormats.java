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
package net.bluemind.eas.backend.bm.compat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Set;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.eas.backend.BackendSession;
import net.bluemind.eas.backend.HierarchyNode;
import net.bluemind.eas.backend.MSAttendee;
import net.bluemind.eas.backend.MSContact;
import net.bluemind.eas.backend.MSEvent;
import net.bluemind.eas.backend.MSTask;
import net.bluemind.eas.data.ResponseRequestedHelper;
import net.bluemind.eas.dto.calendar.CalendarResponse;
import net.bluemind.eas.dto.calendar.CalendarResponse.Attendee;
import net.bluemind.eas.dto.calendar.CalendarResponse.Attendee.AttendeeStatus;
import net.bluemind.eas.dto.calendar.CalendarResponse.Attendee.AttendeeType;
import net.bluemind.eas.dto.calendar.CalendarResponse.MeetingStatus;
import net.bluemind.eas.dto.calendar.CalendarResponse.ResponseType;
import net.bluemind.eas.dto.contact.ContactResponse;
import net.bluemind.eas.dto.sync.CollectionId;
import net.bluemind.eas.dto.tasks.TasksResponse;
import net.bluemind.eas.dto.tasks.TasksResponse.Importance;
import net.bluemind.eas.dto.user.MSUser;
import net.bluemind.eas.exception.CollectionNotFoundException;
import net.bluemind.eas.impl.Backends;
import net.bluemind.eas.timezone.EASTimeZone;
import net.bluemind.eas.timezone.EASTimeZoneHelper;

public class OldFormats {

	private static final Logger logger = LoggerFactory.getLogger(OldFormats.class);

	public static CalendarResponse update(MSEvent event, MSUser user) {
		CalendarResponse cr = new CalendarResponse();
		cr.uid = event.getUID();
		cr.subject = event.getSubject();
		cr.location = event.getLocation();

		TimeZone tz = event.getTimeZone();
		if (tz == null) {
			// BM-9671 default TZ
			tz = TimeZone.getTimeZone(user.getTimeZone());
		}
		EASTimeZone easTz = EASTimeZoneHelper.from(tz);
		cr.timezone = easTz.toBase64();

		cr.busyStatus = event.getBusyStatus();
		cr.allDayEvent = event.getAllDayEvent();
		cr.sensitivity = event.getSensitivity();
		cr.meetingStatus = event.getMeetingStatus();

		if (event.getAllDayEvent() != Boolean.TRUE) {
			cr.startTime = event.getStartTime();
			cr.endTime = event.getEndTime();
		} else {
			// BM-7264 All-day
			Calendar c = Calendar.getInstance(tz);
			c.setTimeInMillis(event.getStartTime().getTime());
			c.set(Calendar.HOUR_OF_DAY, 0);
			c.set(Calendar.MINUTE, 0);
			c.set(Calendar.SECOND, 0);
			c.set(Calendar.MILLISECOND, 0);

			cr.startTime = c.getTime();

			c.setTimeInMillis(event.getEndTime().getTime());
			c.set(Calendar.HOUR_OF_DAY, 0);
			c.set(Calendar.MINUTE, 0);
			c.set(Calendar.SECOND, 0);
			c.set(Calendar.MILLISECOND, 0);
			cr.endTime = c.getTime();
		}

		Set<MSAttendee> attendees = event.getAttendees();
		cr.attendees = new ArrayList<>(attendees.size());
		AttendeeStatus status = null;
		for (MSAttendee msa : attendees) {
			Attendee updatedAttendee = new Attendee();
			if (cr.organizerEmail != null && cr.organizerEmail.equals(msa.getEmail())) {
				continue;
			}
			if (user.getEmails().contains(msa.getEmail())) {
				status = status(msa.getAttendeeStatus());
				if (status != AttendeeStatus.NotResponded) {
					updatedAttendee.status = status;
				}

			} else {
				updatedAttendee.status = status(msa.getAttendeeStatus());
			}
			updatedAttendee.email = msa.getEmail();
			updatedAttendee.name = msa.getName();
			updatedAttendee.type = type(msa.getAttendeeType());
			cr.attendees.add(updatedAttendee);
		}
		cr.organizerEmail = event.getOrganizerEmail();
		cr.organizerName = event.getOrganizerName();
		cr.dtStamp = event.getDtStamp();

		// Reminder **BEFORE** calendar item's start
		// ignore reminder after event starts
		if (event.getReminder() != null && event.getReminder() < 0) {
			cr.reminder = event.getReminder() * -1;
		}

		cr.recurrence = event.getRecurrence();
		cr.exceptions = event.getExceptions();

		boolean organize = user.getEmails().contains(cr.organizerEmail);

		if (!cr.attendees.isEmpty()) {
			if (organize) {
				cr.meetingStatus = MeetingStatus.MeetingAndUserIsOrganizer;
			} else {
				cr.meetingStatus = MeetingStatus.MeetingAndUserIsNotOrganizer;
			}
			cr.responseRequested = ResponseRequestedHelper.isResponseRequested(event);
		} else {
			cr.meetingStatus = MeetingStatus.Appointment;
		}

		if (status != null) {
			switch (status) {
			case Accepted:
				cr.responseType = ResponseType.Accepted;
				cr.appointmentReplyTime = new Date();
				break;
			case Declined:
				cr.responseType = ResponseType.Declined;
				cr.appointmentReplyTime = new Date();
				break;
			case NotResponded:
				cr.responseType = ResponseType.NotResponded;
				break;
			case ResponseUnknown:
				cr.responseType = ResponseType.None;
				break;
			case Tentative:
				cr.responseType = ResponseType.Tentative;
				cr.appointmentReplyTime = new Date();
				break;
			default:
				break;

			}
		} else if (organize) {
			cr.responseType = ResponseType.Organizer;
		}

		return cr;

	}

	public static CalendarResponse update(BackendSession bs, MSEvent event, MSUser user, CollectionId collectionId) {
		CalendarResponse cr = update(event, user);

		// BM-6571
		try {
			HierarchyNode node = Backends.internalStorage().getHierarchyNode(bs, collectionId);
			if (!ICalendarUids.defaultUserCalendar(user.getUid()).equals(node.containerUid)) {
				// Event synced in a user_created_calendar
				// Set as Appointment to prevent notification
				cr.meetingStatus = MeetingStatus.Appointment;
				// force no response requested
				cr.responseRequested = false;

				// BM-7359
				// no reminder for shared calendars
				cr.reminder = null;

			}
		} catch (CollectionNotFoundException e) {
			logger.error(e.getMessage(), e);
		}

		return cr;
	}

	private static AttendeeType type(net.bluemind.eas.data.calendarenum.AttendeeType attendeeType) {
		switch (attendeeType) {
		case REQUIRED:
			return AttendeeType.Required;
		case RESOURCE:
			return AttendeeType.Resource;
		case OPTIONAL:
		default:
			return AttendeeType.Optional;
		}
	}

	private static AttendeeStatus status(net.bluemind.eas.data.calendarenum.AttendeeStatus attendeeStatus) {
		switch (attendeeStatus) {
		case ACCEPT:
			return AttendeeStatus.Accepted;
		case DECLINE:
			return AttendeeStatus.Declined;
		case NOT_RESPONDED:
			return AttendeeStatus.NotResponded;
		case RESPONSE_UNKNOWN:
			return AttendeeStatus.ResponseUnknown;
		case TENTATIVE:
			return AttendeeStatus.Tentative;
		default:
			return AttendeeStatus.ResponseUnknown;
		}
	}

	public static ContactResponse update(MSContact c) {
		ContactResponse cr = new ContactResponse();
		cr.lastName = c.getLastName();
		cr.firstName = c.getFirstName();
		cr.middleName = c.getMiddleName();
		cr.suffix = c.getSuffix();
		cr.nickName = c.getNickName();
		cr.jobTitle = c.getJobTitle();
		cr.title = c.getTitle();
		cr.department = c.getDepartment();
		cr.companyName = c.getCompanyName();
		cr.spouse = c.getSpouse();
		cr.assistantName = c.getAssistantName();
		cr.managerName = c.getManagerName();
		cr.categories = c.getCategories();
		cr.anniversary = c.getAnniversary();
		cr.birthday = c.getBirthday();
		cr.webPage = c.getWebPage();

		cr.businessAddressStreet = c.getBusinessStreet();
		cr.businessAddressPostalCode = c.getBusinessPostalCode();
		cr.businessAddressCity = c.getBusinessAddressCity();
		cr.businessAddressCountry = c.getBusinessAddressCountry();
		cr.businessAddressState = c.getBusinessState();

		cr.homeAddressStreet = c.getHomeAddressStreet();
		cr.homeAddressPostalCode = c.getHomeAddressPostalCode();
		cr.homeAddressCity = c.getHomeAddressCity();
		cr.homeAddressCountry = c.getHomeAddressCountry();
		cr.homeAddressState = c.getHomeAddressState();

		cr.otherAddressStreet = c.getOtherAddressStreet();
		cr.otherAddressPostalCode = c.getOtherAddressPostalCode();
		cr.otherAddressCity = c.getOtherAddressCity();
		cr.otherAddressCountry = c.getOtherAddressCountry();
		cr.otherAddressState = c.getOtherAddressState();

		cr.homePhoneNumber = c.getHomePhoneNumber();
		cr.home2PhoneNumber = c.getHome2PhoneNumber();
		cr.mobilePhoneNumber = c.getMobilePhoneNumber();
		cr.businessPhoneNumber = c.getBusinessPhoneNumber();
		cr.business2PhoneNumber = c.getBusiness2PhoneNumber();
		cr.homeFaxNumber = c.getHomeFaxNumber();
		cr.businessFaxNumber = c.getBusinessFaxNumber();
		cr.pagerNumber = c.getPagerNumber();

		cr.imAddress = c.getIMAddress();
		cr.imAddress2 = c.getIMAddress2();
		cr.imAddress3 = c.getIMAddress3();

		cr.email1Address = c.getEmail1Address();
		cr.email2Address = c.getEmail2Address();
		cr.email3Address = c.getEmail3Address();

		if (c.getPicture() != null && !c.getPicture().isEmpty()) {
			cr.picture = c.getPicture();
		}

		return cr;
	}

	public static TasksResponse update(MSTask msTask) {
		TasksResponse tr = new TasksResponse();

		tr.subject = msTask.subject;

		if (msTask.importance != null) {
			switch (msTask.importance) {
			case 9:
				tr.importance = Importance.Low;
				break;
			case 5:
				tr.importance = Importance.Normal;
				break;
			case 1:
				tr.importance = Importance.High;
				break;
			default:
				tr.importance = Importance.Normal;
				break;
			}
		} else {
			tr.importance = Importance.Normal;
		}

		tr.utcStartDate = msTask.utcStartDate;
		tr.startDate = msTask.startDate;
		tr.utcDueDate = msTask.utcDueDate;
		tr.dueDate = msTask.dueDate;
		tr.categories = msTask.categories;
		tr.recurrence = msTask.recurrence;
		tr.complete = msTask.complete;
		tr.dateCompleted = msTask.dateCompleted;
		tr.sensitivity = msTask.sensitivity;
		tr.reminderSet = msTask.reminderSet;
		tr.reminderTime = msTask.reminderTime;

		// public Date ordinalDate;
		// public String subOrdinalDate;

		return tr;
	}
}
