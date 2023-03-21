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
import net.bluemind.icalendar.api.ICalendarElement.CUType;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Period;
import net.fortuna.ical4j.model.Property;
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

		PropertyList<Property> properties = vfreebusy.getProperties();

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

		PropertyList<Property> properties = vfreebusy.getProperties();

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
			VFreebusy.Type type = getFreebusyType(event, freebusyOwner);
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

	private static Type getFreebusyType(VEvent event, String freebusyOwner) {
		if (event.transparency == VEvent.Transparency.Opaque) {
			return getTypeForOpaque(event, freebusyOwner);
		} else {
			return getTypeForTransparent(event, freebusyOwner);
		}
	}

	private static Type getTypeForTransparent(VEvent event, String freebusyOwner) {
		for (Attendee a : event.attendees) {
			if (a.dir != null && a.dir.endsWith("/" + freebusyOwner)) {
				if (a.cutype == CUType.Resource || a.cutype == CUType.Room) {
					return getTypeForAttendee(event, a);
				}
				break;
			}
		}
		return Type.FREE;
	}

	private static Type getTypeForOpaque(VEvent event, String freebusyOwner) {
		for (Attendee a : event.attendees) {
			if (a.dir != null && a.dir.endsWith("/" + freebusyOwner)) {
				return getTypeForAttendee(event, a);
			}
		}
		return getTypeForStatus(event);
	}

	private static Type getTypeForStatus(VEvent event) {
		Type value = Type.BUSY;
		if (event.status != null) {
			switch (event.status) {
			case Cancelled:
				value = Type.FREE;
				break;
			case NeedsAction:
			case Tentative:
				value = Type.BUSYTENTATIVE;
				break;
			case Confirmed:
			default:
				value = Type.BUSY;
				break;
			}
		}
		return value;
	}

	private static Type getTypeForAttendee(VEvent event, Attendee a) {
		Type type;
		switch (a.partStatus) {
		case Declined:
			type = Type.FREE;
			break;
		case NeedsAction:
		case Tentative:
			type = Type.BUSYTENTATIVE;
			break;
		default:
			type = Type.BUSY;
			break;
		}
		return mergeTypes(getTypeForStatus(event), type);
	}

	private static Type mergeTypes(Type event, Type attendee) {
		if (event == Type.FREE || attendee == Type.FREE) {
			return Type.FREE;
		}
		if (event == Type.BUSYTENTATIVE || attendee == Type.BUSYTENTATIVE) {
			return Type.BUSYTENTATIVE;
		}
		return Type.BUSY;
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
