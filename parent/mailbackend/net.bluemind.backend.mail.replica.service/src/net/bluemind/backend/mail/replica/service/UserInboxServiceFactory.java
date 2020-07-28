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
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.backend.mail.api.IUserInbox;
import net.bluemind.backend.mail.replica.api.IDbByContainerReplicatedMailboxes;
import net.bluemind.backend.mail.replica.api.MailboxReplicaRootDescriptor.Namespace;
import net.bluemind.backend.mail.replica.api.utils.Subtree;
import net.bluemind.backend.mail.replica.utils.SubtreeContainer;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;

public class UserInboxServiceFactory implements ServerSideServiceProvider.IServerSideServiceFactory<IUserInbox> {

	private static final Logger logger = LoggerFactory.getLogger(UserInboxServiceFactory.class);

	@Override
	public Class<IUserInbox> factoryClass() {
		return IUserInbox.class;
	}

	private static final Map<String, String> uidToInboxUid = new ConcurrentHashMap<>();

	private static final class NoopUserInbox implements IUserInbox {

		private static final Integer ZERO = Integer.valueOf(0);
		private final String failedUid;
		private final String failedDomain;

		private NoopUserInbox(String uid, String domainUid) {
			this.failedUid = uid;
			this.failedDomain = domainUid;
		}

		@Override
		public Integer unseen() {
			logger.warn("NOOP unseen count for {} @ {}", failedUid, failedDomain);
			return ZERO;
		}

	}

	@Override
	public IUserInbox instance(BmContext context, String... params) {
		if (params.length < 2) {
			throw new ServerFault("domainUid & userUid are required");
		}
		String domainUid = params[0];
		String userUid = params[1];
		Subtree subtree = SubtreeContainer.mailSubtreeUid(domainUid, Namespace.users, userUid);
		try {
			String inboxUid = uidToInboxUid.computeIfAbsent(subtree.subtreeUid(), treeUid -> {
				IDbByContainerReplicatedMailboxes foldersApi = context.provider()
						.instance(IDbByContainerReplicatedMailboxes.class, treeUid);
				return Optional.ofNullable(foldersApi.byName("INBOX")).map(f -> f.uid)
						.orElseThrow(() -> new NullPointerException("INBOX not found"));
			});
			return new UserInboxService(context, domainUid, userUid, inboxUid);
		} catch (Exception e) {// NOSONAR log or re-throw
			logger.warn("Failed to map uid {} (dom: {}) to an inbox uid", userUid, domainUid, e.getMessage());
			return new NoopUserInbox(userUid, domainUid);
		}
	}

}
