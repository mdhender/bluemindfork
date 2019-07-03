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
package net.bluemind.imap.sieve;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderAdapter;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolEncoderAdapter;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;

public final class SieveCodecFactory implements ProtocolCodecFactory {

	private ProtocolDecoder decoder = new ProtocolDecoderAdapter() {
		private SieveMessageParser parser = new SieveMessageParser();

		@Override
		public void decode(IoSession session, IoBuffer in, ProtocolDecoderOutput out) throws Exception {

			if (parser.parse(in)) {
				SieveMessage message = parser.getMessage();
				parser.reset();
				out.write(message);
			}
		}
	};

	private ProtocolEncoder encoder = new ProtocolEncoderAdapter() {

		@Override
		public void encode(IoSession arg0, Object arg1, ProtocolEncoderOutput arg2) throws Exception {
			byte[] raw = (byte[]) arg1;
			IoBuffer b = IoBuffer.wrap(raw);
			arg2.write(b);
		}
	};

	@Override
	public ProtocolDecoder getDecoder(org.apache.mina.core.session.IoSession session) throws Exception {
		return decoder;
	}

	@Override
	public ProtocolEncoder getEncoder(org.apache.mina.core.session.IoSession session) throws Exception {
		return encoder;
	}

}
