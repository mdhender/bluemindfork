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
package net.bluemind.eas.partnership;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.eas.utils.RunnableExtensionLoader;

public class Provider {

	private static final Logger logger = LoggerFactory.getLogger(Provider.class);

	private static final IDevicePartnershipProvider provider = init();

	static void classLoad() {

	}

	private static IDevicePartnershipProvider init() {
		RunnableExtensionLoader<IDevicePartnershipProvider> rel = new RunnableExtensionLoader<>();
		List<IDevicePartnershipProvider> providers = rel.loadExtensions("net.bluemind.eas.partnership", "provider",
				"provider", "impl");
		IDevicePartnershipProvider provImpl = providers.get(0);
		logger.info("Partnershis provider is {}", provImpl);
		return provImpl;
	}

	public static IDevicePartnershipProvider get() {
		return provider;
	}

}
