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
package net.bluemind.calendar.service.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTimeZone;
import org.joda.time.LocalTime;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;

import net.bluemind.calendar.api.CalendarSettingsData;
import net.bluemind.calendar.api.ICalendarSettings;
import net.bluemind.core.api.ParametersValidator;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.service.ContainerSettings;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.rest.BmContext;

public class CalendarSettings implements ICalendarSettings {

	static final String CFG_WORKING_DAYS = "calendar.workingDays";
	static final String CFG_MIN_DURATION = "calendar.minDuration";
	static final String CFG_DAY_START = "calendar.dayStart";
	static final String CFG_DAY_END = "calendar.dayEnd";
	static final String CFG_TIMEZONE = "calendar.timezone";

	private ContainerSettings containerSettings;
	private RBACManager rbac;

	public CalendarSettings(BmContext context, Container calendarContainer) throws ServerFault {
		this.containerSettings = new ContainerSettings(context, calendarContainer);
		rbac = new RBACManager(context).forContainer(calendarContainer);
	}

	@Override
	public void set(CalendarSettingsData config) throws ServerFault {
		rbac.check(Verb.Manage.name());
		validate(config);
		containerSettings.mutate(adapt(config));
	}

	@Override
	public CalendarSettingsData get() throws ServerFault {
		rbac.check(Verb.Read.name(), Verb.Manage.name());
		return adapt(containerSettings.get());
	}

	static CalendarSettingsData adapt(Map<String, String> map) {
		CalendarSettingsData ret = new CalendarSettingsData();

		if (map == null) {
			map = new HashMap<>();
		}
		String wd = map.get(CFG_WORKING_DAYS);
		if (wd != null && wd.length() != 0) {
			List<String> days = Splitter.on(",").splitToList(wd);
			ret.workingDays = new ArrayList<>(days.size());
			for (String d : days) {
				ret.workingDays.add(CalendarSettingsData.Day.valueOf(d));
			}
		}

		String minDuration = map.get(CFG_MIN_DURATION);
		if (minDuration != null) {
			ret.minDuration = Integer.parseInt(minDuration);
		}

		String dayStart = map.get(CFG_DAY_START);
		if (dayStart != null) {
			ret.dayStart = LocalTime.parse(dayStart).getMillisOfDay();
		}

		String dayEnd = map.get(CFG_DAY_END);
		if (dayEnd != null) {
			ret.dayEnd = LocalTime.parse(dayEnd).getMillisOfDay();
		}

		ret.timezoneId = map.get(CFG_TIMEZONE);
		return ret;
	}

	static Map<String, String> adapt(CalendarSettingsData config) {

		String daysAsString = Joiner.on(",").join(config.workingDays);
		String minDuration = config.minDuration != null ? config.minDuration.toString() : null;

		String ds = LocalTime.fromMillisOfDay((long) config.dayStart).toString("HH:mm");
		String de = LocalTime.fromMillisOfDay((long) config.dayEnd).toString("HH:mm");

		String tz = config.timezoneId;

		return ImmutableMap.<String, String>builder() //
				.put(CFG_WORKING_DAYS, daysAsString) //
				.put(CFG_MIN_DURATION, minDuration) //
				.put(CFG_DAY_START, ds) //
				.put(CFG_DAY_END, de) //
				.put(CFG_TIMEZONE, tz).build();
	}

	static void validate(CalendarSettingsData settings) throws ServerFault {

		ParametersValidator.notNull(settings);
		ParametersValidator.notNull(settings.workingDays);
		ParametersValidator.notNull(settings.dayStart);
		ParametersValidator.notNull(settings.dayEnd);
		ParametersValidator.notNullAndNotEmpty(settings.timezoneId);

		try {
			DateTimeZone.forID(settings.timezoneId);
		} catch (IllegalArgumentException e) {
			throw new ServerFault("timezone " + settings.timezoneId + " is not valid");
		}

	}
}
