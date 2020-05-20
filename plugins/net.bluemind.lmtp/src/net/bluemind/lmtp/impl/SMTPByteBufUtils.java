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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.lmtp.impl;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.ByteProcessor;

public class SMTPByteBufUtils {

	private static final ByteBuf RETURN_DOUBLE_DOT = Unpooled.wrappedBuffer(new byte[] { '\n', '.', '.' });
	private static final ByteBuf RETURN_DOT = Unpooled.wrappedBuffer(new byte[] { '\n', '.' });

	private static class NewDotLineProcessor implements ByteProcessor {

		private enum State {
			LOOK_FOR_NEWLINE, //
			LOOK_FOR_DOT
		}

		private State state = State.LOOK_FOR_NEWLINE;

		@Override
		public boolean process(byte value) throws Exception {
			switch (state) {
			case LOOK_FOR_DOT:
				state = State.LOOK_FOR_NEWLINE;
				return value != '.';
			default:
			case LOOK_FOR_NEWLINE:
				if (value == '\n') {
					state = State.LOOK_FOR_DOT;
				}
				return true;
			}
		}

	}

	private static class NewDotDotLineProcessor implements ByteProcessor {

		private enum State {
			LOOK_FOR_NEWLINE, //
			LOOK_FOR_FIRST_DOT, LOOK_FOR_SECOND_DOT
		}

		private State state = State.LOOK_FOR_NEWLINE;

		@Override
		public boolean process(byte value) throws Exception {
			switch (state) {
			case LOOK_FOR_FIRST_DOT:
				if (value == '.') {
					state = State.LOOK_FOR_SECOND_DOT;
				} else {
					state = State.LOOK_FOR_NEWLINE;
				}
				return true;
			case LOOK_FOR_SECOND_DOT:
				state = State.LOOK_FOR_NEWLINE;
				return value != '.';
			default:
			case LOOK_FOR_NEWLINE:
				if (value == '\n') {
					state = State.LOOK_FOR_FIRST_DOT;
				}
				return true;
			}
		}

	}

	public static ByteBuf transformToSMTP(ByteBuf buf) {
		NewDotLineProcessor proc = new NewDotLineProcessor();

		buf = buf.duplicate();
		CompositeByteBuf transforme = Unpooled.compositeBuffer();
		while (buf.readableBytes() > 0) {
			int index = buf.forEachByte(proc);
			if (index != -1) {
				// make it relative to readerIndex
				index = index - buf.readerIndex();
			}

			if (index == -1 || // skip last character
					buf.readableBytes() - index == 1) {
				int lastSliceLength = buf.readableBytes();
				transforme.addComponent(buf.readSlice(lastSliceLength));
				transforme.writerIndex(transforme.writerIndex() + lastSliceLength);
			} else {
				transforme.addComponent(buf.readSlice(index - 1));
				transforme.writerIndex(transforme.writerIndex() + index - 1);
				// skip \n.
				buf.skipBytes(2);
				transforme.addComponent(RETURN_DOUBLE_DOT.duplicate());
				transforme.writerIndex(transforme.writerIndex() + RETURN_DOUBLE_DOT.readableBytes());
			}
		}

		return transforme.resetReaderIndex();

	}

	public static ByteBuf transformFromSMTP(ByteBuf buf) {
		NewDotDotLineProcessor proc = new NewDotDotLineProcessor();

		buf = buf.duplicate();
		CompositeByteBuf transforme = Unpooled.compositeBuffer();
		while (buf.readableBytes() > 0) {
			int index = buf.forEachByte(proc);
			if (index != -1) {
				// make it relative to readerIndex
				index = index - buf.readerIndex();
			}
			if (index == -1 || // skip last character
					buf.readableBytes() - index == 1) {
				int r = buf.readableBytes();
				transforme.addComponent(buf.readSlice(r));
				transforme.writerIndex(transforme.writerIndex() + r);
			} else {
				// - 3 => \n.., index == pos of last '.'
				transforme.addComponent(buf.readSlice(index - RETURN_DOUBLE_DOT.readableBytes() + 1));
				// skip \n..
				buf.skipBytes(RETURN_DOUBLE_DOT.readableBytes());
				transforme.writerIndex(transforme.writerIndex() + index - RETURN_DOUBLE_DOT.readableBytes() + 1);
				transforme.addComponent(RETURN_DOT.duplicate());
				transforme.writerIndex(transforme.writerIndex() + RETURN_DOT.readableBytes());
			}
		}

		return transforme.resetReaderIndex();
	}

}
