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

import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.backend.mail.replica.api.WithId;
import net.bluemind.backend.mail.replica.service.internal.ItemsTransferServiceFactory.BodyTransfer;
import net.bluemind.core.rest.BmContext;

public class SameMailboxTranferService extends BaseMailboxTranferService {

	public SameMailboxTranferService(BmContext context, TransferContext transferContext) {
		super(context, transferContext);
	}

	@Override
	protected BodyTransfer bodyXfer() {
		return BodyTransfer.NOOP;
	}

	@Override
	protected Consumer<List<WithId<MailboxRecord>>> getPostMoveOperation() {
		return records ->
		// On a move to same mailbox, we could always hard delete as we're not coming
		// from imap and the messages still exist somewhere in the mailbox
		records.stream().map(withId -> withId.itemId).forEach(transferContext.fromRecords()::deleteById);
	}

}
