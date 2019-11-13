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
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.backend.cyrus.replication.link.probe;

import java.util.Deque;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeoutException;

import org.vertx.java.core.Vertx;

import net.bluemind.backend.cyrus.replication.observers.IReplicationObserver;
import net.bluemind.backend.cyrus.replication.observers.IReplicationObserverProvider;

public class ReplicationFeebackObserver implements IReplicationObserverProvider {

	protected static final Set<String> toWatch = ConcurrentHashMap.newKeySet();
	private static final Deque<CompletableFuture<Void>> watchers = new ConcurrentLinkedDeque<>();

	public static final CompletableFuture<Void> addWatcher(Vertx vertx) {
		CompletableFuture<Void> future = new CompletableFuture<>();
		watchers.addLast(future);
		vertx.setTimer(60000,
				tid -> future.completeExceptionally(new TimeoutException("Replication feedback is slow")));
		return future;
	}

	private static final IReplicationObserver OBS = new IReplicationObserver() {

		@Override
		public void onApplyMessages(int total) {
			// we just track apply mailbox calls
		}

		@Override
		public void onApplyMailbox(String mboxUniqueId) {
			if (toWatch.contains(mboxUniqueId)) {
				CompletableFuture<Void> watcher = watchers.poll();
				while (watcher != null) {
					watcher.complete(null);
					watcher = watchers.poll();
				}
			}
		}
	};

	@Override
	public IReplicationObserver create(Vertx vertx) {
		return OBS;
	}

}
