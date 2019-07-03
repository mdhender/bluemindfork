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
package net.bluemind.hsm.storage;

import java.util.List;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.eclipse.common.RunnableExtensionLoader;

public class Activator implements BundleActivator {
	private static final Logger logger = LoggerFactory.getLogger(Activator.class);

	private static BundleContext context;
	private static IHSMStorage storage;

	static BundleContext getContext() {
		return context;
	}

	@Override
	public void start(BundleContext context) throws Exception {
		Activator.context = context;

		RunnableExtensionLoader<IHSMStorage> rel = new RunnableExtensionLoader<IHSMStorage>();
		List<IHSMStorage> stores = rel.loadExtensions("net.bluemind.hsm", "storage", "hsm_store", "implementation");

		int priority = 0;
		for (IHSMStorage ids : stores) {
			if (ids.getPriority() > priority) {
				storage = ids;
				priority = ids.getPriority();
			}
		}

		if (storage == null) {
			logger.error("No IHSMStorage implementation found!");
		} else {
			logger.info("IHSMStorage implementation: {}", storage.getClass());
		}
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		Activator.context = null;
	}

	public static IHSMStorage getHSMStorage() {
		return storage;
	}

}