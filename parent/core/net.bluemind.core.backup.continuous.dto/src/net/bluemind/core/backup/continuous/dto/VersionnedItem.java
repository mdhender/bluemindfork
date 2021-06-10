/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.core.backup.continuous.dto;

import java.util.EnumSet;

import net.bluemind.core.api.BMVersion;
import net.bluemind.core.container.model.ItemFlag;
import net.bluemind.core.container.model.ItemValue;

public class VersionnedItem<T> extends ItemValue<T> {

	private static final String BM_VERSION = BMVersion.getVersionName();

	public String producedBy;
	public String valueClass;

	public VersionnedItem() {

	}

	public VersionnedItem(ItemValue<T> item) {
		created = item.created;
		updated = item.updated;
		createdBy = item.createdBy;
		updatedBy = item.updatedBy;
		uid = item.uid;
		version = item.version;
		externalId = item.externalId;
		displayName = item.displayName;
		this.value = item.value;
		internalId = item.internalId;
		flags = item.flags.isEmpty() ? EnumSet.noneOf(ItemFlag.class) : EnumSet.copyOf(item.flags);
		producedBy = BM_VERSION;
		valueClass = item.value == null ? null : value.getClass().getCanonicalName();
	}

}
