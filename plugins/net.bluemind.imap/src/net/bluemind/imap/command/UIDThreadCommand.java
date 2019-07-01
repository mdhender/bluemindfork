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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.bluemind.imap.impl.IMAPResponse;
import net.bluemind.imap.impl.MailThread;

public class UIDThreadCommand extends Command<List<MailThread>> {

	public UIDThreadCommand() {
	}

	@Override
	protected CommandArgument buildCommand() {
		String cmd = "UID THREAD REFERENCES UTF-8 NOT DELETED";
		CommandArgument args = new CommandArgument(cmd, null);
		return args;
	}

	@Override
	public void responseReceived(List<IMAPResponse> rs) {
		data = new LinkedList<MailThread>();

		IMAPResponse ok = rs.get(rs.size() - 1);
		if (ok.isOk()) {
			String threads = null;
			Iterator<IMAPResponse> it = rs.iterator();
			for (int j = 0; j < rs.size() - 1; j++) {
				String resp = it.next().getPayload();
				if (resp.startsWith("* THREAD ")) {
					threads = resp;
					break;
				}
			}

			if (threads != null) {
				parseParenList(data, threads.substring("* THREAD ".length()));
				if (logger.isDebugEnabled()) {
					logger.debug("extracted " + data.size() + " threads");
				}
			}
		}
	}

	private void parseParenList(List<MailThread> data, String substring) {
		int parentCnt = 0;
		MailThread m = new MailThread();
		StringBuilder sb = new StringBuilder();
		for (char c : substring.toCharArray()) {
			if (c == '(') {
				if (parentCnt == 0) {
					m = new MailThread();
				}
				parentCnt++;
			} else if (c == ')') {
				parentCnt--;
				if (sb.length() > 0) {
					sb = addUid(m, sb);
				}
				if (m.size() > 0 && parentCnt == 0) {
					data.add(m);
				}
			} else if (Character.isDigit(c)) {
				sb.append(c);
			} else if (sb.length() > 0) {
				sb = addUid(m, sb);
			}
		}
	}

	private StringBuilder addUid(MailThread m, StringBuilder sb) {
		long l = Long.parseLong(sb.toString());
		m.add(l);
		sb = new StringBuilder();
		return sb;
	}

}
