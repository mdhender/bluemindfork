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
package net.bluemind.imap.endpoint.driver;

import com.google.common.base.MoreObjects;

import net.bluemind.backend.mail.replica.api.IDbMailboxRecords;
import net.bluemind.backend.mail.replica.api.MailboxReplica;
import net.bluemind.core.container.model.ItemValue;

public class SelectedFolder {

	public final ItemValue<MailboxReplica> folder;
	public final long exist;
	public final long unseen;
	public final String partition;
	public final IDbMailboxRecords recApi;
	public final ImapMailbox mailbox;

	public SelectedFolder(ImapMailbox mailbox, ItemValue<MailboxReplica> f, IDbMailboxRecords recApi, String partition,
			long exist, long unseen) {
		this.mailbox = mailbox;
		this.folder = f;
		this.partition = partition;
		this.exist = exist;
		this.unseen = unseen;
		this.recApi = recApi;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(SelectedFolder.class).add("n", folder.value.fullName).toString();
	}

}
