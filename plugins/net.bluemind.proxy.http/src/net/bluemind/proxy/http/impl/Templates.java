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
package net.bluemind.proxy.http.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.ByteStreams;

public final class Templates {

	private static final Logger logger = LoggerFactory.getLogger(Templates.class);
	private static final ConcurrentHashMap<String, CachedTemplate> cache;

	static {
		cache = new ConcurrentHashMap<String, CachedTemplate>();
	}

	/**
	 * @param furi
	 *            templates/texture.jpg
	 * @return
	 * @throws IOException
	 */
	public static final CachedTemplate getCached(String furi) throws IOException {
		CachedTemplate ct = cache.get(furi);
		if (ct == null) {
			try (InputStream in = Templates.class.getClassLoader().getResourceAsStream(furi)) {
				byte[] tContent = ByteStreams.toByteArray(in);
				String cType = MimeHelper.of(furi);
				long lastMod = System.currentTimeMillis();
				ct = new CachedTemplate(tContent, lastMod, cType);
				cache.put(furi, ct);
				logger.info("Template " + furi + " cached in memory (size: " + tContent.length + ")");
			} catch (IOException e) {
				logger.error("Error loading template with uri: " + furi);
				throw e;
			}
		}
		return ct;
	}

}
