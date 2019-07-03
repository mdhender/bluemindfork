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

import net.bluemind.imap.IMAPHeaders;
import net.bluemind.imap.command.parser.HeadersParser;
import net.bluemind.imap.impl.IMAPResponse;
import net.bluemind.imap.impl.MessageSet;

public final class UIDFetchHeadersCommand extends Command<Collection<IMAPHeaders>> {

	private final Collection<Integer> uids;
	private final String[] headers;

	public UIDFetchHeadersCommand(Collection<Integer> uid, String[] headers) {
		this.uids = uid;
		this.headers = headers;
	}

	@Override
	protected CommandArgument buildCommand() {
		StringBuilder sb = new StringBuilder();
		if (!uids.isEmpty()) {
			sb.append("UID FETCH ");
			sb.append(MessageSet.asString(uids));
			sb.append(" (UID BODY.PEEK[HEADER.FIELDS (");
			for (int i = 0; i < headers.length; i++) {
				if (i > 0) {
					sb.append(" ");
				}
				sb.append(headers[i].toUpperCase());
			}
			sb.append(")])");
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
		if (uids.isEmpty()) {
			data = Collections.emptyList();
			return;
		}
		IMAPResponse ok = rs.get(rs.size() - 1);
		if (ok.isOk()) {
			data = new ArrayList<IMAPHeaders>(uids.size());
			Iterator<IMAPResponse> it = rs.iterator();
			for (int i = 0; it.hasNext() && i < uids.size();) {
				IMAPResponse r = it.next();
				String payload = r.getPayload();
				if (!payload.contains(" FETCH")) {
					logger.debug("not a fetch: " + payload);
					continue;
				}
				int uidIdx = payload.indexOf("(UID ") + "(UID ".length();
				int endUid = payload.indexOf(' ', uidIdx);
				String uidStr = payload.substring(uidIdx, endUid);
				int uid = 0;
				try {
					uid = Integer.parseInt(uidStr);
				} catch (NumberFormatException nfe) {
					logger.error("cannot parse uid for string '" + uid + "' (payload: " + payload + ")");
					continue;
				}

				IMAPHeaders hs = HeadersParser.literalToHeaders(uid, r);
				data.add(hs);
				i++;
			}
		} else {
			logger.warn("error on fetch: " + ok.getPayload());
			data = Collections.emptyList();
		}
	}

}
