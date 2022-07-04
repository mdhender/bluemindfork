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

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageProducer;
import io.vertx.core.net.NetSocket;
import io.vertx.core.streams.WriteStream;
import net.bluemind.imap.endpoint.ImapContext;
import net.bluemind.imap.endpoint.cmd.UidFetchCommand;
import net.bluemind.imap.endpoint.driver.FetchedItem;
import net.bluemind.imap.endpoint.driver.MailPart;
import net.bluemind.imap.endpoint.driver.MailboxConnection;
import net.bluemind.lib.vertx.Result;

public class UidFetchProcessor extends SelectedStateCommandProcessor<UidFetchCommand> {

	private static final Logger logger = LoggerFactory.getLogger(UidFetchProcessor.class);

	@Override
	public Class<UidFetchCommand> handledType() {
		return UidFetchCommand.class;
	}

	@Override
	protected void checkedOperation(UidFetchCommand command, ImapContext ctx, Handler<AsyncResult<Void>> completed) {
		MailboxConnection con = ctx.mailbox();

		FetchedItemStream output = new FetchedItemStream(ctx, command.fetchSpec());

		con.fetch(ctx.selected(), command.idset(), command.fetchSpec(), output).thenAccept(v -> {
			ctx.write(command.raw().tag() + " OK Completed\r\n");
			completed.handle(Result.success());
		}).exceptionally(t -> {
			completed.handle(Result.fail(t));
			return null;
		});
	}

	public static class FetchedItemStream implements WriteStream<FetchedItem> {

		private final NetSocket socket;
		private final MessageProducer<Buffer> sender;
		private List<MailPart> spec;
		private int writeCnt = 0;

		public FetchedItemStream(ImapContext ctx, List<MailPart> fetchSpec) {
			this.socket = ctx.socket();
			this.sender = ctx.sender();
			this.spec = fetchSpec;
		}

		@Override
		public WriteStream<FetchedItem> exceptionHandler(Handler<Throwable> handler) {
			// error does not exist
			return this;
		}

		@Override
		public Future<Void> write(FetchedItem data) {
			writeCnt++;
			return sender.write(toBuffer(data));
		}

		@Override
		public void write(FetchedItem data, Handler<AsyncResult<Void>> handler) {
			writeCnt++;
			sender.write(toBuffer(data), handler::handle);
		}

		@Override
		public void end(Handler<AsyncResult<Void>> handler) {
			logger.info("ending fetch after {} write(s)", writeCnt);
			handler.handle(Result.success());
		}

		@Override
		public WriteStream<FetchedItem> setWriteQueueMaxSize(int maxSize) {
			return this;
		}

		@Override
		public boolean writeQueueFull() {
			return socket.writeQueueFull();
		}

		@Override
		public WriteStream<FetchedItem> drainHandler(Handler<Void> handler) {
			socket.drainHandler(handler::handle);
			return this;
		}

		private Buffer toBuffer(FetchedItem fetched) {
			Buffer b = Buffer.buffer();
			b.appendString("* " + fetched.seq + " FETCH (UID " + fetched.uid);

			for (MailPart mp : spec) {
				String k = mp.toString();
				ByteBuf v = fetched.properties.get(k);
				if (v != null) {
					b.appendByte((byte) ' ');
					b.appendString(mp.outName()).appendByte((byte) ' ');
					b.appendBuffer(Buffer.buffer(v));
				}
			}
			b.appendString(")\r\n");
			return b;
		}

	}

}
