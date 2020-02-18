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
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.backend.mail.replica.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.bluemind.backend.mail.api.IMailboxFoldersByContainer;
import net.bluemind.backend.mail.api.IUserInbox;
import net.bluemind.backend.mail.replica.api.MailboxReplicaRootDescriptor.Namespace;
import net.bluemind.backend.mail.replica.api.utils.Subtree;
import net.bluemind.backend.mail.replica.utils.SubtreeContainer;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;

public class UserInboxServiceFactory implements ServerSideServiceProvider.IServerSideServiceFactory<IUserInbox> {

	@Override
	public Class<IUserInbox> factoryClass() {
		return IUserInbox.class;
	}

	private static final Map<String, String> uidToInboxUid = new ConcurrentHashMap<>();

	@Override
	public IUserInbox instance(BmContext context, String... params) {
		if (params.length < 2) {
			throw new ServerFault("domainUid & userUid are required");
		}
		String domainUid = params[0];
		String userUid = params[1];
		Subtree subtree = SubtreeContainer.mailSubtreeUid(domainUid, Namespace.users, userUid);
		String inboxUid = uidToInboxUid.computeIfAbsent(subtree.subtreeUid(), treeUid -> {
			IMailboxFoldersByContainer foldersApi = context.provider().instance(IMailboxFoldersByContainer.class,
					treeUid);
			return foldersApi.byName("INBOX").uid;
		});
		return new UserInboxService(context, domainUid, userUid, inboxUid);
	}

}
