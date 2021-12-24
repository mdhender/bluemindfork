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
package net.bluemind.ui.adminconsole.directory.calendar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.json.client.JSONObject;

import net.bluemind.calendar.api.CalendarSettingsData;
import net.bluemind.calendar.api.CalendarSettingsData.Day;
import net.bluemind.calendar.api.gwt.endpoint.CalendarSettingsGwtEndpoint;
import net.bluemind.calendar.api.gwt.js.JsCalendarSettingsData;
import net.bluemind.calendar.api.gwt.serder.CalendarSettingsDataGwtSerDer;
import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.gwtconsoleapp.base.editor.ModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtModelHandler;
import net.bluemind.ui.common.client.forms.Ajax;

public class CalendarSettingsModelHandler implements IGwtModelHandler {

	public static final String TYPE = "bm.ac.CalendarSettingsModelHandler";

	public static void registerType() {
		GwtModelHandler.register(TYPE, new IGwtDelegateFactory<IGwtModelHandler, ModelHandler>() {

			@Override
			public IGwtModelHandler create(ModelHandler modelHandler) {
				return new CalendarSettingsModelHandler();
			}
		});
	}

	@Override
	public void load(JavaScriptObject model, final AsyncHandler<Void> handler) {
		final JsMapStringJsObject map = model.cast();
		String s = getId(map);
		CalendarSettingsGwtEndpoint cse = new CalendarSettingsGwtEndpoint(Ajax.TOKEN.getSessionId(), s);
		cse.get(new AsyncHandler<CalendarSettingsData>() {

			@Override
			public void success(CalendarSettingsData value) {
				map.put("calendar-settings",
						new CalendarSettingsDataGwtSerDer().serialize(value).isObject().getJavaScriptObject());
				handler.success(null);
			}

			@Override
			public void failure(Throwable e) {
				handler.failure(e);
			}
		});
	}

	@Override
	public void save(JavaScriptObject model, final AsyncHandler<Void> handler) {
		final JsMapStringJsObject map = model.cast();
		String s = getId(map);

		CalendarSettingsGwtEndpoint cse = new CalendarSettingsGwtEndpoint(Ajax.TOKEN.getSessionId(), s);
		JsCalendarSettingsData settings = map.get("calendar-settings").cast();
		cse.set(new CalendarSettingsDataGwtSerDer().deserialize(new JSONObject(settings)), handler);
	}

	private String getId(final JsMapStringJsObject map) {
		String s = map.getString("calendarId");
		if (s == null) {
			s = "calendar:" + map.getString("resourceId");
		}
		return s;
	}

	public static CalendarSettingsData createCalendarSettings(Map<String, String> domainSettings) {
		CalendarSettingsData calSettings = new CalendarSettingsData();
		if (domainSettings.containsKey("working_days")) {
			calSettings.workingDays = getWorkingDays(domainSettings.get("working_days"));
		} else {
			calSettings.workingDays = Arrays.asList(new Day[] { Day.MO, Day.TU, Day.WE, Day.TH, Day.FR });
		}
		if (domainSettings.containsKey("timezone")) {
			calSettings.timezoneId = domainSettings.get("timezone");
		} else {
			calSettings.timezoneId = "UTC";
		}
		if (domainSettings.containsKey("work_hours_start")) {
			calSettings.dayStart = toMillisOfDay(domainSettings.get("work_hours_start"));
		} else {
			calSettings.dayStart = 9 * 60 * 60 * 1000;
		}
		if (domainSettings.containsKey("work_hours_end")) {
			calSettings.dayEnd = toMillisOfDay(domainSettings.get("work_hours_end"));
		} else {
			calSettings.dayEnd = 18 * 60 * 60 * 1000;
		}
		if (domainSettings.containsKey("min_duration")) {
			calSettings.minDuration = Math.max(60, Integer.parseInt(domainSettings.get("min_duration")));
		} else {
			calSettings.minDuration = 60;
		}
		if (!validMinDuration(calSettings.minDuration)) {
			calSettings.minDuration = 60;
		}
		return calSettings;
	}

	private static boolean validMinDuration(Integer minDuration) {
		return minDuration == 60 || minDuration == 120 || minDuration == 720 || minDuration == 1440;
	}

	private static Integer toMillisOfDay(String value) {
		double time = Double.parseDouble(value);
		int timeHour = (int) Double.parseDouble(value);
		int timeMinute = (int) ((time - timeHour) * 60);
		int minutes = timeHour * 60 + timeMinute;
		return minutes * 60 * 1000;
	}

	private static List<Day> getWorkingDays(String string) {
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
