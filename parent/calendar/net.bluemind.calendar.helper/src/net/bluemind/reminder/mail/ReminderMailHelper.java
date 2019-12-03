package net.bluemind.reminder.mail;
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

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;

import org.apache.james.mime4j.dom.TextBody;
import org.apache.james.mime4j.message.BasicBodyFactory;
import org.apache.james.mime4j.message.BodyPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.template.Template;
import freemarker.template.TemplateException;
import net.bluemind.common.freemarker.FreeMarkerMsg;
import net.bluemind.common.freemarker.MessagesResolver;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.icalendar.api.ICalendarElement;
import net.bluemind.icalendar.api.ICalendarElement.VAlarm;

public abstract class ReminderMailHelper<T extends ICalendarElement> {

	protected Logger logger = LoggerFactory.getLogger(ReminderMailHelper.class);

	protected abstract Template getTemplate(String templateName, Locale locale) throws IOException;

	/**
	 * @param text
	 * @return
	 */
	public BodyPart createTextPart(String text) {
		BasicBodyFactory bodyFactory = new BasicBodyFactory();
		TextBody body = bodyFactory.textBody(text, StandardCharsets.UTF_8);

		BodyPart bodyPart = new BodyPart();
		bodyPart.setText(body);

		return bodyPart;
	}

	/**
	 * @param templateName
	 * @param locale
	 * @param data
	 * @return
	 */
	public String buildSubject(String templateName, String locale, MessagesResolver messagesResolver,
			Map<String, Object> data) {
		if (null == locale) {
			locale = "fr";
		}
		StringWriter sw = new StringWriter();
		Template t;
		data.put("msg", new FreeMarkerMsg(messagesResolver));
		try {
			t = getTemplate(templateName, new Locale(locale));
			t.process(data, sw);
		} catch (TemplateException e1) {
			logger.error(e1.getMessage(), e1);
		} catch (IOException e1) {
			logger.error(e1.getMessage(), e1);
		}

		return sw.toString();
	}

	/**
	 * @param templateName
	 * @param locale
	 * @param data
	 * @return
	 * @throws IOException
	 * @throws TemplateException
	 */
	public BodyPart buildBody(String templateName, String locale, MessagesResolver messagesResolver,
			Map<String, Object> data) throws IOException, TemplateException {
		if (null == locale) {
			locale = "fr";
		}

		StringWriter sw = new StringWriter();
		Template t = getTemplate(templateName, new Locale(locale));
		data.put("msg", new FreeMarkerMsg(messagesResolver));
		t.process(data, sw);
		sw.flush();

		return createTextPart(sw.toString());
	}

	/**
	 * Extract {@link VEvent} data
	 * 
	 * @param entity the {@link VEvent} to extract
	 * @return a {@link Map} containing the {@link VEvent} data
	 */
	protected void addICalendarelementDataToMap(T entity, VAlarm valarm, Map<String, Object> data) {
		data.put("title", entity.summary);
		if (entity.location != null && !entity.location.isEmpty()) {
			data.put("location", entity.location);
		}

		data.put("datebegin", new BmDateTimeWrapper(entity.dtstart).toDate());

		if (entity.description != null && !entity.description.isEmpty()) {
			// FIXME pretty description
			String desc = entity.description.replace("\r\n", "\n");
			data.put("description", desc);
		}
		data.put("available", true);

		if (entity.organizer != null && entity.organizer.commonName != null) {
			data.put("owner", entity.organizer.commonName);
		}

		if (valarm != null) {
			if (valarm.trigger.intValue() == 0) {
				data.put("reminder_unit", "seconds");
				data.put("reminder_duration", 0);
			} else if (valarm.trigger.intValue() % 60 != 0) {
				data.put("reminder_unit", "seconds");
				data.put("reminder_duration", Math.abs(valarm.trigger.intValue()));
			} else if (valarm.trigger.intValue() % 3600 != 0) {
				data.put("reminder_unit", "minutes");
				data.put("reminder_duration", Math.abs(valarm.trigger.intValue()) / 60);
			} else if (valarm.trigger.intValue() % 86400 != 0) {
				data.put("reminder_unit", "hours");
				data.put("reminder_duration", Math.abs(valarm.trigger.intValue()) / 3600);
			} else {
				data.put("reminder_unit", "days");
				data.put("reminder_duration", Math.abs(valarm.trigger.intValue()) / 86400);
			}
			if (valarm.summary != null && !valarm.summary.isEmpty()) {
				data.put("reminder_summary", valarm.summary);
			}
		}

		if (entity.rrule != null) {
			data.put("recurrenceKind", entity.rrule.frequency);
			data.put("recurrenceFreq", entity.rrule.interval);
			data.put("recurrenceDays", entity.rrule.byDay);
			data.put("recurrenceEnd", entity.rrule.until);
		}
	}
}
