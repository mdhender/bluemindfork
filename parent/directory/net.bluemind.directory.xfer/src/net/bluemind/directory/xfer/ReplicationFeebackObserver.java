/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2022
  *
  * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License)
  * or the CeCILL as published by CeCILL.info (version 2 of the License).
  *
  * There are special exceptions to the terms and conditions of the
  * licenses as they are applied to this program. See LICENSE.txt in
  * the directory of this program distribution.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.directory.xfer;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.vertx.core.Vertx;
import net.bluemind.backend.cyrus.replication.observers.IReplicationObserver;
import net.bluemind.backend.cyrus.replication.observers.IReplicationObserverProvider;

public class ReplicationFeebackObserver implements IReplicationObserverProvider {

	private static final Map<String, Watch> toWatch = new ConcurrentHashMap<>();

	public static final CompletableFuture<Void> addWatcher(Vertx vertx, String mailboxUniqueId, long imapUid,
			int timeoutValue, TimeUnit timeoutUnit) {
		Watch w = new Watch(mailboxUniqueId, imapUid);
		vertx.setTimer(timeoutUnit.toMillis(timeoutValue), tid -> w.watcher.completeExceptionally(new TimeoutException(
				"Replication latency is > " + timeoutUnit.convert(timeoutValue, TimeUnit.SECONDS) + " seconds")));
		toWatch.put(mailboxUniqueId, w);
		return w.watcher;
	}

	private static class Watch {
		final String uniqueId;
		final long imapUid;
		final CompletableFuture<Void> watcher;

		public Watch(String mailboxUniqueId, long imapUid) {
			this.uniqueId = mailboxUniqueId;
			this.imapUid = imapUid;
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
		public void onApplyMailbox(String mboxUniqueId, long lastUid) {
			Watch w = toWatch.get(mboxUniqueId);
			if (w != null) {
				if (lastUid >= w.imapUid) {
					toWatch.remove(mboxUniqueId);
					w.watcher.complete(null);
				}
			}
		}
	};

	@Override
	public IReplicationObserver create(Vertx vertx) {
		return OBS;
	}
}
