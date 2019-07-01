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

public class UIDFetchMessageCommand extends Command<IMAPByteSource> {

	private long uid;

	public UIDFetchMessageCommand(long uid) {
		this.uid = uid;
	}

	@Override
	protected CommandArgument buildCommand() {
		String cmd = new StringBuilder("UID FETCH ").append(uid).append(" (UID BODY.PEEK[])").toString();
		CommandArgument args = new CommandArgument(cmd, null);
		return args;
	}

	@Override
	public void responseReceived(List<IMAPResponse> rs) {
		IMAPResponse stream = null;
		for (IMAPResponse ir : rs) {
			if (ir.getStreamData() != null) {
				stream = ir;
			}
		}
		IMAPResponse ok = rs.get(rs.size() - 1);
		if (ok.isOk() && stream != null && stream.getStreamData() != null) {
			data = stream.getStreamData();
		} else {
			if (ok.isOk()) {
				logger.warn("fetch message uid " + uid
						+ " is ok with no stream in response. Printing received responses :");
				for (IMAPResponse ir : rs) {
					logger.warn("    <= " + ir.getPayload());
				}
				data = IMAPByteSource.wrap(new byte[0]);

			} else {
				logger.error("UIDFetchMessage failed for uid " + uid + ": " + ok.getPayload());
			}
		}
	}

}
