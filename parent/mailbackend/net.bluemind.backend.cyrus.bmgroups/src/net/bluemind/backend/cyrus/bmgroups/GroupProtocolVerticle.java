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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetSocket;
import io.vertx.core.net.SocketAddress;
import net.bluemind.core.rest.http.HttpClientProvider;

public class GroupProtocolVerticle extends AbstractVerticle {
	private static Logger logger = LoggerFactory.getLogger(GroupProtocolVerticle.class);

	private static String SOCKET_PATH = socketPath("/var/run/cyrus/socket/bm-ptsock");

	public static String socketPath() {
		return SOCKET_PATH;
	}

	public static String socketPath(String p) {
		try {
			SOCKET_PATH = p;
			Files.deleteIfExists(Paths.get(p));
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
		return p;
	}

	@Override
	public void start() throws IOException {
		NetServer server = vertx.createNetServer();
		final HttpClientProvider clientProvider = new HttpClientProvider(getVertx());
		server.connectHandler((NetSocket socket) -> {
			socket.exceptionHandler((Throwable event) -> {
				logger.error("error ", event);
				if (event.getCause() != null) {
					logger.error("error ", event.getCause());
				}
				socket.close();
			});
			socket.handler(new GroupProtocolHandler(clientProvider, socket));
		});

		server.listen(SocketAddress.domainSocketAddress(SOCKET_PATH), (

				AsyncResult<NetServer> res) -> {
			if (res.failed()) {
				Throwable t = res.cause();
				logger.error("failed to create domain socket at {} : {}", SOCKET_PATH, t.getMessage(), t);
			} else {
				logger.info("Socket bound at {}", SOCKET_PATH);
			}
		});

		vertx.eventBus().consumer("invalidate.cache", msg -> {
			JsonObject cm = (JsonObject) msg.body();
			String login = cm.getString("login");
			String domain = cm.getString("domain");

			String key = domain + "-" + login;

			GroupProtocolHandler.getUsersCache().invalidate(key);
			GroupProtocolHandler.getMemberOfCache().invalidate(key);
		});

	}
}
