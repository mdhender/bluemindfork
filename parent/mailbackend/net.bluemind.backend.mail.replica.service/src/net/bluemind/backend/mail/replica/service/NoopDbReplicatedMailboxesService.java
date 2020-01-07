/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2019
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

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.backend.mail.api.MailboxFolderSearchQuery;
import net.bluemind.backend.mail.api.SearchResult;
import net.bluemind.backend.mail.replica.api.IDbByContainerReplicatedMailboxes;
import net.bluemind.backend.mail.replica.api.IDbReplicatedMailboxes;
import net.bluemind.backend.mail.replica.api.MailboxReplica;
import net.bluemind.backend.mail.replica.api.MailboxReplicaRootDescriptor;
import net.bluemind.core.container.model.ContainerChangelog;
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.container.model.ItemChangelog;
import net.bluemind.core.container.model.ItemFlagFilter;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.ItemVersion;

public class NoopDbReplicatedMailboxesService implements IDbReplicatedMailboxes, IDbByContainerReplicatedMailboxes {

	private static final Logger logger = LoggerFactory.getLogger(NoopDbReplicatedMailboxesService.class);

	private final MailboxReplicaRootDescriptor mailboxRoot;
	private final String domainUid;

	public NoopDbReplicatedMailboxesService(MailboxReplicaRootDescriptor mailboxRoot, String domainUid) {
		this.mailboxRoot = mailboxRoot;
		this.domainUid = domainUid;
	}

	@Override
	public void xfer(String serverUid) {
		logger.info("NOOP xfer on deleted mailbox {}@{}", mailboxRoot.name, domainUid);
	}

	@Override
	public ItemChangelog itemChangelog(String itemUid, Long since) {
		logger.info("NOOP itemChangelog on deleted mailbox {}@{}", mailboxRoot.name, domainUid);
		return null;
	}

	@Override
	public long getVersion() {
		logger.info("NOOP getVersion on deleted mailbox {}@{}", mailboxRoot.name, domainUid);
		return -1;
	}

	@Override
	public ContainerChangeset<ItemVersion> filteredChangesetById(Long since, ItemFlagFilter filter) {
		logger.info("NOOP filteredChangesetById on deleted mailbox {}@{}", mailboxRoot.name, domainUid);
		return null;
	}

	@Override
	public ContainerChangelog containerChangelog(Long since) {
		logger.info("NOOP containerChangelog on deleted mailbox {}@{}", mailboxRoot.name, domainUid);
		return null;
	}

	@Override
	public ContainerChangeset<Long> changesetById(Long since) {
		logger.info("NOOP changesetById on deleted mailbox {}@{}", mailboxRoot.name, domainUid);
		return null;
	}

	@Override
	public ContainerChangeset<String> changeset(Long since) {
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

}
