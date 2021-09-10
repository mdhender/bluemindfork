/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.core.backup.continuous.api;

import java.util.List;

import net.bluemind.eclipse.common.RunnableExtensionLoader;

public class Providers {

	private static boolean disabled = false;

	private Providers() {

	}

	private static final FactoryProvider loaded = load();

	public static final IBackupStoreFactory get() {
		return disabled ? NoopStore.INSTANCE : loaded.get();
	}

	public static void disable() {
		disabled = true;
	}

	private static FactoryProvider load() {
		RunnableExtensionLoader<FactoryProvider> rel = new RunnableExtensionLoader<>();
		List<FactoryProvider> plugins = rel.loadExtensions("net.bluemind.core.backup.continuous.api", "factoryprovider",
				"factory_provider", "impl");
		if (plugins.isEmpty()) {
			return () -> NoopStore.INSTANCE;
		} else {
			return plugins.get(0);
		}
	}

}
