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
package net.bluemind.pop3.endpoint;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageProducer;
import io.vertx.core.net.NetSocket;
import net.bluemind.eclipse.common.RunnableExtensionLoader;

public class PopContext {

	private static final Logger logger = LoggerFactory.getLogger(PopContext.class);
	private static final PopDriver driver = loadDriver();

	private static PopDriver loadDriver() {
		RunnableExtensionLoader<PopDriver> rel = new RunnableExtensionLoader<>();
		List<PopDriver> loaded = rel.loadExtensionsWithPriority("net.bluemind.pop3.endpoint", "driver", "driver",
				"impl");
		return loaded.isEmpty() ? null : loaded.get(0);
	}

	private NetSocket socket;
	private Vertx vertx;
	private MessageProducer<Buffer> sender;

	private MailboxConnection con;
	private String login;

	public PopContext(Vertx vertx, NetSocket socket) {
		this.vertx = vertx;
		this.socket = socket;
		this.sender = vertx.eventBus().sender(socket.writeHandlerID());
	}

	public CompletableFuture<Void> write(String s) {
		logger.info("S: {}", s.replaceAll("\r\n$", ""));
		return sender.write(Buffer.buffer(s)).toCompletionStage().toCompletableFuture();
	}

	public Future<Void> writeFuture(String s) {
		logger.info("S: {}", s.replaceAll("\r\n$", ""));
		return sender.write(Buffer.buffer(s));
	}

	public void setLogin(String login) {
		this.login = login;
	}

	public boolean connect(String pass) {
		if (login == null) {
			return false;
		}
		if (driver == null) {
			logger.warn("No driver available for {}", login);
			return false;
		}
		con = driver.connect(login, pass);
		return con != null;
	}

	public void close() {
		if (con != null) {
			con.close();
		}
		socket.close();
	}

	public NetSocket socket() {
		return socket;
	}

	public MailboxConnection connection() {
		return con;
	}

}
