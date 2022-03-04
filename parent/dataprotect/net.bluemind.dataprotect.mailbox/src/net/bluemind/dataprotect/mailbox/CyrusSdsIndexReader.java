/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.dataprotect.mailbox;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

public class CyrusSdsIndexReader {
	private static final Logger logger = LoggerFactory.getLogger(CyrusSdsIndexReader.class);
	private Map<String, Path> jsonmap = new HashMap<>();

	public CyrusSdsIndexReader(Path indexPath) throws IOException {
		Path parentRoot = indexPath.getParent();
		JsonFactory jfactory = new JsonFactory();
		try (JsonParser parser = jfactory.createParser(indexPath.toFile())) {
			while (parser.nextToken() != JsonToken.END_ARRAY) {
				String filename = null;
				String mailboxUid = null;

				while (parser.nextToken() != JsonToken.END_OBJECT) {
					String fieldName = parser.getCurrentName();
					if (fieldName == null) {
						continue;
					}
					switch (fieldName) {
					case "mailboxUid":
						mailboxUid = parser.nextTextValue();
						break;
					case "filename":
						filename = parser.nextTextValue();
						break;
					default:
						logger.warn("unknown field: {} in {}", fieldName, indexPath);
					}
				}
				if (mailboxUid != null && filename != null) {
					jsonmap.put(mailboxUid, parentRoot.resolve(filename));
				} else {
					logger.warn("invalid sds index: empty mailboxUid or empty filename in {}", indexPath);
				}
			}
		}
	}

	public Path getMailbox(String mailboxUid) {
		return jsonmap.get(mailboxUid);
	}
}