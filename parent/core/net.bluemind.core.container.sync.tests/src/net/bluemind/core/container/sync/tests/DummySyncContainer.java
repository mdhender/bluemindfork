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
package net.bluemind.core.container.sync.tests;

import java.util.Map;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ContainerSyncResult;
import net.bluemind.core.container.model.ContainerSyncStatus;
import net.bluemind.core.container.sync.ISyncableContainer;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.task.service.IServerTaskMonitor;

public class DummySyncContainer implements ISyncableContainer {

	@SuppressWarnings("unused")
	private BmContext context;
	@SuppressWarnings("unused")
	private Container container;

	public DummySyncContainer(BmContext context, Container container) {
		this.container = container;
		this.context = context;
	}

	@Override
	public ContainerSyncResult sync(Map<String, String> syncTokens, IServerTaskMonitor monitor) throws ServerFault {
		ContainerSyncResult ret = new ContainerSyncResult();
		ret.status = new ContainerSyncStatus();
		ret.status.nextSync = 42L;
		ret.status.syncTokens.put("Tata", "Suzanne");
		ret.added = 3;
		ret.removed = 4;
		ret.updated = 5;
		return ret;
	}

}
