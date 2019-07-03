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
package net.bluemind.imap.vertx.impl;

import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.bluemind.imap.vertx.ImapProtocolListener;
import net.bluemind.imap.vertx.utils.LiteralSize;
import net.bluemind.imap.vertx.utils.QuickParser;
import net.bluemind.imap.vertx.utils.QuickParser.BinStream;
import net.bluemind.imap.vertx.utils.QuickParser.TextChunk;

public class ImapRecordParser implements Handler<Buffer> {
	static final Logger logger = LoggerFactory.getLogger(ImapRecordParser.class);

	private final QuickParser quickParser;

	private ImapProtocolListener<?> listener;

	private static final byte[] CRLF = "\r\n".getBytes();

	public ImapRecordParser() {
		Consumer<BinStream> streams = stream -> {
			ByteBuf complete = Unpooled.buffer(stream.expectedSize());
			stream.dataHandler(b -> {
				complete.writeBytes(b);
			});
			stream.endHandler(v -> {
				if (logger.isDebugEnabled()) {
					logger.debug("Got {}byte(s)", stream.expectedSize());
				}
				listener.onBinary(complete);
			});
		};
		Consumer<TextChunk> texts = textChunk -> {
			int size = LiteralSize.of(textChunk.buf);
			listener.onStatusResponse(textChunk.buf);
			if (size > 0) {
				textChunk.parser.fixed(size);
			}
		};
		this.quickParser = new QuickParser(Unpooled.wrappedBuffer(CRLF), texts, streams);
	}

	public void listener(ImapProtocolListener<?> listener) {
		this.listener = listener;
	}

	@Override
	public void handle(Buffer event) {
		// logger.info("S: {}", event);
		quickParser.handle(event.getByteBuf());
	}

}
