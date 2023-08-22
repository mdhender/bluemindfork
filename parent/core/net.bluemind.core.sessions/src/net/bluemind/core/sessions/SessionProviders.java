/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2019
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
package net.bluemind.core.sessions;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.eclipse.common.RunnableExtensionLoader;

public class SessionProviders {

	private static final List<ISessionsProvider> providers = load();
	private static final Logger logger = LoggerFactory.getLogger(SessionProviders.class);

	private SessionProviders() {

	}

	private static List<ISessionsProvider> load() {
		RunnableExtensionLoader<ISessionsProvider> rel = new RunnableExtensionLoader<>();
		logger.info("SCL - SessionProviders");
		return rel.loadExtensionsWithPriority("net.bluemind.core.sessions", "provider", "provider", "impl");
	}

	public static List<ISessionsProvider> get() {
		return providers;
	}
}
