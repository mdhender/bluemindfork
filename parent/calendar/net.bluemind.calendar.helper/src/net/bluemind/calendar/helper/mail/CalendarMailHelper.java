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
package net.bluemind.calendar.helper.mail;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.james.mime4j.dom.BinaryBody;
import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.message.BasicBodyFactory;
import org.apache.james.mime4j.message.BodyPart;

import com.google.common.base.Strings;

import freemarker.template.Configuration;
import freemarker.template.Template;
import net.bluemind.calendar.api.VEvent;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.sendmail.SendmailHelper;
import net.bluemind.icalendar.api.ICalendarElement.Organizer;
import net.bluemind.icalendar.api.ICalendarElement.ParticipationStatus;
import net.bluemind.icalendar.api.ICalendarElement.VAlarm;
import net.bluemind.reminder.mail.ReminderMailHelper;

public class CalendarMailHelper extends ReminderMailHelper<VEvent> {

	private Configuration cfg;

	public CalendarMailHelper() {
		cfg = new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
		cfg.setClassForTemplateLoading(this.getClass(), "/");
	}

	public BodyPart createBinaryPart(byte[] part) {

		BasicBodyFactory bodyFactory = new BasicBodyFactory();
		BinaryBody body = bodyFactory.binaryBody(part);

		BodyPart bodyPart = new BodyPart();
		bodyPart.setBody(body);

		return bodyPart;
	}

	/**
	 * Extract {@link VEvent} data
	 * 
	 * @param vevent the {@link VEvent} to extract
	 * @return a {@link Map} containing the {@link VEvent} data
	 */
	public Map<String, Object> extractVEventDataToMap(VEvent vevent, Organizer organizer, VAlarm valarm) {
		Map<String, Object> data = new HashMap<>();

		Long duration = vevent.dtend != null
				? (new BmDateTimeWrapper(vevent.dtend).toUTCTimestamp()
						- new BmDateTimeWrapper(vevent.dtstart).toUTCTimestamp()) / 1000
				: null;

		data.put("duration", duration);

		Organizer resolvedOrganizer = vevent.organizer != null ? vevent.organizer : organizer;
		if (resolvedOrganizer != null) {
			Mailbox orgMailbox = SendmailHelper.formatAddress(resolvedOrganizer.commonName, resolvedOrganizer.mailto);
			data.put("owner", orgMailbox.getName() != null ? orgMailbox.getName() : orgMailbox.getAddress());
		}

		if (vevent.dtend != null) {
			Date dtend = new BmDateTimeWrapper(vevent.dtend).toDate();
			if (vevent.allDay()) {
				Calendar c = Calendar.getInstance();
				c.setTimeInMillis(dtend.getTime());
				c.add(Calendar.DATE, -1);
				data.put("dateend", c.getTime());
			} else {
				data.put("dateend", dtend);
			}
		} else {
			data.put("dateend", new BmDateTimeWrapper(vevent.dtstart).toDate());
		}

		data.put("allday", vevent.allDay() ? "true" : "false");

		List<String> attendees = vevent.attendees.stream().map(CalendarMailHelper::attendeeDisplayName).toList();

		if (!attendees.isEmpty()) {
			data.put("attendees", attendees);
		}

		if (vevent.dtstart.timezone != null) {
			data.put("tz", vevent.dtstart.timezone);
		}

		if (!Strings.isNullOrEmpty(vevent.url)) {
			data.put("url", vevent.url);
		}

		if (!Strings.isNullOrEmpty(vevent.conference)) {
			data.put("conference", vevent.conference);
		}

		super.addICalendarelementDataToMap(vevent, valarm, data);

		return data;
	}

	public static String attendeeDisplayName(VEvent.Attendee attendee) {
		if (Strings.isNullOrEmpty(attendee.commonName)) {
			return attendee.mailto;
		} else {
			return attendee.commonName;
		}
	}

	/**
	 * @param vevent
	 * @return
	 */
	public Map<String, Object> extractVEventData(VEvent vevent, Organizer organizer) {
		return extractVEventDataToMap(vevent, organizer, null);
	}

	/**
	 * @param vevent
	 * @return
	 */
	public Map<String, Object> extractVEventData(VEvent vevent) {
		return extractVEventDataToMap(vevent, vevent.organizer, null);
	}

	/**
	 * @param partStatus
	 * @return
	 */
	public static String extractPartState(ParticipationStatus partStatus) {
		if (ParticipationStatus.Accepted == partStatus)
			return "ACCEPTED";
		if (ParticipationStatus.Declined == partStatus)
			return "DECLINED";
		if (ParticipationStatus.NeedsAction == partStatus)
			return "NEEDS-ACTION";
		if (ParticipationStatus.Tentative == partStatus)
			return "TENTATIVE";
		// TODO other status
		return "NEEDS-ACTION";
	}

	@Override
	protected Template getTemplate(String templateName, Locale locale) throws IOException {
		return cfg.getTemplate(templateName, locale);
	}

}
