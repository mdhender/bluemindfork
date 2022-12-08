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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.backend.mail.replica.service.internal;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import net.bluemind.backend.mail.api.IItemsTransfer;
import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.backend.mail.api.flags.MailboxItemFlag;
import net.bluemind.backend.mail.replica.api.AppendTx;
import net.bluemind.backend.mail.replica.api.IDbByContainerReplicatedMailboxes;
import net.bluemind.backend.mail.replica.api.IDbMailboxRecords;
import net.bluemind.backend.mail.replica.api.IDbReplicatedMailboxes;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.backend.mail.replica.api.MailboxRecord.InternalFlag;
import net.bluemind.backend.mail.replica.api.WithId;
import net.bluemind.backend.mail.replica.service.sds.MessageBodyObjectStore;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemIdentifier;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.persistence.DataSourceRouter;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;

public class ItemsTransferService implements IItemsTransfer {

	public static class Factory implements ServerSideServiceProvider.IServerSideServiceFactory<IItemsTransfer> {

		@Override
		public Class<IItemsTransfer> factoryClass() {
			return IItemsTransfer.class;
		}

		@Override
		public IItemsTransfer instance(BmContext context, String... params) {
			if (params.length != 2) {
				throw new ServerFault("fromMailboxUid & toMailboxUid are required.");
			}
			return new ItemsTransferService(context, params[0], params[1]);
		}

	}

	@FunctionalInterface
	public interface PostCopyOp {
		void operation(List<WithId<MailboxRecord>> srcItems);
	}

	public interface ICopyStrategy {
		List<ItemIdentifier> copy(List<Long> itemIds, PostCopyOp op);
	}

	private static final Logger logger = LoggerFactory.getLogger(ItemsTransferService.class);

	private final IDbMailboxRecords fromRecords;
	private final IDbMailboxRecords toRecords;
	private final ICopyStrategy copyStrat;

	private final ItemValue<Mailbox> fromOwner;
	private final ItemValue<Mailbox> toOwner;

	private IDbReplicatedMailboxes toFolders;

	private ItemValue<MailboxFolder> target;

	public ItemsTransferService(BmContext context, String fromUid, String toUid) {
		IContainers contApi = context.provider().instance(IContainers.class);
		ContainerDescriptor fromContainer = contApi.getIfPresent(IMailReplicaUids.mboxRecords(fromUid));
		if (fromContainer == null) {
			throw ServerFault.notFound("container " + IMailReplicaUids.mboxRecords(fromUid) + " not found.");
		}
		ContainerDescriptor toContainer = contApi.getIfPresent(IMailReplicaUids.mboxRecords(toUid));
		if (toContainer == null) {
			throw ServerFault.notFound("container " + IMailReplicaUids.mboxRecords(toUid) + " not found.");
		}

		IMailboxes mboxApi = context.su().provider().instance(IMailboxes.class, fromContainer.domainUid);
		this.fromOwner = mboxApi.getComplete(fromContainer.owner);
		this.toOwner = fromContainer.owner.equals(toContainer.owner) ? fromOwner
				: mboxApi.getComplete(toContainer.owner);

		this.fromRecords = context.provider().instance(IDbMailboxRecords.class, fromUid);
		this.toRecords = context.provider().instance(IDbMailboxRecords.class, toUid);
		this.toFolders = context.provider().instance(IDbByContainerReplicatedMailboxes.class,
				IMailReplicaUids.subtreeUid(toContainer.domainUid, toOwner));
		this.target = toFolders.getComplete(toUid);
		this.copyStrat = loadStrat(context, fromUid, toUid);
	}

	private ICopyStrategy loadStrat(BmContext context, String fromUid, String toUid) {
		String loc1 = DataSourceRouter.location(context, IMailReplicaUids.mboxRecords(fromUid));
		String loc2 = DataSourceRouter.location(context, IMailReplicaUids.mboxRecords(toUid));
		return new MailApiCopyStrategy(context, loc1, loc2);
	}

	@Override
	public List<ItemIdentifier> copy(List<Long> itemIds) {
		return transferImpl(itemIds, x -> {
		});
	}

	public interface BodyTransfer {
		public static final BodyTransfer NOOP = (guid, date) -> {
		};

		void transfer(String guid, Date date);
	}

	public class MailApiCopyStrategy implements ICopyStrategy {

		private BodyTransfer bodyXfer;

		public MailApiCopyStrategy(BmContext context, String sourceLocation, String targetLocation) {
			bodyXfer = BodyTransfer.NOOP;
			if (!targetLocation.equals(sourceLocation)) {
				MessageBodyObjectStore srcBodies = new MessageBodyObjectStore(context, sourceLocation);
				if (!srcBodies.isSingleNamespaceBody()) {
					MessageBodyObjectStore tgtBodies = new MessageBodyObjectStore(context, targetLocation);
					this.bodyXfer = (guid, date) -> {
						File toXfer = null;
						try {
							toXfer = srcBodies.open(guid).toFile();
							tgtBodies.store(guid, date, toXfer);
						} finally {
							if (toXfer != null) {
								toXfer.delete();// NOSONAR do not throw
							}
						}
					};
				}
			}
		}

		public List<ItemIdentifier> copy(List<Long> itemIds, PostCopyOp op) {
			List<ItemIdentifier> ret = new ArrayList<>(itemIds.size());
			for (List<Long> slice : Lists.partition(itemIds, 500)) {
				List<WithId<MailboxRecord>> records = fromRecords.slice(slice);
				AppendTx tx = toFolders.prepareAppend(target.internalId, records.size());
				long start = tx.imapUid - (records.size() - 1);
				long end = tx.imapUid;
				logger.info("Create imapUids [ {} - {} ]", start, end);
				long cnt = start;
				List<MailboxRecord> copies = new ArrayList<>(records.size());
				for (WithId<MailboxRecord> iv : records) {
					MailboxRecord copy = iv.value.copy();
					copy.imapUid = cnt++;
					bodyXfer.transfer(copy.messageBody, copy.internalDate);
					copies.add(copy);
				}
				ret.addAll(toRecords.multiCreate(copies));
				op.operation(records);
			}
			return ret;
		}
	}

	public List<ItemIdentifier> transferImpl(List<Long> itemIds, PostCopyOp op) {
		return copyStrat.copy(itemIds, op);
	}

	@Override
	public List<ItemIdentifier> move(List<Long> itemIds) {

		return transferImpl(itemIds, origSlice -> {
			MailboxItemFlag delFlag = MailboxItemFlag.System.Deleted.value();
			List<MailboxRecord> flagged = origSlice.stream().map(wid -> {
				MailboxRecord ret = wid.value;
				ret.flags.add(delFlag);
				ret.internalFlags.add(InternalFlag.expunged);
				return ret;
			}).toList();
			fromRecords.updates(flagged);
		});
	}

}
