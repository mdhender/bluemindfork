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

import net.bluemind.imap.IMAPByteSource;
import net.bluemind.imap.impl.IMAPResponse;

public class UIDFetchPartCommand extends Command<IMAPByteSource> {

	private long uid;
	private String section;

	public UIDFetchPartCommand(long uid, String section) {
		this.uid = uid;
		this.section = section;
	}

	@Override
	protected CommandArgument buildCommand() {
		String cmd = "UID FETCH " + uid + " (UID BODY.PEEK[" + section + "])";
		CommandArgument args = new CommandArgument(cmd, null);
		return args;
	}

	@Override
	public void responseReceived(List<IMAPResponse> rs) {
		if (logger.isDebugEnabled()) {
			for (IMAPResponse r : rs) {
				logger.debug("r: " + r.getPayload() + " [stream:" + (r.getStreamData() != null) + "]");
			}
		}
		IMAPResponse stream = rs.get(0);
		IMAPResponse ok = rs.get(rs.size() - 1);
		if (ok.isOk() && stream.getStreamData() != null) {
			data = stream.getStreamData();
		} else {
			if (ok.isOk()) {
				data = IMAPByteSource.wrap(("[part " + section + " is missing]").getBytes());
			} else {
				logger.warn("Fetch of part " + section + " in uid " + uid + " failed: " + ok.getPayload());
			}
		}
	}

}
