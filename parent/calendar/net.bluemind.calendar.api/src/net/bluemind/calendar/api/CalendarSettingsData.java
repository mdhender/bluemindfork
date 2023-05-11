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

	public static boolean validMinDuration(Integer minDuration) {
		return minDuration == 60 || minDuration == 120 || minDuration == 720 || minDuration == 1440;
	}

	public static Integer toMillisOfDay(String value) {
		double time = Double.parseDouble(value);
		int timeHour = (int) Double.parseDouble(value);
		int timeMinute = (int) ((time - timeHour) * 60);
		int minutes = timeHour * 60 + timeMinute;
		return minutes * 60 * 1000;
	}

	public static List<Day> getWorkingDays(String string) {
		List<Day> days = new ArrayList<>();
		for (String dayString : string.split(",")) {
			switch (dayString.trim().toLowerCase()) {
			case "mon":
				days.add(Day.MO);
				break;
			case "tue":
				days.add(Day.TU);
				break;
			case "wed":
				days.add(Day.WE);
				break;
			case "thu":
				days.add(Day.TH);
				break;
			case "fri":
				days.add(Day.FR);
				break;
			case "sat":
				days.add(Day.SA);
				break;
			case "sun":
				days.add(Day.SU);
				break;
			}
		}
		return days;
	}

}
