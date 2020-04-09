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
package net.bluemind.cli.mail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class IndexedMessageBody {

	public final List<String> content;
	public final Map<String, Object> data;
	private final Map<String, String> headers;
	public final String subject;
	public final String messageId;
	public final List<String> references;
	public final String uid;
	public final String preview;

	private IndexedMessageBody(String uid, List<String> content, Map<String, Object> data, Map<String, String> headers,
			String subject, String preview, String messageId, List<String> references) {
		this.uid = uid;
		this.content = content;
		this.data = data;
		this.headers = headers;
		this.subject = subject;
		this.preview = preview;
		this.messageId = messageId;
		this.references = references;
	}

	public Map<String, Object> toMap() {
		Map<String, Object> asMap = new HashMap<>();
		asMap.put("content", content);
		asMap.put("messageId", messageId);
		asMap.put("references", references);
		asMap.put("preview", preview);
		asMap.put("subject", subject);
		asMap.put("subject_kw", subject);
		asMap.put("headers", headers);
		asMap.putAll(data);
		return asMap;
	}

	public static IndexedMessageBody fromJson(String value) {
		Function<JsonArray, List<String>> toList = arr -> {
			List<String> list = new ArrayList<>();
			for (Iterator<Object> iter = arr.iterator(); iter.hasNext();) {
				list.add((String) iter.next());
			}
			return list;
		};
		JsonObject json = new JsonObject(value);
		String uid = json.getString("uid");
		JsonArray contentArray = json.getJsonArray("content");
		String messageId = json.getString("messageId");
		JsonArray referencesArray = json.getJsonArray("references");
		String preview = json.getString("preview");
		String subject = json.getString("subject");
		Map<String, Object> headersMap = json.getJsonObject("headers").getMap();
		Map<String, String> asHeaders = headersMap.keySet().stream()
				.collect(Collectors.toMap(key -> key, key -> (String) headersMap.get(key)));
		Map<String, Object> data = json.getJsonObject("data").getMap();
		return new IndexedMessageBody(uid, toList.apply(contentArray), data, asHeaders, subject, preview, messageId,
				toList.apply(referencesArray));
	}

}
