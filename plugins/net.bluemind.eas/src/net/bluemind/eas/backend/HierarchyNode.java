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

public class HierarchyNode {

	public final CollectionId collectionId;
	public final String containerUid;
	public final String containerType;

	public HierarchyNode(CollectionId collectionId, String containerUid, String containerType) {
		this.collectionId = collectionId;
		this.containerUid = containerUid;
		this.containerType = containerType;
	}

}
