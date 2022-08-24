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

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageProducer;
import io.vertx.core.net.NetSocket;
import net.bluemind.imap.endpoint.cmd.RawImapCommand;
import net.bluemind.imap.endpoint.driver.MailboxConnection;
import net.bluemind.imap.endpoint.driver.SelectedFolder;
import net.bluemind.imap.endpoint.events.EventNexus;

public class ImapContext {

	private static final Logger logger = LoggerFactory.getLogger(ImapContext.class);

	private final NetSocket ns;
	private final Vertx vertx;
	private final EventNexus nexus;
	private final String logConnectionId;

	private SessionState state;
	private MailboxConnection mailbox;
	private Map<String, String> clientId;

	private final MessageProducer<Buffer> sender;

	private SelectedFolder selected;

	private String idlingTag;

	public ImapContext(Vertx vertx, NetSocket ns, EventNexus nexus) {
		this.vertx = vertx;
		this.ns = ns;
		this.nexus = nexus;
		this.state = SessionState.NOT_AUTHENTICATED;
		this.clientId = Collections.emptyMap();
		this.sender = vertx.eventBus().sender(ns.writeHandlerID());
		this.logConnectionId = ns.writeHandlerID().replace("__vertx.net.", "").replace("-", "");
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
		if (logger.isInfoEnabled()) {
			logger.info("[{}] C: {} {}", logConnectionId, event.tag(), event.cmd());
		}
	}

	/**
	 * @see {@link #writePromise(String)}
	 * @param resp the imap response to write
	 * @return
	 */
	public Future<Void> write(Buffer b) {
		if (logger.isInfoEnabled()) {
			logger.info("[{}] S: {}", logConnectionId, b.toString(StandardCharsets.US_ASCII).replaceAll("\r\n$", ""));
		}
		return sender.write(b);
	}

	public void write(Buffer b, Handler<AsyncResult<Void>> v) {
		sender.write(b, v);
	}

	public CompletableFuture<Void> writePromise(String resp) {
		if (logger.isInfoEnabled()) {
			logger.info("[{}] S: {}", logConnectionId, resp.replaceAll("\r\n$", ""));
		}
		return sender.write(Buffer.buffer(resp)).toCompletionStage().toCompletableFuture();
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

	public void selected(SelectedFolder f) {
		this.selected = f;
	}

	public SelectedFolder selected() {
		return selected;
	}

	public void state(SessionState state) {
		this.state = state;
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
		return MoreObjects.toStringHelper(ImapContext.class).add("con", mailbox).toString();
	}

}