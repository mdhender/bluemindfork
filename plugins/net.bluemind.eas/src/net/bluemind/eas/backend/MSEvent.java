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
package net.bluemind.eas.backend;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import net.bluemind.eas.dto.calendar.CalendarResponse.BusyStatus;
import net.bluemind.eas.dto.calendar.CalendarResponse.EventException;
import net.bluemind.eas.dto.calendar.CalendarResponse.MeetingStatus;
import net.bluemind.eas.dto.calendar.CalendarResponse.Recurrence;
import net.bluemind.eas.dto.calendar.CalendarResponse.Sensitivity;
import net.bluemind.eas.dto.type.ItemDataType;

public class MSEvent implements IApplicationData {

	private String organizerName;
	private String organizerEmail;
	private String location;
	private String subject;
	private String uID;
	private String description;
	private Date dtStamp;
	private Date endTime;
	private Date startTime;
	private Boolean allDayEvent;
	private BusyStatus busyStatus;
	private Sensitivity sensitivity;
	private MeetingStatus meetingStatus;
	private Integer reminder;
	private Set<MSAttendee> attendees;
	private List<String> categories;
	private Recurrence recurrence;
	private List<EventException> exceptions;
	private TimeZone timeZone;
	private Date exceptionStartTime;
	private boolean deletedException;
	private int bmUID;

	public MSEvent() {
		this.attendees = new HashSet<MSAttendee>();
	}

	public TimeZone getTimeZone() {
		return timeZone;
	}

	public void setTimeZone(TimeZone timeZone) {
		this.timeZone = timeZone;
	}

	public String getOrganizerName() {
		return organizerName;
	}

	public void setOrganizerName(String organizerName) {
		this.organizerName = organizerName;
	}

	public String getOrganizerEmail() {
		return organizerEmail;
	}

	public void setOrganizerEmail(String organizerEmail) {
		this.organizerEmail = organizerEmail;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getUID() {
		return uID;
	}

	public void setUID(String uid) {
		uID = uid;
	}

	public Boolean getAllDayEvent() {
		return allDayEvent;
	}

	public void setAllDayEvent(Boolean allDayEvent) {
		this.allDayEvent = allDayEvent;
	}

	public BusyStatus getBusyStatus() {
		return busyStatus;
	}

	public void setBusyStatus(BusyStatus busyStatus) {
		this.busyStatus = busyStatus;
	}

	public Sensitivity getSensitivity() {
		return sensitivity;
	}

	public void setSensitivity(Sensitivity sensitivity) {
		this.sensitivity = sensitivity;
	}

	public MeetingStatus getMeetingStatus() {
		return meetingStatus;
	}

	public void setMeetingStatus(MeetingStatus meetingStatus) {
		this.meetingStatus = meetingStatus;
	}

	public Integer getReminder() {
		return reminder;
	}

	public void setReminder(Integer reminder) {
		this.reminder = reminder;
	}

	public Set<MSAttendee> getAttendees() {
		return attendees;
	}

	public void addAttendee(MSAttendee att) {
		if (!attendees.contains(att)) {
			attendees.add(att);
		}
	}

	public List<String> getCategories() {
		return categories;
	}

	public void setCategories(List<String> categories) {
		this.categories = categories;
	}

	public Recurrence getRecurrence() {
		return recurrence;
	}

	public void setRecurrence(Recurrence recurrence) {
		this.recurrence = recurrence;
	}

	public List<EventException> getExceptions() {
		return exceptions;
	}

	public void setExceptions(List<EventException> exceptions) {
		this.exceptions = exceptions;
	}

	public void setDeleted(boolean deleted) {
		this.deletedException = deleted;
	}

	public boolean isDeletedException() {
		return deletedException;
	}

	@Override
	public ItemDataType getType() {
		return ItemDataType.CALENDAR;
	}

	public Date getDtStamp() {
		if (dtStamp != null) {
			return dtStamp;
		}
		return new Date(0);
	}

	public void setDtStamp(Date dtStamp) {
		this.dtStamp = dtStamp;
	}

	public Date getEndTime() {
		return endTime;
	}

	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}

	public Date getStartTime() {
		return startTime;
	}

	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	public Date getExceptionStartTime() {
		return exceptionStartTime;
	}

	public void setExceptionStartTime(Date exceptionStartTime) {
		this.exceptionStartTime = exceptionStartTime;
	}

	public int getBmUID() {
		return bmUID;
	}

	public void setBmUID(int bmUID) {
		this.bmUID = bmUID;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

}
