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
package net.bluemind.eas.impl;

import java.util.List;

import net.bluemind.eas.backend.IBackend;
import net.bluemind.eas.backend.IBackendFactory;
import net.bluemind.eas.store.IStorageFactory;
import net.bluemind.eas.store.ISyncStorage;
import net.bluemind.eas.utils.RunnableExtensionLoader;

public final class Backends {

	private static final IBackend backend;
	private static final ISyncStorage storage;

	static {
		RunnableExtensionLoader<IStorageFactory> sto = new RunnableExtensionLoader<IStorageFactory>();
		List<IStorageFactory> storages = sto.loadExtensions("net.bluemind.eas", "storage", "storage", "implementation");
		if (storages.size() > 0) {
			IStorageFactory stoFactoryImpl = storages.get(0);
			storage = stoFactoryImpl.createStorage();

		} else {
			throw new RuntimeException("No storage implementation found");
		}

		RunnableExtensionLoader<IBackendFactory> rel = new RunnableExtensionLoader<IBackendFactory>();
		List<IBackendFactory> backs = rel.loadExtensions("net.bluemind.eas", "backend", "backend", "implementation");
		if (backs.size() > 0) {
			IBackendFactory bf = backs.get(0);
			backend = bf.create(storage);
		} else {
			throw new RuntimeException("No push backend found.");
		}
	}

	/**
	 * Called by activator to trigger the static initializer
	 */
	public static void classLoad() {
	}

	public static IBackend dataAccess() {
		return backend;
	}

	public static ISyncStorage internalStorage() {
		return storage;
	}

}
