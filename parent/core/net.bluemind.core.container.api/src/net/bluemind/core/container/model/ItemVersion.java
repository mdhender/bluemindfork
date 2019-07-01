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
public class ItemVersion implements Comparable<ItemVersion> {

	public long id;
	public long version;

	public ItemVersion() {
		id = 0L;
		version = 0L;
	}

	public ItemVersion(long id, long v) {
		this.id = id;
		this.version = v;
	}

	public ItemVersion(ChangeLogEntry cl) {
		this(cl.internalId, cl.version);
	}

	public ItemVersion(ItemDescriptor cl) {
		this(cl.internalId, cl.version);
	}

	@Override
	public int compareTo(ItemVersion o) {
		if (o.id == id) {
			return Long.compare(version, o.version);
		} else {
			return Long.compare(id, o.id);
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (id ^ (id >>> 32));
		return result;
	}

	public String toString() {
		return "[ItemVersion " + id + "v" + version + "]";
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ItemVersion other = (ItemVersion) obj;
		if (id != other.id)
			return false;
		return true;
	}
}
