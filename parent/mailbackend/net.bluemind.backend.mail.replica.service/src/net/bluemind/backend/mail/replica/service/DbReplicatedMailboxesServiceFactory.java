/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2017
 *
 * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License)
 * or the CeCILL as published by CeCILL.info (version 2 of the License).
 *
 * There are special exceptions to the terms and conditions of the
 * licenses as they are applied to this program. See LICENSE.txt in
 * the directory of this program distribution.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.backend.mail.replica.service;

import java.util.Collections;
import java.util.List;

import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.backend.mail.api.MailboxFolderSearchQuery;
import net.bluemind.backend.mail.api.SearchResult;
import net.bluemind.backend.mail.replica.api.IDbReplicatedMailboxes;
import net.bluemind.backend.mail.replica.api.MailboxReplica;
import net.bluemind.backend.mail.replica.api.MailboxReplicaRootDescriptor;
import net.bluemind.backend.mail.replica.persistence.MailboxReplicaStore;
import net.bluemind.backend.mail.replica.service.internal.DbReplicatedMailboxesService;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ContainerChangelog;
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.container.model.ItemChangelog;
import net.bluemind.core.container.model.ItemFlagFilter;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.ItemVersion;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.service.internal.ContainerStoreService;
import net.bluemind.core.rest.BmContext;

public class DbReplicatedMailboxesServiceFactory
		extends AbstractReplicatedMailboxesServiceFactory<IDbReplicatedMailboxes> {

	public DbReplicatedMailboxesServiceFactory() {
	}

	@Override
	public Class<IDbReplicatedMailboxes> factoryClass() {
		return IDbReplicatedMailboxes.class;
	}

	@Override
	protected IDbReplicatedMailboxes create(MailboxReplicaRootDescriptor root, Container cont, BmContext context,
			MailboxReplicaStore mboxReplicaStore, ContainerStoreService<MailboxReplica> storeService,
			ContainerStore containerStore) {
		return new DbReplicatedMailboxesService(root, cont, context, mboxReplicaStore, storeService, containerStore);
	}

	@Override
	protected IDbReplicatedMailboxes createNoopService(MailboxReplicaRootDescriptor mailboxRoot, String domainUid) {
		return new IDbReplicatedMailboxes() {

			@Override
			public void xfer(String serverUid) throws ServerFault {
				logger.info("NOOP xfer on deleted mailbox {}@{}", mailboxRoot.name, domainUid);
			}

			@Override
			public ItemChangelog itemChangelog(String itemUid, Long since) throws ServerFault {
				logger.info("NOOP itemChangelog on deleted mailbox {}@{}", mailboxRoot.name, domainUid);
				return null;
			}

			@Override
			public long getVersion() throws ServerFault {
				logger.info("NOOP getVersion on deleted mailbox {}@{}", mailboxRoot.name, domainUid);
				return -1;
			}

			@Override
			public ContainerChangeset<ItemVersion> filteredChangesetById(Long since, ItemFlagFilter filter)
					throws ServerFault {
				logger.info("NOOP filteredChangesetById on deleted mailbox {}@{}", mailboxRoot.name, domainUid);
				return null;
			}

			@Override
			public ContainerChangelog containerChangelog(Long since) throws ServerFault {
				logger.info("NOOP containerChangelog on deleted mailbox {}@{}", mailboxRoot.name, domainUid);
				return null;
			}

			@Override
			public ContainerChangeset<Long> changesetById(Long since) throws ServerFault {
				logger.info("NOOP changesetById on deleted mailbox {}@{}", mailboxRoot.name, domainUid);
				return null;
			}

			@Override
			public ContainerChangeset<String> changeset(Long since) throws ServerFault {
				logger.info("NOOP changeset on deleted mailbox {}@{}", mailboxRoot.name, domainUid);
				return null;
			}

			@Override
			public ItemValue<MailboxFolder> getComplete(String uid) {
				logger.info("NOOP getComplete on deleted mailbox {}@{}", mailboxRoot.name, domainUid);
				return null;
			}

			@Override
			public ItemValue<MailboxFolder> byName(String name) {
				logger.info("NOOP byName on deleted mailbox {}@{}", mailboxRoot.name, domainUid);
				return null;
			}

			@Override
			public List<ItemValue<MailboxFolder>> all() {
				logger.info("NOOP all on deleted mailbox {}@{}", mailboxRoot.name, domainUid);
				return Collections.emptyList();
			}

			@Override
			public void update(String uid, MailboxReplica replica) {
				logger.info("NOOP update on deleted mailbox {}@{}", mailboxRoot.name, domainUid);
			}

			@Override
			public void delete(String uid) {
				logger.info("NOOP delete on deleted mailbox {}@{}", mailboxRoot.name, domainUid);
			}

			@Override
			public void create(String uid, MailboxReplica replica) {
				logger.info("NOOP create on deleted mailbox {}@{}", mailboxRoot.name, domainUid);
			}

			@Override
			public ItemValue<MailboxReplica> byReplicaName(String name) {
				logger.info("NOOP byReplicaName on deleted mailbox {}@{}", mailboxRoot.name, domainUid);
				return null;
			}

			@Override
			public List<ItemValue<MailboxReplica>> allReplicas() {
				logger.info("NOOP allReplicas on deleted mailbox {}@{}", mailboxRoot.name, domainUid);
				return Collections.emptyList();
			}

			@Override
			public SearchResult searchItems(MailboxFolderSearchQuery query) {
				logger.info("NOOP searchItems on deleted mailbox {}@{}", mailboxRoot.name, domainUid);
				return new SearchResult();
			}

		};
	}

}
