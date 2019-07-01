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
import java.util.List;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.date.BmDateTime;

@BMApi(version = "3")
public class VFreebusy {

	public BmDateTime dtstart;
	public BmDateTime dtend;
	public List<Slot> slots;

	public VFreebusy() {
		slots = new ArrayList<Slot>();
	}

	/**
	 * A free/busy slot
	 *
	 */
	@BMApi(version = "3")
	public static class Slot {
		public BmDateTime dtstart;
		public BmDateTime dtend;
		public Type type;
		public String summary;

		public static Slot create(BmDateTime dtstart, BmDateTime dtend, String summary, Type type) {
			Slot slot = new Slot();
			slot.dtstart = dtstart;
			slot.dtend = dtend;
			slot.summary = summary;
			slot.type = type;
			return slot;
		}

	}

	/**
	 *
	 *
	 */
	@BMApi(version = "3")
	public enum Type {
		FREE, BUSY, BUSYUNAVAILABLE, BUSYTENTATIVE;
	}
}
