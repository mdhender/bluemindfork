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
package net.bluemind.tag.persistence;

public class ItemTagRef {

	public String containerUid;
	public String itemUid;

	public static ItemTagRef create(String tagContainerUid, String tagItemUid) {
		ItemTagRef ref = new ItemTagRef();
		ref.containerUid = tagContainerUid;
		ref.itemUid = tagItemUid;
		return ref;
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
		ItemTagRef other = (ItemTagRef) obj;
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
