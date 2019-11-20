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
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.backend.mail.replica.service.internal.hooks;

import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.backend.mail.replica.api.IReplicatedMailboxesRootMgmt;
import net.bluemind.backend.mail.replica.api.MailboxReplicaRootDescriptor;
import net.bluemind.backend.mail.replica.api.MailboxReplicaRootDescriptor.MailboxReplicaRootUpdate;
import net.bluemind.backend.mail.replica.api.MailboxReplicaRootDescriptor.Namespace;
import net.bluemind.backend.mail.replica.persistence.DeletedMailboxesStore;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.mailbox.api.MailFilter;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.hook.IMailboxHook;

public class MailboxSubtreeHook implements IMailboxHook {

	private static final Logger logger = LoggerFactory.getLogger(MailboxSubtreeHook.class);

	@Override
	public void preMailboxCreated(BmContext context, String domainUid, String name) throws ServerFault {
		forgetDeletion(context, domainUid, name);
	}

	@Override
	public void onMailboxCreated(BmContext context, String domainUid, ItemValue<Mailbox> boxItem) throws ServerFault {
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

	}

	private void forgetDeletion(BmContext context, String domainUid, String name) {
		try {
			logger.info("Ensure we don't consider {}@{} as deleted", name, domainUid);
			DeletedMailboxesStore deletedData = new DeletedMailboxesStore(context.getDataSource());
			deletedData.deleteByName(domainUid, name);
		} catch (SQLException e) {
			logger.error(e.getMessage(), e);
		}
	}

	private MailboxReplicaRootDescriptor asRootDescriptor(ItemValue<Mailbox> boxItem) {
		MailboxReplicaRootDescriptor root = MailboxReplicaRootDescriptor
				.create(boxItem.value.type.sharedNs ? Namespace.shared : Namespace.users, boxItem.value.name);
		root.dataLocation = boxItem.value.dataLocation;
		return root;
	}

	@Override
	public void onMailboxUpdated(BmContext context, String domainUid, ItemValue<Mailbox> previousBoxItem,
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
		upd.from = asRootDescriptor(previousBoxItem);
		upd.to = asRootDescriptor(currentBoxItem);
		rootMgmtApi.update(upd);
	}

	@Override
	public void onMailboxDeleted(BmContext context, String domainUid, ItemValue<Mailbox> boxItem) throws ServerFault {
		CyrusPartition partition = CyrusPartition.forServerAndDomain(boxItem.value.dataLocation, domainUid);
		IReplicatedMailboxesRootMgmt rootMgmtApi = context.provider().instance(IReplicatedMailboxesRootMgmt.class,
				partition.name);

		MailboxReplicaRootDescriptor root = asRootDescriptor(boxItem);
		logger.info("Deleting subtree {} on {}", root, partition);
		rootMgmtApi.delete(root.ns.name(), root.name);
	}

	@Override
	public void onMailFilterChanged(BmContext context, String domainUid, ItemValue<Mailbox> mailbox, MailFilter filter)
			throws ServerFault {
	}

	@Override
	public void onDomainMailFilterChanged(BmContext context, String domainUid, MailFilter filter) throws ServerFault {
	}

	@Override
	public void preMailboxDeleted(BmContext context, String domainUid, ItemValue<Mailbox> value) throws ServerFault {
		DeletedDataMementos.preDelete(context, domainUid, value);
	}

}
