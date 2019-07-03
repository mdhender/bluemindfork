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

import java.util.Collections;
import java.util.List;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class CalendarSettingsData {

	@BMApi(version = "3")
	public static enum Day {
		SU, MO, TU, WE, TH, FR, SA
	}

	public List<Day> workingDays = Collections.emptyList();
	public Integer dayStart;
	public Integer dayEnd;
	public Integer minDuration; // in minutes
	public String timezoneId;

	public boolean present() {
		return !(workingDays.isEmpty() && //
				dayStart == null && //
				dayEnd == null && //
				minDuration == null && //
				timezoneId == null);
	}
}
