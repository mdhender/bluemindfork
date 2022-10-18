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

import java.util.Collections;
import java.util.Set;

import org.apache.james.mime4j.dom.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.delivery.conversationreference.api.IConversationReference;
import net.bluemind.delivery.lmtp.common.DeliveryContent;
import net.bluemind.delivery.lmtp.common.IDeliveryContext;
import net.bluemind.delivery.lmtp.common.IDeliveryHook;
import net.bluemind.delivery.lmtp.common.ResolvedBox;

public class ConversationReferenceHook implements IDeliveryHook {

	private static final Logger logger = LoggerFactory.getLogger(ConversationReferenceHook.class);

	@Override
	public DeliveryContent preDelivery(IDeliveryContext ctx, DeliveryContent content) {
		ResolvedBox mailbox = content.box();
		Message message = content.message();
		MailboxRecord record = content.mailboxRecord();

		IConversationReference api = ctx.provider().instance(IConversationReference.class, mailbox.dom.uid,
				mailbox.entry.entryUid);
		Long conversationId = api.lookup(message.getMessageId(), references(message));
		record.conversationId = conversationId;
		if (logger.isDebugEnabled()) {
			logger.debug("Message {}@{} updated with conversationId {}", message.getMessageId(),
					mailbox.mbox.displayName, record.conversationId);
		}
		return content;
	}

	private Set<String> references(Message message) {
		return message.getHeader().getFields().stream() //
				.filter(field -> "references".equalsIgnoreCase(field.getName())) //
				.map(field -> (Set<String>) Sets.newHashSet(field.getBody().split(" "))) //
				.findFirst().orElse(Collections.emptySet());
	}

}
