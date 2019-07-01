/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.imap.vertx;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Context;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.net.NetClient;
import org.vertx.java.core.net.NetSocket;
import org.vertx.java.core.streams.Pump;
import org.vertx.java.core.streams.ReadStream;

import io.netty.buffer.ByteBuf;
import net.bluemind.imap.vertx.ImapResponseStatus.Status;
import net.bluemind.imap.vertx.cmd.AppendCommandHelper;
import net.bluemind.imap.vertx.cmd.AppendListener;
import net.bluemind.imap.vertx.cmd.AppendResponse;
import net.bluemind.imap.vertx.cmd.FetchListener;
import net.bluemind.imap.vertx.cmd.FetchResponse;
import net.bluemind.imap.vertx.cmd.SelectListener;
import net.bluemind.imap.vertx.cmd.SelectResponse;
import net.bluemind.imap.vertx.impl.ImapRecordParser;
import net.bluemind.imap.vertx.impl.TagListener;
import net.bluemind.imap.vertx.impl.TagOrGoAheadListener;
import net.bluemind.lib.jutf7.UTF7Converter;
import net.bluemind.lib.vertx.VertxPlatform;

public class VXStoreClient {

	private static final Logger logger = LoggerFactory.getLogger(VXStoreClient.class);

	private static class ConnectionState {
		private final Context context;
		private final NetSocket socket;

		public ConnectionState(Context c, NetSocket s) {
			this.context = c;
			this.socket = s;
		}
	}

	private final String login;
	private final String password;
	private final NetClient client;
	private final CompletableFuture<NetSocket> connectFuture;
	private final CompletableFuture<Void> closeFuture;
	private final CompletableFuture<ConnectionState> setupFuture;
	private final ImapRecordParser recordParser;
	private long tags = 0;
	private String selected;
	private Vertx vertx;

	public static VXStoreClient create(String host, int port, String login, String password) {
		CompletableFuture<VXStoreClient> cli = new CompletableFuture<>();
		Vertx vx = VertxPlatform.getVertx();
		vx.setTimer(1, tid -> {
			cli.complete(new VXStoreClient(vx, host, port, login, password));
		});
		return cli.join();
	}

	public VXStoreClient(Vertx vertx, String host, int port, String login, String password) {
		this.vertx = vertx;
		this.login = login;
		this.password = password;
		this.connectFuture = new CompletableFuture<NetSocket>();
		this.closeFuture = new CompletableFuture<Void>();
		this.setupFuture = new CompletableFuture<ConnectionState>();

		logger.info("Before netClient...");
		client = vertx.createNetClient();
		logger.info("NetClient created {}", client);
		client.setReuseAddress(true).setTCPNoDelay(true).setTCPKeepAlive(true).setUsePooledBuffers(true);
		recordParser = new ImapRecordParser();

		ImapProtocolListener<ConnectionState> bannerListener = new ImapProtocolListener<ConnectionState>(setupFuture) {

			public void onStatusResponse(ByteBuf banner) {
				logger.info("Got banner {}", banner.toString(StandardCharsets.US_ASCII));
				future.complete(new ConnectionState(vertx.currentContext(), connectFuture.join()));
			}

		};
		recordParser.listener(bannerListener);

		logger.info("Connecting...");
		client.connect(port, host, ar -> {
			if (ar.succeeded()) {
				NetSocket sock = ar.result();
				sock.dataHandler(recordParser);
				sock.closeHandler(v -> {
					logger.info("Closed.");
					closeFuture.complete(null);
				});
				logger.info("IMAP connection setup is complete, on {}", sock);
				connectFuture.complete(sock);
			} else {
				connectFuture.completeExceptionally(ar.cause());
			}
		});

	}

	public Vertx vertx() {
		return vertx;
	}

	/**
	 * @param cmdCons
	 *                     the consumers receives a {@link StringBuilder} with the
	 *                     IMAP tag + space char already added
	 * @param listener
	 *                     the listener receives the spurious responses (eg. * FETCH
	 *                     42...)
	 * @return
	 */
	private <T> CompletableFuture<ImapResponseStatus<T>> run(Consumer<StringBuilder> cmdCons,
			ImapProtocolListener<T> listener) {
		String tag = "V" + (++tags);
		TagListener<T> tl = new TagListener<>(tag, listener);
		setupFuture.thenAccept(conState -> {
			recordParser.listener(tl);
			conState.context.runOnContext(v -> {
				String fullCmd = tagged(tag, cmdCons);
				conState.socket.write(fullCmd);
			});
		});
		return tl.future;
	}

	private <T, W> CompletableFuture<ImapResponseStatus<T>> runLiteralAtEnd(Consumer<StringBuilder> cmdCons,
			ImapProtocolListener<T> listener, ReadStream<W> literal) {
		String tag = "V" + (++tags);

		Runnable onGoAhead = () -> {
			setupFuture.thenAccept(conState -> {
				conState.context.runOnContext(v -> {
					literal.endHandler(end -> {
						logger.info("Finished streaming literal.");
						conState.socket.write("\r\n");
					});
					Pump pump = Pump.createPump(literal, conState.socket);
					pump.start();
				});
			});
		};

		TagOrGoAheadListener<T> tl = new TagOrGoAheadListener<>(tag, listener, onGoAhead);
		setupFuture.thenAccept(conState -> {
			recordParser.listener(tl);
			conState.context.runOnContext(v -> {
				String fullCmd = tagged(tag, cmdCons);
				conState.socket.write(fullCmd);
			});
		});
		return tl.future;
	}

	private String tagged(String tag, Consumer<StringBuilder> sbCons) {
		StringBuilder sb = new StringBuilder(tag);
		sb.append(' ');
		sbCons.accept(sb);
		sb.append("\r\n");
		return sb.toString();
	}

	public CompletableFuture<ImapResponseStatus<Void>> login() {
		return run(sb -> {
			sb.append("LOGIN ").append(login).append(" \"").append(password).append("\"");
		}, ImapProtocolListener.noExpectations());
	}

	private static final CompletableFuture<ImapResponseStatus<SelectResponse>> SELECTED = CompletableFuture
			.completedFuture(new ImapResponseStatus<SelectResponse>(Status.Ok, new SelectResponse()));

	public void unselect() {
		selected = null;
	}

	public CompletableFuture<ImapResponseStatus<SelectResponse>> select(String mailbox) {
		if (selected != null && selected.equals(mailbox)) {
			return SELECTED;
		}
		String quotedUtf7 = UTF7Converter.encode(mailbox);
		CompletableFuture<ImapResponseStatus<SelectResponse>> promise = run(sb -> {
			sb.append("SELECT \"").append(quotedUtf7).append("\"");
		}, new SelectListener());
		return promise.thenCompose(resp -> {
			if (resp.status != Status.Ok) {
				logger.warn("Selection failed, cmd was: SELECT \"{}\"", quotedUtf7);
				selected = null;
			} else {
				selected = mailbox;
			}
			return promise;
		});
	}

	public CompletableFuture<ImapResponseStatus<FetchResponse>> fetch(long uid, String part) {
		return run(sb -> {
			sb.append("UID FETCH ").append(uid).append(" (BODY.PEEK[").append(part).append("])");
		}, new FetchListener());
	}

	public <W> CompletableFuture<ImapResponseStatus<AppendResponse>> append(String mailbox, Date receivedDate,
			Collection<String> flags, int streamSize, ReadStream<W> eml) {
		String quotedUtf7 = UTF7Converter.encode(mailbox);
		return runLiteralAtEnd(sb -> {
			sb.append("APPEND \"").append(quotedUtf7).append("\" ");
			AppendCommandHelper.flags(sb, flags);
			AppendCommandHelper.deliveryDate(sb, receivedDate);
			sb.append("{").append(streamSize).append("}");
		}, new AppendListener(), eml);
	}

	public CompletableFuture<Void> close() {
		return connectFuture.thenCompose(sock -> {
			sock.close();
			return closeFuture;
		});
	}

	public boolean isClosed() {
		return closeFuture.isDone();
	}

}
