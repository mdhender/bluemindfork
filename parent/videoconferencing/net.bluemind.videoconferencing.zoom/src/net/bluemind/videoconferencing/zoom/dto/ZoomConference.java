/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.videoconferencing.zoom.dto;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import net.bluemind.icalendar.api.ICalendarElement.RRule;
import net.bluemind.icalendar.api.ICalendarElement.RRule.Frequency;
import net.bluemind.icalendar.api.ICalendarElement.RRule.WeekDay;

public class ZoomConference {

	public final String scheduleFor = "me";
	public final String title;
	public final String start;
	public final String timezone;
	public final Optional<RRule> reccurrence;
	public final List<ZoomInivitee> invitees;
	private final int duration;

	public ZoomConference(String title, String start, String timezone, int duration, Optional<RRule> reccurrence,
			List<ZoomInivitee> invitees) {
		this.title = title;
		this.start = start;
		this.duration = duration;
		this.timezone = timezone;
		this.reccurrence = reccurrence;
		this.invitees = invitees;
	}

	public String toJson() {
		JsonObject json = new JsonObject();
		json.put("agenda", title);
		json.put("start_time", start);
		json.put("duration", duration);
		json.put("timezone", timezone);
		reccurrence.ifPresent(rec -> {
			json.put("type", 8); // recurrent meeting with fixed time
			json.put("recurrence", toJson(rec));
		});
		if (!invitees.isEmpty()) {
			JsonArray inviteesArray = new JsonArray();
			for (ZoomInivitee invitee : invitees) {
				inviteesArray.add(invitee.toJson());
			}
			JsonObject settings = new JsonObject();
			settings.put("invitees", inviteesArray);
			json.put("settings", settings);
		}
		return json.encode();
	}

	private JsonObject toJson(RRule rec) {
		JsonObject recurrence = new JsonObject();

		if (rec.until != null) {
			recurrence.put("end_date_time", rec.until.iso8601);
		} else if (rec.count != null && rec.count > 0) {
			recurrence.put("end_times", rec.count);
		}

		if (rec.interval != null && rec.interval > 0) {
			recurrence.put("repeat_interval", rec.interval);
		}

		if (rec.frequency == Frequency.DAILY) {
			recurrence.put("type", 1);
		} else if (rec.frequency == Frequency.WEEKLY) {
			recurrence.put("type", 2);
			if (rec.byDay != null && !rec.byDay.isEmpty()) {
				List<String> values = rec.byDay.stream().map(day -> {
					if (day.equals(WeekDay.SU)) {
						return "1";
					} else if (day.equals(WeekDay.MO)) {
						return "2";
					} else if (day.equals(WeekDay.TU)) {
						return "3";
					} else if (day.equals(WeekDay.WE)) {
						return "4";
					} else if (day.equals(WeekDay.TH)) {
						return "5";
					} else if (day.equals(WeekDay.FR)) {
						return "6";
					} else
						return "7";
				}).collect(Collectors.toList());
				recurrence.put("weekly_days", String.join(",", values));
			}
		} else if (rec.frequency == Frequency.MONTHLY) {
			recurrence.put("type", 3);
			if (rec.byMonthDay != null && !rec.byMonthDay.isEmpty()) {
				recurrence.put("monthly_day", rec.byMonthDay.get(0));
			}
		}

		return recurrence;
	}

}
