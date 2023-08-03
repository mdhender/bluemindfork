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
import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.MessageProducer;
import net.bluemind.common.vertx.contextlogging.ContextualData;
import net.bluemind.eclipse.common.RunnableExtensionLoader;
import net.bluemind.lib.vertx.ContextNetSocket;
import net.bluemind.lib.vertx.VertxPlatform;

public class Pop3Context {

	private static final Logger logger = LoggerFactory.getLogger(Pop3Context.class);
	private static final IPop3Driver driver = loadDriver();

	private static IPop3Driver loadDriver() {
		RunnableExtensionLoader<IPop3DriverFactory> rel = new RunnableExtensionLoader<>();
		List<IPop3DriverFactory> loaded = rel.loadExtensionsWithPriority("net.bluemind.pop3.endpoint", "driver",
				"driver", "impl");
		return loaded.isEmpty() ? null : loaded.get(0).create(VertxPlatform.getVertx());
	}

	private ContextNetSocket socket;
	private MessageProducer<Buffer> sender;
	public final Context vertxContext;

	private MailboxConnection con;
	private String login;
	public ConcurrentMap<Integer, MailItemData> mapMailsForSession = new ConcurrentHashMap<>();
	public ConcurrentMap<Integer, Long> mailsToDelete = new ConcurrentHashMap<>();

	public static class ContextProducer implements MessageProducer<Buffer> {
		private ContextNetSocket ns;

		public ContextProducer(ContextNetSocket ns) {
			this.ns = ns;
		}

		@Override
		public MessageProducer<Buffer> deliveryOptions(DeliveryOptions options) {
			return this;
		}

		@Override
		public String address() {
			return "yeah";
		}

		@Override
		public void write(Buffer body, Handler<AsyncResult<Void>> handler) {
			ns.write(body, handler);
		}

		@Override
		public Future<Void> write(Buffer body) {
			return ns.write(body);
		}

		@Override
		public Future<Void> close() {
			return ns.close();
		}

		@Override
		public void close(Handler<AsyncResult<Void>> handler) {
			ns.close(handler);
		}
	}

	public Pop3Context(Context vertxContext, ContextNetSocket socket) {
		this.socket = socket;
		this.vertxContext = vertxContext;
		this.sender = new ContextProducer(socket);
		ContextualData.put("endpoint", "pop3");
	}

	public CompletableFuture<Void> write(String s) {
		return writeFuture(s).toCompletionStage().toCompletableFuture();
	}

	public CompletableFuture<Void> write(ByteBuf bb) {
		CompletableFuture<Void> prom = new CompletableFuture<>();
		sender.write(Buffer.buffer(bb), ar -> {
			if (ar.succeeded()) {
				if (logger.isDebugEnabled()) {
					logger.debug("S: {} bytes", bb.readableBytes());
				}
				prom.complete(null);
			} else {
				logger.error("S: unable to send {} bytes", bb.readableBytes(), ar.cause());
				prom.completeExceptionally(ar.cause());
			}
		});
		return prom;
	}

	public Future<Void> writeFuture(String s) {
		return sender.write(Buffer.buffer(s)).onSuccess(v -> {
			if (logger.isDebugEnabled()) {
				logger.debug("S: {}", s.stripTrailing());
			}
		}).onFailure(t -> logger.error("S: unable to send {}", s.stripTrailing(), t));
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
			connectResult.completeExceptionally(new Pop3Error("login is null"));
			return connectResult;
		}

		if (driver == null) {
			logger.warn("No driver available for {}", login);
			connectResult.completeExceptionally(new Pop3Error("No driver available for " + login));
			return connectResult;
		}
		driver.connect(login, pass).thenAccept(coreCon -> {
			if (coreCon == null) {
				connectResult.complete(false);
			} else {
				this.con = coreCon;
				ContextualData.put("user", con.logId());
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
		sender.close();
	}

	public ContextNetSocket socket() {
		return socket;
	}

	public MailboxConnection connection() {
		return con;
	}

	public void logRequest(String request) {
		logger.info("{} - C: {}", getLogin(), request);
	}
}
