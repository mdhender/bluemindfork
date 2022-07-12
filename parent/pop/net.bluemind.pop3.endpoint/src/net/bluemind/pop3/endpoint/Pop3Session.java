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

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetSocket;
import io.vertx.core.parsetools.RecordParser;

public class Pop3Session {

	private static final Logger logger = LoggerFactory.getLogger(Pop3Session.class);
	private final NetSocket socket;
	private final Vertx vertx;

	public interface PopProcessor {
		CompletableFuture<Void> run(PopContext ctx, String cmd);
	}

	private static final Map<String, PopProcessor> procs = buildProcMap();

	public Pop3Session(Vertx vertx, NetSocket socket) {
		this.vertx = vertx;
		this.socket = socket;
	}

	private static Map<String, PopProcessor> buildProcMap() {
		ConcurrentHashMap<String, PopProcessor> ret = new ConcurrentHashMap<>();
		ret.put("user", new UserProcessor());
		ret.put("pass", new PassProcessor());
		ret.put("capa", new CapaProcessor());
		ret.put("stat", new StatProcessor());
		ret.put("list", new ListProcessor());
		ret.put("quit", new QuitProcessor());
		return ret;
	}

	public void start() {
		PopContext ctx = new PopContext(vertx, socket);

		RecordParser parser = RecordParser.newDelimited("\r\n", rec -> onChunk(ctx, rec));
		socket.handler(parser::handle);

		socket.closeHandler(v -> {
			logger.info("Connection closed");
		});
		socket.exceptionHandler(t -> {
			logger.error("bouh bouh bouh", t);
		});
		ctx.write("+OK POP3 ready\r\n");
		logger.info("{} started", this);
	}

	private void onChunk(PopContext ctx, Buffer chunk) {
		logger.info("C: {}", chunk);

		String cmd = chunk.toString(StandardCharsets.US_ASCII);

		int space = cmd.indexOf(' ');
		String cmdKey = cmd.toLowerCase();
		if (space > 0) {
			cmdKey = cmd.substring(0, space).toLowerCase();
		}

		PopProcessor proc = procs.get(cmdKey);
		if (proc == null) {
			logger.error("cmd {} not supported", cmd);
			ctx.write("-ERR unknown command\r\n");
		} else {

			vertx.executeBlocking(prom -> {
				proc.run(ctx, cmd).whenComplete((v, ex) -> {
					if (ex != null) {
						prom.fail(ex);
					} else {
						prom.complete();
					}
				});
			}, true, ar -> {
				if (ar.failed()) {
					logger.error("cmd {} failed", cmd, ar.cause());
				}
			});

		}

	}

	private static class QuitProcessor implements PopProcessor {
		public CompletableFuture<Void> run(PopContext ctx, String cmd) {
			return ctx.write("+OK\r\n").thenAccept(v -> {
				ctx.close();
			});
		}
	}

	private static class UserProcessor implements PopProcessor {
		public CompletableFuture<Void> run(PopContext ctx, String cmd) {
			String login = cmd.substring("USER ".length());
			ctx.setLogin(login);
			return ctx.write("+OK\r\n");
		}

	}

	private static class PassProcessor implements PopProcessor {
		public CompletableFuture<Void> run(PopContext ctx, String cmd) {
			String pass = cmd.substring("PASS ".length());
			if (ctx.connect(pass)) {
				return ctx.write("+OK\r\n");
			} else {
				return ctx.write("-ERR Invalid login or password\r\n");
			}
		}
	}

	private static class CapaProcessor implements PopProcessor {

		public CompletableFuture<Void> run(PopContext ctx, String cmd) {
			StringBuilder sb = new StringBuilder();
			sb.append("+OK Capability list follows\r\n");
			sb.append("USER\r\n");
			sb.append(".\r\n");
			return ctx.write(sb.toString());
		}
	}

	private static class StatProcessor implements PopProcessor {

		public CompletableFuture<Void> run(PopContext ctx, String cmd) {
			if (ctx.connection() == null) {
				return ctx.write("-ERR invalid command\r\n");
			} else {
				Stat stat = ctx.connection().stat();
				return ctx.write("+OK " + stat.count() + " " + stat.size() + "\r\n");
			}
		}
	}

	private static class ListProcessor implements PopProcessor {

		public CompletableFuture<Void> run(PopContext ctx, String cmd) {
			if (ctx.connection() == null) {
				return ctx.write("-ERR invalid command\r\n");
			} else {
				ctx.write("+OK\r\n");
				ListItemStream stream = new ListItemStream(ctx);
				return ctx.connection().list(stream).thenCompose(v -> ctx.write(".\r\n"));
			}
		}
	}

}
