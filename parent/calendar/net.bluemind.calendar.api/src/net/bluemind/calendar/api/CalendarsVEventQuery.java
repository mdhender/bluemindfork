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

import java.util.List;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class CalendarsVEventQuery {
	public String owner;
	public List<String> containers;
	public VEventQuery eventQuery;

	public static CalendarsVEventQuery create(VEventQuery eventQuery, List<String> containers) {
		CalendarsVEventQuery ret = new CalendarsVEventQuery();
		ret.containers = containers;
		ret.eventQuery = eventQuery;
		return ret;
	}

	public static CalendarsVEventQuery create(VEventQuery eventQuery, String owner) {
		CalendarsVEventQuery ret = new CalendarsVEventQuery();
		ret.owner = owner;
		ret.eventQuery = eventQuery;
		return ret;
	}

}
