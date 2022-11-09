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
import java.util.Collections;
import java.util.List;

import javax.sql.DataSource;

import jakarta.ws.rs.PathParam;
import net.bluemind.backend.mail.api.Conversation;
import net.bluemind.backend.mail.api.IMailConversation;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.backend.mail.replica.persistence.MailboxRecordConversationStore;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.SortDescriptor;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.rest.BmContext;
import net.bluemind.mailbox.api.IMailboxAclUids;

public class MailConversationService implements IMailConversation {
	private final MailboxRecordConversationStore conversationStore;
	private final RBACManager rbacManager;
	private final ContainerStore containerStore;

	public MailConversationService(BmContext context, DataSource ds, Container subtreeContainer) {
		this.rbacManager = RBACManager.forContext(context)
				.forContainer(IMailboxAclUids.uidForMailbox(subtreeContainer.owner));
		this.containerStore = new ContainerStore(context, ds, context.getSecurityContext());
		this.conversationStore = new MailboxRecordConversationStore(ds, subtreeContainer);
	}

	@Override
	public Conversation get(@PathParam(value = "uid") String uid) {
		rbacManager.check(Verb.Read.name());
		try {
			return conversationStore.get(uid);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	@Override
	public List<Conversation> multipleGet(List<String> uids) throws ServerFault {
		rbacManager.check(Verb.Read.name());
		if (uids.isEmpty()) {
			return Collections.emptyList();
		} else if (uids.size() > 200) {
			throw new ServerFault("Invalid parameters: should not specify more than 200 conversationUids at a time");
		}
		try {
			return conversationStore.getMultiple(uids);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	@Override
	public List<String> byFolder(String folderUid, SortDescriptor sorted) {
		rbacManager.check(Verb.Read.name());
		try {
			return conversationStore.getConversationIds(getFolderIdFromUid(folderUid), sorted).stream()
					.map(Long::toHexString).toList();
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	private long getFolderIdFromUid(String folderUid) {
		try {
			return containerStore.get(IMailReplicaUids.mboxRecords(folderUid)).id;
		} catch (SQLException e) {
			throw new ServerFault(e);
		}
	}

}
