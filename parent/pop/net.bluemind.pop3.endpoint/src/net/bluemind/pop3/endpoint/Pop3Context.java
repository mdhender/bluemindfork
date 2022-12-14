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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageProducer;
import io.vertx.core.net.NetSocket;
import net.bluemind.eclipse.common.RunnableExtensionLoader;

public class Pop3Context {

	private static final Logger logger = LoggerFactory.getLogger(Pop3Context.class);
	private static final Pop3Driver driver = loadDriver();

	private static Pop3Driver loadDriver() {
		RunnableExtensionLoader<Pop3Driver> rel = new RunnableExtensionLoader<>();
		List<Pop3Driver> loaded = rel.loadExtensionsWithPriority("net.bluemind.pop3.endpoint", "driver", "driver",
				"impl");
		return loaded.isEmpty() ? null : loaded.get(0);
	}

	private NetSocket socket;
	private Vertx vertx;
	private MessageProducer<Buffer> sender;

	private MailboxConnection con;
	private String login;
	public ConcurrentMap<Integer, MailItemData> mapMailsForSession = new ConcurrentHashMap<>();
	public ConcurrentMap<Integer, Long> mailsToDelete = new ConcurrentHashMap<>();

	public Pop3Context(Vertx vertx, NetSocket socket) {
		this.vertx = vertx;
		this.socket = socket;
		this.sender = vertx.eventBus().sender(socket.writeHandlerID());
	}

	public CompletableFuture<Void> write(String s) {
		logger.debug("S: {}", s.replaceAll("\r\n$", ""));
		return sender.write(Buffer.buffer(s)).toCompletionStage().toCompletableFuture();
	}

	public CompletableFuture<Void> write(ByteBuf bb) {
		logger.debug("S: {} bytes", bb.readableBytes());
		return sender.write(Buffer.buffer(bb)).toCompletionStage().toCompletableFuture();
	}

	public Future<Void> writeFuture(String s) {
		logger.debug("S: {}", s.replaceAll("\r\n$", ""));
		return sender.write(Buffer.buffer(s));
	}

	public void setLogin(String login) {
		this.login = login;
	}

	public String getLogin() {
		return login;
	}

	public CompletableFuture<Boolean> connect(String pass) {
		CompletableFuture<Boolean> connectResult = new CompletableFuture<>();
		if (login == null) {
			connectResult.completeExceptionally(new Exception("login is null"));
			return connectResult;
		}

		if (driver == null) {
			logger.warn("No driver available for {}", login);
			connectResult.completeExceptionally(new Exception("No driver available for " + login));
			return connectResult;
		}
		driver.connect(login, pass).thenAccept(coreCon -> {
			if (coreCon == null) {
				connectResult.complete(false);
			} else {
				this.con = coreCon;
				connectResult.complete(true);
			}
		}).exceptionally(e -> {
			connectResult.completeExceptionally(e);
			return null;
		});
		return connectResult;
	}

	public CompletableFuture<ConcurrentMap<Integer, MailItemData>> getMap() {
		if (mapMailsForSession.size() == 0) {
			return con.mapPopIdtoMailId().thenApply(map -> {
				mapMailsForSession = map;
				return map;
			});
		} else {
			return CompletableFuture.completedFuture(mapMailsForSession);
		}
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
