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

package net.bluemind.systemcheck.checks;

import java.util.Map;

import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.system.api.IInstallation;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.system.api.SystemConf;

public class SubscriptionContactCheck extends AbstractCheck {

	private static final String subContactCheckKey = "check.subscriptionContact";

	@Override
	public CheckResult verify(IServiceProvider provider, SetupCheckResults results, Map<String, String> collected)
			throws Exception {
		String dbVersion = provider.instance(IInstallation.class).getVersion().databaseVersion;
		SystemConf conf = provider.instance(ISystemConfiguration.class).getValues();

		if (conf.stringValue("db") == null) {
			// dont check subscription contact if db is not up (for a fresh install for
			// example)
			return cr(CheckState.OK, subContactCheckKey,
					"(Subscription contact are not checked if database is not up, for example during a new installation)");
		} else if (dbVersion.matches("3.1.*")) {
			// dont check subscription contact if migrating from 3.5 to 4.0
			return cr(CheckState.OK, subContactCheckKey,
					"(Subscription contact are not checked for migration from 3.5 to 4.0)");
		} else if (collected.containsKey(subContactCheckKey)) {
			String contacts = collected.get(subContactCheckKey);
			return cr(CheckState.OK, subContactCheckKey, "(" + contacts + ")");
		}
		return cr(CheckState.ERROR, subContactCheckKey,
				"You must define at least one contact to receive subscription notifications.");
	}
}
