/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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
package net.bluemind.backend.cyrus.annotationdb;

import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SetMultimap;

import net.bluemind.backend.cyrus.annotationdb.ConversationInfo.Builder;
import net.bluemind.backend.cyrus.annotationdb.ConversationInfo.FORMAT;

public class AnnotationDb implements Consumer<String> {

	private static final Pattern pattern1 = Pattern.compile("G([^:]+):\\d:\\d+\\s+([a-f0-9]+)$");
	private static final Pattern pattern2 = Pattern.compile("([^\\t]+)\\t\\d\\s([a-f0-9]+)\\s\\d+$");

	private final SetMultimap<String, String> convBodies;
	private FORMAT format;

	public AnnotationDb() {
		this.convBodies = MultimapBuilder.hashKeys().hashSetValues().build();
	}

	public void accept(String line) {
		if (!parse(line, pattern1, FORMAT.BODY_GUID)) {
			parse(line, pattern2, FORMAT.MESSAGE_ID);
		}
	}

	public ConversationInfo get() {
		ConversationInfo info = new ConversationInfo();
		convBodies.asMap().forEach((id, values) -> {
			Builder conversation = ConversationInfo.Builder.create().conversationId(id);
			values.forEach(val -> conversation.message(val, format));
			info.add(conversation.build());
		});
		return info;
	}

	private boolean parse(String line, Pattern pattern, FORMAT format) {
		Matcher matcher = pattern.matcher(line);

		if (matcher.find()) {
			String bodyGuid = matcher.group(1);
			String convId = matcher.group(2);
			if (this.format != null && this.format == FORMAT.MESSAGE_ID && format == FORMAT.BODY_GUID) {
				// file contains both formats, reset all entries based on the message-id
				this.convBodies.clear();
			}
			convBodies.put(convId, bodyGuid);

			this.format = format;
			return true;
		}
		return false;
	}

}
