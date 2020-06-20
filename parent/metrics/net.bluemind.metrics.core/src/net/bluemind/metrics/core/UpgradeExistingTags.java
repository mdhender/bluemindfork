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
package net.bluemind.metrics.core;

import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.system.schemaupgrader.AtEveryUpgrade;
import net.bluemind.system.schemaupgrader.UpdateResult;

public class UpgradeExistingTags implements AtEveryUpgrade {
	public UpgradeExistingTags() {
	}

	@Override
	public UpdateResult executeUpdate(IServerTaskMonitor monitor) {
		ServerSideServiceProvider prov = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		IInCoreTickConfiguration tickConfApi = prov.instance(IInCoreTickConfiguration.class);
		tickConfApi.reconfigure(monitor, prov.getContext());
		return UpdateResult.ok();
	}
}
