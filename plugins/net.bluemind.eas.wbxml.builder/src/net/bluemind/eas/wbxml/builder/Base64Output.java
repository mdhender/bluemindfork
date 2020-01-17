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
package net.bluemind.eas.wbxml.builder;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.buffer.Buffer;
import net.bluemind.eas.wbxml.WbxmlOutput;

public class Base64Output extends WbxmlOutput {

	// This array is a lookup table that translates 6-bit positive integer index
	// values into their "Base64 Alphabet" equivalents as specified in Table 1
	// of RFC 2045.
	static final byte[] BASE64_TABLE = { 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P',
			'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k',
			'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5',
			'6', '7', '8', '9', '+', '/' };

	// Byte used to pad output.
	private static final byte BASE64_PAD = '=';

	// This set contains all base64 characters including the pad character. Used
	// solely to check if a line separator contains any of these characters.
	private static final Set<Byte> BASE64_CHARS = new HashSet<Byte>();

	static {
		for (byte b : BASE64_TABLE) {
			BASE64_CHARS.add(b);
		}
		BASE64_CHARS.add(BASE64_PAD);
	}

	// Mask used to extract 6 bits
	private static final int MASK_6BITS = 0x3f;

	private static final int ENCODED_BUFFER_SIZE = 2048;

	private final byte[] encoded = new byte[ENCODED_BUFFER_SIZE];;
	private int position = 0;

	private int data = 0;
	private int modulus = 0;

	private final WbxmlOutput out;
	private static final Logger logger = LoggerFactory.getLogger(Base64Output.class);

	public Base64Output(WbxmlOutput out) {
		this.out = out;
	}

	@Override
	public void write(int b) throws IOException {
		throw new IOException();
	}

	@Override
	public void write(byte[] data) throws IOException {
		throw new IOException();

	}

	@Override
	public void write(byte[] data, QueueDrained drained) {
		try {
			write0(data, 0, data.length, drained);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
			drained.drained();
		}
	}

	@Override
	public String end() {
		return null;
	}

	public void flush() {
		try {
			close0();
		} catch (IOException e) {
		}
	}

	private void write0(final byte[] buffer, final int from, final int to, final QueueDrained drained)
			throws IOException {
		if (logger.isDebugEnabled()) {
			logger.info("write0(buf, {}, {})", from, to);
		}
		for (int i = from; i < to; i++) {
			data = (data << 8) | (buffer[i] & 0xff);

			if (++modulus == 3) {
				modulus = 0;

				// encode data into 4 bytes

				if (encoded.length - position < 4) {
					flush0();
				}
				encoded[position++] = BASE64_TABLE[(data >> 18) & MASK_6BITS];
				encoded[position++] = BASE64_TABLE[(data >> 12) & MASK_6BITS];
				encoded[position++] = BASE64_TABLE[(data >> 6) & MASK_6BITS];
				encoded[position++] = BASE64_TABLE[data & MASK_6BITS];
			}
		}
		drained.drained();
	}

	private void close0() throws IOException {
		if (modulus != 0) {
			writePad();
		}

		flush0();
	}

	private void writePad() throws IOException {
		// encode data into 4 bytes

		if (encoded.length - position < 4) {
			flush0();
		}

		if (modulus == 1) {
			encoded[position++] = BASE64_TABLE[(data >> 2) & MASK_6BITS];
			encoded[position++] = BASE64_TABLE[(data << 4) & MASK_6BITS];
			encoded[position++] = BASE64_PAD;
			encoded[position++] = BASE64_PAD;
		} else {
			encoded[position++] = BASE64_TABLE[(data >> 10) & MASK_6BITS];
			encoded[position++] = BASE64_TABLE[(data >> 4) & MASK_6BITS];
			encoded[position++] = BASE64_TABLE[(data << 2) & MASK_6BITS];
			encoded[position++] = BASE64_PAD;
		}
	}

	private void flush0() {
		if (position > 0) {
			Buffer slide = Buffer.buffer().appendBytes(encoded, 0, position);
			try {
				out.write(slide.getBytes());
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
			}
			position = 0;
		}
	}

}
