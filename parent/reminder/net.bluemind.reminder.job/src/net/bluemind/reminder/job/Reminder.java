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
package net.bluemind.reminder.job;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.icalendar.api.ICalendarElement;
import net.bluemind.icalendar.api.ICalendarElement.VAlarm;
import net.bluemind.user.api.User;

public class Reminder<T extends ICalendarElement> {

	public final User user;
	public final ItemValue<T> entity;
	public final VAlarm valarm;

	public Reminder(User user, ItemValue<T> entity, VAlarm valarm) {
		this.user = user;
		this.entity = entity;
		this.valarm = valarm;
	}
}
