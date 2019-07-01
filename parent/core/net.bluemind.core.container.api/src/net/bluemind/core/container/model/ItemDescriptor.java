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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;

import net.bluemind.core.api.BMApi;

/**
 * Descriptor of an item
 *
 */
@BMApi(version = "3")
public class ItemDescriptor {
	/**
	 * item uid
	 */
	public String uid;
	/**
	 * item version
	 */
	public long version;
	/**
	 * item displayname
	 */
	public String displayName;
	/**
	 * item external id
	 */
	public String externalId;

	/**
	 * The item internal id, use carefully, prefer {@link ItemDescriptor#uid} if
	 * possible
	 */
	public long internalId;

	/**
	 * creation author
	 */
	public String createdBy;
	/**
	 * modification author
	 */
	public String updatedBy;
	/**
	 * creation date
	 */
	public Date created;
	/**
	 * modification date
	 */
	public Date updated;

	public Collection<ItemFlag> flags = EnumSet.noneOf(ItemFlag.class);

	public ItemDescriptor() {
	}

	public ItemDescriptor(Item item) {
		this.uid = item.uid;
		this.version = item.version;
		this.displayName = item.displayName;
		this.externalId = item.externalId;
		this.internalId = item.id;
		this.createdBy = item.createdBy;
		this.updatedBy = item.updatedBy;
		this.created = item.created;
		this.updated = item.updated;
		this.flags = item.flags;
	}

	public static List<ItemDescriptor> get(List<Item> items) {
		List<ItemDescriptor> itemsDescriptors = new ArrayList<>(items.size());
		for (Item item : items) {
			itemsDescriptors.add(new ItemDescriptor(item));
		}

		return itemsDescriptors;
	}
}
