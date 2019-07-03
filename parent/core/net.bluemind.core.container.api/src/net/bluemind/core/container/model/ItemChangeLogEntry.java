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
package net.bluemind.core.container.model;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class ItemChangeLogEntry extends ChangeLogEntry {

	public ItemChangeLogEntry() {

	}

	public ItemChangeLogEntry(ChangeLogEntry entry) {
		this.author = entry.author;
		this.date = entry.date;
		this.itemExtId = entry.itemExtId;
		this.itemUid = entry.itemUid;
		this.origin = entry.origin;
		this.type = entry.type;
		this.version = entry.version;
		this.internalId = entry.internalId;
		this.weightSeed = entry.weightSeed;
	}

	/**
	 * changes author display name
	 */
	public String authorDisplayName;
}
