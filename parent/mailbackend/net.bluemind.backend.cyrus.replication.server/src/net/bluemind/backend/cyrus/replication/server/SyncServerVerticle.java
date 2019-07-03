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
import org.vertx.java.core.Future;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.net.NetServer;
import org.vertx.java.platform.Verticle;

import net.bluemind.backend.cyrus.replication.observers.IReplicationObserver;
import net.bluemind.backend.cyrus.replication.observers.ReplicationObservers;
import net.bluemind.backend.cyrus.replication.server.state.ReadyStateNotifier;
import net.bluemind.core.rest.http.HttpClientProvider;

public class SyncServerVerticle extends Verticle {

	// 2500 conflicts with bm-milter
	public static final int PORT = 2501;

	private static final Logger logger = LoggerFactory.getLogger(SyncServerVerticle.class);

	public void start(Future<Void> start) {
		NetServer srv = vertx.createNetServer();
		srv.setAcceptBacklog(1024).setTCPNoDelay(true).setTCPKeepAlive(true).setReuseAddress(true);
		HttpClientProvider prov = new HttpClientProvider(vertx);
		List<IReplicationObserver> observers = ReplicationObservers.create(vertx);
		srv.connectHandler(new SyncServerConnection(vertx, prov, observers));

		srv.listen(PORT, result -> {
			if (result.succeeded()) {
				logger.info("Listening on port {}", PORT);
				start.setResult(null);
				notifyReadyState(vertx);
			} else {
				start.setFailure(result.cause());
			}
		});
	}

	private void notifyReadyState(Vertx vertx) {
		ReadyStateNotifier.INSTANCE.notifyReady(vertx);
	}

}
