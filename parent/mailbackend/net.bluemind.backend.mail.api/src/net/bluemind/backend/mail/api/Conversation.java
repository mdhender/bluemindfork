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
package net.bluemind.backend.mail.api;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import net.bluemind.core.api.BMApi;

/** Conversation of messages. */
@BMApi(version = "3")
public class Conversation {

	/**
	 * The messages of this conversation. Implementations should ensure there are no
	 * duplicates.
	 */
	public List<MessageRef> messageRefs = Collections.emptyList();

	public long conversationId;
	public String conversationUid;

	public static Conversation of(List<MessageRef> messageRefs, long conversationId) {
		Conversation conversation = new Conversation();
		conversation.conversationId = conversationId;
		conversation.conversationUid = Long.toHexString(conversationId);
		conversation.messageRefs = messageRefs;
		return conversation;
	}

	@BMApi(version = "3")
	public static class MessageRef {
		/** The message 's folder unique identifier. */
		public String folderUid;

		/** The unique identifier of the message's item. */
		public long itemId;

		/** The creation date of the message's item. */
		public Date date;

		public static MessageRef of(String folderUid, long itemId, Date date) {
			MessageRef newref = new MessageRef();
			newref.folderUid = folderUid;
			newref.itemId = itemId;
			newref.date = date;
			return newref;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((folderUid == null) ? 0 : folderUid.hashCode());
			result = prime * result + (int) (itemId ^ (itemId >>> 32));
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			MessageRef other = (MessageRef) obj;
			if (folderUid == null) {
				if (other.folderUid != null)
					return false;
			} else if (!folderUid.equals(other.folderUid)) {
				return false;
			}
			return itemId == other.itemId;
		}

	}

	public void removeMessage(String containerUid, Long itemId) {
		this.messageRefs = this.messageRefs.stream()
				.filter(message -> !message.folderUid.equals(containerUid) || message.itemId != itemId)
				.collect(Collectors.toList());
	}
}
