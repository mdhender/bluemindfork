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
package net.bluemind.backend.mail.replica.service.internal;

import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

import net.bluemind.backend.mail.api.IMailConversationActions;
import net.bluemind.backend.mail.api.IMailboxItems;
import net.bluemind.backend.mail.api.flags.ConversationFlagUpdate;
import net.bluemind.backend.mail.api.flags.FlagUpdate;
import net.bluemind.backend.mail.replica.persistence.MailboxRecordStore;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.Ack;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.rest.BmContext;
import net.bluemind.mailbox.api.IMailboxAclUids;

public class MailConversationActionsService implements IMailConversationActions {

	private final RBACManager rbacManager;
	private final BmContext context;
	private final Container container;
	private final String replicatedMailboxUid;
	private final MailboxRecordStore recordStore;

	public MailConversationActionsService(BmContext context, DataSource ds, Container conversationContainer,
			String replicatedMailboxUid, MailboxRecordStore recordStore) {
		this.container = conversationContainer;
		this.rbacManager = RBACManager.forContext(context)
				.forContainer(IMailboxAclUids.uidForMailbox(conversationContainer.owner));
		this.context = context;
		this.replicatedMailboxUid = replicatedMailboxUid;
		this.recordStore = recordStore;
	}

	@Override
	public Ack addFlag(ConversationFlagUpdate flagUpdate) {
		rbacManager.check(Verb.Write.name());
		IMailboxItems mailboxItemsService = context.getServiceProvider().instance(IMailboxItems.class,
				replicatedMailboxUid);
		List<Long> itemsByConversations;
		try {
			itemsByConversations = recordStore.getItemsByConversations(flagUpdate.conversationUids);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
		return mailboxItemsService.addFlag(FlagUpdate.of(itemsByConversations, flagUpdate.mailboxItemFlag));
	}

}
