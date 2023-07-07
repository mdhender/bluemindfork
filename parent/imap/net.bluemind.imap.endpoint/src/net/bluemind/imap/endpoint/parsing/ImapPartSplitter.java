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
package net.bluemind.imap.endpoint.parsing;

import io.netty.buffer.ByteBuf;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.parsetools.RecordParser;
import net.bluemind.configfile.imap.ImapConfig;
import net.bluemind.imap.endpoint.EndpointConfig;
import net.bluemind.imap.endpoint.ImapContext;
import net.bluemind.imap.endpoint.ImapMetricsHolder;
import net.bluemind.imap.endpoint.parsing.LiteralSize.LiteralLength;
import net.bluemind.imap.endpoint.ratelimiter.ThroughputLimiter.LimiterResult;

public class ImapPartSplitter implements Handler<Buffer> {

	private static final int IMAP_LITERAL_CHUNK_SIZE = (int) EndpointConfig.get().getMemorySize(ImapConfig.CHUNK_SIZE)
			.toBytes();

	private final ImapRequestParser parser;
	private final RecordParser split;
	private final ImapContext ctx;
	private final ImapMetricsHolder metricsHolder;

	private int literalBytes;
	private State state;
	private boolean stopped;

	private enum State {
		COMMAND, LITERAL;
	}

	public ImapPartSplitter(ImapContext ctx, ImapRequestParser parser, ImapMetricsHolder metricsHolder) {
		this.parser = parser;
		this.ctx = ctx;
		this.metricsHolder = metricsHolder;
		this.split = RecordParser.newDelimited("\r\n");
		split.handler(this::splittedChunk);
		this.state = State.COMMAND;
	}

	@Override
	public void handle(Buffer event) {
		if (stopped) {
			return;
		}

		ctx.throughputLimiterRegistry().get(ctx.mailbox()).limit(ctx, event.length()) //
				.thenAccept(limiterResult -> {
					if (ctx.mailbox() != null) {
						metricsHolder.monitorBufferStatus(ctx.mailbox().login(), limiterResult);
					}
					split.handle(event);
				});
	}

	private void splittedChunk(Buffer chunk) {
		if (stopped) {
			return;
		}
		ByteBuf buf = chunk.getByteBuf();
		if (state == State.LITERAL) {
			processLiteral(buf);
		} else {
			processCommandText(buf);
		}
	}

	private void processCommandText(ByteBuf buf) {
		LiteralLength lit = LiteralSize.of(buf);
		if (lit.total() > 0) {
			if (ctx.mailbox() != null && lit.total() > ctx.mailbox().maxLiteralSize()) {
				metricsHolder.monitorBufferStatus(ctx.mailbox().login(), LimiterResult.literalOverflow(lit.total()));
				ctx.writePromise(
						"* ALERT too much data (" + lit.total() + " > " + ctx.mailbox().maxLiteralSize() + ")\r\n")
						.thenAccept(v -> ctx.socket().close());
				stopped = true;
				return;
			}

			if (!lit.inline()) {
				ctx.sendContinuation();
			}
			parser.parse(Part.followedByLiteral(buf));
			literalBytes = lit.total();
			split.fixedSizeMode(Math.min(IMAP_LITERAL_CHUNK_SIZE, literalBytes));
			state = State.LITERAL;
		} else {
			parser.parse(Part.endOfCommand(buf));
		}
	}

	private void processLiteral(ByteBuf buf) {
		if (buf.readableBytes() == literalBytes) {
			parser.parse(Part.literalChunk(buf, 0));
			split.delimitedMode("\r\n");
			state = State.COMMAND;
		} else if (buf.readableBytes() < literalBytes) {
			int rem = literalBytes - buf.readableBytes();
			parser.parse(Part.literalChunk(buf, rem));
			literalBytes = rem;
			split.fixedSizeMode(Math.min(IMAP_LITERAL_CHUNK_SIZE, literalBytes));
		}
	}

}
