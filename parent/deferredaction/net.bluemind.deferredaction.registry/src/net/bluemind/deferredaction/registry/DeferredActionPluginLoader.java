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
package net.bluemind.deferredaction.registry;

import java.util.List;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.eclipse.common.RunnableExtensionLoader;

public class DeferredActionPluginLoader implements BundleActivator {

	static List<IDeferredActionExecutorFactory> executors;

	private static final Logger logger = LoggerFactory.getLogger(DeferredActionPluginLoader.class);

	@Override
	public void start(BundleContext context) throws Exception {
		DeferredActionPluginLoader.executors = loadExecutors();
		logger.info("Loaded {} deferred action executors", DeferredActionPluginLoader.executors.size());
	}

	@Override
	public void stop(BundleContext context) throws Exception {
	}

	private static List<IDeferredActionExecutorFactory> loadExecutors() {
		RunnableExtensionLoader<IDeferredActionExecutorFactory> epLoader = new RunnableExtensionLoader<>();
		return epLoader.loadExtensions("net.bluemind.deferredaction.registry", "executor", "executor",
				"implementation");
	}
}
