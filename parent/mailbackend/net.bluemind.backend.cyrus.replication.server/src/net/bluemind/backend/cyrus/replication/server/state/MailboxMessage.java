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
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.backend.cyrus.replication.server.state;

import net.bluemind.backend.cyrus.replication.server.Token;

/**
 * The immutable part of a message, with its body
 *
 */
public class MailboxMessage {

	private final Token content;
	private final String partition;
	private final String guid;
	private final int length;

	public static class MailboxMessageBuilder {

		private Token content;
		private String partition;
		private String guid;
		private int length;

		private MailboxMessageBuilder() {
		}

		public MailboxMessageBuilder content(Token binary) {
			this.content = binary;
			return this;
		}

		public MailboxMessageBuilder guid(String guid) {
			this.guid = guid;
			return this;
		}

		public MailboxMessageBuilder partition(String partition) {
			this.partition = partition;
			return this;
		}

		public MailboxMessageBuilder length(int length) {
			this.length = length;
			return this;
		}

		public MailboxMessage build() {
			return new MailboxMessage(partition, guid, content, length);
		}

	}

	public static MailboxMessageBuilder builder() {
		return new MailboxMessageBuilder();
	}

	private MailboxMessage(String partition, String guid, Token content, int length) {
		this.partition = partition;
		this.guid = guid;
		this.content = content;
		this.length = length;
	}

	public Token content() {
		return content;
	}

	public String partition() {
		return partition;
	}

	public String guid() {
		return guid;
	}

	public int length() {
		return length;
	}

	public String toString() {
		return String.format("[MSGBODY len: %s, ref: %s, guid: %s]", length, content.value(), guid);
	}

}
