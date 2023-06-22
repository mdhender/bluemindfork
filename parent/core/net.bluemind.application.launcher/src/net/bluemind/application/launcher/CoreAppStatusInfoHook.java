/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.application.launcher;

import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.system.api.IInstallation;
import net.bluemind.system.application.registration.hook.IAppStatusInfoHook;
import net.bluemind.system.application.registration.model.ApplicationInfoModel;

public class CoreAppStatusInfoHook implements IAppStatusInfoHook {

	@Override
	public String getVersion() {
		return ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IInstallation.class)
				.getVersion().softwareVersion;
	}

	@Override
	public String getState() {
		return ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IInstallation.class)
				.getSystemState().name();
	}

	@Override
	public void updateStateAndVersion(ApplicationInfoModel info) {
		info.state.state = getState();
		info.state.version = getVersion();
	}

}
