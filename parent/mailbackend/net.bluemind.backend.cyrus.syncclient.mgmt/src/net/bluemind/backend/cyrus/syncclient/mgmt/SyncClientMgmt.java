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
package net.bluemind.backend.cyrus.syncclient.mgmt;

import java.util.List;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

import com.google.common.collect.ImmutableList;

import net.bluemind.backend.cyrus.syncclient.mgmt.api.ISyncClientMgmt;
import net.bluemind.backend.cyrus.syncclient.mgmt.api.ISyncClientObserver;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.node.api.ProcessHandler;
import net.bluemind.node.shared.ExecDescriptor;
import net.bluemind.node.shared.ExecRequest;
import net.bluemind.node.shared.ExecRequest.Options;

public class SyncClientMgmt implements ISyncClientMgmt, ProcessHandler {

	private static final Logger logger = LoggerFactory.getLogger(SyncClientMgmt.class);
	private final List<ISyncClientObserver> observers;
	private final INodeClient node;
	private final Executor observersPool;
	private final String replicationChannel;
	private final Vertx vertx;
	private Long timerId;
	private boolean stopped;
	private boolean started;
	private String activeTask;
	private final Handler<Message<JsonObject>> uplinkHandler;
	private final int shardIndex;

	public SyncClientMgmt(Vertx vertx, String cyrusBackendAddress, String replicationChannel, int shardIndex,
			List<ISyncClientObserver> obs, Executor observersPool) {
		this.observers = ImmutableList.copyOf(obs);
		this.observersPool = observersPool;
		this.node = NodeActivator.get(cyrusBackendAddress);
		this.vertx = vertx;
		this.replicationChannel = replicationChannel;
		this.shardIndex = shardIndex;
		this.uplinkHandler = (Message<JsonObject> msg) -> {
			String linkStatus = msg.body().getString("status");
			if ("UP".equals(linkStatus)) {
				for (ISyncClientObserver sco : this.observers) {
					observersPool.execute(() -> sco.replicationStarted(started));
				}
				started = true;
			} else {
				logger.warn("Uplink status: {}", linkStatus);
			}
		};
	}

	public void startRollingReplication() {
		logger.info("Start/Replace rolling replication process.");
		if (timerId != null) {
			vertx.cancelTimer(timerId);
		}
		vertx.eventBus().registerLocalHandler("mailreplica.uplink", uplinkHandler);

		ExecRequest syncClientReq = ExecRequest.named("mail_replication_" + shardIndex, "sync_client",
				"/usr/sbin/sync_client -n " + replicationChannel + " -i " + shardIndex + " -R -l -v",
				Options.REPLACE_IF_EXISTS);
		try {
			node.asyncExecute(syncClientReq, this);
		} catch (ServerFault sf) {
			logger.warn("Failed to start sync_client ({}), retrying in 1sec.", sf.getMessage());
			vertx.setTimer(1000, tid -> startRollingReplication());
		}
	}

	public void stopRollingReplication() {
		this.stopped = true;
		vertx.eventBus().unregisterHandler("mailreplica.uplink", uplinkHandler);
		node.interrupt(ExecDescriptor.forTask(activeTask));
	}

	@Override
	public void log(String l) {
		for (ISyncClientObserver obs : observers) {
			observersPool.execute(() -> obs.log(l));
		}
	}

	@Override
	public void completed(int exitCode) {
		if (!stopped) {
			logger.warn("Respawn as the task ended on node.");
			startRollingReplication();
		} else {
			logger.info("SyncClient termined with exitCode {}", exitCode);
			for (ISyncClientObserver obs : observers) {
				observersPool.execute(obs::replicationStopped);
			}
		}
	}

	@Override
	public void starting(String taskRef) {
		logger.info("************* SYNC_CLIENT started, ref: {}", taskRef);
		this.activeTask = taskRef;
	}

}
