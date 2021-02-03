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
package net.bluemind.calendar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.api.VEventChanges;
import net.bluemind.calendar.api.VEventChanges.ItemAdd;
import net.bluemind.calendar.api.VEventChanges.ItemDelete;
import net.bluemind.calendar.api.VEventChanges.ItemModify;
import net.bluemind.calendar.api.VEventCounter;
import net.bluemind.calendar.api.VEventOccurrence;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.core.api.date.BmDateTime;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.utils.UIDGenerator;
import net.bluemind.icalendar.api.ICalendarElement.Attendee;

public class EventChangesMerge {

	public static IVEventSeriesMerge getStrategy(List<ItemValue<VEventSeries>> bmSeries, VEventSeries imipSeries) {
		if (!bmSeries.isEmpty() && bmSeries.get(0).value.main != null) {
			return new UpdateSeries();
		} else if (imipSeries.main != null) {
			return new CreateSeries();
		} else {
			return new StoreOrphans();
		}
	}

	public static interface IVEventSeriesMerge {
		public VEventChanges merge(List<ItemValue<VEventSeries>> bmSeries, VEventSeries imipSeries);
	}

	private static class UpdateSeries implements IVEventSeriesMerge {

		@Override
		public VEventChanges merge(List<ItemValue<VEventSeries>> bmSeries, VEventSeries imipSeries) {

			ItemValue<VEventSeries> vevent = bmSeries.get(0);
			vevent.value = updateMain(vevent.value, imipSeries.main);
			vevent.value.acceptCounters = imipSeries.acceptCounters;
			for (VEventOccurrence imipEvent : imipSeries.occurrences) {
				List<VEventOccurrence> occ = vevent.value.occurrences.stream()
						.filter(r -> !r.recurid.equals(imipEvent.recurid)).collect(Collectors.toList());
				occ.add(imipEvent);
				List<VEventCounter> counters = vevent.value.counters.stream()
						.filter(r -> r.counter.recurid == null || !r.counter.recurid.equals(imipEvent.recurid))
						.collect(Collectors.toList());
				vevent.value.counters = counters;
				vevent.value.occurrences = occ;
			}
			return VEventChanges.create(null, Arrays.asList(ItemModify.create(vevent.uid, vevent.value, false)), null);
		}

		private VEventSeries updateMain(VEventSeries series, VEvent main) throws ServerFault {
			if (main == null) {
				return series;
			}
			series.counters = new ArrayList<>();
			if (eventDatesChanged(series.main, main)) {
				main.exdate = null;
				series.occurrences = Collections.emptyList();
			} else {
				adjustEventExceptionsValues(series, main);
			}

			adjustAlarms(series.main, main);
			series.main = main;
			return series;
		}

		private void adjustAlarms(VEvent bmEvent, VEvent evt) {
			if (bmEvent.hasAlarm()) {
				evt.alarm = bmEvent.alarm;
			}
		}

		private void adjustEventExceptionsValues(final VEventSeries oldEvent, final VEvent imipVEvent)
				throws ServerFault {

			VEventChanges changes = new VEventChanges();
			changes.modify = new ArrayList<>();

			VEvent existingEvent = oldEvent.main;

			oldEvent.occurrences.forEach(evt -> {
				evt.location = adjustEventValue(existingEvent.location, imipVEvent.location, evt.location);
				evt.summary = adjustEventValue(existingEvent.summary, imipVEvent.summary, evt.summary);
				evt.classification = adjustEventValue(existingEvent.classification, imipVEvent.classification,
						evt.classification);
				evt.organizer = adjustEventValue(existingEvent.organizer, imipVEvent.organizer, evt.organizer);
				evt.description = adjustEventValue(existingEvent.description, imipVEvent.description, evt.description);
				evt.categories = adjustEventValue(existingEvent.categories, imipVEvent.categories, evt.categories);
				adjustAttendees(existingEvent.attendees, imipVEvent.attendees, evt.attendees);
				adjustAlarms(evt, imipVEvent);

			});
		}

		private <T extends Object> T adjustEventValue(T oldValue, T newValue, T exceptionValue) {

			if (oldValue == null && newValue == null) {
				return exceptionValue;
			}

			if (oldValue != null) {
				if (oldValue.equals(newValue)) {
					// value not modified
					return exceptionValue;
				}
			}

			if ((oldValue == null && exceptionValue != null) || (oldValue != null && exceptionValue == null)) {
				return exceptionValue;
			}

			if (oldValue != null) {
				if (!oldValue.equals(exceptionValue)) {
					// value has already been modified in exception, don't
					// overwrite
					return exceptionValue;
				}
			}

			// updating value
			return newValue;

		}

		private void adjustAttendees(List<Attendee> oldValue, List<Attendee> newValue, List<Attendee> exceptionValue) {

			for (Attendee attendee : newValue) {
				if (!oldValue.contains(attendee) && !exceptionValue.contains(attendee)) {
					exceptionValue.add(attendee);
				}
			}

			for (Attendee attendee : oldValue) {
				if (!newValue.contains(attendee)) {
					exceptionValue.remove(attendee);
				}
			}

		}

		private boolean eventDatesChanged(VEvent value, VEvent imipVEvent) {
			if ((null == value.dtstart && imipVEvent.dtstart != null)
					|| (null != value.dtstart && imipVEvent.dtstart == null)) {
				return true;
			}

			if (!dateEquals(value.dtstart, imipVEvent.dtstart)) {
				return true;
			}

			if ((null == value.dtend && imipVEvent.dtend != null)
					|| (null != value.dtend && imipVEvent.dtend == null)) {
				return true;
			}

			if (!dateEquals(value.dtend, imipVEvent.dtend)) {
				return true;
			}

			return false;
		}

		private boolean dateEquals(BmDateTime a, BmDateTime b) {
			return new BmDateTimeWrapper(a).toUTCTimestamp() == new BmDateTimeWrapper(b).toUTCTimestamp();
		}

	}

	private static class CreateSeries implements IVEventSeriesMerge {

		@Override
		public VEventChanges merge(List<ItemValue<VEventSeries>> bmSeries, VEventSeries imipSeries) {

			VEventChanges changes = VEventChanges.create(new ArrayList<ItemAdd>(), null, new ArrayList<ItemDelete>());

			for (ItemValue<VEventSeries> toDelete : bmSeries) {
				changes.delete.add(ItemDelete.create(toDelete.uid, false));
			}
			changes.add.add(ItemAdd.create(imipSeries.icsUid, imipSeries, false));
			return changes;
		}

	}

	private static class StoreOrphans implements IVEventSeriesMerge {

		@Override
		public VEventChanges merge(List<ItemValue<VEventSeries>> bmSeries, VEventSeries imipSeries) {

			VEventChanges changes = VEventChanges.create(new ArrayList<ItemAdd>(), new ArrayList<ItemModify>(), null);
			for (VEventOccurrence occ : imipSeries.occurrences) {
				boolean found = false;
				for (ItemValue<VEventSeries> oldOcc : bmSeries) {
					if (oldOcc.value.occurrence(occ.recurid) != null) {
						oldOcc.value.occurrences = Arrays.asList(occ);
						changes.modify.add(ItemModify.create(oldOcc.uid, oldOcc.value, false));
						found = true;
						break;
					}
				}

				if (!found) {
					VEventSeries oneOcc = new VEventSeries();
					oneOcc.icsUid = imipSeries.icsUid;
					oneOcc.occurrences = Arrays.asList(occ);
					changes.add.add(ItemAdd.create(UIDGenerator.uid(), oneOcc, false));
				}
			}
			return changes;
		}

	}

}
