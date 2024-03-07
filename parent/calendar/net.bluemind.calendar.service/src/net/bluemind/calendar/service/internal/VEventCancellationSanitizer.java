package net.bluemind.calendar.service.internal;

import java.util.ArrayList;
import java.util.List;

import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.api.VEventOccurrence;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.icalendar.api.ICalendarElement.Status;

public class VEventCancellationSanitizer {

	public static void sanitize(VEventSeries oldEvent, VEventSeries toUpdateEvent) {
		List<VEventOccurrence> sanitizedOccurences = new ArrayList<>();
		for (VEvent updatedVEvent : toUpdateEvent.flatten()) {
			if (updatedVEvent instanceof VEventOccurrence occurence) {
				VEventOccurrence oldOccurrence = oldEvent.occurrence(occurence.recurid);
				if (oldOccurrence != null && oldOccurrence.status == Status.Cancelled) {
					sanitizedOccurences.add(oldOccurrence);
				} else {
					sanitizedOccurences.add(occurence);
				}
			} else {
				if (oldEvent.main.status == Status.Cancelled) {
					toUpdateEvent.main = oldEvent.main;
				}
			}
		}
		toUpdateEvent.occurrences = sanitizedOccurences;
	}
}
