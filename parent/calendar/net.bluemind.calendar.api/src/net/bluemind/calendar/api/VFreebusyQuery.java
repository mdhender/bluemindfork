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

import java.util.ArrayList;
import java.util.List;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.date.BmDateTime;

@BMApi(version = "3")
public class VFreebusyQuery {
	public BmDateTime dtstart;
	public BmDateTime dtend;
	public List<String> excludedEvents;
	public boolean withOOFSlots = true;

	public static VFreebusyQuery create(BmDateTime dtstart, BmDateTime dtend) {
		VFreebusyQuery q = new VFreebusyQuery();
		q.dtstart = dtstart;
		q.dtend = dtend;
		q.excludedEvents = new ArrayList<>();
		q.withOOFSlots = true;
		return q;
	}
}
