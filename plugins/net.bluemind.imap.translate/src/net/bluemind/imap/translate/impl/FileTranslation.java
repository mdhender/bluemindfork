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
package net.bluemind.imap.translate.impl;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.json.JsonObject;

import com.google.common.base.Splitter;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.io.Files;

import net.bluemind.lib.jutf7.UTF7Converter;

public class FileTranslation implements Translation {

	private final BiMap<String, String> trans = HashBiMap.create();
	private static final Logger logger = LoggerFactory.getLogger(FileTranslation.class);

	public FileTranslation(File props) throws IOException {
		byte[] bytes = Files.toByteArray(props);
		JsonObject jso = new JsonObject(new String(bytes, "utf-8"));
		Map<String, Object> p = jso.toMap();
		for (Entry<String, Object> entry : p.entrySet()) {
			String key = UTF7Converter.encode(entry.getKey());
			String value = UTF7Converter.encode(entry.getValue().toString());
			trans.put(key, value);
			logger.info("{} => {}", key, value);
		}

	}

	@Override
	public String toImap(String f) {
		BiMap<String, String> reverseTrans = trans.inverse();
		String ret = translate(f, reverseTrans);
		logger.debug("'{}' to imap is '{}'", f, ret);
		return ret;
	}

	@Override
	public String toUser(String f) {
		String ret = translate(f, trans);
		logger.debug("'{}' to imap is '{}'", f, ret);
		return ret;
	}

	private String translate(String f, BiMap<String, String> translated) {
		String unquoted = unquote(f);
		String ret = unquoted;
		Iterator<String> pathPieces = Splitter.on('/').split(ret).iterator();
		StringBuilder sb = new StringBuilder();
		while (pathPieces.hasNext()) {
			String pathPiece = pathPieces.next();
			if (translated.containsKey(pathPiece)) {
				sb.append(translated.get(pathPiece));
			} else {
				sb.append(pathPiece);
			}
			if (pathPieces.hasNext()) {
				sb.append('/');
			}
		}
		ret = sb.toString();

		logger.debug("'{}' to user is '{}'", f, ret);

		return quote(ret);
	}

	@Override
	public boolean isTranslated(String name) {
		boolean ret = false;
		for (Entry<String, String> entry : trans.entrySet()) {
			String translated = entry.getValue();
			String key = entry.getKey();
			if (key.equals(translated)) {
				continue;
			}
			String utf7 = UTF7Converter.encode(name);
			if (translated.equals(name) || translated.equals(utf7) || name.startsWith(translated + "/")
					|| utf7.startsWith(translated + "/")) {
				logger.info("{} conflicts with translation", name);
				ret = true;
				break;
			}
		}
		return ret;
	}

	private String quote(String ret) {
		return "\"" + ret + "\"";
	}

	private String unquote(String f) {
		if (f.startsWith("\"")) {
			return f.substring(1, f.length() - 1);
		} else {
			return f;
		}
	}
}
