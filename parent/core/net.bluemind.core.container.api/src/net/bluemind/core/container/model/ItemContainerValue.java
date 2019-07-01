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

@BMApi(version = "3")
public class ItemContainerValue<T> {

	public String containerUid;
	public T value;
	public String uid;
	public long version;
	public String displayName;
	public String externalId;
	public String createdBy;
	public String updatedBy;
	public Date created;
	public Date updated;

	public static <T> ItemContainerValue<T> create(String containerUid, ItemValue<?> item, T value) {
		ItemContainerValue<T> ret = new ItemContainerValue<>();
		ret.created = item.created;
		ret.updated = item.updated;
		ret.createdBy = item.createdBy;
		ret.updatedBy = item.updatedBy;
		ret.uid = item.uid;
		ret.version = item.version;
		ret.externalId = item.externalId;
		ret.displayName = item.displayName;
		ret.value = value;
		ret.containerUid = containerUid;
		return ret;
	}

}
