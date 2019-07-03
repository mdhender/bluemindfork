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
package net.bluemind.system.subscriptionprovider;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.eclipse.common.RunnableExtensionLoader;

public class SubscriptionProviders {

	static final Logger logger = LoggerFactory.getLogger(SubscriptionProviders.class);
	private static ISubscriptionProvider provider;

	static {
		RunnableExtensionLoader<ISubscriptionProvider> loader = new RunnableExtensionLoader<ISubscriptionProvider>();
		List<ISubscriptionProvider> providers = loader.loadExtensions("net.bluemind.system", "subscriptionprovider",
				"sub-provider", "class");
		if (providers.size() == 0) {
			logger.warn("no subscription provider found");
			provider = new EmptySubscriptionProvider();
		} else {
			if (providers.size() > 1) {

				logger.warn("too many subscription providers found ({})", providers.size());
			}
			provider = providers.get(0);
		}
	}

	static public ISubscriptionProvider getSubscriptionProvider() {
		return provider;
	}
}
