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

import net.bluemind.core.jdbc.Columns;
import net.bluemind.core.jdbc.JdbcAbstractStore;

public class ConversationServiceColumns {

	private ConversationServiceColumns() {

	}

	public static final Columns cols = Columns.create() //
			.col("mailbox_id") //
			.col("message_id_hash") //
			.col("conversation_id");

	public static JdbcAbstractStore.EntityPopulator<ConversationReference> populator() {
		return (rs, index, value) -> {
			value.mailboxId = rs.getLong(index++);
			value.messageIdHash = rs.getLong(index++);
			value.conversationId = rs.getLong(index++);
			return index;
		};
	}
}
