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

/**
 * Item of a container
 * 
 * (SHOULD NOT BE EXPOSED THRU API)
 *
 */
public class Item {
	/**
	 * internal id
	 */
	public long id;
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

	public static Item create(String uid, String externalId) {
		Item ret = new Item();
		ret.uid = uid;
		ret.externalId = externalId;
		return ret;
	}

	public static Item create(String uid, long id, ItemFlag first, ItemFlag... flags) {
		Item ret = create(uid, id);
		ret.flags = EnumSet.of(first, flags);
		return ret;
	}

	public static Item create(String uid, long id) {
		Item ret = new Item();
		ret.uid = uid;
		ret.id = id;
		return ret;
	}

}
