/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2024
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
package net.bluemind.imap.vt.cmd;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.buffer.Buffer;
import net.bluemind.imap.vt.ClientFault;
import net.bluemind.imap.vt.dto.IdleContext;
import net.bluemind.imap.vt.dto.IdleListener;
import net.bluemind.imap.vt.parsing.IncomingChunk;

public class IdleCommand {

	private static final Logger logger = LoggerFactory.getLogger(IdleCommand.class);
	private static final byte[] IDLE = "IDLE\r\n".getBytes();
	private static final byte[] DONE = "DONE\r\n".getBytes();

	private final CommandContext ctx;
	private final IdleListener listener;

	public IdleCommand(CommandContext ctx, IdleListener listener) {
		this.ctx = ctx;
		this.listener = listener;
	}

	public IdleContext execute() throws IOException {
		Buffer buf = Buffer.buffer();
		String tag = ctx.tagProd().nextTag() + " ";

		buf.appendBytes(tag.getBytes());
		buf.appendBytes(IDLE);

		ctx.out().write(buf.getBytes());

		InternalIdleContext idleCtx = new InternalIdleContext();
		Thread.ofVirtual().name("imap-idle").start(() -> untilTag(tag, idleCtx));
		idleCtx.idlingProm.join();
		return idleCtx;
	}

	private class InternalIdleContext implements IdleContext {

		private CompletableFuture<Void> doneProm = new CompletableFuture<>();
		private CompletableFuture<Void> idlingProm = new CompletableFuture<>();

		@Override
		public void done() {
			try {
				ctx.out().write(DONE);
			} catch (IOException e) {
				throw new ClientFault(e);
			}
		}

		@Override
		public void join() {
			doneProm.join();
		}

	}

	private void untilTag(String tag, InternalIdleContext idleCtx) {
		try {
			do {
				IncomingChunk chunk = ctx.pending().poll(1, TimeUnit.SECONDS);
				if (chunk != null) {
					if (chunk.tagged(tag)) {
						idleCtx.doneProm.complete(null);
						break;
					} else {
						String t = chunk.pieces().getFirst().txt();
						if (t.startsWith("* ")) {
							listener.onEvent(idleCtx, () -> t);
						} else if (t.startsWith("+")) {
							// continuation
							logger.info("IDLING {}", t);
							idleCtx.idlingProm.complete(null);
						}
					}
				}
			} while (true);
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
		}
	}

}
