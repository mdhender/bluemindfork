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

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.net.NetSocket;
import net.bluemind.backend.cyrus.replication.observers.IReplicationObserver;
import net.bluemind.backend.cyrus.replication.server.state.StorageApiLink;
import net.bluemind.core.rest.http.HttpClientProvider;

public class SyncServerConnection implements Handler<NetSocket> {

	private static final Logger logger = LoggerFactory.getLogger(SyncServerConnection.class);
	private final Vertx vertx;
	private final HttpClientProvider http;
	private final List<IReplicationObserver> observers;

	public SyncServerConnection(Vertx vertx, HttpClientProvider http, List<IReplicationObserver> observers) {
		this.vertx = vertx;
		this.http = http;
		this.observers = observers;
	}

	@Override
	public void handle(NetSocket client) {
		String remoteIp = client.remoteAddress().host();
		logger.info("Connected {}", remoteIp);
		StorageApiLink.create(vertx, http, remoteIp).whenComplete((storage, ex) -> {
			if (ex == null) {
				logger.info("Start replication session with {}", client);
				ReplicationSession session = new ReplicationSession(vertx, client, storage, observers);
				session.start();
			} else {
				logger.error(ex.getMessage(), ex);
				client.close();
			}
		});
	}

}
