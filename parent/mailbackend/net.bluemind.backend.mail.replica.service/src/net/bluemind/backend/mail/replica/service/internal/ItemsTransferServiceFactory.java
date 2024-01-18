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

import java.util.Date;

import net.bluemind.backend.mail.api.IItemsTransfer;
import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.backend.mail.replica.api.IDbByContainerReplicatedMailboxes;
import net.bluemind.backend.mail.replica.api.IDbMailboxRecords;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.backend.mail.replica.service.internal.transfer.CrossMailboxCrossLocationTransferService;
import net.bluemind.backend.mail.replica.service.internal.transfer.CrossMailboxTransferService;
import net.bluemind.backend.mail.replica.service.internal.transfer.SameMailboxTranferService;
import net.bluemind.backend.mail.replica.service.internal.transfer.TransferContext;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.BaseContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.persistence.DataSourceRouter;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;

public class ItemsTransferServiceFactory
		implements ServerSideServiceProvider.IServerSideServiceFactory<IItemsTransfer> {

	public interface BodyTransfer {
		public static final BodyTransfer NOOP = (guid, date) -> {
		};

		void transfer(String guid, Date date);
	}

	@Override
	public Class<IItemsTransfer> factoryClass() {
		return IItemsTransfer.class;
	}

	@Override
	public IItemsTransfer instance(BmContext context, String... params) {
		if (params.length != 2) {
			throw new ServerFault("fromMailboxUid & toMailboxUid are required.");
		}

		String fromUid = params[0];
		String toUid = params[1];

		IContainers contApi = context.provider().instance(IContainers.class);
		BaseContainerDescriptor fromContainer = contApi.getLightIfPresent(IMailReplicaUids.mboxRecords(fromUid));
		if (fromContainer == null) {
			throw ServerFault.notFound("container " + IMailReplicaUids.mboxRecords(fromUid) + " not found.");
		}
		BaseContainerDescriptor toContainer = contApi.getLightIfPresent(IMailReplicaUids.mboxRecords(toUid));
		if (toContainer == null) {
			throw ServerFault.notFound("container " + IMailReplicaUids.mboxRecords(toUid) + " not found.");
		}

		IMailboxes mboxApi = context.su().provider().instance(IMailboxes.class, fromContainer.domainUid);
		ItemValue<Mailbox> fromOwner = mboxApi.getComplete(fromContainer.owner);
		ItemValue<Mailbox> toOwner = fromContainer.owner.equals(toContainer.owner) ? fromOwner
				: mboxApi.getComplete(toContainer.owner);
		IDbMailboxRecords fromRecords = context.provider().instance(IDbMailboxRecords.class, fromUid);
		IDbMailboxRecords toRecords = context.provider().instance(IDbMailboxRecords.class, toUid);
		IDbByContainerReplicatedMailboxes toFolders = context.provider().instance(
				IDbByContainerReplicatedMailboxes.class, IMailReplicaUids.subtreeUid(toContainer.domainUid, toOwner));
		IDbByContainerReplicatedMailboxes fromFolders = context.provider().instance(
				IDbByContainerReplicatedMailboxes.class, IMailReplicaUids.subtreeUid(toContainer.domainUid, fromOwner));
		ItemValue<MailboxFolder> sourceFolder = fromFolders.getComplete(fromUid);
		ItemValue<MailboxFolder> targetFolder = toFolders.getComplete(toUid);

		String fromLocation = DataSourceRouter.location(context, IMailReplicaUids.mboxRecords(fromUid));
		String toLocation = DataSourceRouter.location(context, IMailReplicaUids.mboxRecords(toUid));

		TransferContext transferContext = new TransferContext(fromRecords, toRecords, fromOwner, toOwner, toFolders,
				sourceFolder, targetFolder, fromContainer.domainUid);

		if (fromOwner.uid.equals(toOwner.uid)) {
			return new SameMailboxTranferService(context, transferContext);
		} else if (fromLocation.equals(toLocation)) {
			return new CrossMailboxTransferService(context, transferContext);
		} else {
			return new CrossMailboxCrossLocationTransferService(context, transferContext, fromLocation, toLocation);
		}
	}

}
