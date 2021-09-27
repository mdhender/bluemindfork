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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.common.base.Splitter;

import net.bluemind.imap.impl.IMAPResponse;
import net.bluemind.imap.impl.MessageSet;

public class UIDCopyCommand extends Command<Map<Integer, Integer>> {

	private String setString;
	private String destMailbox;
	private int sizeHint;

	public UIDCopyCommand(Collection<Integer> uids, String destMailbox) {
		this(MessageSet.asString(uids), destMailbox);
		this.sizeHint = uids.size();
	}

	public UIDCopyCommand(String setString, String destMailbox) {
		this.setString = setString;
		this.destMailbox = destMailbox;
		this.sizeHint = 64;
	}

	@Override
	protected CommandArgument buildCommand() {
		StringBuilder sb = new StringBuilder();
		sb.append("UID COPY ");
		sb.append(setString);
		sb.append(' ');
		sb.append(toUtf7(destMailbox));
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

		if (ok.isOk() && ok.getPayload().contains("[")) {
			if (logger.isDebugEnabled()) {
				logger.debug("ok: " + ok.getPayload());
			}
			data = parseMessageSet(ok.getPayload());
		} else {
			if (ok.isOk()) {
				logger.warn("cyrus did not send [COPYUID ...] token: {}", ok.getPayload());
			} else {
				logger.error("error on uid copy of '{}': {}", setString, ok.getPayload());
			}
			data = Collections.emptyMap();
		}
	}

	private Map<Integer, Integer> parseMessageSet(String payload) {
		// payload:
		// A7 OK [COPYUID 1521549901 1:2,4 5:7] Completed

		String s = payload.substring(payload.indexOf("[") + 1, payload.indexOf("]"));
		Iterator<String> it = Splitter.on(" ").split(s).iterator();
		it.next(); // command
		it.next();// uid validity
		String source = it.next();
		String destination = it.next();

		logger.debug("source {}", source);
		logger.debug("destination {}", destination);

		Iterator<Integer> keys = MessageSet.asLongCollection(source, sizeHint).iterator();
		Iterator<Integer> values = MessageSet.asLongCollection(destination, sizeHint).iterator();

		Map<Integer, Integer> ret = new HashMap<Integer, Integer>();
		while (keys.hasNext()) {
			ret.put(keys.next(), values.next());
		}

		return ret;
	}

}
