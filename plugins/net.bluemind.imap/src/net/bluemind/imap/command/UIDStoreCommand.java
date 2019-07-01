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
import java.util.List;

import net.bluemind.imap.FlagsList;
import net.bluemind.imap.impl.IMAPResponse;
import net.bluemind.imap.impl.MessageSet;

public class UIDStoreCommand extends Command<Boolean> {

	private String uidSet;
	private FlagsList fl;
	private boolean set;

	public UIDStoreCommand(Collection<Integer> uids, FlagsList fl, boolean set) {
		this(MessageSet.asString(uids), fl, set);
	}

	public UIDStoreCommand(String uidSet, FlagsList fl, boolean set) {
		this.uidSet = uidSet;
		this.fl = fl;
		this.set = set;
	}

	@Override
	public void responseReceived(List<IMAPResponse> rs) {
		IMAPResponse ok = rs.get(rs.size() - 1);
		data = ok.isOk();
		if (logger.isDebugEnabled()) {
			logger.debug(ok.getPayload());
		}
	}

	@Override
	protected CommandArgument buildCommand() {
		String cmd = "UID STORE " + uidSet + " " + (set ? "+" : "-") + "flags.silent " + fl.toString();

		if (logger.isDebugEnabled()) {
			logger.debug("cmd: " + cmd);
		}

		return new CommandArgument(cmd, null);
	}

}
