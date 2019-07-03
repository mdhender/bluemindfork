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
package net.bluemind.core.container.api;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.container.model.BaseContainerDescriptor;

@BMApi(version = "3")
public class ContainerHierarchyNode {

	public String containerUid;
	public String containerType;
	public String name;
	public boolean deleted;

	public static String uidFor(String containerUid, String containerType, String domain) {
		return containerType + ":" + containerUid + "@" + domain;
	}

	public static ContainerHierarchyNode of(BaseContainerDescriptor cd) {
		ContainerHierarchyNode chn = new ContainerHierarchyNode();
		chn.containerType = cd.type;
		chn.containerUid = cd.uid;
		chn.name = cd.name;
		chn.deleted = cd.deleted;
		return chn;
	}

	@Override
	public String toString() {
		return "ContainerHierarchyNode{uid: " + containerUid + ", t: " + containerType + "}";
	}

}
