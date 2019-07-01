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

import java.util.Date;

import net.bluemind.core.api.BMApi;

/**
 * Changelog entry
 *
 */
@BMApi(version = "3")
public class ChangeLogEntry {

	@BMApi(version = "3")
	public static enum Type {
		Created, Updated, Deleted
	}

	/**
	 * change version
	 */
	public long version;
	/**
	 * changed item uid
	 */
	public String itemUid;

	/**
	 * changed item external id
	 */
	public String itemExtId;

	/**
	 * changes author
	 */
	public String author;
	/**
	 * changes type (created/updated/deleted)
	 */
	public Type type;

	/**
	 * changes date
	 */
	public Date date;

	/**
	 * origin of the change, as given in the security context
	 */
	public String origin;

	public long internalId;

	public boolean match(@SuppressWarnings("unused") ItemFlagFilter filter) {
		return true;
	}

	public static ChangeLogEntry create(long id, long v, String uid, Type t) {
		ChangeLogEntry cle = new ChangeLogEntry();
		cle.internalId = id;
		cle.version = v;
		cle.itemUid = uid;
		cle.type = t;
		return cle;
	}

	public long weightSeed;

	@Override
	public String toString() {
		return "Entry [version=" + version + ", itemUid=" + itemUid + ", itemExtId=" + itemExtId + ", author=" + author
				+ ", type=" + type + ", date=" + date + ", origin=" + origin + "]";
	}

}
