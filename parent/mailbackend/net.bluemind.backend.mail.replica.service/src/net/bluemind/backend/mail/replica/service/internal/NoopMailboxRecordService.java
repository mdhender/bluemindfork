/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2018
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

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.backend.mail.replica.api.IDbMailboxRecords;
import net.bluemind.backend.mail.replica.api.ImapBinding;
import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.core.api.Stream;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.Count;
import net.bluemind.core.container.model.ContainerChangelog;
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.container.model.ItemChangelog;
import net.bluemind.core.container.model.ItemFlagFilter;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.ItemVersion;
import net.bluemind.core.container.model.SortDescriptor;

public class NoopMailboxRecordService implements IDbMailboxRecords {

	private static final Logger logger = LoggerFactory.getLogger(NoopMailboxRecordService.class);

	@Override
	public ItemChangelog itemChangelog(String itemUid, Long since) throws ServerFault {
		logger.info("NOOP operation IDbMailboxRecords#itemChangelog");
		return null;
	}

	@Override
	public ContainerChangelog containerChangelog(Long since) throws ServerFault {
		logger.info("NOOP operation IDbMailboxRecords#containerChangelog");
		return null;
	}

	@Override
	public ContainerChangeset<String> changeset(Long since) throws ServerFault {
		logger.info("NOOP operation IDbMailboxRecords#changeset");
		return null;
	}

	@Override
	public ContainerChangeset<Long> changesetById(Long since) throws ServerFault {
		logger.info("NOOP operation IDbMailboxRecords#changesetById");
		return null;
	}

	@Override
	public ContainerChangeset<ItemVersion> filteredChangesetById(Long since, ItemFlagFilter filter) throws ServerFault {
		logger.info("NOOP operation IDbMailboxRecords#filteredChangesetById");
		return null;
	}

	@Override
	public long getVersion() throws ServerFault {
		logger.info("NOOP operation IDbMailboxRecords#getVersion");
		return 0;
	}

	@Override
	public void xfer(String serverUid) throws ServerFault {
		logger.info("NOOP operation IDbMailboxRecords#xfer");
	}

	@Override
	public ItemValue<MailboxRecord> getComplete(String uid) {
		logger.info("NOOP operation IDbMailboxRecords#getComplete");
		return null;
	}

	@Override
	public ItemValue<MailboxRecord> getCompleteById(long id) {
		logger.info("NOOP operation IDbMailboxRecords#getCompleteById");
		return null;
	}

	@Override
	public ItemValue<MailboxRecord> getCompleteByImapUid(long id) {
		logger.info("NOOP operation IDbMailboxRecords#getCompleteByImapUid({})", id);
		return null;
	}

	@Override
	public List<ImapBinding> imapBindings(List<Long> ids) {
		logger.info("NOOP operation IDbMailboxRecords#imapBindings");
		return Collections.emptyList();
	}

	@Override
	public List<ItemValue<MailboxRecord>> all() {
		logger.info("NOOP operation IDbMailboxRecords#all");
		return Collections.emptyList();
	}

	@Override
	public void create(String uid, MailboxRecord mail) {
		logger.info("NOOP operation IDbMailboxRecords#create");

	}

	@Override
	public void update(String uid, MailboxRecord mail) {
		logger.info("NOOP operation IDbMailboxRecords#update");

	}

	@Override
	public void delete(String uid) {
		logger.info("NOOP operation IDbMailboxRecords#containerChangelog");

	}

	@Override
	public void updates(List<MailboxRecord> records) {
		logger.info("NOOP operation IDbMailboxRecords#updates");

	}

	@Override
	public void deleteImapUids(List<Long> uids) {
		logger.info("NOOP operation IDbMailboxRecords#deleteImapUids");

	}

	@Override
	public void deleteAll() {
		logger.info("NOOP operation IDbMailboxRecords#deleteAll");

	}

	@Override
	public void prepareContainerDelete() {
		logger.info("NOOP operation IDbMailboxRecords#prepareContainerDelete");

	}

	@Override
	public Stream fetchComplete(long imapUid) {
		logger.info("NOOP operation IDbMailboxRecords#fetchComplete");
		return null;
	}

	@Override
	public List<ImapBinding> havingBodyVersionLowerThan(int version) {
		logger.info("NOOP operation IDbMailboxRecords#havingBodyVersionLowerThan");
		return Collections.emptyList();
	}

	@Override
	public Count count(ItemFlagFilter filter) throws ServerFault {
		return Count.of(0);
	}

	@Override
	public List<Long> sortedIds(SortDescriptor sorted) throws ServerFault {
		return Collections.emptyList();
	}

	@Override
	public List<ItemValue<MailboxRecord>> multipleGetById(List<Long> ids) {
		logger.info("NOOP operation IDbMailboxRecords#multipleGetById");
		return Collections.emptyList();
	}

	@Override
	public List<Long> imapIdSet(String set, String filter) {
		return Collections.emptyList();
	}

}
