/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.todolist.hook.internal;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.james.mime4j.dom.TextBody;
import org.apache.james.mime4j.message.BasicBodyFactory;
import org.apache.james.mime4j.message.BodyPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import net.bluemind.common.freemarker.FreeMarkerMsg;
import net.bluemind.common.freemarker.MessagesResolver;
import net.bluemind.todolist.api.VTodo;

public class TodoMailHelper {

	private Configuration cfg;
	private static final Logger logger = LoggerFactory.getLogger(TodoMailHelper.class);

	public TodoMailHelper() {
		this.cfg = new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
		this.cfg.setClassForTemplateLoading(this.getClass(), "/");
	}

	public Map<? extends String, ? extends Object> extractVTodoData(VTodo vtodo) {
		Map<String, Object> data = new HashMap<>();

		data.put("status", vtodo.status);

		if (vtodo.organizer != null) {
			StringBuilder owner = new StringBuilder();
			owner.append(vtodo.organizer.mailto);
			if (null != vtodo.organizer.commonName) {
				owner.append(String.format("<%s>", vtodo.organizer.commonName));
			}
			data.put("owner", owner.toString());
		}

		List<String> attendees = new LinkedList<>();
		for (VTodo.Attendee attendee : vtodo.attendees) {
			if (Strings.isNullOrEmpty(attendee.commonName)) {
				attendees.add(attendee.mailto);
			} else {
				attendees.add(attendee.commonName);
			}
		}
		if (!attendees.isEmpty()) {
			data.put("attendees", attendees);
		}

		data.put("summary", vtodo.summary);

		return data;
	}

	public String buildSubject(String templateName, String locale, MessagesResolver messagesResolver,
			Map<String, Object> data) {
		if (null == locale) {
			locale = "fr";
		}
		StringWriter sw = new StringWriter();
		Template t;
		data.put("msg", new FreeMarkerMsg(messagesResolver));
		try {
			t = getTemplate(templateName, Locale.of(locale));
			t.process(data, sw);
		} catch (TemplateException e1) {
			logger.error(e1.getMessage(), e1);
		} catch (IOException e1) {
			logger.error(e1.getMessage(), e1);
		}

		return sw.toString();
	}

	protected Template getTemplate(String templateName, Locale locale) throws IOException {
		return cfg.getTemplate(templateName, locale);
	}

	public BodyPart createTextPart(String text) {
		BasicBodyFactory bodyFactory = new BasicBodyFactory();
		TextBody body = bodyFactory.textBody(text, StandardCharsets.UTF_8);

		BodyPart bodyPart = new BodyPart();
		bodyPart.setText(body);

		return bodyPart;
	}

	public BodyPart buildBody(String templateName, Locale locale, MessagesResolver messagesResolver,
			Map<String, Object> data) throws IOException, TemplateException {
		if (null == locale) {
			locale = Locale.FRENCH;
		}

		StringWriter sw = new StringWriter();
		Template t = getTemplate(templateName, locale);
		data.put("msg", new FreeMarkerMsg(messagesResolver));
		t.process(data, sw);
		sw.flush();

		return createTextPart(sw.toString());
	}
}
