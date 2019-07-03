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
package net.bluemind.dav.server.store;

import java.util.Base64;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Splitter;

public class SyncTokens {

	private static final Logger logger = LoggerFactory.getLogger(SyncTokens.class);

	private static final String uuid = "bmdav";

	public static final String get(DavResource dr, long timestamp) {
		return get(dr.getPath(), timestamp);
	}

	public static final String get(String path, long timestamp) {
		// osx server ids are formatted with <number>_<number>
		// we don't really care about lnum
		long lnum = (long) path.hashCode() + Integer.MAX_VALUE;
		StringBuilder sb = new StringBuilder(64);
		sb.append(uuid).append('_');
		sb.append(lnum).append('_').append(timestamp);
		return sb.toString();
	}

	public static final Long getDate(String t) {
		String token = t;
		if (token.startsWith("data:")) {
			token = t.substring(5);
		}
		if (token.isEmpty()) {
			logger.info("****** empty token: initial sync ******");
			return 0L;
		}
		Iterator<String> split = Splitter.on('_').split(token).iterator();
		String server = split.next();
		if (uuid.equals(server)) {
			split.next(); // hashcode of collection
			return Long.parseLong(split.next());
		} else {
			logger.info("****** forced initial sync (server uuid mismatch) ******");
			return 0L;
		}
	}

	public static final Long getEtagDate(String etag) {
		String unquoted = etag.substring(1, etag.length() - 1);
		logger.info("unquoted etag: '{}', {}", unquoted, etag);
		byte[] encoded = Base64.getDecoder().decode(unquoted);
		String dec = new String(encoded);
		logger.info("Decoded etag to {}", dec);
		return getDate(dec);
	}

	public static String getEtag(String path, long timestamp) {
		String etag = get(path, timestamp);
		etag = Base64.getEncoder().encodeToString(etag.getBytes());
		StringBuilder sb = new StringBuilder(64);
		return sb.append('"').append(etag).append('"').toString();
	}

}
