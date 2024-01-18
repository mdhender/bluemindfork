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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.backend.mail.replica.service.internal.transfer;

import java.util.List;
import java.util.function.Consumer;

import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.backend.mail.replica.api.WithId;
import net.bluemind.backend.mail.replica.service.internal.ItemsTransferServiceFactory.BodyTransfer;
import net.bluemind.backend.mail.replica.service.internal.Trash;
import net.bluemind.core.rest.BmContext;

public class CrossMailboxTransferService extends BaseMailboxTranferService {

	public CrossMailboxTransferService(BmContext context, TransferContext transferContext) {
		super(context, transferContext);
	}

	@Override
	protected BodyTransfer bodyXfer() {
		return BodyTransfer.NOOP;
	}

	@Override
	protected Consumer<List<WithId<MailboxRecord>>> getPostMoveOperation() {
		return records -> {
			String subtreeContainer = IMailReplicaUids.subtreeUid(transferContext.domain(),
					transferContext.fromOwner());
			long folderId = transferContext.fromFolder().internalId;

			Trash trash = new Trash(context, subtreeContainer, transferContext.fromRecords());
			trash.deleteItems(folderId, records.stream().map(rec -> rec.itemId).toList());
		};
	}

}
