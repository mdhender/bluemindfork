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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.core.container.repair;

import java.util.List;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import net.bluemind.eclipse.common.RunnableExtensionLoader;

public class Activator implements BundleActivator {

	public static List<ContainerRepairOp> ops;

	@Override
	public void start(BundleContext context) throws Exception {
		Activator.ops = lookupContainerRepairOps();
	}

	private List<ContainerRepairOp> lookupContainerRepairOps() {
		RunnableExtensionLoader<ContainerRepairOp> epLoader = new RunnableExtensionLoader<>();
		List<ContainerRepairOp> extensions = epLoader.loadExtensions("net.bluemind.core.container", "repair", "repair",
				"impl");

		return extensions;
	}

	@Override
	public void stop(BundleContext context) throws Exception {

	}

}
