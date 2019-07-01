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
import net.bluemind.core.container.model.ContainerChangeset;

/**
 * This object is used for storing a per-user sharded {@link ContainerChangeset}
 * on subscriptions.
 *
 */
@BMApi(version = "3")
public class ContainerSubscriptionModel extends ContainerSubscription {

	public String containerType;
	public String owner;
	public boolean defaultContainer;
	public String name;

	public static ContainerSubscriptionModel create(String uid, String type, String owner, boolean defaultContainer,
			boolean offlineSync, String name) {
		ContainerSubscriptionModel cs = new ContainerSubscriptionModel();
		cs.containerUid = uid;
		cs.containerType = type;
		cs.owner = owner;
		cs.offlineSync = offlineSync;
		cs.defaultContainer = defaultContainer;
		cs.name = name;
		return cs;
	}

	public static ContainerSubscriptionModel create(BaseContainerDescriptor cd, boolean offlineSync) {
		ContainerSubscriptionModel cs = new ContainerSubscriptionModel();
		cs.containerUid = cd.uid;
		cs.containerType = cd.type;
		cs.offlineSync = offlineSync;
		cs.owner = cd.owner;
		cs.defaultContainer = cd.defaultContainer;
		cs.name = cd.name;
		return cs;
	}
}
