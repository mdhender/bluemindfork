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

import net.bluemind.eas.dto.base.ChangeType;
import net.bluemind.eas.dto.foldersync.FolderType;

public class FolderChangeReference {

	public String folderId;
	public String parentId;
	public String displayName;
	public FolderType itemType;
	public ChangeType changeType;

	public FolderChangeReference() {
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((changeType == null) ? 0 : changeType.hashCode());
		result = prime * result + ((displayName == null) ? 0 : displayName.hashCode());
		result = prime * result + ((folderId == null) ? 0 : (folderId.hashCode()));
		result = prime * result + ((itemType == null) ? 0 : itemType.hashCode());
		result = prime * result + ((parentId == null) ? 0 : (parentId.hashCode()));
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
		FolderChangeReference other = (FolderChangeReference) obj;
		if (changeType != other.changeType)
			return false;
		if (displayName == null) {
			if (other.displayName != null)
				return false;
		} else if (!displayName.equals(other.displayName))
			return false;
		if (folderId == null) {
			if (other.folderId != null)
				return false;
		} else if (!folderId.equals(other.folderId))
			return false;
		if (itemType != other.itemType)
			return false;
		if (parentId == null) {
			if (other.parentId != null)
				return false;
		} else if (!parentId.equals(other.parentId))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "FolderChangeReference [folderId=" + folderId + ", parentId=" + parentId + ", displayName=" + displayName
				+ ", itemType=" + itemType + ", changeType=" + changeType + "]";
	}

}
