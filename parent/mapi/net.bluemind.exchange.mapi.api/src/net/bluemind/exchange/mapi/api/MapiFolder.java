/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.exchange.mapi.api;

import com.google.common.base.MoreObjects;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class MapiFolder {

	public String replicaGuid;
	public String displayName;
	public String pidTagContainerClass;
	public String containerUid;
	public String parentContainerUid;
	public Long expectedId;

	public String toString() {
		return MoreObjects.toStringHelper(MapiFolder.class)//
				.add("repl", replicaGuid)//
				.add("dn", displayName)//
				.add("cuid", containerUid)//
				.add("parent", parentContainerUid)//
				.toString();
	}

}
