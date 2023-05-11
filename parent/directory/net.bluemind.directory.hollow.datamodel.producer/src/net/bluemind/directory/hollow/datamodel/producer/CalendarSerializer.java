/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.directory.hollow.datamodel.producer;

import net.bluemind.calendar.api.CalendarDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.hollow.datamodel.producer.Value.StringValue;

public class CalendarSerializer extends DirEntrySerializer {

	private final CalendarDescriptor calendar;

	protected CalendarSerializer(CalendarDescriptor calendar, ItemValue<DirEntry> dirEntry, String domainUid) {
		super(dirEntry, domainUid);
		this.calendar = calendar;
	}

	@Override
	public Value get(Property property) {
		switch (property) {
		case DisplayName:
			return new StringValue(calendar.name);
		default:
			return super.get(property);
		}
	}
}
