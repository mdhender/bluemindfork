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

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

import org.vertx.java.core.Vertx;

import net.bluemind.backend.cyrus.replication.observers.IReplicationObserver;
import net.bluemind.backend.cyrus.replication.observers.IReplicationObserverProvider;

public class ReplicationFeebackObserver implements IReplicationObserverProvider {

	private static final Map<String, Watch> toWatch = new ConcurrentHashMap<>();

	public static final CompletableFuture<Void> addWatcher(Vertx vertx, String mailboxUniqueId) {
		Watch w = new Watch(mailboxUniqueId);
		vertx.setTimer(10000,
				tid -> w.watcher.completeExceptionally(new TimeoutException("Replication latency is > 10sec")));
		toWatch.put(mailboxUniqueId, w);
		return w.watcher;
	}

	private static class Watch {
		final String uniqueId;
		final CompletableFuture<Void> watcher;

		public Watch(String mailboxUniqueId) {
			this.uniqueId = mailboxUniqueId;
			watcher = new CompletableFuture<>();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((uniqueId == null) ? 0 : uniqueId.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Watch other = (Watch) obj;
			if (uniqueId == null) {
				if (other.uniqueId != null)
					return false;
			} else if (!uniqueId.equals(other.uniqueId)) {
				return false;
			}
			return true;
		}
	}

	private static final IReplicationObserver OBS = new IReplicationObserver() {

		@Override
		public void onApplyMessages(int total) {
			// we just track apply mailbox calls
		}

		@Override
		public void onApplyMailbox(String mboxUniqueId) {
			Optional.ofNullable(toWatch.remove(mboxUniqueId)).ifPresent(w -> w.watcher.complete(null));
		}
	};

	@Override
	public IReplicationObserver create(Vertx vertx) {
		return OBS;
	}

}
