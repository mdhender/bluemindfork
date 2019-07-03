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

import net.bluemind.imap.ListInfo;
import net.bluemind.imap.ListResult;
import net.bluemind.imap.impl.IMAPResponse;

public class AbstractListCommand extends SimpleCommand<ListResult> {

	protected boolean subscribedOnly;

	protected AbstractListCommand(boolean subscribedOnly) {
		super((subscribedOnly ? "LSUB " : "LIST ") + "\"\" \"*\"");
		this.subscribedOnly = subscribedOnly;
	}

	protected AbstractListCommand(String mailboxName) {
		super("LIST \"\" " + toUtf7(mailboxName));
	}

	@Override
	public void responseReceived(List<IMAPResponse> rs) {
		ListResult lr = new ListResult(rs.size() - 1);
		for (int i = 0; i < rs.size() - 1; i++) {
			String p = rs.get(i).getPayload();
			if (!p.contains(subscribedOnly ? "LSUB " : " LIST ")) {
				continue;
			}
			int oParen = p.indexOf('(', 5);
			int cPren = p.indexOf(')', oParen);
			String flags = p.substring(oParen + 1, cPren);
			if (i == 0) {
				char imapSep = p.charAt(cPren + 3);
				lr.setImapSeparator(imapSep);
			}
			String mbox = fromUtf7(p.substring(cPren + 6, p.length()).replace("\"", ""));
			lr.add(new ListInfo(mbox, !flags.contains("\\Noselect")));
		}
		data = lr;
	}

}
