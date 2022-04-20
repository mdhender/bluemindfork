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
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import net.bluemind.backend.mail.api.IItemsTransfer;
import net.bluemind.backend.mail.api.IMailConversationActions;
import net.bluemind.backend.mail.api.IMailboxFolders;
import net.bluemind.backend.mail.api.IMailboxItems;
import net.bluemind.backend.mail.api.ImportMailboxItemSet;
import net.bluemind.backend.mail.api.ImportMailboxItemSet.MailboxItemId;
import net.bluemind.backend.mail.api.ImportMailboxItemsStatus;
import net.bluemind.backend.mail.api.flags.ConversationFlagUpdate;
import net.bluemind.backend.mail.api.flags.FlagUpdate;
import net.bluemind.backend.mail.api.flags.ImportMailboxConversationSet;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.backend.mail.replica.persistence.MailboxRecordStore;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.Ack;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemIdentifier;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.rest.BmContext;
import net.bluemind.mailbox.api.IMailboxAclUids;
import net.bluemind.user.api.IUser;

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
		return flagAction(flagUpdate,
				(service, items) -> service.addFlag(FlagUpdate.of(items, flagUpdate.mailboxItemFlag)));
	}

	@Override
	public Ack deleteFlag(ConversationFlagUpdate flagUpdate) {
		return flagAction(flagUpdate,
				(service, items) -> service.deleteFlag(FlagUpdate.of(items, flagUpdate.mailboxItemFlag)));
	}

	@Override
	public List<ItemIdentifier> copy(String targetMailboxUid, List<String> conversationUids) {
		rbacManager.check(Verb.Write.name());
		return transferAction(targetMailboxUid, conversationUids, (service, itemds) -> service.copy(itemds));
	}

	@Override
	public List<ItemIdentifier> move(String targetMailboxUid, List<String> conversationUids) {
		rbacManager.check(Verb.Write.name());
		return transferAction(targetMailboxUid, conversationUids, (service, itemds) -> service.move(itemds));
	}

	private List<ItemIdentifier> transferAction(String targetMailboxUid, List<String> conversationUids,
			BiFunction<IItemsTransfer, List<Long>, List<ItemIdentifier>> op) {
		String sourceMailboxUid = IMailReplicaUids.mboxRecords(replicatedMailboxUid);
		List<Long> itemsByConversations = getItemidsByConversationUids(conversationUids);
		IItemsTransfer transferService = context.getServiceProvider().instance(IItemsTransfer.class, sourceMailboxUid,
				targetMailboxUid);
		return op.apply(transferService, itemsByConversations);
	}

	private Ack flagAction(ConversationFlagUpdate flagUpdate, BiFunction<IMailboxItems, List<Long>, Ack> action) {
		rbacManager.check(Verb.Write.name());
		IMailboxItems mailboxItemsService = context.getServiceProvider().instance(IMailboxItems.class,
				replicatedMailboxUid);
		List<Long> itemsByConversations = getItemidsByConversationUids(flagUpdate.conversationUids);
		return action.apply(mailboxItemsService, itemsByConversations);
	}

	@Override
	public ImportMailboxItemsStatus importItems(long folderDestinationId, ImportMailboxConversationSet conversationSet)
			throws ServerFault {
		rbacManager.check(Verb.Write.name());
		String partition = container.domainUid.replace('.', '_');
		String userLogin = "user." + context.getServiceProvider().instance(IUser.class, container.domainUid)
				.getComplete(container.owner).value.login.replace('.', '^');

		IMailboxFolders mailboxItemsService = context.getServiceProvider().instance(IMailboxFolders.class, partition,
				userLogin);
		List<Long> itemsByConversations = getItemidsByConversationUids(conversationSet.conversationUids);

		ImportMailboxItemSet itemSet = ImportMailboxItemSet.of(conversationSet.mailboxFolderId,
				itemsByConversations.stream().map(MailboxItemId::of).collect(Collectors.toList()), null,
				conversationSet.deleteFromSource);
		return mailboxItemsService.importItems(folderDestinationId, itemSet);
	}

	private List<Long> getItemidsByConversationUids(List<String> conversationUids) {
		List<Long> itemsByConversations;
		try {
			itemsByConversations = recordStore.getItemsByConversations(conversationUids);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
		return itemsByConversations;
	}

}
