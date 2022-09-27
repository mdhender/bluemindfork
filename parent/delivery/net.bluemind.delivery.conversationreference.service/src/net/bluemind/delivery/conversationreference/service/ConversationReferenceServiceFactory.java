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

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider.IServerSideServiceFactory;
import net.bluemind.delivery.conversationreference.api.IConversationReference;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;

public class ConversationReferenceServiceFactory implements IServerSideServiceFactory<IConversationReference> {

	@Override
	public Class<IConversationReference> factoryClass() {
		return IConversationReference.class;
	}

	private IConversationReference getService(BmContext context, String domainUid, String ownerUid) throws ServerFault {
		IMailboxes mailboxes = context.su().provider().instance(IMailboxes.class, domainUid);
		ItemValue<Mailbox> mailbox = mailboxes.getComplete(ownerUid);
		if (mailbox == null) {
			throw ServerFault.notFound("mailbox for owner uid=" + ownerUid + " not found");
		}
		return new ConversationReferenceService(context, mailbox);
	}

	@Override
	public IConversationReference instance(BmContext context, String... params) throws ServerFault {
		if (params == null || params.length < 2) {
			throw new ServerFault("wrong number of instance parameters");
		}
		return getService(context, params[0], params[1]);
	}

}
