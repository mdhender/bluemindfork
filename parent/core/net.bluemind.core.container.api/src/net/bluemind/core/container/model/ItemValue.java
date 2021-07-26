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

import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;

import net.bluemind.core.api.BMApi;

/**
 * Generic container item value {@link Item}
 * 
 */
@BMApi(version = "3")
public class ItemValue<T> {

	public T value;
	public String uid;
	public long internalId;
	public long version;
	public String displayName;
	public String externalId;
	public String createdBy;
	public String updatedBy;
	public Date created;
	public Date updated;
	public Collection<ItemFlag> flags = EnumSet.noneOf(ItemFlag.class);

	public static <T> ItemValue<T> create(ItemValue<?> item, T value) {
		ItemValue<T> ret = new ItemValue<>();
		ret.created = item.created;
		ret.updated = item.updated;
		ret.createdBy = item.createdBy;
		ret.updatedBy = item.updatedBy;
		ret.uid = item.uid;
		ret.version = item.version;
		ret.externalId = item.externalId;
		ret.displayName = item.displayName;
		ret.value = value;
		ret.internalId = item.internalId;
		ret.flags = item.flags.isEmpty() ? EnumSet.noneOf(ItemFlag.class) : EnumSet.copyOf(item.flags);
		return ret;

	}

	public static <T> ItemValue<T> create(Item item, T value) {
		ItemValue<T> ret = new ItemValue<>();
		ret.created = item.created;
		ret.updated = item.updated;
		ret.createdBy = item.createdBy;
		ret.updatedBy = item.updatedBy;
		ret.uid = item.uid;
		ret.version = item.version;
		ret.externalId = item.externalId;
		ret.displayName = item.displayName;
		ret.value = value;
		ret.internalId = item.id;
		ret.flags = item.flags;
		return ret;
	}

	public static <T> ItemValue<T> create(String uid, T value) {
		ItemValue<T> ret = new ItemValue<>();
		ret.uid = uid;
		ret.value = value;
		return ret;
	}

	public Item item() {
		Item item = new Item();
		item.created = created;
		item.updated = updated;
		item.createdBy = createdBy;
		item.updatedBy = updatedBy;
		item.uid = uid;
		item.version = version;
		item.externalId = externalId;
		item.displayName = displayName;
		item.id = internalId;
		item.flags = flags;
		return item;
	}

	@Override
	public String toString() {
		return "ItemValue{uid: " + uid + ", id: " + internalId + ", extId: " + externalId + ", dn: " + displayName
				+ ", flags: " + (flags != null ? flags : "[]") + ", value: " + value + "}";
	}

}
