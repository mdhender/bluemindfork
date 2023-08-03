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
package net.bluemind.imap.endpoint;

import java.nio.channels.ClosedChannelException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.MessageProducer;
import io.vertx.core.net.NetSocket;
import net.bluemind.common.vertx.contextlogging.ContextualData;
import net.bluemind.imap.endpoint.cmd.RawImapCommand;
import net.bluemind.imap.endpoint.driver.Drivers;
import net.bluemind.imap.endpoint.driver.MailboxConnection;
import net.bluemind.imap.endpoint.driver.SelectedFolder;
import net.bluemind.imap.endpoint.events.EventNexus;
import net.bluemind.imap.endpoint.ratelimiter.ThroughputLimiterRegistry;
import net.bluemind.lib.vertx.ContextNetSocket;

public class ImapContext {

	private static final Logger logger = LoggerFactory.getLogger(ImapContext.class);
	private static final Logger rawLogger = LoggerFactory.getLogger("net.bluemind.imap.endpoint_raw");

	private final ContextNetSocket ns;
	private final Vertx vertx;
	private final EventNexus nexus;
	private final String logConnectionId;
	private final ThroughputLimiterRegistry throughputLimiterRegistry;

	private SessionState state;
	private MailboxConnection mailbox;
	private Map<String, String> clientId;

	private final MessageProducer<Buffer> sender;

	private SelectedFolder selected;

	private String idlingTag;

	public final Context vertxContext;

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

	public ImapContext(Vertx vertx, Context vertxContext, ContextNetSocket ns, EventNexus nexus) {
		this.vertx = vertx;
		this.vertxContext = vertxContext;
		this.ns = ns;
		this.nexus = nexus;
		this.state = SessionState.NOT_AUTHENTICATED;
		this.clientId = Collections.emptyMap();
		this.sender = new ContextProducer(ns);
		this.logConnectionId = ns.writeHandlerID().replace("__vertx.net.", "").replace("-", "");
		ContextualData.put("endpoint", "imap");
		this.throughputLimiterRegistry = ThroughputLimiterRegistry.get(Drivers.activeDriver().maxLiteralSize());
	}

	public Vertx vertx() {
		return vertx;
	}

	public MessageProducer<Buffer> sender() {
		return sender;
	}

	public NetSocket socket() {
		return ns;
	}

	public void sendContinuation() {
		write(Buffer.buffer("+ OK\r\n"));
	}

	/**
	 * @see {@link #writePromise(String)}
	 * @param resp the imap response to write
	 */
	public void write(String resp) {
		write(Buffer.buffer(resp));
	}

	public void clientCommand(RawImapCommand event) {
		if (logger.isDebugEnabled()) {
			logger.debug("[{}] C: {} {}", logConnectionId, event.tag(), event.cmd());
		}
		if (rawLogger.isDebugEnabled()) {
			rawLogger.debug("< {} {}\n", event.tag(), event.cmd().stripTrailing());
		}
	}

	/**
	 * @see {@link #writePromise(String)}
	 * @param resp the imap response to write
	 * @return
	 */
	public Future<Void> write(Buffer b) {
		return sender.write(b).onSuccess(f -> logRespBuffer(b)).onFailure(t -> {
			if (!(t instanceof ClosedChannelException)) {
				logger.error("Unable to send {} bytes", b.length(), t);
			}
		});
	}

	private void logRespBuffer(Buffer b) {
		if (logger.isDebugEnabled()) {
			logger.debug("[{}] S: {}", logConnectionId,
					truncate(b.toString(StandardCharsets.US_ASCII)).replaceAll("\r\n$", ""));
		}
		if (rawLogger.isDebugEnabled()) {
			rawLogger.debug("> {}", b.toString(StandardCharsets.US_ASCII).stripTrailing());
		}
	}

	private String truncate(String string) {
		if (string.length() > 256) {
			return string.substring(0, 256) + "...(truncated)";
		}
		return string;
	}

	public void write(Buffer b, Handler<AsyncResult<Void>> v) {
		sender.write(b, ar -> {
			if (ar.succeeded()) {
				logRespBuffer(b);
			}
			v.handle(ar);
		});
	}

	public CompletableFuture<Void> writePromise(String resp) {
		return sender.write(Buffer.buffer(resp)).onSuccess(f -> {
			if (logger.isDebugEnabled()) {
				logger.debug("[{}] S: {}", logConnectionId, truncate(resp).replaceAll("\r\n$", ""));
			}
			if (rawLogger.isDebugEnabled()) {
				rawLogger.debug("> {}", resp.stripTrailing());
			}
		}).toCompletionStage().toCompletableFuture();
	}

	public EventNexus nexus() {
		return nexus;
	}

	public void close() {
		ns.close();
	}

	public void mailbox(MailboxConnection connection) {
		this.mailbox = connection;
	}

	public MailboxConnection mailbox() {
		return mailbox;
	}

	public ThroughputLimiterRegistry throughputLimiterRegistry() {
		return this.throughputLimiterRegistry;
	}

	public void selected(SelectedFolder f) {
		this.selected = f;
	}

	public SelectedFolder selected() {
		return selected;
	}

	public void state(SessionState state) {
		this.state = state;
		if (state.equals(SessionState.AUTHENTICATED)) {
			ContextualData.put("user", mailbox.logId());
		}
		nexus.dispatchStateChanged(state);
	}

	public SessionState state() {
		return state;
	}

	public void clientId(Map<String, String> clientId) {
		this.clientId = clientId;
	}

	public Map<String, String> clientId() {
		return clientId;
	}

	public void idlingTag(String tag) {
		this.idlingTag = tag;
	}

	public String idlingTag() {
		return idlingTag;
	}

	@Override
	public String toString() {
		ToStringHelper build = MoreObjects.toStringHelper(ImapContext.class).add("con", mailbox);
		if (clientId != null && clientId.containsKey("name")) {
			String id = clientId.get("name") + "/" + clientId.get("version");
			build.add("id", id);
		}
		return build.toString();
	}

}