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
package net.bluemind.eas.backend;

import java.util.Optional;

import net.bluemind.eas.dto.base.AppData;
import net.bluemind.eas.dto.base.ChangeType;
import net.bluemind.eas.dto.base.CollectionItem;
import net.bluemind.eas.dto.type.ItemDataType;

public class ItemChangeReference {

	private CollectionItem serverId;
	private ChangeType changeType;
	private final ItemDataType type;

	private Optional<AppData> data;

	public ItemChangeReference(ItemDataType type) {
		changeType = ChangeType.ADD;
		data = Optional.empty();
		this.type = type;
	}

	public CollectionItem getServerId() {
		return serverId;
	}

	public void setServerId(CollectionItem serverId) {
		this.serverId = serverId;
	}

	public Optional<AppData> getData() {
		return data;
	}

	public void setData(AppData appMeta) {
		data = Optional.of(appMeta);
	}

	public final boolean isUpdate() {
		return ChangeType.CHANGE == changeType;
	}

	public final ChangeType getChangeType() {
		return changeType;
	}

	public final void setChangeType(ChangeType changeKind) {
		this.changeType = changeKind;
	}

	public ItemDataType getType() {
		return type;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((serverId == null) ? 0 : serverId.hashCode());
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
		ItemChangeReference other = (ItemChangeReference) obj;
		if (serverId == null) {
			if (other.serverId != null)
				return false;
		} else if (!serverId.equals(other.serverId))
			return false;
		return true;
	}

}
