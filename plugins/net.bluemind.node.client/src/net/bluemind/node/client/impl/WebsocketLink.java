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
package net.bluemind.node.client.impl;

import java.util.Arrays;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.asynchttpclient.ws.WebSocket;
import org.asynchttpclient.ws.WebSocketListener;
import org.asynchttpclient.ws.WebSocketUpgradeHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.json.JsonObject;
import net.bluemind.node.api.ProcessHandler;
import net.bluemind.node.client.impl.ahc.JsonHelper;
import net.bluemind.node.shared.ExecRequest;

public class WebsocketLink {

	private static class NodeTextListener implements WebSocketListener {

		private final AtomicReference<WebSocket> wsRef;
		private final WebsocketLink link;
		private final CompletableFuture<Void> firstConnect;

		public NodeTextListener(AtomicReference<WebSocket> socketQueue, CompletableFuture<Void> firstConnect,
				WebsocketLink link) {
			this.wsRef = socketQueue;
			this.link = link;
			this.firstConnect = firstConnect;
		}

		@Override
		public void onOpen(WebSocket ws) {
			logger.debug("websocket opened: {}", ws);
			wsRef.set(ws);
			if (!firstConnect.isDone()) {
				firstConnect.complete(null);
			}
		}

		@Override
		public void onError(Throwable t) {
			logger.error("websocket error: {}", t.getMessage());
			retryLater();
		}

		@Override
		public void onClose(WebSocket websocket, int code, String reason) {
			logger.info("ws closed {} ({})", websocket, reason);
			retryLater();
		}

		private void retryLater() {
			if (!link.isSecure()) {
				return;
			}
			new Timer("ws-retry-" + System.nanoTime(), true).schedule(new TimerTask() {

				@Override
				public void run() {
					link.retry();
				}
			}, 1000L);
		}

		@Override
		public void onTextFrame(String message, boolean finalFragment, int rsv) {
			link.onMessage(message);
		}

	}

	private static class NodeSocketHandler extends WebSocketUpgradeHandler {

		public NodeSocketHandler(NodeTextListener listener) {
			super(Arrays.asList(listener));
		}

	}

	private static final Logger logger = LoggerFactory.getLogger(WebsocketLink.class);
	private static final AtomicLong wsIdGen = new AtomicLong();

	private final AtomicReference<WebSocket> webSocket;
	private final CompletableFuture<Void> firstConnect;
	private final Map<Long, ProcessHandler> execHandlers;
	private final NodeTextListener msgListener;
	private final NodeSocketHandler upgradeHandler;
	private final HostPortClient cli;

	public WebsocketLink(HostPortClient cli) {
		this.execHandlers = new ConcurrentHashMap<>();
		this.webSocket = new AtomicReference<>();
		this.firstConnect = new CompletableFuture<>();
		this.cli = cli;
		this.msgListener = new NodeTextListener(webSocket, firstConnect, this);
		this.upgradeHandler = new NodeSocketHandler(msgListener);

		retry();
		cli.setWebsocketLink(this);
	}

	public boolean isSecure() {
		return cli.isSSL();
	}

	public void retry() {
		String wsUrl = (cli.isSSL() ? "wss" : "ws") + "://" + cli.getHost() + ":" + cli.getPort() + "/ws";
		logger.info("Retry con to {}", wsUrl);
		cli.getClient().prepareGet(wsUrl).execute(upgradeHandler);
	}

	private void onMessage(String message) {
		logger.debug("onMessage: {}", message);
		JsonObject msg = new JsonObject(message);
		long rid = msg.getLong("ws-rid", 0L);
		ProcessHandler ph = execHandlers.get(rid);
		if (ph != null) {
			handleWebSocketFrame(rid, ph, msg);
		} else {
			String kind = msg.getString("kind");
			switch (kind) {
			case "node-start":
				logger.info("Node has restarted on {}, dropping {} task handlers.", cli.getHost(), execHandlers.size());
				execHandlers.forEach((runId, handler) -> {
					handler.log("Node has restarted.");
					handler.completed(1);
				});
				execHandlers.clear();
				break;
			case "notification":
				// does not exist yet
				break;
			default:
				logger.warn("Unknown frame kind {}", kind);
			}
		}
	}

	public void waitAvailable(long time, TimeUnit unit) {
		try {
			firstConnect.get(time, unit);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	private void handleWebSocketFrame(long rid, ProcessHandler ph, JsonObject payload) {
		if (logger.isDebugEnabled()) {
			logger.debug("WS - S: {}", payload.encodePrettily());
		}
		String kind = payload.getString("kind");
		switch (kind) {
		case "start":
			ph.starting(payload.getLong("task", 0L).toString());
			break;
		case "log":
			ph.log(payload.getString("log"));
			break;
		case "completion":
			ph.completed(payload.getInteger("exit", 0));
			execHandlers.remove(rid);
			break;
		default:
			logger.warn("Unknown frame kind {}", kind);
			break;
		}

	}

	public void startWsAction(ExecRequest wsReq, ProcessHandler ph) {
		WebSocket ws = webSocket.get();
		if (ws == null) {
			logger.error("Error command as websocket is missing");
			ph.completed(1);
		} else {
			if (!ws.isOpen()) {
				logger.error("Rejecting command as websocket is closed.");
				ph.completed(1);
			} else {
				long rid = wsIdGen.incrementAndGet();
				execHandlers.put(rid, ph);
				ws.sendTextFrame(JsonHelper.toJson(wsReq, rid), true, 0);
			}
		}

	}

}
