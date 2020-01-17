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
package net.bluemind.imap.vertx.utils;

import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufProcessor;
import io.netty.buffer.Unpooled;
import io.vertx.core.Handler;
import net.bluemind.imap.vertx.BuffersStream;

public final class QuickParser {

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(QuickParser.class);

	public static final class TextChunk {
		public final ByteBuf buf;
		public final QuickParser parser;

		public TextChunk(ByteBuf buf, QuickParser parser) {
			this.buf = buf;
			this.parser = parser;
		}

	}

	public static final class BinStream implements BuffersStream {
		private Handler<ByteBuf> data;
		private Handler<Void> end;
		private final int expectedSize;

		private BinStream(int expectedSize) {
			this.expectedSize = expectedSize;
		}

		@Override
		public void dataHandler(Handler<ByteBuf> handler) {
			this.data = handler;
		}

		public BinStream write(ByteBuf b) {
			data.handle(b);
			return this;
		}

		public void end() {
			end.handle(null);
		}

		public int expectedSize() {
			return expectedSize;
		}

		@Override
		public void endHandler(Handler<Void> endHandler) {
			this.end = endHandler;
		}

	}

	private final ByteBuf delimiter;
	private final int delimSize;
	private boolean delimited;
	private final Consumer<TextChunk> texts;
	private final Consumer<BinStream> binary;
	private int remainingBinBytes;
	private ByteBuf remainder;
	private BinStream stream;
	private int handled;
	private final TextChunk emptyChunk;

	public QuickParser(ByteBuf delimiter, Consumer<TextChunk> texts, Consumer<BinStream> binary) {
		this.delimiter = delimiter.copy();
		this.delimSize = this.delimiter.readableBytes();
		this.delimited = true;
		this.texts = texts;
		this.binary = binary;
		this.remainder = Unpooled.buffer();
		this.handled = 0;
		this.emptyChunk = new TextChunk(Unpooled.EMPTY_BUFFER, this);
	}

	public void fixed(int size) {
		delimited = false;
		remainingBinBytes = size;
		stream = new BinStream(size);
		binary.accept(stream);
	}

	public void handle(ByteBuf inc) {
		if (remainder.readableBytes() == 0) {
			remainder = inc;
		} else {
			remainder.writeBytes(inc);
		}

		parseRemainder();

		if ((handled++ % 100) == 0) {
			remainder.discardSomeReadBytes();
		}

	}

	private void parseRemainder() {
		boolean loop = false;
		if (!delimited) {
			int avail = remainder.readableBytes();
			if (avail < remainingBinBytes) {
				ByteBuf read = remainder.readSlice(avail);
				stream.write(read);
				remainingBinBytes -= avail;
			} else if (avail == remainingBinBytes) {
				ByteBuf read = remainder.readSlice(avail);
				stream.write(read).end();
				remainingBinBytes = 0;
				delimited = true;
			} else {
				ByteBuf read = remainder.readSlice(remainingBinBytes);
				stream.write(read).end();
				remainingBinBytes = 0;
				delimited = true;
				loop = true;
			}
		} else {
			int idx = nextDelimited();
			if (idx >= 0) {
				loop = true;
			}
		}
		if (loop) {
			parseRemainder();
		}
	}

	private int nextDelimited() {
		// int delimIdx = indexOf(remainder);
		int delimIdx = newIndexOf(remainder);
		if (delimIdx == 0) {
			texts.accept(emptyChunk);
			remainder.skipBytes(delimSize);
		} else if (delimIdx > 0) {
			ByteBuf txtSlice = remainder.readSlice(delimIdx);
			texts.accept(new TextChunk(txtSlice, this));
			remainder.skipBytes(delimSize);
		}
		return delimIdx;
	}

	private int newIndexOf(ByteBuf haystack) {
		int ret = haystack.forEachByte(ByteBufProcessor.FIND_LF);
		if (ret > 0) {
			return ret - 1 - haystack.readerIndex();
		}
		return ret;
	}

}
