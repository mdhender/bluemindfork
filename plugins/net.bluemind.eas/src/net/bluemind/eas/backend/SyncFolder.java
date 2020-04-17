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

import net.bluemind.eas.dto.sync.CollectionId;
import net.bluemind.eas.dto.type.ItemDataType;

public class SyncFolder {

	private ItemDataType pimDataType;
	private String displayName;
	private CollectionId serverId;
	private CollectionId parentId;

	public SyncFolder() {

	}

	public ItemDataType getPimDataType() {
		return pimDataType;
	}

	public void setPimDataType(ItemDataType pimDataType) {
		this.pimDataType = pimDataType;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public CollectionId getParentId() {
		return parentId;
	}

	public void setParentId(CollectionId parentId) {
		this.parentId = parentId;
	}

	public CollectionId getServerId() {
		return serverId;
	}

	public void setServerId(CollectionId serverId) {
		this.serverId = serverId;
	}

}
