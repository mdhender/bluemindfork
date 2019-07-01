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
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import net.bluemind.imap.InternalDate;
import net.bluemind.imap.impl.DecoderUtils;
import net.bluemind.imap.impl.IMAPResponse;
import net.bluemind.imap.impl.MessageSet;

public final class UIDFetchInternalDateCommand extends Command<InternalDate[]> {

	private String uidSet;

	public UIDFetchInternalDateCommand(String uidSet) {
		this.uidSet = uidSet;
	}

	public UIDFetchInternalDateCommand(Collection<Integer> uid) {
		this.uidSet = uid == null || uid.isEmpty() ? "0" : MessageSet.asString(uid);
	}

	@Override
	protected CommandArgument buildCommand() {

		StringBuilder sb = new StringBuilder();
		sb.append("UID FETCH ");
		sb.append(uidSet);
		sb.append(" (UID INTERNALDATE)");
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
			data = new InternalDate[rs.size() - 1];
			Iterator<IMAPResponse> it = rs.iterator();
			int len = rs.size() - 1;
			for (int i = 0; i < len; i++) {
				IMAPResponse r = it.next();
				String payload = r.getPayload();

				int fidx = payload.indexOf("INTERNALDATE \"") + "INTERNALDATE \"".length();

				if (fidx == -1 + "INTERNALDATE \"".length()) {
					continue;
				}

				int endDate = payload.indexOf('"', fidx);
				String internalDate = "";
				if (fidx > 0 && endDate >= fidx) {
					internalDate = payload.substring(fidx, endDate);
				} else {
					logger.error("Failed to get dates in fetch response: " + payload);
				}

				int uidIdx = payload.indexOf("UID ") + "UID ".length();
				int endUid = uidIdx;
				while (Character.isDigit(payload.charAt(endUid))) {
					endUid++;
				}
				int uid = Integer.parseInt(payload.substring(uidIdx, endUid));
				data[i] = new InternalDate(uid, parseDate(internalDate));
			}
		} else {
			logger.warn("error on fetch: " + ok.getPayload());
			data = new InternalDate[0];
		}
	}

	private final Date parseDate(String date) {
		try {
			return DecoderUtils.decodeDateTime(date);
		} catch (Exception e) {
			logger.error("Can't parse '{}'", date, e);
			return new Date();
		}
	}

}
