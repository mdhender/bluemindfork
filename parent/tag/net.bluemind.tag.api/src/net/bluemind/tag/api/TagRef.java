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
package net.bluemind.tag.api;

import javax.validation.constraints.NotNull;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.container.model.ItemValue;

/**
 * A {@link TagRef} associates label & color to an {@link ItemValue} in a
 * container
 *
 */
@BMApi(version = "3")
public class TagRef {

	@NotNull
	public String containerUid;
	@NotNull
	public String itemUid;
	public String label;
	public String color;

	public static TagRef create(String containerUid, ItemValue<Tag> tag) {
		return create(containerUid, tag.uid, tag.value);
	}

	public static TagRef create(String containerUid, String uid, Tag tag) {
		return create(containerUid, uid, tag.label, tag.color);
	}

	public static TagRef create(String containerUid, String uid, String label, String color) {
		TagRef ref = new TagRef();
		ref.containerUid = containerUid;
		ref.itemUid = uid;
		ref.label = label;
		ref.color = color;
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
		TagRef other = (TagRef) obj;
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

	public TagRef copy() {
		TagRef copy = new TagRef();
		copy.containerUid = this.containerUid;
		copy.itemUid = this.itemUid;
		copy.label = this.label;
		copy.color = this.color;
		return copy;
	}

}
