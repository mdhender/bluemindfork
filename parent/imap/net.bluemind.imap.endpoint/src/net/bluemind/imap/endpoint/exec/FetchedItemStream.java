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

import java.nio.charset.StandardCharsets;
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
import net.bluemind.imap.endpoint.driver.FetchedItem;
import net.bluemind.imap.endpoint.driver.MailPart;
import net.bluemind.lib.vertx.Result;

public class FetchedItemStream implements WriteStream<FetchedItem> {

	private static final Logger logger = LoggerFactory.getLogger(FetchedItemStream.class);

	private final NetSocket socket;
	private final MessageProducer<Buffer> sender;
	private final List<MailPart> spec;
	private final String why;
	private int writeCnt = 0;

	private volatile boolean ended;

	public FetchedItemStream(ImapContext ctx, String why, List<MailPart> fetchSpec) {
		this.socket = ctx.socket();
		this.sender = ctx.sender();
		this.spec = fetchSpec;
		this.why = why;
	}

	@Override
	public String toString() {
		return "fetchStream{%s}".formatted(why);
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
		if (!ended) {
			ended = true;
			logger.debug("ending fetch after {} write(s)", writeCnt);
			handler.handle(Result.success());
		}
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

	private Buffer ascii(Buffer b, String s) {
		return b.appendBytes(s.getBytes(StandardCharsets.US_ASCII));
	}

	private static final byte[] FETCH_END = ")\r\n".getBytes(StandardCharsets.US_ASCII);

	private Buffer toBuffer(FetchedItem fetched) {
		Buffer b = Buffer.buffer(8192);
		ascii(b, "* " + fetched.seq + " FETCH (UID " + fetched.uid);

		for (MailPart mp : spec) {
			String k = mp.toString();
			ByteBuf v = fetched.properties.get(k);
			if (v != null) {
				b.appendBytes(mp.outName()).appendBuffer(Buffer.buffer(v));
			}
		}
		b.appendBytes(FETCH_END);
		return b;
	}

}