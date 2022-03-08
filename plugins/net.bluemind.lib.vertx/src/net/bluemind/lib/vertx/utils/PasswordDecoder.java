/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.lib.vertx.utils;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.util.concurrent.FastThreadLocal;

public class PasswordDecoder {
	private static final Logger logger = LoggerFactory.getLogger(PasswordDecoder.class);

	private static final FastThreadLocal<CharsetDecoder> localUtf8 = new FastThreadLocal<>();
	private static final FastThreadLocal<CharsetDecoder> localIso = new FastThreadLocal<>();

	public static String getPassword(String login, byte[] password) {
		return new String(password, PasswordDecoder.guessEncoding(login, password));
	}

	private static Charset guessEncoding(String login, byte[] password) {
		CharsetDecoder dec;

		dec = decoder(StandardCharsets.UTF_8, localUtf8);
		if (checkDec(password, dec)) {
			return StandardCharsets.UTF_8;
		}

		dec = decoder(StandardCharsets.ISO_8859_1, localIso);
		if (checkDec(password, dec)) {
			return StandardCharsets.ISO_8859_1;
		}

		logger.warn("[{}] password bytes are not compatible with utf-8 nor iso-8859-1", login);
		return StandardCharsets.UTF_8;
	}

	private static boolean checkDec(byte[] password, CharsetDecoder dec) {
		try {
			dec.decode(ByteBuffer.wrap(password));
			return true;
		} catch (CharacterCodingException e) {
			return false;
		}
	}

	private static CharsetDecoder decoder(Charset cs, FastThreadLocal<CharsetDecoder> local) {
		CharsetDecoder dec = local.get();
		if (dec == null) {
			dec = cs.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
					.onUnmappableCharacter(CodingErrorAction.REPORT);
			local.set(dec);
		}

		return dec;
	}
}
