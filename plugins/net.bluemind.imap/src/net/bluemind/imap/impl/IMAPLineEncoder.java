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

import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoderAdapter;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IMAPLineEncoder extends ProtocolEncoderAdapter {

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(IMAPLineEncoder.class);

	private static final String ENCODER = IMAPLineEncoder.class.getName() + ".encoder";

	private final Charset charset;

	private final byte[] CRLF = "\r\n".getBytes();

	public IMAPLineEncoder() {
		this.charset = Charset.forName("US-ASCII");
	}

	public void encode(IoSession session, Object message, ProtocolEncoderOutput out) throws Exception {
		CharsetEncoder encoder = (CharsetEncoder) session.getAttribute(ENCODER);
		if (encoder == null) {
			encoder = charset.newEncoder();
			session.setAttribute(ENCODER, encoder);
		}
		if (message instanceof String) {
			String value = message.toString();
			IoBuffer buf = IoBuffer.allocate(value.length() + 2);
			buf.putString(value, encoder).put(CRLF);
			buf.flip();
			out.write(buf);
		} else if (message instanceof byte[]) {
			byte[] value = (byte[]) message;
			IoBuffer buf = IoBuffer.allocate(value.length + 2);
			buf.put(value).put(CRLF).flip();
			out.write(buf);
		}
	}

	public void dispose() throws Exception {
	}
}
