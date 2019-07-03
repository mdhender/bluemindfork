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

/**
 * Util class to build an uri for an item : containerUid/itemUid
 *
 */
@BMApi(version = "3")
public class ItemUri {

	public String containerUid;
	public String itemUid;

	@Override
	public String toString() {
		return uri(containerUid, itemUid);
	}

	public static String uri(String containerUid, String itemUid) {
		return containerUid + ":" + itemUid;
	}

	public static ItemUri parse(String uri) {
		int index = uri.indexOf(':');
		if (index < 0) {
			return null;
		} else {
			return ItemUri.create(uri.substring(0, index), uri.substring(index + 1));
		}
	}

	public static ItemUri create(String containerUid, String itemUid) {
		ItemUri itemUri = new ItemUri();
		itemUri.containerUid = containerUid;
		itemUri.itemUid = itemUid;
		return itemUri;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((containerUid == null) ? 0 : containerUid.hashCode());
		result = prime * result + ((itemUid == null) ? 0 : itemUid.hashCode());
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
		ItemUri other = (ItemUri) obj;
		if (containerUid == null) {
			if (other.containerUid != null)
				return false;
		} else if (!containerUid.equals(other.containerUid))
			return false;
		if (itemUid == null) {
			if (other.itemUid != null)
				return false;
		} else if (!itemUid.equals(other.itemUid))
			return false;
		return true;
	}

}
