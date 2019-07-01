/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.backend.cyrus.syncclient.mgmt.api;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executor;

import org.vertx.java.core.Vertx;

import net.bluemind.backend.cyrus.syncclient.mgmt.SyncClientMgmt;

public interface ISyncClientMgmt {

	public static class SyncClientMgmtBuilder {
		private Vertx vertx;
		private String cyrusBackendAddress;
		private String replicationChannel;
		private List<ISyncClientObserver> observers;
		private Executor observersPool;

		private SyncClientMgmtBuilder() {
			observers = new LinkedList<>();
		}

		public SyncClientMgmtBuilder vertx(Vertx vertx) {
			this.vertx = vertx;
			return this;
		}

		public SyncClientMgmtBuilder cyrusBackendAddress(String cyrusBackendAddress) {
			this.cyrusBackendAddress = cyrusBackendAddress;
			return this;
		}

		public SyncClientMgmtBuilder replicationChannel(String channel) {
			this.replicationChannel = channel;
			return this;
		}

		public SyncClientMgmtBuilder observer(ISyncClientObserver obs) {
			this.observers.add(obs);
			return this;
		}

		public SyncClientMgmtBuilder observersExecutor(Executor exec) {
			this.observersPool = exec;
			return this;
		}

		public ISyncClientMgmt build() {
			return new SyncClientMgmt(vertx, cyrusBackendAddress, replicationChannel, observers, observersPool);
		}

	}

	public static SyncClientMgmtBuilder builder() {
		return new SyncClientMgmtBuilder();
	}

	void stopRollingReplication();

	/**
	 * Starts, or restarts, rolling replication
	 */
	void startRollingReplication();

}
