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
package net.bluemind.imap.endpoint.driver;

import net.bluemind.backend.mail.replica.api.IDbReplicatedMailboxes;
import net.bluemind.backend.mail.replica.api.MailboxReplica;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.mailbox.api.Mailbox;

public class ImapMailbox {

	public ItemValue<Mailbox> owner;
	public String domainUid;
	public boolean readable;
	public boolean readOnly;
	public IDbReplicatedMailboxes foldersApi;

	public String replicaName;

	/**
	 * Return a copy with a fresh replica name
	 * 
	 * @param imapName
	 * @return
	 */
	public ImapMailbox forReplicaName(String imapName) {
		ImapMailbox mr = new ImapMailbox();
		mr.owner = owner;
		mr.domainUid = domainUid;
		mr.readable = readable;
		mr.readOnly = readOnly;
		mr.foldersApi = foldersApi;

		mr.replicaName = imapName;
		return mr;
	}

	public ItemValue<MailboxReplica> replica() {
		return foldersApi.byReplicaName(replicaName);
	}

}
