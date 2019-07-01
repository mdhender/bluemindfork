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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.joda.time.DateTimeZone;
import org.joda.time.LocalTime;
import org.junit.Test;

import net.bluemind.calendar.api.CalendarSettingsData;
import net.bluemind.calendar.api.CalendarSettingsData.Day;
import net.bluemind.core.api.fault.ServerFault;

public class CalendarSettingsValidateAndAdaptTests {

	@Test
	public void testValidate() {
		CalendarSettingsData s = defaultSettings();
		try {
			CalendarSettings.validate(s);
		} catch (ServerFault e) {
			fail(e.getMessage());
		}

		s.dayStart = null;
		try {
			CalendarSettings.validate(s);
			fail();
		} catch (ServerFault e) {
		}

		s = defaultSettings();
		s.dayEnd = null;
		try {
			CalendarSettings.validate(s);
			fail();
		} catch (ServerFault e) {
		}

		s = defaultSettings();
		s.workingDays = null;
		try {
			CalendarSettings.validate(s);
			fail();
		} catch (ServerFault e) {
		}

		s = defaultSettings();
		s.timezoneId = "";
		try {
			CalendarSettings.validate(s);
			fail();
		} catch (ServerFault e) {
		}

		s = defaultSettings();
		s.timezoneId = "FakeId";
		try {
			CalendarSettings.validate(s);
			fail();
		} catch (ServerFault e) {
		}
	}

	@Test
	public void testAdapt() {
		Map<String, String> map = CalendarSettings.adapt(defaultSettings());
		assertEquals("08:00", map.get(CalendarSettings.CFG_DAY_START));
		assertEquals("18:00", map.get(CalendarSettings.CFG_DAY_END));
		assertEquals("MO,FR", map.get(CalendarSettings.CFG_WORKING_DAYS));
		assertEquals("5", map.get(CalendarSettings.CFG_MIN_DURATION));
		assertEquals("UTC", map.get(CalendarSettings.CFG_TIMEZONE));

		CalendarSettingsData s = CalendarSettings.adapt(map);
		assertEquals(new LocalTime(8, 0).getMillisOfDay(), s.dayStart.intValue());
		assertEquals(new LocalTime(18, 0).getMillisOfDay(), s.dayEnd.intValue());
		assertEquals(DateTimeZone.UTC.getID(), s.timezoneId);
		assertEquals(new Integer(5), s.minDuration);
		assertEquals(Arrays.asList(Day.MO, Day.FR), s.workingDays);

		try {
			CalendarSettings.adapt(new HashMap<String, String>());
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	private CalendarSettingsData defaultSettings() {
		CalendarSettingsData s = new CalendarSettingsData();
		s.dayStart = new LocalTime(8, 0).getMillisOfDay();
		s.dayEnd = new LocalTime(18, 0).getMillisOfDay();
		s.timezoneId = DateTimeZone.UTC.getID();
		s.minDuration = 5;
		s.workingDays = Arrays.asList(Day.MO, Day.FR);
		return s;
	}
}
