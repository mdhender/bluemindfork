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

import java.util.Collections;
import java.util.Date;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import net.bluemind.core.api.Regex;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.icalendar.api.ICalendarElement.Attendee;
import net.bluemind.icalendar.api.ICalendarElement.CUType;
import net.bluemind.icalendar.api.ICalendarElement.Organizer;
import net.bluemind.icalendar.api.ICalendarElement.Status;
import net.bluemind.todolist.api.VTodo;

public class VTodoSanitizer {

	private static final Logger logger = LoggerFactory.getLogger(VTodoSanitizer.class);
	private final BmContext context;
	private final Container todo;

	public VTodoSanitizer(BmContext ctx, Container todo) {
		this.context = ctx;
		this.todo = todo;
	}

	public void sanitize(VTodo vtodo) {
		sanitize(null, vtodo);
	}

	public void sanitize(ItemValue<VTodo> oldVtodo, VTodo vtodo) {
		if (null == vtodo) {
			return;
		}
		if (Strings.isNullOrEmpty(vtodo.summary)) {
			logger.warn("VToto.summary is empty for .");
			vtodo.summary = "(New Todo)";// FIXME: Set to i18n("New Todo").
		}

		if (Strings.isNullOrEmpty(vtodo.uid)) {
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
		resolveOrganizer(oldVtodo, vtodo);
		resolveAttendees(oldVtodo, vtodo);

		// TODO BJR50 ?

		// TODO public collected

	}

	private void resolveAttendees(ItemValue<VTodo> oldVTodo, VTodo todo) throws ServerFault {
		if (todo.attendees == null || todo.attendees.isEmpty()) {
			if (oldVTodo != null && oldVTodo.value.attendees != null && !oldVTodo.value.attendees.isEmpty()) {
				todo.attendees = oldVTodo.value.attendees;
			}
			return;
		}
		if (todo.attendees == null) {
			todo.attendees = Collections.emptyList();
		}

		for (Attendee attendee : todo.attendees) {

			if (attendee.commonName == null) {
				attendee.commonName = attendee.mailto;
			}

			if (!Strings.isNullOrEmpty(attendee.mailto) && !Regex.EMAIL.validate(attendee.mailto)) {
				attendee.mailto = null;
			}
			DirEntry dir = resolve(attendee.dir, attendee.mailto);
			if (dir != null) {
				attendee.dir = "bm://" + dir.path;
				attendee.commonName = dir.displayName;
				attendee.mailto = dir.email;
				attendee.internal = true;
				if (dir.kind == Kind.RESOURCE) {
					attendee.cutype = CUType.Resource;
				}
			} else {
				attendee.dir = null;
				attendee.internal = false;
			}
		}
	}

	private void resolveOrganizer(ItemValue<VTodo> oldVTodo, VTodo todo) throws ServerFault {
		if (todo.organizer == null) {
			if (oldVTodo != null && oldVTodo.value.organizer != null) {
				todo.organizer = oldVTodo.value.organizer;
			}
			return;
		}

		Organizer organizer = todo.organizer;

		if (!Strings.isNullOrEmpty(organizer.mailto)) {
			if (!Regex.EMAIL.validate(organizer.mailto)) {
				organizer.mailto = null;
			}
		}

		DirEntry dirEntry = resolve(organizer.dir, organizer.mailto);
		if (dirEntry != null) {
			organizer.dir = "bm://" + dirEntry.path;
			organizer.mailto = dirEntry.email;
			organizer.commonName = dirEntry.displayName;
		} else {
			organizer.dir = null;
			if (organizer.commonName == null) {
				organizer.commonName = organizer.mailto;
			}
		}

	}

	private DirEntry resolve(String dir, String mailto) throws ServerFault {
		if (dir != null && dir.startsWith("bm://")) {
			return directory().getEntry(dir.substring("bm://".length()));
		}

		if (mailto != null) {
			return directory().getByEmail(mailto);
		}

		return null;
	}

	private IDirectory _dir;

	private IDirectory directory() throws ServerFault {
		if (_dir == null) {
			_dir = context.provider().instance(IDirectory.class, todo.domainUid);
		}

		return _dir;
	}

}
