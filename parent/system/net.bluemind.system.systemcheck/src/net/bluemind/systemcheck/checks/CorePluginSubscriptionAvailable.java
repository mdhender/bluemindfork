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
package net.bluemind.systemcheck.checks;

import java.util.Map;

import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.system.api.IInstallation;
import net.bluemind.system.api.SubscriptionInformations;

public class CorePluginSubscriptionAvailable extends AbstractCheck {

	@Override
	public CheckResult verify(IServiceProvider provider, SetupCheckResults results, Map<String, String> collected)
			throws Exception {
		CheckResult cr = ok("check.subscription.package.ok");

		IInstallation inst = provider.instance(IInstallation.class);

		SubscriptionInformations subInfo = inst.getSubscriptionInformations();

		if (!subInfo.validProvider) {
			cr = error("check.subscription.package");
		}

		return cr;
	}

}
