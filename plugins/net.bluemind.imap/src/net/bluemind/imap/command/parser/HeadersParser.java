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
package net.bluemind.imap.command.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;

import net.bluemind.imap.IMAPByteSource;
import net.bluemind.imap.IMAPHeaders;
import net.bluemind.imap.impl.IMAPResponse;
import net.bluemind.utils.DOMUtils;

public final class HeadersParser {

	private static final Logger logger = LoggerFactory.getLogger(HeadersParser.class);

	private static final Map<String, String> empty = ImmutableMap.of();

	public static final Map<String, String> parseRawHeaders(Reader reader) throws IOException {
		Map<String, String> headers = new HashMap<String, String>();
		BufferedReader br = new BufferedReader(reader);
		String line = null;
		StringBuilder curHead = null;
		String lastKey = null;
		while ((line = br.readLine()) != null) {
			// collapse rfc822 headers into one line
			int lineLength = line.length();
			if (!(lineLength > 1)) {
				continue;
			}
			char first = line.charAt(0);

			if (Character.isWhitespace(first)) {
				int nbSpaces = 1;
				while (nbSpaces < lineLength && Character.isWhitespace(line.charAt(nbSpaces))) {
					nbSpaces++;
				}
				if (nbSpaces < lineLength) {
					curHead.append(' ').append(line.substring(nbSpaces));
				}
			} else {
				if (lastKey != null) {
					headers.put(lastKey, DOMUtils.stripNonValidXMLCharacters(curHead.toString()));
				}
				curHead = new StringBuilder();
				lastKey = null;

				int split = line.indexOf(':');
				if (split > 0) {
					lastKey = line.substring(0, split).toLowerCase();
					String value = line.substring(split + 1).trim();
					curHead.append(value);
				}

			}
		}
		if (lastKey != null) {
			headers.put(lastKey, DOMUtils.stripNonValidXMLCharacters(curHead.toString()));
		}
		return headers;
	}

	public static IMAPHeaders literalToHeaders(int uid, IMAPResponse r) {
		Map<String, String> rawHeaders = empty;
		IMAPByteSource in = r.getStreamData();
		if (in != null) {
			try {
				InputStreamReader reader = new InputStreamReader(in.source().openStream());
				rawHeaders = parseRawHeaders(reader);
				in.close();
			} catch (IOException e) {
				logger.error("Error reading headers stream on uid " + uid, e);
			} catch (Exception t) {
				logger.error("error parsing headers stream " + uid, t);
			}
		} else {
			// cyrus search command can return uid's that no longer
			// exist in the mailbox
			logger.warn("cyrus did not return any header for uid " + uid);
		}

		IMAPHeaders imapHeaders = new IMAPHeaders();
		imapHeaders.setUid(uid);
		imapHeaders.setRawHeaders(rawHeaders);
		return imapHeaders;
	}
}
