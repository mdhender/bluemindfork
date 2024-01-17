/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2018
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
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

public class MailFolder {

	public final CollectionId collectionId;
	public final String uid;
	public final String name;
	public final String fullName;
	public final String parentUid;

	public MailFolder(CollectionId collectionId, String uid, String name, String fullName, String parentUid) {
		this.collectionId = collectionId;
		this.uid = uid;
		this.name = name;
		this.fullName = fullName;
		this.parentUid = parentUid;
	}

	@Override
	public String toString() {
		return "MailFolder [collectionId=" + collectionId + ", uid=" + uid + ", name=" + name + ", fullName=" + fullName
				+ ", parentUid=" + parentUid + "]";
	}

}
