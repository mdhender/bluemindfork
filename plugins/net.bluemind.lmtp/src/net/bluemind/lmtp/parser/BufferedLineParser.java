/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2016
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
package net.bluemind.lmtp.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.vertx.core.buffer.Buffer;

public class BufferedLineParser {

	private static final Logger logger = LoggerFactory.getLogger(BufferedLineParser.class);
	public static final Buffer NEED_MORE = Buffer.buffer();
	private CompositeByteBuf currentBuffer = Unpooled.compositeBuffer();
	private int pos;
	private int delimPos;
	private byte[] delim;

	public void feed(Buffer buffer) {
		logger.debug("feed with {}", buffer);
		ByteBuf bb = buffer.getByteBuf();
		currentBuffer.addComponent(bb);
		currentBuffer.writerIndex(currentBuffer.writerIndex() + bb.writerIndex());
	}

	public Buffer next() {
		if (currentBuffer == null || (pos >= currentBuffer.readableBytes())) {
			logger.debug("we need more");
			return NEED_MORE;
		}
		int len = currentBuffer.readableBytes();
		for (; pos < len; pos++) {
			if (currentBuffer.getByte(pos + currentBuffer.readerIndex()) == delim[delimPos]) {
				delimPos++;
				if (delimPos == delim.length) {
					Buffer ret = Buffer.buffer(currentBuffer.readSlice(pos + 1 - delim.length).copy());
					currentBuffer.skipBytes(delim.length);
					currentBuffer.discardReadBytes();
					pos = 0;
					delimPos = 0;
					return ret;
				}
			} else {
				if (delimPos > 0) {
					delimPos = 0;
					pos--;
				}
			}
		}
		return NEED_MORE;
	}

	public void setDelimitedMode(String string) {
		delim = string.getBytes();
		pos = 0;
		delimPos = 0;
	}
}
