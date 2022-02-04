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
package net.bluemind.core.backup.continuous;

import java.util.List;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.backup.continuous.api.IBackupStoreFactory;
import net.bluemind.core.backup.continuous.api.InstallationWriteLeader;
import net.bluemind.core.backup.continuous.impl.BackupReader;
import net.bluemind.core.backup.continuous.impl.BackupStoreFactory;
import net.bluemind.core.backup.continuous.leader.DefaultLeader;
import net.bluemind.core.backup.continuous.store.ITopicStore;
import net.bluemind.eclipse.common.RunnableExtensionLoader;

public class DefaultBackupStore {

	private static final Logger logger = LoggerFactory.getLogger(DefaultBackupStore.class);

	private static class StoreAndElection {
		public StoreAndElection(ITopicStore loaded, Supplier<InstallationWriteLeader> leader) {
			this.store = loaded;
			this.election = leader;
		}

		final ITopicStore store;
		final Supplier<InstallationWriteLeader> election;
	}

	private static final StoreAndElection active = load();

	private static final StoreAndElection load() {
		RunnableExtensionLoader<ITopicStore> rel = new RunnableExtensionLoader<>();
		List<ITopicStore> stores = rel.loadExtensionsWithPriority("net.bluemind.core.backup.continuous", "store",
				"store", "impl");
		for (ITopicStore loaded : stores) {
			if (loaded.isEnabled()) {
				logger.info("Selected backup store is {}", loaded);
				return new StoreAndElection(loaded, DefaultLeader::leader);
			} else {
				logger.warn("Loaded {} but it is not enabled", loaded);
			}
		}
		logger.warn("NOOP store for backup");
		return new StoreAndElection(NoopStore.NOOP, () -> new InstallationWriteLeader() {

			@Override
			public void releaseLeadership() {
				// ok
			}

			@Override
			public boolean isLeader() {
				return true;
			}
		});
	}

	private DefaultBackupStore() {
	}

	public static IBackupStoreFactory store() {
		return new BackupStoreFactory(active.store, active.election);
	}

	public static IBackupReader reader() {
		return new BackupReader(active.store);
	}

}
