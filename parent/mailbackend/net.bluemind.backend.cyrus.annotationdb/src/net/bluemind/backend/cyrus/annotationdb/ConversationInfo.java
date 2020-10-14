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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.io.BaseEncoding;

public class ConversationInfo {

	public final List<ConversationElement> conversations = new ArrayList<>();

	public void add(ConversationElement conversation) {
		conversations.add(conversation);
	}

	public static class ConversationElement {
		public final long conversationId;
		public final Set<String> uids;
		public final FORMAT format;

		public ConversationElement(String conversationId, Set<String> uids, FORMAT format) {
			this.conversationId = new BigInteger(BaseEncoding.base16().decode(conversationId.toUpperCase()))
					.longValue();
			this.uids = uids;
			this.format = format;
		}

	}

	public static class Builder {

		private String conversationId;
		private Set<String> uids;
		private FORMAT format;

		public static Builder create() {
			Builder builder = new Builder();
			builder.uids = new HashSet<>();
			return builder;
		}

		public Builder conversationId(String conversationId) {
			this.conversationId = conversationId;
			return this;
		}

		public Builder message(String uid, FORMAT format) {
			this.uids.add(uid);
			this.format = format;
			return this;
		}

		public ConversationElement build() {
			return new ConversationElement(conversationId, uids, format);
		}

		public boolean hasMessages() {
			return !uids.isEmpty();
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("\r\n");
		for (ConversationElement conversationElement : conversations) {
			sb.append(conversationElement.conversationId + " --> " + conversationElement.format.name() + " --> "
					+ String.join(",", conversationElement.uids.toArray(new String[0])) + "\r\n");
		}
		return sb.toString();
	}

	public enum FORMAT {
		BODY_GUID, MESSAGE_ID;
	}
}
