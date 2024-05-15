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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.imap.endpoint;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Stopwatch;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import net.bluemind.common.vertx.contextlogging.ContextualData;
import net.bluemind.imap.endpoint.events.EventNexus;
import net.bluemind.imap.endpoint.events.StateChangeListener;
import net.bluemind.imap.endpoint.exec.ImapCommandHandler;
import net.bluemind.imap.endpoint.parsing.ImapPartSplitter;
import net.bluemind.imap.endpoint.parsing.ImapRequestParser;
import net.bluemind.lib.vertx.ContextNetSocket;

public class ImapSession implements StateChangeListener {

	private static final Logger logger = LoggerFactory.getLogger(ImapSession.class);

	private static final ByteBuf GREETING = Unpooled
			.unreleasableBuffer(Unpooled.directBuffer().writeBytes("* OK IMAP4 ready\r\n".getBytes()));

	public static ImapSession create(Vertx vertx, Context context, ContextNetSocket ns,
			ImapMetricsHolder metricsHolder) {
		return new ImapSession(vertx, context, ns, metricsHolder);
	}

	private final ImapContext ctx;
	private final Context vertxContext;
	private final Stopwatch startTime;

	public ImapSession(Vertx vertx, Context context, ContextNetSocket ns, ImapMetricsHolder metricsHolder) {
		vertxContext = context;
		EventNexus nexus = new EventNexus(ns.writeHandlerID(), vertx.eventBus());
		this.ctx = new ImapContext(vertx, vertxContext, ns, nexus);
		this.startTime = Stopwatch.createStarted();

		ImapCommandHandler exec = new ImapCommandHandler(ctx);
		ImapRequestParser parser = new ImapRequestParser(exec);
		ImapPartSplitter split = new ImapPartSplitter(ctx, parser, metricsHolder);

		nexus.addStateListener(this);

		ns.exceptionHandler(t -> {
			logger.error("ns {} failure: {}", ns, t.getMessage(), t);
			ns.close().onFailure(closeerr -> logger.warn("Failed to close {}: {}", ns, closeerr.getMessage()));
		});
		ns.handler(buffer -> {
			var mailbox = ctx.mailbox();
			if (mailbox != null) {
				ContextualData.put("user", mailbox.logId());
			}
			split.handle(buffer);
		});
		ns.closeHandler(v -> {
			ctx.close();
			parser.close();
			exec.close();
			nexus.close();
			logger.info("Connection closed after {}ms", startTime.elapsed(TimeUnit.MILLISECONDS));
		});
		ctx.write(Buffer.buffer(GREETING.duplicate()));
	}

	@Override
	public void stateChanged(SessionState newState) {
		// ok
	}

}
