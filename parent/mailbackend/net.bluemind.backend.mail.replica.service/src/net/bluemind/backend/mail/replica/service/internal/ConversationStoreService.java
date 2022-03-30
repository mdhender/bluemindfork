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
package net.bluemind.backend.mail.replica.service.internal;

import java.util.List;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import net.bluemind.backend.mail.replica.persistence.ConversationStore;
import net.bluemind.backend.mail.replica.persistence.InternalConversation;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;

public class ConversationStoreService extends ContainerWithoutChangelogService<InternalConversation> {

	private ConversationStore conversationStore;

	public ConversationStoreService(DataSource pool, SecurityContext securityContext, Container container) {
		super(pool, securityContext, container, new ConversationStore(pool, container));
		conversationStore = (ConversationStore) itemValueStore;
	}

	public List<ItemValue<InternalConversation>> byConversationsId(List<Long> conversationIds) {
		return super.getMultiple(conversationIds.stream().map(Long::toHexString).collect(Collectors.toList()));
	}
}
