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

import java.util.Set;

import javax.sql.DataSource;

import net.bluemind.core.api.BMVersion;
import net.bluemind.core.api.VersionInfo;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.system.schemaupgrader.IVersionedUpdater;
import net.bluemind.system.schemaupgrader.UpdateAction;
import net.bluemind.system.schemaupgrader.UpdateResult;

public class UpgradeExistingTags implements IVersionedUpdater {

	private VersionInfo versionInfo;

	public UpgradeExistingTags() {
		this.versionInfo = VersionInfo.create(BMVersion.getVersion());
	}

	@Override
	public UpdateResult executeUpdate(net.bluemind.core.task.service.IServerTaskMonitor monitor, DataSource pool,
			Set<UpdateAction> handledActions) {
		ServerSideServiceProvider prov = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		IInCoreTickConfiguration tickConfApi = prov.instance(IInCoreTickConfiguration.class);
		tickConfApi.reconfigure(monitor, prov.getContext());
		return UpdateResult.ok();
	}

	@Override
	public int major() {
		// run at every upgrade
		return Integer.parseInt(versionInfo.major) + 1;
	}

	@Override
	public int buildNumber() {
		// run at every upgrade
		return Integer.parseInt(versionInfo.release);
	}

	@Override
	public boolean afterSchemaUpgrade() {
		return true;
	}

}
