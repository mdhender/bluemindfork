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
import net.bluemind.core.api.date.BmDateTime;

@BMApi(version = "3")
public class VEventQuery {

	public String query;
	public int from;
	public int size;
	public boolean escapeQuery;

	// FIXME why not set default value in "default construtor like that
	// public int size = 10000; ?
	public VEventQuery() {
		this.from = 0;
		// Note that from + size can not be more than the
		// index.max_result_window index setting which defaults to 10,000
		this.size = 10000;
		this.escapeQuery = false;
	}

	/**
	 * Lower bound. vevent.dtend filter
	 */
	public BmDateTime dateMin;

	/**
	 * Upper bound. vevent.dtstart filter
	 */
	public BmDateTime dateMax;

	// FIXME default should be false
	public boolean resolveAttendees = false;

	public VEventAttendeeQuery attendee;

	/**
	 * @param query
	 * @return
	 */
	public static VEventQuery create(String query) {
		VEventQuery q = new VEventQuery();
		q.query = query;
		return q;
	}

	/**
	 * @param dateMin
	 * @param dateMax
	 * @return
	 */
	public static VEventQuery create(BmDateTime dateMin, BmDateTime dateMax) {
		VEventQuery q = new VEventQuery();
		q.dateMin = dateMin;
		q.dateMax = dateMax;
		return q;
	}

}
