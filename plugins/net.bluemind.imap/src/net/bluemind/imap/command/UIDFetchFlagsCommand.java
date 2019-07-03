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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import net.bluemind.imap.FlagsList;
import net.bluemind.imap.command.parser.FlagsStringParser;
import net.bluemind.imap.impl.IMAPResponse;
import net.bluemind.imap.impl.MessageSet;

public class UIDFetchFlagsCommand extends Command<Collection<FlagsList>> {

	private Collection<Integer> uids;
	private String uidSet;

	public UIDFetchFlagsCommand(Collection<Integer> uid) {
		this.uids = uid;
	}

	public UIDFetchFlagsCommand(String set) {
		this.uidSet = set;
		this.uids = Collections.emptyList();
	}

	@Override
	protected CommandArgument buildCommand() {

		StringBuilder sb = new StringBuilder();
		if (!uids.isEmpty()) {
			sb.append("UID FETCH ");
			sb.append(MessageSet.asString(uids));
			sb.append(" (UID FLAGS)");
		} else if (uidSet != null) {
			sb.append("UID FETCH ");
			sb.append(uidSet);
			sb.append(" (UID FLAGS)");
		} else {
			sb.append("NOOP");
		}
		String cmd = sb.toString();
		if (logger.isDebugEnabled()) {
			logger.debug("cmd: " + cmd);
		}
		CommandArgument args = new CommandArgument(cmd, null);
		return args;
	}

	@Override
	public void responseReceived(List<IMAPResponse> rs) {
		if (uids.isEmpty() && uidSet == null) {
			data = Collections.emptyList();
			return;
		}

		IMAPResponse ok = rs.get(rs.size() - 1);
		if (ok.isOk()) {
			ArrayList<FlagsList> list = new ArrayList<FlagsList>(rs.size() - 1);
			Iterator<IMAPResponse> it = rs.iterator();
			for (int i = 0; i < rs.size() - 1; i++) {
				IMAPResponse r = it.next();
				String payload = r.getPayload();

				int fidx = payload.indexOf("FLAGS (") + "FLAGS (".length();

				if (fidx == -1 + "FLAGS (".length()) {
					continue;
				}

				int endFlags = payload.indexOf(')', fidx);
				String flags = "";
				if (fidx > 0 && endFlags >= fidx) {
					flags = payload.substring(fidx, endFlags);
				} else {
					logger.error("Failed to get flags in fetch response: " + payload);
				}

				int uidIdx = payload.indexOf("UID ") + "UID ".length();
				int endUid = uidIdx;
				while (Character.isDigit(payload.charAt(endUid))) {
					endUid++;
				}
				int uid = Integer.parseInt(payload.substring(uidIdx, endUid));

				// logger.info("payload: " + r.getPayload()+" uid: "+uid);

				FlagsList flagsList = new FlagsList();
				FlagsStringParser.parse(flags, flagsList);
				flagsList.setUid(uid);
				list.add(flagsList);
			}
			data = list;
		} else {
			logger.warn("error on fetch: " + ok.getPayload());
			data = Collections.emptyList();
		}
	}

}
