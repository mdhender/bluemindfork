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
package net.bluemind.eas.dto.tasks;

import java.util.Date;
import java.util.List;

import net.bluemind.eas.dto.calendar.CalendarResponse.Recurrence;
import net.bluemind.eas.dto.calendar.CalendarResponse.Sensitivity;

public class TasksResponse {
	public static enum Importance {

		Low(0), Normal(1), High(2);

		private final String xmlValue;

		private Importance(int value) {
			xmlValue = Integer.toString(value);
		}

		public String xmlValue() {
			return xmlValue;
		}
	}

	public String subject;
	public Importance importance;
	public Date utcStartDate;
	public Date startDate;
	public Date utcDueDate;
	public Date dueDate;
	public List<String> categories;
	public Recurrence recurrence;
	public Boolean complete;
	public Date dateCompleted;
	public Sensitivity sensitivity;
	public Date reminderTime;
	public Boolean reminderSet;
	public Date ordinalDate;
	public String subOrdinalDate;

}
