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

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import net.bluemind.imap.impl.IMAPResponse;
import net.bluemind.imap.impl.MessageSet;

public class UIDExpungeCommand extends Command<Collection<Integer>> {

	private Collection<Integer> uids;

	public UIDExpungeCommand(Collection<Integer> uid) {
		this.uids = uid;
	}

	@Override
	protected CommandArgument buildCommand() {
		StringBuilder sb = new StringBuilder();
		sb.append("UID EXPUNGE ");
		sb.append(MessageSet.asString(uids));
		String cmd = sb.toString();
		if (logger.isDebugEnabled()) {
			logger.debug("cmd: " + cmd);
		}
		CommandArgument args = new CommandArgument(cmd, null);
		return args;
	}

	@Override
	public void responseReceived(List<IMAPResponse> rs) {
		IMAPResponse ok = rs.get(rs.size() - 1);

		if (ok.isOk()) {
			logger.debug("ok: {}", ok.getPayload());
		} else {
			if (ok.isOk()) {
				logger.warn("cyrus did not send [UID EXPUNGE ...] token: " + ok.getPayload());
			} else {
				logger.error("error on uid copy: " + ok.getPayload());
			}
			data = Collections.emptyList();
		}
	}
}
