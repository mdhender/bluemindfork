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
package net.bluemind.eas.backend.bm.task;

import java.util.ArrayList;
import java.util.Calendar;

import net.bluemind.core.api.date.BmDateTime.Precision;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.eas.backend.IApplicationData;
import net.bluemind.eas.backend.MSTask;
import net.bluemind.eas.dto.calendar.CalendarResponse.Sensitivity;
import net.bluemind.icalendar.api.ICalendarElement;
import net.bluemind.icalendar.api.ICalendarElement.Status;
import net.bluemind.tag.api.TagRef;
import net.bluemind.todolist.api.VTodo;

public class TaskConverter {

	public MSTask convert(VTodo vtodo, String timezone) {
		MSTask ret = new MSTask();

		ret.subject = vtodo.summary;
		ret.importance = vtodo.priority;

		if (vtodo.dtstart != null) {
			Calendar dtstartUTC = Calendar.getInstance();
			dtstartUTC.setTimeInMillis(new BmDateTimeWrapper(vtodo.dtstart).toTimestamp(timezone));
			ret.utcStartDate = dtstartUTC.getTime();

			Calendar dtstart = Calendar.getInstance();
			dtstart.setTimeInMillis(new BmDateTimeWrapper(vtodo.dtstart).toTimestamp("UTC"));
			ret.startDate = dtstart.getTime();
		}

		if (vtodo.due != null) {
			Calendar dueDateUTC = Calendar.getInstance();
			dueDateUTC.setTimeInMillis(new BmDateTimeWrapper(vtodo.due).toTimestamp(timezone));
			ret.utcDueDate = dueDateUTC.getTime();

			Calendar dueDate = Calendar.getInstance();
			dueDate.setTimeInMillis(new BmDateTimeWrapper(vtodo.due).toTimestamp("UTC"));
			ret.dueDate = dueDate.getTime();
		}

		if (vtodo.categories != null) {
			ret.categories = new ArrayList<String>(vtodo.categories.size());
			for (TagRef category : vtodo.categories) {
				ret.categories.add(category.label);
			}
		}

		ret.complete = vtodo.status == Status.Completed;

		if (vtodo.completed != null) {
			Calendar dateComplete = Calendar.getInstance();
			dateComplete.setTimeInMillis(new BmDateTimeWrapper(vtodo.completed).toTimestamp(timezone));
			ret.dateCompleted = dateComplete.getTime();
		}

		ret.sensitivity = getSensitivity(vtodo);

		ret.reminderSet = false;

		if (vtodo.due != null && vtodo.alarm != null && !vtodo.alarm.isEmpty()) {
			Calendar reminderTime = Calendar.getInstance();
			reminderTime.setTimeInMillis(
					new BmDateTimeWrapper(vtodo.due).toTimestamp(timezone) - (vtodo.alarm.get(0).trigger * 1000));
			ret.reminderTime = reminderTime.getTime();
			ret.reminderSet = true;
		}

		ret.description = vtodo.description;

		return ret;
	}

	private Sensitivity getSensitivity(VTodo vtodo) {
		if (vtodo.classification == null) {
			return Sensitivity.Normal;
		}
		switch (vtodo.classification) {
		case Confidential:
			return Sensitivity.Confidential;
		case Private:
			return Sensitivity.Private;
		default:
			return Sensitivity.Normal;
		}
	}

	public VTodo convert(IApplicationData data) {
		MSTask task = (MSTask) data;

		VTodo ret = new VTodo();

		if (task.startDate != null) {
			ret.dtstart = BmDateTimeWrapper.fromTimestamp(task.startDate.getTime(), null, Precision.Date);
		}

		if (task.dueDate != null) {
			ret.due = BmDateTimeWrapper.fromTimestamp(task.dueDate.getTime(), null, Precision.Date);
		}
		ret.summary = task.subject;
		ret.classification = getClassification(task.sensitivity);
		ret.description = task.description;
		ret.status = task.complete ? Status.Completed : Status.NeedsAction;
		ret.priority = task.importance;

		if (task.reminderSet && task.utcDueDate != null) {
			ret.alarm = new ArrayList<ICalendarElement.VAlarm>(1);
			ret.alarm.add(ICalendarElement.VAlarm
					.create((int) ((task.utcDueDate.getTime() - task.reminderTime.getTime()) / 1000)));
		}

		ret.percent = task.complete ? 100 : 0;
		if (task.dateCompleted != null) {
			ret.completed = BmDateTimeWrapper.fromTimestamp(task.dateCompleted.getTime());
			ret.completed.precision = Precision.Date;
		}
		ret.description = task.description;

		return ret;
	}

	private VTodo.Classification getClassification(Sensitivity sensitivity) {
		if (sensitivity == null) {
			return VTodo.Classification.Public;
		}

		switch (sensitivity) {
		case Confidential:
			return VTodo.Classification.Confidential;
		case Personal:
		case Private:
			return VTodo.Classification.Private;
		case Normal:
		default:
			return VTodo.Classification.Public;
		}

	}

}
