/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.delivery.conversationreference.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;

import net.bluemind.core.jdbc.JdbcAbstractStore.Creator;

public class ConversationReference {
	public long conversationId;
	public long messageIdHash;
	public long mailboxId;

	public ConversationReference(long messageIdHash, long conversationId, long mailboxId) {
		this.messageIdHash = messageIdHash;
		this.conversationId = conversationId;
		this.mailboxId = mailboxId;
	}

	public static class ConversationReferencePopulator implements Creator<ConversationReference> {
		@Override
		public ConversationReference create(ResultSet rs) throws SQLException {
			return ConversationReference.of(rs.getLong(1), rs.getLong(2), rs.getLong(3));
		}

	}

	public static ConversationReference of(long messageIdHash, long conversationId, long mailboxId) {
		return new ConversationReference(messageIdHash, conversationId, mailboxId);
	}

	@Override
	public String toString() {
		return "mailbox_id = " + mailboxId + ", conversation_id = " + conversationId + ", message_id_hash = "
				+ messageIdHash;
	}
}
