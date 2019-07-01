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
package net.bluemind.calendar.helper.ical4j;

import java.util.List;

import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.api.VFreebusy;
import net.bluemind.calendar.api.VFreebusy.Slot;
import net.bluemind.calendar.api.VFreebusy.Type;
import net.bluemind.core.api.date.BmDateTime;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.icalendar.api.ICalendarElement.Attendee;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Period;
import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.model.component.VFreeBusy;
import net.fortuna.ical4j.model.parameter.FbType;
import net.fortuna.ical4j.model.property.DtEnd;
import net.fortuna.ical4j.model.property.DtStart;
import net.fortuna.ical4j.model.property.FreeBusy;

public class VFreebusyServiceHelper {

	/**
	 * @param events
	 * @return
	 */
	public static String asIcs(VFreebusy fb) {
		VFreeBusy vfreebusy = new VFreeBusy();

		PropertyList properties = vfreebusy.getProperties();

		net.fortuna.ical4j.model.DateTime ical4jDtStart = convertToUtcIcsDate(fb.dtstart);
		properties.add(new DtStart(ical4jDtStart));

		net.fortuna.ical4j.model.DateTime ical4jDtEnd = convertToUtcIcsDate(fb.dtend);
		properties.add(new DtEnd(ical4jDtEnd));

		for (Slot slot : fb.slots) {
			if (slot.type != VFreebusy.Type.FREE) {
				vfreebusy.getProperties().add(convertToIcal4jVFreebusy(slot));
			}
		}

		Calendar cal = new Calendar();
		cal.getComponents().add(vfreebusy);

		return cal.toString();
	}

	/**
	 * @param values
	 * @return
	 */
	public static String convertToFreebusyString(VFreebusy fb) {

		VFreeBusy vfreebusy = new VFreeBusy();

		PropertyList properties = vfreebusy.getProperties();

		net.fortuna.ical4j.model.DateTime ical4jDtStart = convertToUtcIcsDate(fb.dtstart);
		properties.add(new DtStart(ical4jDtStart));

		net.fortuna.ical4j.model.DateTime ical4jDtEnd = convertToUtcIcsDate(fb.dtend);
		properties.add(new DtEnd(ical4jDtEnd));

		for (Slot slot : fb.slots) {
			if (slot.type != VFreebusy.Type.FREE) {
				vfreebusy.getProperties().add(convertToIcal4jVFreebusy(slot));
			}
		}

		return vfreebusy.toString();
	}

	/**
	 * @param events
	 * @return
	 */
	public static VFreebusy convertToFreebusy(String domainUid, String freebusyOwner, BmDateTime dtstart,
			BmDateTime dtend, List<ItemValue<VEvent>> events, List<ItemValue<VEvent>> oof) {
		VFreebusy freebusy = new VFreebusy();
		freebusy.dtstart = dtstart;
		freebusy.dtend = dtend;

		for (ItemValue<VEvent> item : events) {
			VEvent event = item.value;
			VFreebusy.Type type = (event.transparency == VEvent.Transparency.Opaque) ? VFreebusy.Type.BUSY
					: VFreebusy.Type.FREE;

			for (Attendee a : event.attendees) {
				if (a.dir != null && a.dir.endsWith("/" + freebusyOwner)) {
					switch (a.partStatus) {
					case Accepted:
						type = Type.BUSY;
						break;
					case Declined:
						type = Type.FREE;
						break;
					case NeedsAction:
						type = Type.BUSYTENTATIVE;
						break;
					case Tentative:
						type = Type.BUSYTENTATIVE;
						break;
					default:
						type = Type.BUSY;
						break;
					}
				}
			}

			Slot s = Slot.create(event.dtstart, event.dtend, event.summary, type);
			freebusy.slots.add(s);
		}

		for (ItemValue<VEvent> item : oof) {
			VEvent event = item.value;
			VFreebusy.Type type = VFreebusy.Type.BUSYUNAVAILABLE;
			Slot s = Slot.create(event.dtstart, event.dtend, null, type);
			freebusy.slots.add(s);
		}

		return freebusy;
	}

	private static FreeBusy convertToIcal4jVFreebusy(Slot slot) {

		net.fortuna.ical4j.model.DateTime ical4jDtStart = convertToUtcIcsDate(slot.dtstart);

		net.fortuna.ical4j.model.DateTime ical4jDtEnd = convertToUtcIcsDate(slot.dtend);

		FreeBusy fb = new FreeBusy();
		FbType fbt = null;
		switch (slot.type) {
		case BUSYTENTATIVE:
			fbt = FbType.BUSY_TENTATIVE;
			break;
		case BUSYUNAVAILABLE:
			fbt = FbType.BUSY_UNAVAILABLE;
			break;
		case FREE:
			fbt = FbType.FREE;
			break;
		default:
		case BUSY:
			fbt = FbType.BUSY;
			break;

		}
		fb.getParameters().add(fbt);
		fb.getPeriods().add(new Period(ical4jDtStart, ical4jDtEnd));
		return fb;
	}

	private static net.fortuna.ical4j.model.DateTime convertToUtcIcsDate(BmDateTime date) {
		net.fortuna.ical4j.model.DateTime dt = new net.fortuna.ical4j.model.DateTime(
				new BmDateTimeWrapper(date).toUTCTimestamp());
		dt.setUtc(true);
		return dt;
	}

}
