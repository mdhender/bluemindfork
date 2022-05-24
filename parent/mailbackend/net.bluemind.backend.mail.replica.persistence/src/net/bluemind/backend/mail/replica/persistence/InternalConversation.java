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
package net.bluemind.backend.mail.replica.persistence;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class InternalConversation {

	public List<InternalMessageRef> messageRefs = Collections.emptyList();

	public static class InternalMessageRef {
		public long folderId;
		public long itemId;
		public Date date;
	}

	public void removeMessage(long folderId, Long itemId) {
		this.messageRefs = this.messageRefs.stream()
				.filter(message -> message.folderId != folderId || message.itemId != itemId)
				.collect(Collectors.toList());
	}
}
