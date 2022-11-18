/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.systemcheck;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.system.api.InstallationVersion;
import net.bluemind.systemcheck.checks.AbstractCheck;
import net.bluemind.systemcheck.checks.CheckResult;
import net.bluemind.systemcheck.checks.CoreCountCheck;
import net.bluemind.systemcheck.checks.CorePluginSubscriptionAvailable;
import net.bluemind.systemcheck.checks.DomainConflictCheck;
import net.bluemind.systemcheck.checks.ElasticSearchNodeCheck;
import net.bluemind.systemcheck.checks.EnUSLocaleCheck;
import net.bluemind.systemcheck.checks.MemoryCheck;
import net.bluemind.systemcheck.checks.NetworkCheck;
import net.bluemind.systemcheck.checks.PgCheck;
import net.bluemind.systemcheck.checks.ServerConformityCheck;
import net.bluemind.systemcheck.checks.SetupCheckResults;
import net.bluemind.systemcheck.checks.SubscriptionContactCheck;
import net.bluemind.systemcheck.checks.UpgraderAvailableCheck;
import net.bluemind.systemcheck.collect.DataCollector;

public class UpgradeCheck {

	public static boolean setupOk(IServiceProvider provider, IServerTaskMonitor monitor, InstallationVersion version) {

		try {
			SetupCheckResults checks = new SetupCheckResults();
			Map<String, String> customerData = DataCollector.collectForUpgrade(provider);

			List<AbstractCheck> checkers = Arrays.asList(new MemoryCheck(), new CoreCountCheck(), new EnUSLocaleCheck(),
					new NetworkCheck(), new PgCheck(), new ElasticSearchNodeCheck(), new ServerConformityCheck(),
					new UpgraderAvailableCheck(), new CorePluginSubscriptionAvailable(), new SubscriptionContactCheck(),
					new DomainConflictCheck());

			List<AbstractCheck> filteredCheckers = checkers.stream().filter(c -> c.canCheckWithVersion(version))
					.collect(Collectors.toList());

			for (AbstractCheck check : filteredCheckers) {
				try {
					CheckResult cr = check.verify(provider, checks, customerData);
					monitor.log("Status of check " + cr.getTitleKey() + ": " + cr.getState().name() + " - "
							+ cr.getReasonKey());
					checks.add(cr);
				} catch (Exception e) {
					monitor.log("Check " + check.getClass().getName() + " failed : " + e.getMessage());
					return false;
				}
			}

			return checks.isOk();
		} catch (Exception e) {
			monitor.log("error during check" + e.getMessage());
			return false;
		}

	}

}
