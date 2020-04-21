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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.date.BmDateTime;
import net.bluemind.directory.api.IDirEntryPath;
import net.bluemind.icalendar.api.ICalendarElement;

@BMApi(version = "3")
public class VEventSeries {

	public VEvent main;
	public List<VEventOccurrence> occurrences = Collections.emptyList();
	public Map<String, String> properties;
	public String icsUid;

	public VEventOccurrence occurrence(BmDateTime recurid) {
		return occurrences.stream().filter(r -> r.recurid.equals(recurid)).findFirst().orElse(null);
	}

	public static VEventSeries create(VEvent master, VEventOccurrence... eventOccurrences) {
		VEventSeries series = new VEventSeries();
		series.main = master;
		if (eventOccurrences != null) {
			series.occurrences = Arrays.asList(eventOccurrences);
		}
		return series;
	}

	public VEventSeries copy() {
		VEventSeries copy = new VEventSeries();
		if (this.main != null) {
			copy.main = this.main.copy();
		}
		List<VEventOccurrence> copyOccurrences = new ArrayList<>();
		this.occurrences.forEach(occ -> {
			copyOccurrences.add(occ.copy());
		});
		copy.occurrences = copyOccurrences;

		if (this.properties != null) {
			copy.properties = new HashMap<>(this.properties);
		}
		copy.icsUid = icsUid;
		return copy;
	}

	public String displayName() {
		if (this.main != null && this.main.summary != null && this.main.summary.length() > 0) {
			return this.main.summary;
		}
		for (VEventOccurrence occ : occurrences) {
			if (occ.summary != null && occ.summary.length() > 0) {
				return occ.summary;
			}
		}
		return "";
	}

	public boolean hasAlarm() {
		if (main != null && main.hasAlarm()) {
			return true;
		}

		return occurrences.stream().filter(o -> o.hasAlarm()).findAny().isPresent();
	}

	public VEvent mainOccurrence() {
		return main != null ? main : occurrences.get(0);
	}

	public List<VEvent> flatten() {
		final List<VEvent> evts = new ArrayList<>();
		if (this.main != null) {
			evts.add(this.main);
		}
		this.occurrences.forEach(occurrence -> {
			evts.add(occurrence);
		});
		return evts;
	}

	@Override
	public String toString() {
		return "VEventSeries{icsUid: " + icsUid + ", main: " + main + ", occs: " + occurrences + "}";
	}

	/**
	 * @param event
	 * @return
	 */
	public boolean meeting() {
		return mainOccurrence().meeting() || occurrences.stream().anyMatch(ICalendarElement::meeting);
	}

	public boolean master(String domainUid, String ownerEntryUid) {

		if (main == null) {
			return false;
		}

		if (!mainOccurrence().meeting()) {
			return true;
		}

		if (main.organizer == null || main.organizer.dir == null) {
			return false;
		}

		String eventOrganizerPath = main.organizer.dir.substring("bm://".length());
		return IDirEntryPath.getDomain(eventOrganizerPath).equals(domainUid)
				&& IDirEntryPath.getEntryUid(eventOrganizerPath).equals(ownerEntryUid);
	}
}
