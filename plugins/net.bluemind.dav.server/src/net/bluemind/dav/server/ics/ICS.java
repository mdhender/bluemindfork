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
package net.bluemind.dav.server.ics;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.lib.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.UnfoldingReader;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.TimeZone;
import net.fortuna.ical4j.model.TimeZoneRegistry;
import net.fortuna.ical4j.model.TimeZoneRegistryFactory;
import net.fortuna.ical4j.model.component.VTimeZone;

public class ICS {

	private static final Logger logger = LoggerFactory.getLogger(ICS.class);

	private static final ConcurrentHashMap<String, VTimeZone> osxVtz = new ConcurrentHashMap<>();

	private static final TimeZoneRegistry tzRegistry = TimeZoneRegistryFactory.getInstance().createRegistry();

	private static final TimeZone getTimeZoneIcal(String id) {
		return tzRegistry.getTimeZone(id);
	}

	public static final TimeZone getTimeZone(String id) {
		return getTimeZoneIcal(id);
	}

	public static final VTimeZone getVTimeZone(String id) {
		VTimeZone cached = osxVtz.get(id);
		if (cached != null) {
			logger.info("Sending cached timezone def {}", id);
			return cached;
		}
		String file = "osx_timezones/" + id.replace('/', '_') + ".ics";
		InputStream in = ICS.class.getClassLoader().getResourceAsStream(file);
		if (in == null) {
			logger.warn("No custom tz def in {}", file);
			return getTimeZone(id).getVTimeZone();
		} else {
			CalendarBuilder builder = new CalendarBuilder();

			try (Reader icsReader = new InputStreamReader(in);
					UnfoldingReader ur = new UnfoldingReader(icsReader, true)) {
				logger.info("Parsing custom tz infos {}, id {}", file, id);
				BiConsumer<Calendar, Component> consumer = (calendar, component) -> {
					osxVtz.put(id, (VTimeZone) component);
				};
				builder.build(ur, consumer);

				return osxVtz.get(id);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
				return getTimeZone(id).getVTimeZone();
			}

		}
	}
}
