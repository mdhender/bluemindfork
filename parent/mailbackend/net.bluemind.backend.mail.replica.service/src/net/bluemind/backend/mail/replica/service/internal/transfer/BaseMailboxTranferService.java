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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import net.bluemind.backend.mail.api.IItemsTransfer;
import net.bluemind.backend.mail.replica.api.AppendTx;
import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.backend.mail.replica.api.WithId;
import net.bluemind.backend.mail.replica.service.internal.ItemsTransferServiceFactory.BodyTransfer;
import net.bluemind.core.container.model.ItemIdentifier;
import net.bluemind.core.rest.BmContext;

public abstract class BaseMailboxTranferService implements IItemsTransfer {

	protected final Logger logger = LoggerFactory.getLogger(BaseMailboxTranferService.class);
	protected final TransferContext transferContext;
	protected final BmContext context;

	protected BaseMailboxTranferService(BmContext context, TransferContext transferContext) {
		this.context = context;
		this.transferContext = transferContext;
	}

	protected abstract BodyTransfer bodyXfer();

	protected abstract Consumer<List<WithId<MailboxRecord>>> getPostMoveOperation();

	@Override
	public List<ItemIdentifier> move(List<Long> itemIds) {
		return transfer(itemIds, getPostMoveOperation());
	}

	@Override
	public List<ItemIdentifier> copy(List<Long> itemIds) {
		return transfer(itemIds, recs -> {
		});
	}

	private List<ItemIdentifier> transfer(List<Long> itemIds, Consumer<List<WithId<MailboxRecord>>> postOp) {
		List<ItemIdentifier> ret = new ArrayList<>(itemIds.size());
		for (List<Long> slice : Lists.partition(itemIds, 500)) {
			List<WithId<MailboxRecord>> records = transferContext.fromRecords().slice(slice);
			AppendTx tx = transferContext.toFolder().prepareAppend(transferContext.targetFolder().internalId,
					records.size());
			long start = tx.imapUid - (records.size() - 1);
			long end = tx.imapUid;
			logger.debug("Creating imapUids [ {} - {} ]", start, end);
			logger.debug("Moving {} message(s).", records.size());
			long cnt = start;
			List<MailboxRecord> copies = new ArrayList<>(records.size());
			for (WithId<MailboxRecord> iv : records) {
				MailboxRecord copy = iv.value.copy();
				copy.imapUid = cnt++;
				bodyXfer().transfer(copy.messageBody, copy.internalDate);
				copies.add(copy);
			}
			ret.addAll(transferContext.toRecords().multiCreate(copies));
			postOp.accept(records);
		}
		return ret;
	}

}
