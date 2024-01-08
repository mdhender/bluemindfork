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

import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.backend.mail.replica.api.IDbMailboxRecords;
import net.bluemind.backend.mail.replica.api.IDbReplicatedMailboxes;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.mailbox.api.Mailbox;

public record TransferContext(IDbMailboxRecords fromRecords, IDbMailboxRecords toRecords, ItemValue<Mailbox> fromOwner,
		ItemValue<Mailbox> toOwner, IDbReplicatedMailboxes toFolder, ItemValue<MailboxFolder> fromFolder,
		ItemValue<MailboxFolder> targetFolder, String domain) {
}
