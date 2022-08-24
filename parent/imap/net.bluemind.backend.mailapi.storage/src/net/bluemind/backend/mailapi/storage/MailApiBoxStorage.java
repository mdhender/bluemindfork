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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.backend.cyrus.partitions.CyrusUniqueIds;
import net.bluemind.backend.mail.replica.api.IDbReplicatedMailboxes;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.backend.mail.replica.api.IReplicatedMailboxesRootMgmt;
import net.bluemind.backend.mail.replica.api.MailboxReplica;
import net.bluemind.backend.mail.replica.api.MailboxReplicaRootDescriptor;
import net.bluemind.backend.mail.replica.api.MailboxReplicaRootDescriptor.MailboxReplicaRootUpdate;
import net.bluemind.backend.mail.replica.api.MailboxReplicaRootDescriptor.Namespace;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.rest.BmContext;
import net.bluemind.domain.api.Domain;
import net.bluemind.mailbox.api.MailFilter;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.MailboxQuota;
import net.bluemind.mailbox.service.IMailboxesStorage;
import net.bluemind.mailbox.service.common.DefaultFolder;
import net.bluemind.mailbox.service.common.DefaultFolder.Status;
import net.bluemind.server.api.Server;

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
	public void rewriteCyrusConfiguration(String serverUid) {
		// OK
		logger.info("cyrus is not here, rewriteCyrusConfiguration({}) does nothing", serverUid);
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
			logger.warn("***** WTF mbox without datalocation {}", currentBoxItem.value);
			return;
		}
		if (!currentBoxItem.value.dataLocation.equals(previousBoxItem.value.dataLocation)) {
			logger.warn("**** Mailbox has migrated to a new server ({} => {}), let the replication deal with that",
					previousBoxItem.value.dataLocation, currentBoxItem.value.dataLocation);
			return;
		}
		if (currentBoxItem.value.name.equals(previousBoxItem.value.name)) {
			// nothing to do, we are not dealing with a rename
			return;
		}
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

		IDbReplicatedMailboxes foldersApi = context.provider().instance(IDbReplicatedMailboxes.class, partition.name,
				boxItem.value.type.nsPrefix + boxItem.value.name);

		if (boxItem.value.type.sharedNs) {
			String n = boxItem.value.name;
			for (String f : Arrays.asList(n, n + "/Sent", n + "/Trash", n + "/Templates")) {
				MailboxReplica repl = folder(boxItem, f);
				String fn = f.equals(boxItem.value.name) ? "" : repl.name;
				String uid = CyrusUniqueIds.forMailbox(domainUid, boxItem, fn).toString();
				if (foldersApi.getComplete(uid) != null) {
					foldersApi.update(uid, repl);
				} else {
					foldersApi.create(uid, repl);
				}

			}
		} else {
			for (String f : Arrays.asList("INBOX", "Sent", "Drafts", "Trash", "Outbox", "Junk", "Templates")) {
				MailboxReplica repl = folder(boxItem, f);
				String uid = CyrusUniqueIds.forMailbox(domainUid, boxItem, repl.fullName).toString();
				if (foldersApi.getComplete(uid) != null) {
					foldersApi.update(uid, repl);
				} else {
					foldersApi.create(uid, repl);
				}
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
		// TODO Auto-generated method stub
		return new MailboxQuota();
	}

	@Override
	public void changeFilter(BmContext context, ItemValue<Domain> domain, ItemValue<Mailbox> value, MailFilter filter)
			throws ServerFault {
		// TODO Auto-generated method stub

	}

	@Override
	public void changeDomainFilter(BmContext context, String domainUid, MailFilter filter) throws ServerFault {
		// TODO Auto-generated method stub

	}

	@Override
	public void createDomainPartition(BmContext context, ItemValue<Domain> value, ItemValue<Server> server)
			throws ServerFault {
		// TODO Auto-generated method stub
		System.err.println("createDomainPartition " + value);

	}

	@Override
	public void deleteDomainPartition(BmContext context, ItemValue<Domain> value, ItemValue<Server> server)
			throws ServerFault {
		// TODO Auto-generated method stub

	}

	@Override
	public void initialize(BmContext context, ItemValue<Server> server) throws ServerFault {
		// TODO Auto-generated method stub
		logger.info("init server {}", server);
		System.err.println("init " + server);

	}

	@Override
	public boolean mailboxExist(BmContext context, String domainUid, ItemValue<Mailbox> mailbox) throws ServerFault {
		String uid = IMailReplicaUids.subtreeUid(domainUid, mailbox);
		IContainers contApi = context.provider().instance(IContainers.class);
		return contApi.getIfPresent(uid) != null;
	}

	@Override
	public List<MailFolder> checkAndRepairHierarchy(BmContext context, String domainUid, ItemValue<Mailbox> mailbox,
			boolean repair) throws ServerFault {
		return Collections.emptyList();
	}

	@Override
	public void checkAndRepairQuota(BmContext context, String domainUid, ItemValue<Mailbox> mailbox) {
		// OK
	}

	@Override
	public void checkAndRepairFilesystem(BmContext context, String domainUid, ItemValue<Mailbox> mailbox) {
		// OK
	}

	@Override
	public Status checkAndRepairDefaultFolders(BmContext context, String domainUid, ItemValue<Mailbox> mailbox,
			boolean repair) {
		return new DefaultFolder.Status();
	}

	@Override
	public List<MailFolder> checkAndRepairAcl(BmContext context, String domainUid, ItemValue<Mailbox> mailbox,
			List<AccessControlEntry> acls, boolean repair) throws ServerFault {
		return Collections.emptyList();
	}

	@Override
	public CheckAndRepairStatus checkAndRepairSharedSeen(BmContext context, String domainUid,
			ItemValue<Mailbox> mailbox, boolean repair) {
		return new CheckAndRepairStatus(0, 0, 0);
	}

	@Override
	public void move(String domainUid, ItemValue<Mailbox> mailbox, ItemValue<Server> sourceServer,
			ItemValue<Server> dstServer) {
		// OK

	}

}
