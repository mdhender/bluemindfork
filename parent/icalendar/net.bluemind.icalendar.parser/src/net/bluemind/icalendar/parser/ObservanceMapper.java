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

import java.util.ArrayList;
import java.util.Arrays;
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
				mapping.put(id, getBestHit(accumulation, id));
			}
		}

		return mapping;
	}

	private String getBestHit(Map<String, Integer> accumulation, String id) {
		List<String> bestHits = new ArrayList<>();
		int currentBestHit = 0;
		for (Entry<String, Integer> entry : accumulation.entrySet()) {
			if (entry.getValue() >= currentBestHit) {
				if (entry.getValue() == currentBestHit) {
					bestHits.add(entry.getKey());
				} else {
					bestHits = new ArrayList<>();
					bestHits.add(entry.getKey());
					currentBestHit = entry.getValue();
				}
			}
		}
		if (bestHits.size() == 1 || isGenericDstId(id)) {
			return bestHits.get(0);
		} else {
			return detectBestHitById(bestHits, id);
		}

	}

	private boolean isGenericDstId(String id) {
		String idToLower = id.toLowerCase();
		return checkIdOccurrence(idToLower, "gmt") || checkIdOccurrence(idToLower, "utc");
	}

	private boolean checkIdOccurrence(String id, String genericId) {
		return id.contains(genericId) && id.indexOf(genericId) != id.lastIndexOf(genericId);
	}

	private String detectBestHitById(List<String> bestHits, String id) {
		String currentBestHit = null;
		int currentMatches = 0;

		String[] idCleaned = cleanId(id);
		for (String candidate : bestHits) {
			int matches = calculateMatches(cleanId(candidate), idCleaned);
			if (matches >= currentMatches) {
				currentBestHit = candidate;
				currentMatches = matches;
			}
		}
		if (currentMatches == 0) {
			return bestHits.get(0);
		}
		return currentBestHit;
	}

	private int calculateMatches(String[] candidate, String[] id) {
		int matches = 0;
		for (String c : candidate) {
			for (String i : id) {
				if (c.equals(i)) {
					matches++;
				}
			}
		}
		return matches;
	}

	private String[] cleanId(String id) {
		String cleaned = id.toLowerCase().replaceAll("[^a-z0-9\\\\s]", " ");
		return Arrays.asList(cleaned.split("\\s+")).stream().map(s -> s.trim()).filter(s -> !s.isEmpty())
				.toArray(String[]::new);
	}

	private void addAccumulations(Map<String, Integer> accumulation, String[] timezones) {
		for (String tz : timezones) {
			accumulation.computeIfAbsent(tz, (k) -> 0);
			accumulation.computeIfPresent(tz, (k, v) -> v + 1);
		}
	}

}
