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
package net.bluemind.calendar.api;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.icalendar.api.ICalendarElement.VAlarm;

@BMApi(version = "3")
public class Reminder {

	public ItemValue<VEvent> vevent;
	public VAlarm valarm;

	public static Reminder create(ItemValue<VEvent> vevent, VAlarm valarm) {
		Reminder r = new Reminder();
		r.vevent = vevent;
		r.valarm = valarm;
		return r;
	}
}
