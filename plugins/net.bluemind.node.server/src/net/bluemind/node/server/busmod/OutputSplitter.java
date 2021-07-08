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
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.node.server.busmod;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.ByteProcessor;
import io.vertx.core.Handler;

public class OutputSplitter {

	/**
	 * Netty max frame length on websockets is 10240, so we default to something
	 * smaller
	 */
	public static final int DEFAULT_MAX_FRAME_SIZE = 8 * 1024;

	public static class Line {
		public final String log;
		public final boolean continued;

		public Line(String log, boolean continued) {
			this.log = log;
			this.continued = continued;
		}

	}

	@SuppressWarnings("serial")
	public static class SplitException extends RuntimeException {

		public SplitException(String string) {
			super(string);
		}

	}

	private final ByteBuf accum;
	private final Handler<Line> lineHandler;
	private final int maxFrameSize;

	public OutputSplitter(Handler<Line> lineHandler) {
		this(lineHandler, DEFAULT_MAX_FRAME_SIZE);
	}

	public OutputSplitter(Handler<Line> lineHandler, int maxFrameSize) {
		if (maxFrameSize < 5) {
			throw new SplitException("maxFrameSize must be >= 5 ");
		}
		this.maxFrameSize = maxFrameSize;
		this.accum = Unpooled.buffer();
		this.lineHandler = lineHandler;
	}

	public void end() {
		if (accum.readableBytes() > 0) {
			lineHandler.handle(new Line(accum.toString(StandardCharsets.UTF_8), false));
		}
	}

	public OutputSplitter write(ByteBuf outputChunk) {
		int index;
		do {
			int startIndex = outputChunk.readerIndex();
			index = outputChunk.forEachByte(ByteProcessor.FIND_LF);

			if (index == -1) {
				// no \n in the new chunk
				accum.writeBytes(outputChunk);

				if (accum.readableBytes() > maxFrameSize) {
					int len = maxFrameSize;

					CharSequence charSeq = buildValidCharacters(accum, len, StandardCharsets.UTF_8);
					Line l = new Line(charSeq.toString(), true);
					lineHandler.handle(l);
				}

			} else {

				ByteBuf slicetoLF = outputChunk.readSlice(index - startIndex);
				// skip \n
				outputChunk.skipBytes(1);
				ByteBuf upToLF = accum.readableBytes() > 0 ? Unpooled.copiedBuffer(accum, slicetoLF) : slicetoLF;
				CharSequence realLine = upToLF.readCharSequence(upToLF.readableBytes(), StandardCharsets.UTF_8);
				lineHandler.handle(new Line(realLine.toString(), false));
				accum.readerIndex(0).writerIndex(0);
			}
		} while (index != -1 || accum.readableBytes() > maxFrameSize);
		return this;
	}

	/**
	 * Similar to {@link ByteBuf#readCharSequence(int, Charset)} except that this
	 * one can read less than maxLen if the last bytes are not a complete utf-8
	 * character.
	 * 
	 * @param b
	 * @param maxLen
	 * @param cs
	 * @return
	 */
	private CharSequence buildValidCharacters(ByteBuf b, int maxLen, Charset cs) {
		CharsetDecoder dec = cs.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
				.onUnmappableCharacter(CodingErrorAction.REPORT).reset();
		int substract = 0;
		while (maxLen - substract > 0 && substract < 5) {
			byte[] tgt = new byte[maxLen - substract];
			b.markReaderIndex();
			b.readBytes(tgt);
			b.resetReaderIndex();
			ByteBuf forStrBuild = Unpooled.wrappedBuffer(tgt);
			ByteBuffer nioBuf = forStrBuild.nioBuffer();
			try {
				CharBuffer decoded = dec.decode(nioBuf);
				b.skipBytes(tgt.length);
				return decoded.toString();
			} catch (CharacterCodingException e) {
				substract++;
				dec.reset().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT);
			}
		}

		throw new SplitException("Unable to find valid characters with maxLen " + maxLen);
	}

}
