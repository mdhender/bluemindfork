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

import net.bluemind.calendar.api.VEvent;

public class BookingStrategyFactory {
	public static BookingStrategy create(VEvent event) {
		if (event.allDay()) {
			return new OneFreeSlotAndNoBusy();
		} else {
			return new OnlyFreeSlots();
		}
	}
	

	@SuppressWarnings("serial")
	public static class TentativeEventException extends RuntimeException {

	}

	@SuppressWarnings("serial")
	public static class RecurringEventException extends RuntimeException {

	}
}