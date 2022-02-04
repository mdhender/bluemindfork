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

import java.util.concurrent.CompletableFuture;

import net.bluemind.core.container.model.BaseContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;

public final class NoopStore implements IBackupStoreFactory {

	public static final IBackupStoreFactory INSTANCE = new NoopStore();

	@Override
	public <T> IBackupStore<T> forContainer(BaseContainerDescriptor c) {
		return new IBackupStore<T>() {

			@Override
			public CompletableFuture<Void> storeRaw(String partitionKey, byte[] key, byte[] raw) {
				return CompletableFuture.completedFuture(null);
			}

			@Override
			public CompletableFuture<Void> store(ItemValue<T> data) {
				return CompletableFuture.completedFuture(null);
			}

			@Override
			public CompletableFuture<Void> delete(ItemValue<T> data) {
				return CompletableFuture.completedFuture(null);
			}
		};
	}

	private static final InstallationWriteLeader ALWAYS_LEADER = new InstallationWriteLeader() {

		@Override
		public void releaseLeadership() {
			// OK
		}

		@Override
		public boolean isLeader() {
			return true;
		}
	};

	@Override
	public InstallationWriteLeader leadership() {
		return ALWAYS_LEADER;
	}

}
