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
package net.bluemind.eas.serdes.tasks;

import net.bluemind.eas.dto.NamespaceMapping;
import net.bluemind.eas.dto.base.Callback;
import net.bluemind.eas.dto.tasks.TasksResponse;
import net.bluemind.eas.serdes.FastDateTimeFormat;
import net.bluemind.eas.serdes.IEasFragmentFormatter;
import net.bluemind.eas.serdes.IResponseBuilder;

public class TasksResponseFormatter implements IEasFragmentFormatter<TasksResponse> {

	@Override
	public void append(IResponseBuilder builder, double protocolVersion, TasksResponse response,
			Callback<IResponseBuilder> completion) {

		if (notEmpty(response.subject)) {
			builder.text(NamespaceMapping.TASKS, "Subject", response.subject);
		}

		if (response.importance != null) {
			builder.text(NamespaceMapping.TASKS, "Importance", response.importance.xmlValue());
		}

		if (response.utcStartDate != null) {
			builder.text(NamespaceMapping.TASKS, "UtcStartDate", FastDateTimeFormat.format(response.utcStartDate));
		}

		if (response.startDate != null) {
			builder.text(NamespaceMapping.TASKS, "StartDate", FastDateTimeFormat.format(response.startDate));
		}

		if (response.utcDueDate != null) {
			builder.text(NamespaceMapping.TASKS, "UtcDueDate", FastDateTimeFormat.format(response.utcDueDate));
		}

		if (response.dueDate != null) {
			builder.text(NamespaceMapping.TASKS, "DueDate", FastDateTimeFormat.format(response.dueDate));
		}

		if (response.categories != null && !response.categories.isEmpty()) {
			builder.container(NamespaceMapping.TASKS, "Categories");
			for (String c : response.categories) {
				builder.text(NamespaceMapping.TASKS, "Category", c);
			}
			builder.endContainer();
		}

		// TODO: reccurence

		if (response.complete != null) {
			builder.text(NamespaceMapping.TASKS, "Complete", response.complete ? "1" : "0");
		}

		if (response.dateCompleted != null) {
			builder.text(NamespaceMapping.TASKS, "DateCompleted", FastDateTimeFormat.format(response.dateCompleted));
		}

		if (response.sensitivity != null) {
			builder.text(NamespaceMapping.TASKS, "Sensitivity", response.sensitivity.xmlValue());
		}

		if (response.reminderTime != null) {
			builder.text(NamespaceMapping.TASKS, "ReminderTime", FastDateTimeFormat.format(response.reminderTime));
		}
		if (response.reminderSet != null) {
			builder.text(NamespaceMapping.TASKS, "ReminderSet", response.reminderSet ? "1" : "0");
		}

		// <xs:element name="OrdinalDate" type="xs:dateTime"/>
		// <xs:element name="SubOrdinalDate" type="xs:string"/>

		completion.onResult(builder);
	}

	private boolean notEmpty(String s) {
		return s != null && !s.trim().isEmpty();
	}

}
