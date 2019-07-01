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

import net.bluemind.imap.CreateMailboxResult;
import net.bluemind.imap.impl.IMAPResponse;

/**
 * Removes a root mailbox
 * 
 * 
 */
public class DeleteMailboxCommand extends SimpleCommand<CreateMailboxResult> {

	private String mbox;

	public DeleteMailboxCommand(String mailbox) {
		super("DELETE " + toUtf7(mailbox));
		this.mbox = mailbox;
	}

	@Override
	public void responseReceived(List<IMAPResponse> rs) {
		IMAPResponse last = rs.get(rs.size() - 1);
		data = new CreateMailboxResult(last.isOk());

		if (!data.isOk()) {
			for (IMAPResponse ir : rs) {
				logger.error("S[{}]: {}", mbox, ir.getPayload());
			}
			data.setMessage(last.getPayload());
		}
	}

}
