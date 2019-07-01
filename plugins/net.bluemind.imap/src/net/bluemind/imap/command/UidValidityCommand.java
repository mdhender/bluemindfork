/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
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
package net.bluemind.imap.command;

import java.util.List;

import net.bluemind.imap.SyncStatus;
import net.bluemind.imap.impl.IMAPResponse;

public class UidValidityCommand extends SimpleCommand<SyncStatus> {

	private String mailbox;

	public UidValidityCommand(String mailbox) {
		super("STATUS " + toUtf7(mailbox) + " (UIDVALIDITY HIGHESTMODSEQ)");
		this.mailbox = mailbox;
	}

	@Override
	public void responseReceived(List<IMAPResponse> rs) {
		if (isOk(rs)) {
			// * STATUS inbox (UIDVALIDITY 1394648781 HIGHESTMODSEQ 1234)
			String unseen = rs.get(0).getPayload();
			int paren = unseen.indexOf("(UIDVALIDITY");
			String fetched = unseen.substring(paren, unseen.length() - 1);
			String[] splitted = fetched.split(" ");
			long uidValidity = Long.parseLong(splitted[1]);
			long modSeq = Long.parseLong(splitted[3]);
			data = new SyncStatus(uidValidity, modSeq);
		} else {
			for (IMAPResponse ir : rs) {
				logger.error("{}: {}", mailbox, ir.getPayload());
			}
		}
	}

}
