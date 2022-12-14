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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.spectator.api.Registry;
import com.netflix.spectator.api.patterns.PolledMeter;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetSocket;
import io.vertx.core.parsetools.RecordParser;
import net.bluemind.metrics.registry.IdFactory;
import net.bluemind.metrics.registry.MetricsRegistry;

public class Pop3Session {

	private static final String CRLF = "\r\n";
	private static final Logger logger = LoggerFactory.getLogger(Pop3Session.class);
	private final NetSocket socket;
	private final Vertx vertx;

	private static final Registry registry = MetricsRegistry.get();
	private static final IdFactory idFactory = new IdFactory("pop3", MetricsRegistry.get(), Pop3Session.class);
	private static final AtomicInteger activeConnections = new AtomicInteger();

	public interface PopProcessor {
		CompletableFuture<Void> run(Pop3Context ctx, String cmd);
	}

	private static final Map<String, PopProcessor> procs = buildProcMap();

	public Pop3Session(Vertx vertx, NetSocket socket) {
		this.vertx = vertx;
		this.socket = socket;

		PolledMeter.using(registry).withId(idFactory.name("connections")).monitorValue(activeConnections);
		activeConnections.addAndGet(1);
	}

	private static Map<String, PopProcessor> buildProcMap() {
		ConcurrentHashMap<String, PopProcessor> ret = new ConcurrentHashMap<>();
		ret.put("user", new UserProcessor());
		ret.put("pass", new PassProcessor());
		ret.put("capa", new CapaProcessor());
		ret.put("stat", new StatProcessor());
		ret.put("list", new ListProcessor());
		ret.put("uidl", new UidlProcessor());
		ret.put("retr", new RetrProcessor());
		ret.put("dele", new DeleProcessor());
		ret.put("noop", new NoopProcessor());
		ret.put("rset", new RsetProcessor());
		ret.put("top", new TopProcessor());
		ret.put("quit", new QuitProcessor());
		return ret;
	}

	public void start() {
		Pop3Context ctx = new Pop3Context(vertx, socket);

		RecordParser parser = RecordParser.newDelimited(CRLF, rec -> onChunk(ctx, rec));
		socket.handler(parser::handle);

		socket.closeHandler(v -> {
			activeConnections.decrementAndGet();
			logger.info("{} - connection closed", ctx.getLogin());
		});
		socket.exceptionHandler(t -> logger.error("An exception occured", t));
		ctx.write("+OK POP3 ready" + CRLF);
		logger.info("{} started", this);
	}

	private void onChunk(Pop3Context ctx, Buffer chunk) {
		logger.debug("{} - C: {}", ctx.getLogin(), chunk);

		String cmd = chunk.toString(StandardCharsets.US_ASCII);

		int space = cmd.indexOf(' ');
		String cmdKey = cmd.toLowerCase();
		if (space > 0) {
			cmdKey = cmd.substring(0, space).toLowerCase();
		}

		PopProcessor proc = procs.get(cmdKey);
		if (proc == null) {
			logger.error("cmd {} not supported", cmd);
			ctx.write("-ERR unknown command" + CRLF);
		} else {
			proc.run(ctx, cmd).exceptionally(ex -> {
				logger.error("[{}] {} failed: {}", ctx.getLogin(), cmd, ex.getMessage(), ex);
				ctx.write("-ERR command failed" + CRLF);
				return null;
			});
		}
	}

	private static class QuitProcessor implements PopProcessor {
		public CompletableFuture<Void> run(Pop3Context ctx, String cmd) {
			ctx.mapMailsForSession = new ConcurrentHashMap<>();
			if (!ctx.mailsToDelete.isEmpty()) {
				logger.info("[{}] Deleting {} messages before QUIT", ctx.getLogin(), ctx.mailsToDelete.size());
				return ctx.connection().delete(ctx, ctx.mailsToDelete.values().stream().collect(Collectors.toList()))
						.thenCompose(result -> ctx.write("+OK" + CRLF)).thenAccept(v -> ctx.close());
			} else {
				return ctx.write("+OK" + CRLF).thenAccept(v -> ctx.close());
			}
		}
	}

	private static class UserProcessor implements PopProcessor {
		public CompletableFuture<Void> run(Pop3Context ctx, String cmd) {
			String login = cmd.substring("USER ".length());
			ctx.setLogin(login);
			return ctx.write("+OK" + CRLF);
		}

	}

	private static class PassProcessor implements PopProcessor {
		public CompletableFuture<Void> run(Pop3Context ctx, String cmd) {
			String pass = cmd.substring("PASS ".length());
			ctx.connect(pass).thenAccept(connected -> {
				if (Boolean.TRUE.equals(connected)) {
					ctx.write("+OK" + CRLF);
				} else {
					ctx.write("-ERR Invalid login or password\r\n");
				}
			}).exceptionally(ex -> {
				logger.error(ex.getMessage());
				ctx.write("-ERR Invalid login or password\r\n");
				return null;
			});
			return CompletableFuture.completedFuture(null);
		}
	}

	private static class CapaProcessor implements PopProcessor {

		public CompletableFuture<Void> run(Pop3Context ctx, String cmd) {
			StringBuilder sb = new StringBuilder();
			sb.append("+OK Capability list follows\r\n");
			sb.append("USER" + CRLF);
			sb.append("QUIT" + CRLF);
			sb.append("PASS" + CRLF);
			sb.append("UIDL" + CRLF);
			sb.append("LIST" + CRLF);
			sb.append("TOP" + CRLF);
			sb.append("DELE" + CRLF);
			sb.append("RSET" + CRLF);
			sb.append("RETR" + CRLF);
			sb.append("STAT" + CRLF);
			sb.append("NOOP" + CRLF);
			sb.append("." + CRLF);
			return ctx.write(sb.toString());
		}
	}

	private static class StatProcessor implements PopProcessor {

		public CompletableFuture<Void> run(Pop3Context ctx, String cmd) {
			if (ctx.connection() == null) {
				return ctx.write("-ERR invalid command\r\n");
			} else {
				return ctx.connection().stat()
						.thenCompose(stat -> ctx.write("+OK " + stat.count() + " " + stat.size() + CRLF));
			}
		}
	}

	private static class ListProcessor implements PopProcessor {

		public CompletableFuture<Void> run(Pop3Context ctx, String cmd) {
			String param = null;
			if (ctx.connection() == null) {
				return ctx.write("-ERR invalid command\r\n");
			} else {
				int space = cmd.indexOf(' ');
				if (space > 0) {
					param = cmd.substring(space + 1, cmd.length());
				}
				if (param != null) {
					try {
						Integer id = Integer.parseInt(param);
						return ctx.connection().listUnique(ctx, id);
					} catch (NumberFormatException e) {
						logger.error("{} - bad parameter for LIST method: {}. Integer expected", ctx.getLogin(), param);
						return ctx.write("-ERR invalid command\r\n");
					}
				} else {
					ListItemStream stream = new ListItemStream(ctx);
					return ctx.connection().stat().thenCompose(stat -> {
						ctx.write("+OK " + stat.count() + " messages (" + stat.size() + " octets)\r\n");
						return ctx.connection().list(ctx, stream).thenCompose(v -> ctx.write("." + CRLF));
					});
				}
			}
		}
	}

	private static class UidlProcessor implements PopProcessor {
		public CompletableFuture<Void> run(Pop3Context ctx, String cmd) {
			if (ctx.connection() == null) {
				return ctx.write("-ERR invalid command\r\n");
			} else {
				ctx.write("+OK\r\n");
				int space = cmd.indexOf(' ');
				String param = (space > 0) ? cmd.substring(space + 1, cmd.length()) : null;
				if (param != null) {
					Integer id = Integer.parseInt(param);
					try {
						return ctx.connection().uidlUnique(ctx, id);
					} catch (NumberFormatException e) {
						logger.error("{} - bad parameter for UIDL method: {}. Integer expected", ctx.getLogin(), param);
						return ctx.write("-ERR invalid command\r\n");
					}
				} else {
					UidlItemStream stream = new UidlItemStream(ctx);
					return ctx.connection().uidl(ctx, stream).thenCompose(v -> ctx.write("." + CRLF));
				}
			}
		}
	}

	private static class RetrProcessor implements PopProcessor {
		public CompletableFuture<Void> run(Pop3Context ctx, String cmd) {
			if (ctx.connection() == null) {
				return ctx.write("-ERR invalid command" + CRLF);
			} else {
				int space = cmd.indexOf(' ');
				if (space == -1) {
					return ctx.write("-ERR no such message" + CRLF);
				}
				String param = cmd.substring(space, cmd.length()).trim();
				return ctx.connection().retr(ctx, param).thenCompose(item -> {
					if (item == null) {
						return ctx.write("-ERR no such message" + CRLF);
					} else {
						ctx.write("+OK " + item.getMailSize() + " octets" + CRLF);
						return item.completableFuture.thenCompose(bb -> {
							ctx.write(bb);
							return ctx.write("." + CRLF);
						});
					}
				});
			}
		}
	}

	private static class NoopProcessor implements PopProcessor {
		public CompletableFuture<Void> run(Pop3Context ctx, String cmd) {
			if (ctx.connection() == null) {
				return ctx.write("-ERR invalid command" + CRLF);
			} else {
				return ctx.write("+OK" + CRLF);
			}
		}
	}

	private static class DeleProcessor implements PopProcessor {
		public CompletableFuture<Void> run(Pop3Context ctx, String cmd) {
			if (ctx.connection() == null) {
				ctx.write("-ERR invalid command" + CRLF);
				return CompletableFuture.completedFuture(null);
			} else {
				int space = cmd.indexOf(' ');
				if (space == -1) {
					ctx.write("-ERR no such message" + CRLF);
					return CompletableFuture.completedFuture(null);
				}
				String param = cmd.substring(space, cmd.length()).trim();
				Integer id;
				try {
					id = Integer.parseInt(param);
				} catch (NumberFormatException e) {
					ctx.write("-ERR no such message" + CRLF);
					return CompletableFuture.completedFuture(null);
				}
				return ctx.getMap().thenCompose(map -> {
					if (map.containsKey(id)) {
						registry.counter(idFactory.name("deletedMails", "login", ctx.getLogin())).increment();
						ctx.mailsToDelete.put(id, map.get(id).getItemId());
						map.remove(id);
						return ctx.write("+OK message " + param + " deleted" + CRLF);
					} else {
						return ctx.write("-ERR no such message" + CRLF);
					}
				});
			}
		}
	}

	private static class RsetProcessor implements PopProcessor {
		public CompletableFuture<Void> run(Pop3Context ctx, String cmd) {
			if (ctx.connection() == null) {
				return ctx.write("-ERR invalid command\r\n");
			} else {
				int space = cmd.indexOf(' ');
				if (space == -1) {
					return ctx.write("-ERR invalid command\r\n");
				}
				String param = cmd.substring(space, cmd.length()).trim();
				Integer id = Integer.parseInt(param);
				ctx.mailsToDelete.remove(id);
				logger.debug("{} - remove mail with id {} from toRemove list", ctx.getLogin(), id);
				return ctx.write("+OK" + CRLF);
			}
		}
	}

	private static class TopProcessor implements PopProcessor {
		public CompletableFuture<Void> run(Pop3Context ctx, String cmd) {
			if (ctx.connection() == null) {
				return ctx.write("-ERR invalid command" + CRLF);
			} else {
				String[] params = cmd.split(" ");
				if (params.length != 3) {
					return ctx.write("-ERR invalid command" + CRLF);
				} else {
					TopItemStream stream = new TopItemStream(ctx);
					return ctx.connection().top(stream, params[1], params[2], ctx).thenCompose(cf -> {
						return ctx.write("." + CRLF);
					});
				}
			}
		}
	}

}
