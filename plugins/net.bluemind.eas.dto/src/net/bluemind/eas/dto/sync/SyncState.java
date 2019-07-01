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
package net.bluemind.eas.dto.sync;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import net.bluemind.eas.dto.type.ItemDataType;

/**
 * Holds the last sync date for a given sync key & collection
 */
public class SyncState {

	public ZonedDateTime date;
	public ItemDataType type;
	public long version;
	public long subscriptionVersion;

	public SyncState() {
		date = ZonedDateTime.ofInstant(Instant.ofEpochMilli(0), ZoneId.systemDefault());
	}

	@Override
	public String toString() {
		return "type: " + type + "version : " + version + ", date:" + date + ", version2 : " + subscriptionVersion;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((date == null) ? 0 : date.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		result = prime * result + (int) (version ^ (version >>> 32));
		result = prime * result + (int) (subscriptionVersion ^ (subscriptionVersion >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SyncState other = (SyncState) obj;
		if (date == null) {
			if (other.date != null)
				return false;
		} else if (!date.equals(other.date))
			return false;
		if (type != other.type)
			return false;
		if (version != other.version)
			return false;
		if (subscriptionVersion != other.subscriptionVersion)
			return false;
		return true;
	}

}
