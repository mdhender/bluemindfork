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
package net.bluemind.delivery.conversationreference.service;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import com.google.common.base.Strings;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.delivery.conversationreference.api.IConversationReference;
import net.bluemind.delivery.conversationreference.persistence.ConversationReference;
import net.bluemind.delivery.conversationreference.persistence.ConversationReferenceStore;
import net.bluemind.mailbox.api.Mailbox;

public class ConversationReferenceService implements IConversationReference {
	private final ConversationReferenceStore store;
	private final long mailboxId;

	public ConversationReferenceService(BmContext context, ItemValue<Mailbox> mailbox) {
		mailboxId = mailbox.internalId;
		DataSource ds = context.getMailboxDataSource(mailbox.value.dataLocation);
		store = new ConversationReferenceStore(ds);
	}

	private ConversationReference create(Long messageIdHash) throws SQLException {
		return store.create(ConversationReference.of(messageIdHash, messageIdHash, mailboxId));
	}

	/*
	 * Scenerios:
	 * 
	 * unknown conversation, no references: create a new conversation with the
	 * message-id known conversation, new references: add a reference to message-id,
	 * add all references to the conversation.
	 * 
	 * Conflicting cases are handled by INSERT ON CONFLICT This means this scenario,
	 * received in order:
	 *
	 * MSG3: id: 300 references: 301
	 * 
	 * MSG1: id: 100 references: 200
	 * 
	 * MSG2: id: 200 references: 301
	 * 
	 * will produce 2 conflicting conversations for message id=301
	 */

	@Override
	public Long lookup(String messageId, Set<String> references) {
		HashFunction hf = Hashing.sipHash24();
		if (Strings.isNullOrEmpty(messageId)) {
			// This should not happen, because postfix, in from of us should protect from
			// that, but it's not protected when injecting random emails from IMAP.
			messageId = "<" + UUID.randomUUID() + "@random-message-id.invalid>";
		}
		long messageIdHash = hf.hashBytes(messageId.getBytes()).asLong();
		long returnedConversationId;

		List<Long> referencesHash = references.stream().map(s -> hf.hashBytes(s.getBytes()).asLong())
				.collect(Collectors.toList());
		referencesHash.add(messageIdHash);
		ConversationReference returnedConversationReference;
		try {
			returnedConversationId = store.get(mailboxId, referencesHash);
			if (returnedConversationId == 0L) {
				returnedConversationReference = create(messageIdHash);
				returnedConversationId = returnedConversationReference.conversationId;
			}
			// Store all known references
			if (returnedConversationId != 0L) {
				long cid = returnedConversationId; // Thank you java!
				var conversationReferences = store
						.create(referencesHash.stream().map(h -> ConversationReference.of(h, cid, mailboxId)).toList());
				var convRefOpt = conversationReferences.stream().filter(cr -> cr.messageIdHash == messageIdHash)
						.findFirst();
				if (convRefOpt.isPresent()) {
					returnedConversationId = convRefOpt.get().conversationId;
				}
			}
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
		return returnedConversationId;
	}
}
