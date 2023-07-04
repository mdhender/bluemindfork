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
package net.bluemind.eas.data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.google.common.base.Strings;

import net.bluemind.eas.backend.BackendSession;
import net.bluemind.eas.backend.IApplicationData;
import net.bluemind.eas.backend.MSAttendee;
import net.bluemind.eas.backend.MSEvent;
import net.bluemind.eas.data.calendarenum.AttendeeStatus;
import net.bluemind.eas.data.calendarenum.AttendeeType;
import net.bluemind.eas.data.email.Type;
import net.bluemind.eas.dto.base.AirSyncBaseResponse.Attachment;
import net.bluemind.eas.dto.base.AirSyncBaseResponse.Attachment.Method;
import net.bluemind.eas.dto.calendar.CalendarResponse;
import net.bluemind.eas.dto.calendar.CalendarResponse.Attendee;
import net.bluemind.eas.dto.calendar.CalendarResponse.BusyStatus;
import net.bluemind.eas.dto.calendar.CalendarResponse.EventException;
import net.bluemind.eas.dto.calendar.CalendarResponse.MeetingStatus;
import net.bluemind.eas.dto.calendar.CalendarResponse.Recurrence;
import net.bluemind.eas.dto.calendar.CalendarResponse.Recurrence.DayOfWeek;
import net.bluemind.eas.dto.calendar.CalendarResponse.Sensitivity;
import net.bluemind.eas.utils.DOMUtils;
import net.bluemind.eas.utils.RTFUtils;

public class CalendarDecoder extends Decoder implements IDataDecoder {

	@Override
	public IApplicationData decode(BackendSession bs, Element syncData) {
		Element containerNode;
		MSEvent msEvent = new MSEvent();

		// Main attributes
		msEvent.setOrganizerName(parseDOMString(DOMUtils.getUniqueElement(syncData, "OrganizerName")));
		msEvent.setOrganizerEmail(parseDOMString(DOMUtils.getUniqueElement(syncData, "OrganizerEmail")));
		msEvent.setUID(parseDOMString(DOMUtils.getUniqueElement(syncData, "UID")));
		msEvent.setTimeZone(parseDOMTimeZone(DOMUtils.getUniqueElement(syncData, "Timezone"),
				TimeZone.getTimeZone(bs.getUser().getTimeZone())));

		setEventCalendar(msEvent, syncData);
		// MDP-223
		if (msEvent.getMeetingStatus() == null) {
			logger.error("MeetingStatus is NULL, do not proccess");
			return null;
		}

		// Attendees
		containerNode = DOMUtils.getDirectChildElement(syncData, "Attendees");
		if (containerNode != null) {
			decodeAttendees(syncData, containerNode, msEvent);
		}

		// Exceptions
		containerNode = DOMUtils.getUniqueElement(syncData, "Exceptions");
		if (containerNode != null) {
			decodeExceptions(containerNode, msEvent);
		}

		// Recurrence
		containerNode = DOMUtils.getUniqueElement(syncData, "Recurrence");
		if (containerNode != null) {
			decodeRecurrence(containerNode, msEvent);
		}

		containerNode = DOMUtils.getUniqueElement(syncData, "Attachments");
		if (containerNode != null) {
			decodeAttachments(containerNode, msEvent);
		}

		return msEvent;
	}

	private void decodeAttachments(Element containerNode, MSEvent msEvent) {
		List<Attachment> attachments = new ArrayList<>();
		for (int i = 0, n = containerNode.getChildNodes().getLength(); i < n; i += 1) {
			Element node = (Element) containerNode.getChildNodes().item(i);

			Attachment attachment = new Attachment();
			attachment.clientId = parseDOMString(DOMUtils.getUniqueElement(node, "ClientId"));
			attachment.method = Method.NORMAL;// Method.valueOf(parseDOMString(DOMUtils.getUniqueElement(node,
												// "AttMethod")));
			attachment.contentType = parseDOMString(DOMUtils.getUniqueElement(node, "ContentType"));
			attachment.content = parseDOMString(DOMUtils.getUniqueElement(node, "Content"));
			attachment.displayName = parseDOMString(DOMUtils.getUniqueElement(node, "DisplayName"));
			attachments.add(attachment);
		}

		msEvent.setAttachments(attachments);
	}

	private void decodeRecurrence(Element containerNode, MSEvent msEvent) {
		Recurrence recurrence = new Recurrence();

		Date recurrenceUntil = parseDOMDate(DOMUtils.getUniqueElement(containerNode, "Until"));

		if (recurrenceUntil != null) {
			long recUntil = recurrenceUntil.getTime();
			long duration = msEvent.getEndTime().getTime() - msEvent.getStartTime().getTime();
			Date recUntilFixed = new Date(recUntil + duration);
			recurrence.until = recUntilFixed;
		}

		recurrence.weekOfMonth = parseDOMInt(DOMUtils.getUniqueElement(containerNode, "WeekOfMonth"));
		recurrence.monthOfYear = parseDOMInt(DOMUtils.getUniqueElement(containerNode, "MonthOfYear"));
		recurrence.dayOfMonth = parseDOMInt(DOMUtils.getUniqueElement(containerNode, "DayOfMonth"));
		recurrence.occurrences = parseDOMInt(DOMUtils.getUniqueElement(containerNode, "Occurrences"));
		recurrence.interval = parseDOMInt(DOMUtils.getUniqueElement(containerNode, "Interval"));
		Integer i = parseDOMInt(DOMUtils.getUniqueElement(containerNode, "DayOfWeek"));
		if (i != null) {
			recurrence.dayOfWeek = DayOfWeek.fromInt(i);
		}

		switch (parseDOMNoNullInt(DOMUtils.getUniqueElement(containerNode, "Type"))) {
		case 0:
			recurrence.type = Recurrence.Type.DAILY;
			break;
		case 1:
			recurrence.type = Recurrence.Type.WEEKLY;
			break;
		case 2:
			recurrence.type = Recurrence.Type.MONTHLY;
			break;
		case 3:
			recurrence.type = Recurrence.Type.MONTHLY_BY_DAY;
			break;
		case 5:
			recurrence.type = Recurrence.Type.YEARLY;
			break;
		case 6:
			recurrence.type = Recurrence.Type.YEARLY_BY_DAY;
			break;
		default:
			logger.warn("Unknown recurrence type {}",
					parseDOMNoNullInt(DOMUtils.getUniqueElement(containerNode, "Type")));
		}

		msEvent.setRecurrence(recurrence);
	}

	private void decodeExceptions(Element containerNode, MSEvent msEvent) {
		ArrayList<EventException> exceptions = new ArrayList<>();
		for (int i = 0, n = containerNode.getChildNodes().getLength(); i < n; i += 1) {
			Element subnode = (Element) containerNode.getChildNodes().item(i);
			EventException exception = new EventException();

			exception.deleted = parseDOMInt2Boolean(DOMUtils.getUniqueElement(subnode, "Deleted"));
			exception.exceptionStartTime = parseDOMDate(DOMUtils.getUniqueElement(subnode, "ExceptionStartTime"));
			exception.startTime = parseDOMDate(DOMUtils.getUniqueElement(subnode, "StartTime"));
			exception.endTime = parseDOMDate(DOMUtils.getUniqueElement(subnode, "EndTime"));
			exception.subject = parseDOMString(DOMUtils.getUniqueElement(subnode, "Subject"));
			exception.location = getLocation(subnode);

			Element attendeesNode = DOMUtils.getDirectChildElement(subnode, "Attendees");
			if (attendeesNode != null) {
				NodeList att = attendeesNode.getChildNodes();
				List<Attendee> attendees = new ArrayList<>(att.getLength());
				for (int j = 0; j < att.getLength(); j++) {
					Element attElem = (Element) att.item(j);
					MSAttendee attendee = getAttendee(subnode, attElem);

					Attendee a = new Attendee();
					a.email = attendee.getEmail();
					a.name = attendee.getName();
					a.type = convertType(attendee.getAttendeeType());
					a.status = convertStatus(attendee.getAttendeeStatus());
					attendees.add(a);
				}
				exception.attendees = attendees;
			}

			exceptions.add(exception);
		}
		msEvent.setExceptions(exceptions);
	}

	private void decodeAttendees(Element syncData, Element containerNode, MSEvent msEvent) {
		NodeList children = containerNode.getChildNodes();
		int len = children.getLength();
		for (int i = 0; i < len; i++) {
			Element attElem = (Element) children.item(i);
			MSAttendee attendee = getAttendee(syncData, attElem);
			msEvent.addAttendee(attendee);
		}
	}

	private static CalendarResponse.Attendee.AttendeeType convertType(
			net.bluemind.eas.data.calendarenum.AttendeeType attendeeType) {
		if (attendeeType == null) {
			return CalendarResponse.Attendee.AttendeeType.OPTIONAL;
		}
		switch (attendeeType) {
		case REQUIRED:
			return CalendarResponse.Attendee.AttendeeType.REQUIRED;
		case RESOURCE:
			return CalendarResponse.Attendee.AttendeeType.RESOURCE;
		case OPTIONAL:
		default:
			return CalendarResponse.Attendee.AttendeeType.OPTIONAL;
		}
	}

	private static CalendarResponse.Attendee.AttendeeStatus convertStatus(
			net.bluemind.eas.data.calendarenum.AttendeeStatus attendeeStatus) {
		if (attendeeStatus == null) {
			return CalendarResponse.Attendee.AttendeeStatus.RESPONSE_UNKNOWN;
		}
		switch (attendeeStatus) {
		case ACCEPT:
			return CalendarResponse.Attendee.AttendeeStatus.ACCEPTED;
		case DECLINE:
			return CalendarResponse.Attendee.AttendeeStatus.DECLINED;
		case NOT_RESPONDED:
			return CalendarResponse.Attendee.AttendeeStatus.NOT_RESPONDED;
		case RESPONSE_UNKNOWN:
			return CalendarResponse.Attendee.AttendeeStatus.RESPONSE_UNKNOWN;
		case TENTATIVE:
			return CalendarResponse.Attendee.AttendeeStatus.TENTATIVE;
		default:
			return CalendarResponse.Attendee.AttendeeStatus.RESPONSE_UNKNOWN;
		}
	}

	private MSAttendee getAttendee(Element syncData, Element att) {
		MSAttendee attendee = new MSAttendee();

		String email = parseDOMString(DOMUtils.getUniqueElement(att, "Email"));
		if (email == null) {
			email = "";
		}
		attendee.setEmail(email);

		String name = parseDOMString(DOMUtils.getUniqueElement(att, "Name"));
		if (name == null) {
			name = "";
		}
		attendee.setName(name);

		int attStatus = parseDOMNoNullInt(DOMUtils.getUniqueElement(syncData, "AttendeeStatus"));
		attendee.setAttendeeStatus(AttendeeStatus.fromInt(attStatus));

		switch (parseDOMNoNullInt(DOMUtils.getUniqueElement(syncData, "AttendeeType"))) {
		case 2:
			attendee.setAttendeeType(AttendeeType.OPTIONAL);
			break;
		case 3:
			attendee.setAttendeeType(AttendeeType.RESOURCE);
			break;
		default:
		case 1:
			attendee.setAttendeeType(AttendeeType.REQUIRED);
			break;
		}

		return attendee;
	}

	private void setEventCalendar(MSEvent calendar, Element domSource) {

		calendar.setLocation(getLocation(domSource));

		// description
		Element body = DOMUtils.getUniqueElement(domSource, "Body");
		if (body != null) {
			setEventDescription(calendar, body);
		} else {
			calendar.setDescription(null);
		}

		Element rtf = DOMUtils.getUniqueElement(domSource, "Compressed_RTF");
		if (rtf != null) {
			String txt = rtf.getTextContent();
			calendar.setDescription(RTFUtils.extractB64CompressedRTF(txt));
		}

		calendar.setSubject(parseDOMString(DOMUtils.getUniqueElement(domSource, "Subject")));

		Date dtstamp = parseDOMDate(DOMUtils.getUniqueElement(domSource, "DTStamp"));
		if (dtstamp == null) {
			dtstamp = new Date();
		}
		calendar.setDtStamp(dtstamp);

		Date startTime = parseDOMDate(DOMUtils.getUniqueElement(domSource, "StartTime"));
		Date endTime = parseDOMDate(DOMUtils.getUniqueElement(domSource, "EndTime"));

		if (startTime == null && endTime == null) {
			startTime = new Date(0);
			endTime = new Date(0);
		}

		calendar.setStartTime(startTime);
		calendar.setEndTime(endTime);

		calendar.setAllDayEvent(parseDOMInt2Boolean(DOMUtils.getUniqueElement(domSource, "AllDayEvent")));

		// AS-CAL 2.2.2.38
		// Reminder: number of minutes before the calendar item's start
		Element reminder = DOMUtils.getUniqueElement(domSource, "Reminder");
		if (Strings.isNullOrEmpty(reminder.getNodeValue())) {
			calendar.setReminder(parseDOMInt(reminder));
		}

		calendar.setCategories(
				parseDOMStringCollection(DOMUtils.getUniqueElement(domSource, "Categories"), "Category"));

		calendar.setBusyStatus(getCalendarBusyStatus(domSource));
		if (calendar.getBusyStatus() != null) {
			logger.info("BusyStatus: {}", calendar.getBusyStatus());
		}

		calendar.setSensitivity(getCalendarSensitivity(domSource));
		if (calendar.getSensitivity() != null) {
			logger.info("Sensitivity: {}", calendar.getSensitivity());
		}

		calendar.setMeetingStatus(getMeetingStatus(domSource));
		if (calendar.getMeetingStatus() != null) {
			logger.info("MeetingStatus: {}", calendar.getMeetingStatus());
		}
	}

	private String getLocation(Element dom) {
		Element element = DOMUtils.getUniqueElement(dom, "Location");
		if (element == null) {
			return null;
		}

		if (!element.hasChildNodes()) {
			// protocol < 16
			// <Location>In the kitchen</Location>
			return parseDOMString(element);
		}

		// protocol > 16
		// <Location xmlns="AirSyncBase">
		// <DisplayName>BlueMind</DisplayName>
		// <Annotation>40 Rue du Village d'Entreprises 31670 Labège France</Annotation>
		// <Latitude>43.54280248150819</Latitude>
		// <Longitude>1.51013808765032</Longitude>
		// </Location>
		//
		// ignore Latitude and Longitude

		StringBuilder location = new StringBuilder();
		Element displayName = DOMUtils.getUniqueElement(element, "DisplayName");
		if (displayName != null) {
			location.append(displayName.getTextContent()).append(" ");
		}
		Element annotation = DOMUtils.getUniqueElement(element, "Annotation");
		if (annotation != null) {
			location.append(annotation.getTextContent()).append(" ");
		}

		return location.toString();
	}

	private void setEventDescription(MSEvent calendar, Element body) {
		Element data = DOMUtils.getUniqueElement(body, "Data");
		if (data != null) {
			Type bodyType = Type.fromInt(Integer.parseInt(DOMUtils.getUniqueElement(body, "Type").getTextContent()));
			String txt = data.getTextContent();

			if (bodyType == Type.PLAIN_TEXT) {
				calendar.setDescription(txt);
				logger.debug("Desc: {}", txt);
			} else if (bodyType == Type.RTF) {
				txt = RTFUtils.extractB64CompressedRTF(txt);
				calendar.setDescription(txt);
				logger.debug("Desc: {}", txt);
			} else {
				logger.warn("Unsupported body type: {}\n{}", bodyType, txt);
			}
		} else {
			calendar.setDescription(null);
		}
	}

	private BusyStatus getCalendarBusyStatus(Element domSource) {
		switch (parseDOMNoNullInt(DOMUtils.getUniqueElement(domSource, "BusyStatus"))) {
		case 0:
			return BusyStatus.FREE;
		case 1:
			return BusyStatus.TENTATIVE;
		case 2:
			return BusyStatus.BUSY;
		case 3:
			return BusyStatus.OUT_OF_OFFICE;
		default:
			return null;
		}
	}

	private Sensitivity getCalendarSensitivity(Element domSource) {
		switch (parseDOMNoNullInt(DOMUtils.getUniqueElement(domSource, "Sensitivity"))) {
		case 0:
			return Sensitivity.NORMAL;
		case 1:
			return Sensitivity.PERSONAL;
		case 2:
			return Sensitivity.PRIVATE;
		case 3:
			return Sensitivity.CONFIDENTIAL;
		default:
			return null;
		}
	}

	private MeetingStatus getMeetingStatus(Element domSource) {
		switch (parseDOMNoNullInt(DOMUtils.getUniqueElement(domSource, "MeetingStatus"))) {
		case 0:
			return MeetingStatus.APPOINTMENT;
		case 1:
			return MeetingStatus.MEETING_AND_USER_IS_ORGANIZER;
		case 3:
			return MeetingStatus.MEETING_AND_USER_IS_NOT_ORGANIZER;
		case 5:
			return MeetingStatus.CANCELED_AND_USER_WAS_ORGANIZER;
		case 4:
			return null;
		case 7:
			return MeetingStatus.CANCEL_RECEIVED;
		default:
			return null;
		}
	}
}
