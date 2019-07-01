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
package net.bluemind.lib.jutf7;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import com.beetstra.jutf7.CharsetProvider;

public final class UTF7Converter {

	private static final Charset utf7Cs = new CharsetProvider().charsetForName("X-IMAP4-MODIFIED-UTF-7");

	public static final String encode(String mbName) {
		ByteBuffer bb = utf7Cs.encode(mbName);
		byte[] ascii = bb.array();
		return new String(ascii, 0, bb.limit(), StandardCharsets.US_ASCII);
	}

	public static final String decode(String mbUtf7) {
		ByteBuffer bb = ByteBuffer.wrap(mbUtf7.getBytes());
		CharBuffer ascii = utf7Cs.decode(bb);
		return new String(ascii.array(), 0, ascii.limit());
	}

}
