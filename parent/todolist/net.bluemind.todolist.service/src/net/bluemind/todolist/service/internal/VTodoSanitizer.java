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
package net.bluemind.todolist.service.internal;

import java.util.Date;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.icalendar.api.ICalendarElement.Status;
import net.bluemind.todolist.api.VTodo;

public class VTodoSanitizer {

	private static final Logger logger = LoggerFactory.getLogger(VTodoSanitizer.class);

	public void sanitize(VTodo vtodo) {
		if (null == vtodo) {
			return;
		}
		if (StringUtils.isEmpty(vtodo.summary)) {
			logger.warn("VToto.summary is empty for .");
			vtodo.summary = "(New Todo)";// FIXME: Set to i18n("New Todo").
		}

		if (StringUtils.isEmpty(vtodo.uid)) {
			String uid = UUID.randomUUID().toString();
			logger.warn("VEvent.uid is null. set to {}", uid);
			vtodo.uid = uid;
		}

		// 3.8.1.9. Priority
		// This priority is specified as an integer in the range 0
		// to 9.
		if (vtodo.priority != null) {
			if (vtodo.priority < 0) {
				vtodo.priority = 0;
			} else if (vtodo.priority > 9) {
				vtodo.priority = 9;
			}
		}

		// ActiveSync: MS-ASTASK: The DateCompleted element MUST be included in
		// the response if the Complete element (section 2.2.2.7) value is 1.
		if (vtodo.status == Status.Completed && null == vtodo.completed) {
			String timezone = "UTC";
			if (null != vtodo.dtstart && null != vtodo.dtstart.timezone) {
				timezone = vtodo.dtstart.timezone;
			} else {
				if (null != vtodo.due && null != vtodo.due.timezone) {
					timezone = vtodo.due.timezone;
				}
			}
			vtodo.completed = BmDateTimeWrapper.fromTimestamp(new Date().getTime(), timezone);
		}

		if (vtodo.status != Status.Completed) {
			vtodo.completed = null;
		}

		if (vtodo.percent == null || vtodo.percent < 0) {
			vtodo.percent = 0;
		} else if (vtodo.percent > 100) {
			vtodo.percent = 100;
		}
		// Resolve Organizer and Attendees

		// TODO BJR50 ?

		// TODO public collected

	}

}
