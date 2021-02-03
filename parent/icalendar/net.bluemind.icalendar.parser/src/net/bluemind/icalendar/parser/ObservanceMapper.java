/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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
package net.bluemind.icalendar.parser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;
import java.util.stream.Collectors;

import net.fortuna.ical4j.model.ComponentList;
import net.fortuna.ical4j.model.component.CalendarComponent;
import net.fortuna.ical4j.model.component.Observance;
import net.fortuna.ical4j.model.component.VTimeZone;

public class ObservanceMapper {

	private final List<VTimeZone> timezones;

	public ObservanceMapper(List<VTimeZone> timezones) {
		this.timezones = timezones;
	}

	public static ObservanceMapper fromCalendarComponents(List<CalendarComponent> components) {
		List<VTimeZone> tz = components.stream().filter(c -> c instanceof VTimeZone).map(c -> (VTimeZone) c)
				.collect(Collectors.toList());
		return new ObservanceMapper(tz);
	}

	public Map<String, String> getTimezoneMapping() {
		Map<String, String> mapping = new HashMap<>();
		for (VTimeZone tz : timezones) {
			Map<String, Integer> accumulation = new HashMap<>();
			String id = tz.getTimeZoneId().getValue();

			ComponentList observances = tz.getObservances();
			for (int i = 0; i < observances.size(); i++) {
				Observance comp = (Observance) observances.get(i);
				long offset = comp.getOffsetFrom().getOffset().getTotalSeconds() * 1000;
				String[] timezones = TimeZone.getAvailableIDs((int) offset);
				addAccumulations(accumulation, timezones);
			}
			if (!accumulation.isEmpty()) {
				mapping.put(id, getBestHit(accumulation));
			}
		}

		return mapping;
	}

	private String getBestHit(Map<String, Integer> accumulation) {
		Entry<String, Integer> bestHit = null;
		for (Entry<String, Integer> entry : accumulation.entrySet()) {
			if (bestHit == null || entry.getValue() > bestHit.getValue()) {
				bestHit = entry;
			}
		}
		return bestHit.getKey();
	}

	private void addAccumulations(Map<String, Integer> accumulation, String[] timezones) {
		for (String tz : timezones) {
			accumulation.computeIfAbsent(tz, (k) -> 0);
			accumulation.computeIfPresent(tz, (k, v) -> v + 1);
		}
	}

}
