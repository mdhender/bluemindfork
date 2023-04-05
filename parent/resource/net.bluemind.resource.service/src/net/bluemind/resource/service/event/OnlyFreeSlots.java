/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2017
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

package net.bluemind.resource.service.event;

import java.time.ZonedDateTime;

import net.bluemind.calendar.api.IFreebusyUids;
import net.bluemind.calendar.api.IVFreebusy;
import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.api.VFreebusy.Slot;
import net.bluemind.calendar.api.VFreebusy.Type;
import net.bluemind.calendar.api.VFreebusyQuery;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.resource.api.ResourceDescriptor;
import net.bluemind.resource.service.event.BookingStrategyFactory.RecurringEventException;
import net.bluemind.resource.service.event.BookingStrategyFactory.TentativeEventException;

/**
 * @author mehdi All slot must be free
 */
public class OnlyFreeSlots implements BookingStrategy {

	@Override
	public boolean isBusy(ItemValue<ResourceDescriptor> resource, VEvent vEvent) {
		String cal = IFreebusyUids.getFreebusyContainerUid(resource.uid);
		IVFreebusy fb = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IVFreebusy.class, cal);
		VFreebusyQuery q = VFreebusyQuery.create(vEvent.dtstart, vEvent.dtend);

		if (vEvent.rrule != null) {
			throw new RecurringEventException();
		}

		ZonedDateTime evtStart = new BmDateTimeWrapper(vEvent.dtstart).toDateTime();
		ZonedDateTime evtEnd = new BmDateTimeWrapper(vEvent.dtend).toDateTime();

		for (Slot slot : fb.get(q).slots) {
			ZonedDateTime slotStart = new BmDateTimeWrapper(slot.dtstart).toDateTime();
			ZonedDateTime slotEnd = new BmDateTimeWrapper(slot.dtend).toDateTime();

			if (slotStart.isBefore(evtEnd) && slotEnd.isAfter(evtStart)) {
				if (slot.type == Type.BUSYTENTATIVE) {
					// skip the event itself
					if (!slotStart.equals(evtStart) || !slotEnd.equals(evtEnd)) {
						throw new TentativeEventException();
					}
				} else if (slot.type != Type.FREE) {
					return true;
				}
			}
		}
		return false;
	}

}