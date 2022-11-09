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

import jakarta.validation.constraints.NotNull;
import net.bluemind.core.api.BMApi;

/**
 * This object is used when saving a subscription
 *
 */
@BMApi(version = "3")
public class ContainerSubscription {

	@NotNull
	public String containerUid;
	public boolean offlineSync;

	public static ContainerSubscription create(String containerUid, boolean offlineSync) {
		ContainerSubscription cs = new ContainerSubscription();
		cs.containerUid = containerUid;
		cs.offlineSync = offlineSync;
		return cs;
	}

	public String toString() {
		return "[sub to " + containerUid + ", offline: " + offlineSync + "]";
	}

}
