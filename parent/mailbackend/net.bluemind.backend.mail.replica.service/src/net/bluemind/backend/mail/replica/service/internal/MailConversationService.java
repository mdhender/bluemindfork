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
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.sql.DataSource;
import javax.ws.rs.PathParam;

import net.bluemind.backend.mail.api.Conversation;
import net.bluemind.backend.mail.api.Conversation.MessageRef;
import net.bluemind.backend.mail.replica.api.IDbMailboxRecords;
import net.bluemind.backend.mail.replica.api.IInternalMailConversation;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.backend.mail.replica.persistence.ConversationStore;
import net.bluemind.backend.mail.replica.persistence.InternalConversation;
import net.bluemind.backend.mail.replica.persistence.InternalConversation.InternalMessageRef;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemFlagFilter;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.mailbox.api.IMailboxAclUids;

public class MailConversationService implements IInternalMailConversation {

	private final ConversationStoreService storeService;
	private final ConversationStore conversationStore;
	private final RBACManager rbacManager;
	private final ContainerStore containerStore;
	private final BmContext context;
	private final Container container;

	public MailConversationService(BmContext context, DataSource ds, Container conversationContainer) {
		this.container = conversationContainer;
		this.conversationStore = new ConversationStore(ds, conversationContainer);
		this.storeService = new ConversationStoreService(ds, context.getSecurityContext(), conversationContainer);
		this.rbacManager = RBACManager.forContext(context)
				.forContainer(IMailboxAclUids.uidForMailbox(conversationContainer.owner));
		this.containerStore = new ContainerStore(context, ds, context.getSecurityContext());
		this.context = context;
	}

	@Override
	public void create(String uid, Conversation conversation) {
		rbacManager.check(Verb.Write.name());
		storeService.create(uid, createDisplayName(uid), conversationToInternal(conversation));
	}

	@Override
	public void update(String uid, Conversation conversation) {
		rbacManager.check(Verb.Write.name());
		storeService.update(uid, createDisplayName(uid), conversationToInternal(conversation));
	}

	@Override
	public ItemValue<Conversation> getComplete(@PathParam(value = "uid") String uid) {
		rbacManager.check(Verb.Read.name());
		ItemValue<InternalConversation> itemValue = storeService.get(uid, null);
		if (itemValue == null) {
			return null;
		}
		return conversationToPublic(itemValue);
	}

	private String createDisplayName(String uid) {
		return "conversation_" + uid;
	}

	@Override
	public List<ItemValue<Conversation>> byFolder(String folderUid, ItemFlagFilter filter) {
		rbacManager.check(Verb.Read.name());
		Predicate<InternalMessageRef> filterPredicate = null;
		if (!filter.matchAll()) {
			Set<Long> validIds = getValidIds(folderUid, filter);
			filterPredicate = id -> validIds.contains(id.itemId);
		} else {
			filterPredicate = id -> true;
		}
		Predicate<InternalMessageRef> pred = filterPredicate;

		return storeService.byFolder(uidToId(folderUid)).stream() //
				.filter(conversation -> conversation.value.messageRefs.stream() //
						.anyMatch(pred))
				.map(this::conversationToPublic).collect(Collectors.toList());

	}

	private Set<Long> getValidIds(String folderUid, ItemFlagFilter filter) {
		return ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDbMailboxRecords.class, folderUid).filteredChangesetById(0l, filter).created.stream()
						.map(i -> i.id).collect(Collectors.toSet());
	}

	@Override
	public void removeMessage(String folderUid, Long itemId) {
		rbacManager.check(Verb.Write.name());
		try {
			List<ItemValue<Conversation>> conversations = storeService
					.getMultipleById(conversationStore.byMessage(uidToId(folderUid), itemId)).stream()
					.map(this::conversationToPublic).collect(Collectors.toList());
			for (ItemValue<Conversation> conversation : conversations) {
				conversation.value.removeMessage(folderUid, itemId);
				if (conversation.value.messageRefs.isEmpty()) {
					storeService.delete(conversation.uid);
				} else {
					update(conversation.uid, conversation.value);
				}
			}
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	@Override
	public void deleteAll(String folderUid) {
		try {
			conversationStore.deleteMessagesInFolder(uidToId(folderUid));
		} catch (SQLException e) {
			throw new ServerFault(e);
		}
	}

	private InternalConversation conversationToInternal(Conversation conversation) {
		InternalConversation internal = new InternalConversation();
		internal.messageRefs = conversation.messageRefs.stream().map(this::messageToInternal)
				.collect(Collectors.toList());
		return internal;
	}

	private ItemValue<Conversation> conversationToPublic(ItemValue<InternalConversation> itemValue) {
		Conversation external = new Conversation();
		external.messageRefs = itemValue.value.messageRefs.stream().map(this::messageToPublic).filter(Objects::nonNull)
				.collect(Collectors.toList());
		return ItemValue.create(itemValue, external);
	}

	private InternalMessageRef messageToInternal(MessageRef messageid) {
		InternalMessageRef internal = new InternalMessageRef();
		internal.date = messageid.date;
		internal.itemId = messageid.itemId;
		internal.folderId = uidToId(messageid.folderUid);
		return internal;
	}

	private MessageRef messageToPublic(InternalMessageRef internalMessageId) {
		MessageRef external = new MessageRef();
		try {
			external.folderUid = idToUid(internalMessageId.folderId);
		} catch (NullPointerException e) {
			return null;
		}
		external.date = internalMessageId.date;
		external.itemId = internalMessageId.itemId;
		return external;
	}

	private String idToUid(long folderId) {
		try {
			return IMailReplicaUids.getUniqueId(containerStore.get(folderId).uid);
		} catch (SQLException e) {
			throw new ServerFault(e);
		}
	}

	private long uidToId(String folderUid) {
		try {
			Container res = containerStore.get(IMailReplicaUids.mboxRecords(folderUid));
			return res.id;
		} catch (SQLException e) {
			throw new ServerFault(e);
		}
	}

	@Override
	public void xfer(String serverUid) throws ServerFault {
		DataSource ds = context.getMailboxDataSource(serverUid);
		ContainerStore cs = new ContainerStore(null, ds, context.getSecurityContext());
		Container c;
		try {
			c = cs.get(container.uid);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
		storeService.xfer(ds, c, new ConversationStore(ds, c));
	}

}
