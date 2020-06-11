/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
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
package net.bluemind.backend.cyrus.replication.server;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;
import net.bluemind.backend.cyrus.replication.observers.IReplicationObserver;
import net.bluemind.backend.cyrus.replication.observers.ReplicationObservers;
import net.bluemind.backend.cyrus.replication.server.state.ReadyStateNotifier;
import net.bluemind.core.rest.http.HttpClientProvider;
import net.bluemind.system.api.SystemState;

public class SyncServerVerticle extends AbstractVerticle {

	// 2500 conflicts with bm-milter
	public static final int PORT = 2501;

	private static final Logger logger = LoggerFactory.getLogger(SyncServerVerticle.class);

	private boolean started = false;

	@Override
	public void start(Promise<Void> start) {
		super.vertx.eventBus().consumer(SystemState.BROADCAST, (Message<JsonObject> m) -> {
			if (!started) {
				SystemState state = SystemState.fromOperation(m.body().getString("operation"));
				if (state == SystemState.CORE_STATE_RUNNING) {
					started = true;
					startSyncServer();
				}
			}
		});

		start.complete();
	}

	private void startSyncServer() {
		NetServerOptions syncOpts = new NetServerOptions().setAcceptBacklog(1024).setTcpNoDelay(true)
				.setTcpKeepAlive(true).setReuseAddress(true).setUsePooledBuffers(true);
		// syncOpts.setReceiveBufferSize(8 * 1024 * 1024);

		NetServer srv = vertx.createNetServer(syncOpts);
		HttpClientProvider prov = new HttpClientProvider(vertx);
		List<IReplicationObserver> observers = ReplicationObservers.create(vertx);
		srv.connectHandler(new SyncServerConnection(vertx, prov, observers));
		srv.listen(PORT, result -> {
			if (result.succeeded()) {
				logger.info("Listening on port {}", PORT);
				notifyReadyState(vertx);
			}
		});
	}

	private void notifyReadyState(Vertx vertx) {
		ReadyStateNotifier.INSTANCE.notifyReady(vertx);
	}

}
