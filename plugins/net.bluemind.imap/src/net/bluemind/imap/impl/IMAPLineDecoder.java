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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.imap.impl;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;

import net.bluemind.common.io.FileBackedOutputStream;
import net.bluemind.imap.IMAPByteSource;

public final class IMAPLineDecoder implements ProtocolDecoder {

	private final static byte[] delimBuf = new byte[] { (byte) '\r', (byte) '\n' };

	private static final String CONTEXT = IMAPLineDecoder.class.getName() + ".context";

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(IMAPLineDecoder.class);

	public IMAPLineDecoder() {
	}

	@Override
	public void decode(IoSession session, IoBuffer in, ProtocolDecoderOutput out) throws Exception {

		ImapLineContext lc = (ImapLineContext) session.getAttribute(CONTEXT);
		if (lc == null) {
			lc = new ImapLineContext();
			session.setAttribute(CONTEXT, lc);
		}
		lc.decodeNormal(in, out);
	}

	@Override
	public void dispose(IoSession session) throws Exception {
		ImapLineContext lc = (ImapLineContext) session.removeAttribute(CONTEXT);
		if (lc != null) {
			lc.dispose();
		}
	}

	private class ImapLineContext {

		private final IoBuffer buf;

		private FileBackedOutputStream literalBuffer;
		private int expectedInLiteral;
		private int remainingInLiteral;
		private int matchCount = 0;

		private MinaIMAPMessage currentMessage;

		private ImapLineContext() {
			buf = IoBuffer.allocate(80).setAutoExpand(true).setAutoShrink(true);
		}

		public void dispose() {
			buf.free();
		}

		private boolean bufferFollows(String line) {
			int endPos = line.length() - 1;
			if (endPos < 0) {
				return false;
			}
			if (line.charAt(endPos) == '}') {
				endPos--;
				if (line.charAt(endPos) == '+') {
					endPos--;
				}
				int numberStart = endPos;
				for (; numberStart > 0 && numeric(line.charAt(numberStart)); numberStart--) {

				}
				String number = line.substring(numberStart + 1, endPos + 1);

				int literalSize = Integer.parseInt(number);

				literalBuffer = new FileBackedOutputStream(1024 * 1024, "imapline-decoder");
				expectedInLiteral = literalSize;
				remainingInLiteral = literalSize;

				return true;
			}
			return false;
		}

		private void decodeNormal(IoBuffer in, ProtocolDecoderOutput out) throws CharacterCodingException {

			if (literalBuffer != null) {
				// we are in a middle of a literal
				appendBuffer(in);
			}

			// Try to find a match
			int oldPos = in.position();
			int oldLimit = in.limit();
			while (in.hasRemaining()) {
				byte b = in.get();
				if (delimBuf[matchCount] == b) {
					matchCount++;
					if (matchCount == delimBuf.length) {
						// Found a match.
						int pos = in.position();
						in.limit(pos);
						in.position(oldPos);

						buf.put(in);
						buf.flip();
						int len = buf.limit() - matchCount;
						buf.limit(len);

						ByteBuffer bb = buf.buf();
						byte[] line = new byte[len];
						bb.get(line, 0, len);
						String lineAsString = new String(line);

						buf.clear();
						in.limit(oldLimit);
						in.position(pos);
						oldPos = pos;
						matchCount = 0;

						if (currentMessage != null) {
							// text after literal
							currentMessage.addLine(lineAsString);
						} else {
							currentMessage = new MinaIMAPMessage(lineAsString);
						}

						// a literal string follows?
						if (bufferFollows(lineAsString)) {
							appendBuffer(in);
						} else {
							out.write(currentMessage);
							currentMessage = null;
						}
					}
				} else {
					matchCount = 0;
				}
			}

			// Put remainder to buf.
			in.position(oldPos);
			buf.put(in);
			buf.shrink();
		}

		public void appendBuffer(IoBuffer in) {

			int arrivedDataSize = in.remaining();
			int toRead = Math.min(remainingInLiteral, arrivedDataSize);
			byte[] readSome = new byte[toRead];
			in.get(readSome);
			try {
				literalBuffer.write(readSome);
				remainingInLiteral = remainingInLiteral - toRead;
			} catch (IOException e) {
				Throwables.propagate(e);
			}

			if (remainingInLiteral == 0) {
				// we have read the literal,
				// add the buffer to the already built command
				currentMessage.addBuffer(IMAPByteSource.wrap(literalBuffer, expectedInLiteral));
				literalBuffer = null;
				expectedInLiteral = 0;
			}

		}

	}

	final static boolean numeric(char charAt) {
		// fast and ugly hack
		int x = ((int) charAt - (int) '0');
		return x >= 0 && x <= 9;
	}

	@Override
	public void finishDecode(IoSession session, ProtocolDecoderOutput out) throws Exception {
		// TODO Auto-generated method stub
	}

}
