/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.backend.cyrus.bmgroups;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.impl.VertxInternal;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.net.NetSocket;
import org.vertx.java.platform.Verticle;

import net.bluemind.core.rest.http.HttpClientProvider;
import net.bluemind.lib.vertx.domainsocket.DomainSocketServer;

public class GroupProtocolVerticle extends Verticle {
	private static Logger logger = LoggerFactory.getLogger(GroupProtocolVerticle.class);

	public static String SOCKET_PATH = "/var/run/cyrus/socket/bm-ptsock";

	@Override
	public void start() {
		DomainSocketServer server = new DomainSocketServer((VertxInternal) vertx);
		final HttpClientProvider clientProvider = new HttpClientProvider(getVertx());
		server.connectHandler(new Handler<NetSocket>() {

			@Override
			public void handle(NetSocket socket) {
				socket.exceptionHandler(new Handler<Throwable>() {

					@Override
					public void handle(Throwable event) {
						logger.error("error ", event);
						if (event.getCause() != null) {
							logger.error("error ", event.getCause());
						}
						socket.close();
					}
				});
				socket.dataHandler(new GroupProtocolHandler(clientProvider, socket));
			}
		});

		server.listen(SOCKET_PATH, new Handler<AsyncResult<DomainSocketServer>>() {

			@Override
			public void handle(AsyncResult<DomainSocketServer> res) {
				if (res.failed()) {
					Throwable t = res.cause();
					logger.error("failed to create domain socket at {} : {}", SOCKET_PATH, t.getMessage(), t);
				} else {
					logger.info("Socket binded at {}", SOCKET_PATH);
				}
			}
		});

		vertx.eventBus().registerHandler("invalidate.cache", msg -> {
			JsonObject cm = (JsonObject) msg.body();
			String login = cm.getString("login");
			String domain = cm.getString("domain");

			String key = domain + "-" + login;

			GroupProtocolHandler.getUsersCache().invalidate(key);
			GroupProtocolHandler.getMemberOfCache().invalidate(key);
		});

	}
}
