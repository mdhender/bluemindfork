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
package net.bluemind.todolist.job;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import freemarker.template.Configuration;
import freemarker.template.Template;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.icalendar.api.ICalendarElement.VAlarm;
import net.bluemind.reminder.mail.ReminderMailHelper;
import net.bluemind.todolist.api.VTodo;

public class TodoListMailHelper extends ReminderMailHelper<VTodo> {

	private Configuration cfg;

	public TodoListMailHelper() {
		cfg = new Configuration();
		cfg.setClassForTemplateLoading(this.getClass(), "/");
	}

	/**
	 * Extract {@link VTodo} data
	 * 
	 * @param todo
	 *            the {@link VTodo} to extract
	 * @return a {@link Map} containing the {@link VTodo} data
	 */
	public Map<String, Object> extractVTodoDataToMap(VTodo todo, VAlarm valarm) {
		Map<String, Object> data = new HashMap<String, Object>();

		if (todo.due != null) {
			data.put("datedue", new BmDateTimeWrapper(todo.due).toDate());
		} else {
			data.put("datedue", new BmDateTimeWrapper(todo.due).toDate());
		}

		super.addICalendarelementDataToMap(todo, valarm, data);

		return data;
	}

	@Override
	protected Template getTemplate(String templateName, Locale locale) throws IOException {
		return cfg.getTemplate(templateName, locale);
	}
}
