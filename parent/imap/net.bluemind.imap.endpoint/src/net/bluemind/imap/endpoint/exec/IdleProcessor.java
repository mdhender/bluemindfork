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
package net.bluemind.imap.endpoint.exec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.WriteStream;
import net.bluemind.imap.endpoint.ImapContext;
import net.bluemind.imap.endpoint.SessionState;
import net.bluemind.imap.endpoint.cmd.IdleCommand;
import net.bluemind.imap.endpoint.driver.IdleToken;
import net.bluemind.lib.vertx.Result;

public class IdleProcessor extends SelectedStateCommandProcessor<IdleCommand> {

	private static final Logger logger = LoggerFactory.getLogger(IdleProcessor.class);

	@Override
	public void checkedOperation(IdleCommand command, ImapContext ctx, Handler<AsyncResult<Void>> completed) {
		ctx.idlingTag(command.raw().tag());
		ctx.state(SessionState.IDLING);

		IdleWriteStream output = new IdleWriteStream(ctx);
		ctx.mailbox().idleMonitor(ctx.selected(), output);
		logger.info("Monitoring {}", ctx.selected().folder);
		ctx.write("+ idling\r\n");
		completed.handle(Result.success());
	}

	private static class IdleWriteStream implements WriteStream<IdleToken> {

		private ImapContext ctx;

		public IdleWriteStream(ImapContext ctx) {
			this.ctx = ctx;
		}

		private Buffer toBuffer(IdleToken tok) {
			Buffer buf = Buffer.buffer();
			buf.appendString("* " + tok.count + " " + tok.kind + "\r\n");
			return buf;
		}

		@Override
		public WriteStream<IdleToken> exceptionHandler(Handler<Throwable> handler) {
			return this;
		}

		@Override
		public Future<Void> write(IdleToken data) {
			return ctx.write(toBuffer(data));
		}

		@Override
		public void write(IdleToken data, Handler<AsyncResult<Void>> handler) {
			ctx.write(toBuffer(data), handler);
		}

		@Override
		public void end(Handler<AsyncResult<Void>> handler) {
			handler.handle(Result.success());
		}

		@Override
		public WriteStream<IdleToken> setWriteQueueMaxSize(int maxSize) {
			return this;
		}

		@Override
		public boolean writeQueueFull() {
			return ctx.socket().writeQueueFull();
		}

		@Override
		public WriteStream<IdleToken> drainHandler(Handler<Void> handler) {
			return this;
		}

	}

	@Override
	public Class<IdleCommand> handledType() {
		return IdleCommand.class;
	}

}
