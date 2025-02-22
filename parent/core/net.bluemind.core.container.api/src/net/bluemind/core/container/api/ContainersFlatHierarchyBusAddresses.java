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

public class ContainersFlatHierarchyBusAddresses {

	private ContainersFlatHierarchyBusAddresses() {

	}

	private static final String BASE_ADDRESS = "bm." + IFlatHierarchyUids.TYPE + ".hook";

	public static final String ALL_HIERARCHY_CHANGES = BASE_ADDRESS + ".changed";

	public static final String ALL_HIERARCHY_CHANGES_OPS = BASE_ADDRESS + ".changed.ops";

	public static final String containersHierarchyChanges(String ownerUid, String domainUid) {
		return BASE_ADDRESS + "." + IFlatHierarchyUids.getIdentifier(ownerUid, domainUid) + ".changed";
	}
}
