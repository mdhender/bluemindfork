/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
 *
 * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License)
 * or the CeCILL as published by CeCILL.info (version 2 of the License).
 *
 * There are special exceptions to the terms and conditions of the
 * licenses as they are applied to this program. See LICENSE.txt in
 * the directory of this program distribution.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.systemcheck.checks;

import java.util.Map;

import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.system.api.IInstallation;
import net.bluemind.system.api.InstallationVersion;
import net.bluemind.system.api.UpgradeStatus;

public class UpgraderAvailableCheck extends AbstractCheck {

	@Override
	public CheckResult verify(IServiceProvider provider, SetupCheckResults result, Map<String, String> collected)
			throws Exception {
		CheckResult cr = ok("check.upgraders.ok");
		IInstallation inst = provider.instance(IInstallation.class);
		InstallationVersion version = inst.getVersion();

		boolean needUpgraders = !(version.databaseVersion == null
				|| inst.getVersion().databaseVersion.equals(version.softwareVersion));

		UpgradeStatus status = inst.upgradeStatus();
		switch (status.state) {
		case OK:
			cr.setState(CheckState.OK);
			break;
		case UPGRADERS_NOT_RUNNABLE:
			cr.setTitleKey("check.upgraders.subscription");
			if (needUpgraders) {
				cr.setState(CheckState.ERROR);
			} else {
				cr.setState(CheckState.WARNING);
			}
			break;
		default:
			cr.setTitleKey("check.upgraders.package");
			if (needUpgraders) {
				cr.setState(CheckState.ERROR);
			} else {
				cr.setState(CheckState.WARNING);
			}
			break;
		}

		return cr;
	}

}
