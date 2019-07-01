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
package net.bluemind.calendar.pdf.internal;

import java.util.ArrayList;
import java.util.HashMap;

public class ListEvents {
	private HashMap<Float, ArrayList<PrintedEvent>> list;

	public ListEvents() {
		setList(new HashMap<Float, ArrayList<PrintedEvent>>());
	}

	public HashMap<Float, ArrayList<PrintedEvent>> getList() {
		return list;
	}

	public void setList(HashMap<Float, ArrayList<PrintedEvent>> list) {
		this.list = list;
	}

	public void addEvent(float cell, PrintedEvent pe) {
		if (!list.containsKey(cell)) {
			list.put(cell, new ArrayList<PrintedEvent>());
		}
		list.get(cell).add(pe);
	}
}
