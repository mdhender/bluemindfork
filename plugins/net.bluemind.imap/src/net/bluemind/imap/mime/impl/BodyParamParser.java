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
package net.bluemind.imap.mime.impl;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;

import net.bluemind.imap.EncodedWord;
import net.bluemind.imap.mime.BodyParam;

public class BodyParamParser {

	private final String key;
	private final String value;
	private String decodedKey;
	private String decodedValue;

	public BodyParamParser(String key, String value) {
		this.key = key;
		this.value = value;
	}

	public BodyParam parse() {
		if (key.endsWith("*")) {
			decodedKey = key.substring(0, key.length() - 1);
			decodedValue = value.startsWith("=?") ? decodeQuotedPrintable() : decodeAsterixEncodedValue();
		} else {
			decodedKey = key;
			decodedValue = decodeQuotedPrintable();
		}
		return new BodyParam(rewritedKey(decodedKey), decodedValue);
	}

	private String decodeAsterixEncodedValue() {
		final int firstQuote = value.indexOf('\'');
		final int secondQuote = value.indexOf('\'', firstQuote + 1);
		final String charsetName = value.substring(0, firstQuote);
		final String text = value.substring(secondQuote + 1);
		try {
			Charset charset = Charset.forName(charsetName);
			return URLDecoder.decode(text, charset.displayName());
		} catch (UnsupportedEncodingException e) {
		} catch (IllegalCharsetNameException e) {
		} catch (IllegalArgumentException e) {
		}
		return text;
	}

	private String decodeQuotedPrintable() {
		return EncodedWord.decode(value).toString();
	}

	private static String rewritedKey(String decodedKey) {
		if ("filename".equalsIgnoreCase(decodedKey)) {
			return "name";
		}
		return decodedKey.toLowerCase();
	}

}
