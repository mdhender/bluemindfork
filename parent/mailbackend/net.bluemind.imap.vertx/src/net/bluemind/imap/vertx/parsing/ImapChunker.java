/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.imap.vertx.parsing;

import java.nio.charset.StandardCharsets;

import io.netty.buffer.ByteBuf;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.parsetools.RecordParser;
import io.vertx.core.streams.ReadStream;
import net.bluemind.imap.vertx.parsing.ImapChunker.ImapChunk;
import net.bluemind.imap.vertx.utils.LiteralSize;

public class ImapChunker implements ReadStream<ImapChunk> {

	public enum Type {
		Text, Literal;
	}

	public static class ImapChunk {
		private final Type type;
		private final Buffer payload;
		private boolean lastChunk;

		public ImapChunk(Type t, Buffer p, boolean lastChunk) {
			this.type = t;
			this.payload = p;
			this.lastChunk = lastChunk;
		}

		public Type type() {
			return type;
		}

		public Buffer buffer() {
			return payload;
		}

		public boolean isLastChunk() {
			return lastChunk;
		}

		@Override
		public String toString() {
			if (type == Type.Text) {
				return "S: '" + payload.toString(StandardCharsets.US_ASCII) + "'";
			} else {
				return "S: literal [" + payload.length() + "byte(s)]";
			}
		}
	}

	private Type currentType;
	private final ReadStream<Buffer> upstream;
	private int remainingInLiteral;

	public ImapChunker(ReadStream<Buffer> upstream) {
		this.currentType = Type.Text;
		this.upstream = upstream;
	}

	@Override
	public ImapChunker exceptionHandler(Handler<Throwable> handler) {
		upstream.exceptionHandler(handler);
		return this;
	}

	@Override
	public ImapChunker handler(Handler<ImapChunk> handler) {
		RecordParser parser = RecordParser.newDelimited("\r\n");
		parser.handler(buf -> {
			remainingInLiteral = remainingInLiteral - buf.length();
			ImapChunk chunk = new ImapChunk(currentType, buf, remainingInLiteral <= 0);
			handler.handle(chunk);
			if (currentType == Type.Text) {
				ByteBuf bb = buf.getByteBuf();
				int size = LiteralSize.of(bb);
				if (size > 0) {
					currentType = Type.Literal;
					remainingInLiteral = size;
					parser.fixedSizeMode(Math.min(remainingInLiteral, 512 * 1024));
				}
			} else {
				if (remainingInLiteral == 0) {
					currentType = Type.Text;
					parser.delimitedMode("\r\n");
				} else {
					parser.fixedSizeMode(Math.min(remainingInLiteral, 512 * 1024));
				}
			}
		});
		upstream.handler(parser::handle);

		return this;
	}

	@Override
	public ImapChunker pause() {
		upstream.pause();
		return this;
	}

	@Override
	public ImapChunker resume() {
		upstream.resume();
		return this;
	}

	@Override
	public ImapChunker fetch(long amount) {
		upstream.fetch(amount);
		return this;
	}

	@Override
	public ImapChunker endHandler(Handler<Void> endHandler) {
		upstream.endHandler(endHandler);
		return this;
	}

}
