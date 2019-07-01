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

/**
 * This object is used by services to return subscriptions to a client.
 *
 */
@BMApi(version = "3")
public class ContainerSubscriptionDescriptor extends ContainerSubscriptionModel {

	public String ownerDisplayName;
	public String ownerDirEntryPath;

	public static ContainerSubscriptionDescriptor copyOf(ContainerSubscriptionModel model) {
		ContainerSubscriptionDescriptor cs = new ContainerSubscriptionDescriptor();
		cs.containerUid = model.containerUid;
		cs.containerType = model.containerType;
		cs.owner = model.owner;
		cs.offlineSync = model.offlineSync;
		cs.defaultContainer = model.defaultContainer;
		cs.name = model.name;
		return cs;
	}

	public ContainerSubscriptionDescriptor withName(String n) {
		name = n;
		return this;
	}

	public static ContainerSubscriptionDescriptor create(BaseContainerDescriptor cd, boolean offlineSync) {
		ContainerSubscriptionDescriptor cs = new ContainerSubscriptionDescriptor();
		cs.name = cd.name;
		cs.containerUid = cd.uid;
		cs.containerType = cd.type;
		cs.offlineSync = offlineSync;
		cs.owner = cd.owner;
		cs.defaultContainer = cd.defaultContainer;
		cs.ownerDisplayName = cd.ownerDisplayname;
		cs.ownerDirEntryPath = cd.ownerDirEntryPath;
		return cs;
	}
}
