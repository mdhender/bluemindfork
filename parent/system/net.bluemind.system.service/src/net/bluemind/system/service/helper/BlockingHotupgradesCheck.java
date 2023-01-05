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
package net.bluemind.system.service.helper;

import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.system.api.hot.upgrade.HotUpgradeTask;
import net.bluemind.system.api.hot.upgrade.HotUpgradeTaskFilter;
import net.bluemind.system.api.hot.upgrade.HotUpgradeTaskStatus;
import net.bluemind.system.api.hot.upgrade.IHotUpgrade;

public class BlockingHotupgradesCheck {

	public static long blockingHotupgrades(SecurityContext ctx) {
		var hotupgradeService = ServerSideServiceProvider.getProvider(ctx).instance(IHotUpgrade.class);
		return hotupgradeService.list(HotUpgradeTaskFilter.filter(HotUpgradeTaskStatus.FAILURE)).stream()
				.filter(hot -> hot.mandatory && (retriesExhausted(hot) || manyFails(hot))).count();
	}

	private static boolean manyFails(HotUpgradeTask hot) {
		return hot.failure > 10 && infiniteRetries(hot);
	}

	private static boolean retriesExhausted(HotUpgradeTask hot) {
		return hot.failure >= hot.retryCount && !infiniteRetries(hot);
	}

	private static boolean infiniteRetries(HotUpgradeTask hot) {
		return hot.retryCount == 0;
	}

}
