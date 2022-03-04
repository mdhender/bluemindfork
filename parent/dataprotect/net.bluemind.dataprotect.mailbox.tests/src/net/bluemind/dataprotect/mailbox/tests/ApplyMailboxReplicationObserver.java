/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.dataprotect.mailbox.tests;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import io.vertx.core.Vertx;
import net.bluemind.backend.cyrus.replication.observers.IReplicationObserver;
import net.bluemind.backend.cyrus.replication.observers.IReplicationObserverProvider;

public class ApplyMailboxReplicationObserver implements IReplicationObserver {
	private static final Map<String, Watcher> watchers = new ConcurrentHashMap<>();

	public static class Factory implements IReplicationObserverProvider {
		@Override
		public IReplicationObserver create(Vertx vertx) {
			return new ApplyMailboxReplicationObserver();
		}
	}

	public static class Watcher {
		private long waitingUid = 0;
		private long lastUid = 0;

		public Watcher waitForUid(long uid) {
			waitingUid = uid;
			return this;
		}

		public Watcher updateLastUid(long newUid) {
			if (newUid > lastUid) {
				lastUid = newUid;
			}
			return this;
		}

		public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
			long waitms = 0;
			while (waitingUid < lastUid) {
				Thread.sleep(100);
				waitms += 100;
				if (waitms > unit.toMillis(timeout)) {
					return false;
				}
			}
			return true;
		}
	}

	public static Watcher addWatcher(String mboxUniqueId) {
		Watcher w = new Watcher();
		watchers.putIfAbsent(mboxUniqueId, w);
		return w;
	}

	@Override
	public void onApplyMessages(int count) {
		// ok
	}

	@Override
	public void onApplyMailbox(String mboxUniqueId, long lastUid) {
		watchers.computeIfPresent(mboxUniqueId, (mboxuid, w) -> w.updateLastUid(lastUid));
	}

}