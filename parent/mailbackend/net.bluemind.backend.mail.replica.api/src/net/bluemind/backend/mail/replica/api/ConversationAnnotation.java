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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.backend.mail.replica.api;

import java.math.BigInteger;

import net.bluemind.backend.mail.api.Conversation;

public class ConversationAnnotation extends MailboxRecordAnnotation {

	private static final String entryString = "/vendor/cmu/cyrus-imapd/thrid";

	public ConversationAnnotation(long conversationId) {
		super.entry = entryString;
		super.value = BigInteger.valueOf(conversationId).toString(16);
	}

	public ConversationAnnotation(Conversation conversation) {
		this(conversation.conversationId);
	}

}
