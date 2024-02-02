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
package net.bluemind.backend.mailapi.storage;

import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Iterables;

import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.backend.cyrus.partitions.CyrusUniqueIds;
import net.bluemind.backend.mail.replica.api.IDbReplicatedMailboxes;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.backend.mail.replica.api.IReplicatedMailboxesRootMgmt;
import net.bluemind.backend.mail.replica.api.MailboxReplica;
import net.bluemind.backend.mail.replica.api.MailboxReplicaRootDescriptor;
import net.bluemind.backend.mail.replica.api.MailboxReplicaRootDescriptor.MailboxReplicaRootUpdate;
import net.bluemind.backend.mail.replica.api.MailboxReplicaRootDescriptor.Namespace;
import net.bluemind.backend.mail.replica.indexing.IMailIndexService;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.index.MailIndexActivator;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.MailboxQuota;
import net.bluemind.mailbox.service.IMailboxesStorage;
import net.bluemind.mailbox.service.common.DefaultFolder;
import net.bluemind.server.api.Server;
import net.bluemind.utils.ByteSizeUnit;

public class MailApiBoxStorage implements IMailboxesStorage {

	private static final Logger logger = LoggerFactory.getLogger(MailApiBoxStorage.class);

	public MailApiBoxStorage() {
		logger.info("Using mail-api based storage {}", this);
	}

	private MailboxReplicaRootDescriptor asRootDescriptor(ItemValue<Mailbox> boxItem) {
		MailboxReplicaRootDescriptor root = MailboxReplicaRootDescriptor
				.create(boxItem.value.type.sharedNs ? Namespace.shared : Namespace.users, boxItem.value.name);
		root.dataLocation = boxItem.value.dataLocation;
		return root;
	}

	@Override
	public void delete(BmContext context, String domainUid, ItemValue<Mailbox> boxItem) throws ServerFault {
		CyrusPartition partition = CyrusPartition.forServerAndDomain(boxItem.value.dataLocation, domainUid);
		IReplicatedMailboxesRootMgmt rootMgmtApi = context.provider().instance(IReplicatedMailboxesRootMgmt.class,
				partition.name);

		MailboxReplicaRootDescriptor root = asRootDescriptor(boxItem);
		logger.info("Deleting subtree {} on {}", root, partition);
		rootMgmtApi.delete(root.ns.name(), root.name);

	}

	@Override
	public void update(BmContext context, String domainUid, ItemValue<Mailbox> previousBoxItem,
			ItemValue<Mailbox> currentBoxItem) throws ServerFault {

		if (currentBoxItem.value.dataLocation == null) {
			// users & admins group (default groups) seems to be in this case
			logger.warn("Mailbox without datalocation {}", currentBoxItem.value);
			return;
		}
		if (!currentBoxItem.value.dataLocation.equals(previousBoxItem.value.dataLocation)) {
			logger.warn("Mailbox has migrated to a new server ({} => {}), which is not supported on BMv5+",
					previousBoxItem.value.dataLocation, currentBoxItem.value.dataLocation);
			return;
		}
		if (currentBoxItem.value.name.equals(previousBoxItem.value.name)) {
			// nothing to do, we are not dealing with a rename
			return;
		}
		logger.info("Rename from {} to {}", previousBoxItem.value.name, currentBoxItem.value.name);
		CyrusPartition partition = CyrusPartition.forServerAndDomain(currentBoxItem.value.dataLocation, domainUid);
		IReplicatedMailboxesRootMgmt rootMgmtApi = context.provider().instance(IReplicatedMailboxesRootMgmt.class,
				partition.name);
		MailboxReplicaRootUpdate upd = new MailboxReplicaRootUpdate();
		upd.subtreeUid = IMailReplicaUids.subtreeUid(domainUid, currentBoxItem);
		upd.from = asRootDescriptor(previousBoxItem);
		upd.to = asRootDescriptor(currentBoxItem);
		rootMgmtApi.update(upd);

	}

	@Override
	public void create(BmContext context, String domainUid, ItemValue<Mailbox> boxItem) throws ServerFault {
		if (boxItem.value.dataLocation == null) {
			// users & admins group (default groups) seems to be in this case
			logger.warn("***** WTF mbox without datalocation {}", boxItem.value);
			return;
		}
		CyrusPartition partition = CyrusPartition.forServerAndDomain(boxItem.value.dataLocation, domainUid);
		IReplicatedMailboxesRootMgmt rootMgmtApi = context.provider().instance(IReplicatedMailboxesRootMgmt.class,
				partition.name);

		MailboxReplicaRootDescriptor root = asRootDescriptor(boxItem);
		logger.info("Creating subtree {} on {}", root, partition);
		rootMgmtApi.create(root);

		IDbReplicatedMailboxes foldersApi = context.su().provider().instance(IDbReplicatedMailboxes.class,
				partition.name, boxItem.value.type.nsPrefix + boxItem.value.name);

		ensureDefaultFolders(domainUid, boxItem, foldersApi);

	}

	void ensureDefaultFolders(String domainUid, ItemValue<Mailbox> boxItem, IDbReplicatedMailboxes foldersApi) {
		if (boxItem.value.type.sharedNs) {
			mailshareFolders(domainUid, boxItem, foldersApi);
		} else {
			userFolders(domainUid, boxItem, foldersApi);
		}
	}

	private void userFolders(String domainUid, ItemValue<Mailbox> boxItem, IDbReplicatedMailboxes foldersApi) {
		for (String f : Iterables.concat(Collections.singleton("INBOX"), DefaultFolder.USER_FOLDERS_NAME)) {
			MailboxReplica repl = folder(boxItem, f);
			String uid = CyrusUniqueIds.forMailbox(domainUid, boxItem, repl.fullName).toString();
			if (foldersApi.getComplete(uid) != null) {
				foldersApi.update(uid, repl);
			} else {
				foldersApi.create(uid, repl);
			}
		}
	}

	private void mailshareFolders(String domainUid, ItemValue<Mailbox> boxItem, IDbReplicatedMailboxes foldersApi) {
		String n = boxItem.value.name;
		for (String f : Iterables.concat(Collections.singleton(n),
				DefaultFolder.MAILSHARE_FOLDERS_NAME.stream().map(f -> n + "/" + f).toList())) {
			MailboxReplica repl = folder(boxItem, f);
			String uid = CyrusUniqueIds.forMailbox(domainUid, boxItem, repl.name).toString();
			if (foldersApi.getComplete(uid) != null) {
				foldersApi.update(uid, repl);
			} else {
				foldersApi.create(uid, repl);
			}
		}
	}

	private MailboxReplica folder(ItemValue<Mailbox> boxItem, String fullName) {
		MailboxReplica f = new MailboxReplica();
		f.dataLocation = boxItem.value.dataLocation;
		f.deleted = false;
		f.fullName = fullName;
		int idx = fullName.lastIndexOf('/');
		f.name = idx > 0 ? fullName.substring(idx + 1) : fullName;
		f.highestModSeq = 0;
		f.lastUid = 0;
		f.xconvModSeq = 0;
		f.acls = Collections.emptyList();
		f.options = "PS";
		return f;
	}

	@Override
	public MailboxQuota getQuota(BmContext context, String domainUid, ItemValue<Mailbox> value) throws ServerFault {
		var mailboxQuota = new MailboxQuota();
		Integer maxQuota = value.value.quota;
		IMailIndexService mailIndexService = MailIndexActivator.getService();
		if (mailIndexService == null) {
			logger.warn("IMailIndexService is null for {} (bm/es server is required for quotas computations)", value);
		} else {
			try {
				int usedQuota = Math.toIntExact(mailIndexService.getMailboxConsumedStorage(value.uid, ByteSizeUnit.KB));
				mailboxQuota.used = usedQuota;
				mailboxQuota.quota = maxQuota;
			} catch (ArithmeticException e) {
				throw new ServerFault(e);
			}
		}
		return mailboxQuota;
	}

	@Override
	public void initialize(BmContext context, ItemValue<Server> server) throws ServerFault {
		logger.info("init server {}", server);
	}

	@Override
	public boolean mailboxExist(BmContext context, String domainUid, ItemValue<Mailbox> mailbox) throws ServerFault {
		String uid = IMailReplicaUids.subtreeUid(domainUid, mailbox);
		IContainers contApi = context.provider().instance(IContainers.class);
		return contApi.getLightIfPresent(uid) != null;
	}

	@Override
	public void move(String domainUid, ItemValue<Mailbox> mailbox, ItemValue<Server> sourceServer,
			ItemValue<Server> dstServer) {
		// OK
		logger.error("MOVE will not be performed for {}", mailbox);
	}

}
