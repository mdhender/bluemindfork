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

import net.bluemind.imap.impl.IMAPResponse;

public class UidnextStatusCommand extends SimpleCommand<Integer> {

	public UidnextStatusCommand(String mailbox) {
		super("STATUS " + toUtf7(mailbox) + " (UIDNEXT)");
	}

	@Override
	public void responseReceived(List<IMAPResponse> rs) {
		data = 0;
		if (isOk(rs)) {
			// * STATUS inbox (UIDNEXT 788)
			String unseen = rs.get(0).getPayload();
			int s = unseen.lastIndexOf(' ');
			data = Integer.parseInt(unseen.substring(s + 1, unseen.length() - 1));
		}
	}

}
