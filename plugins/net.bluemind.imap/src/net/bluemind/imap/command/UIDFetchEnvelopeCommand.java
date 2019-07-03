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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.google.common.io.ByteSource;

import net.bluemind.imap.Envelope;
import net.bluemind.imap.command.parser.EnvelopeParser;
import net.bluemind.imap.impl.IMAPResponse;
import net.bluemind.imap.impl.MessageSet;
import net.bluemind.imap.mime.impl.AtomHelper;

public class UIDFetchEnvelopeCommand extends Command<Collection<Envelope>> {

	private final Collection<Integer> uids;

	public UIDFetchEnvelopeCommand(Collection<Integer> uid) {
		this.uids = uid;
	}

	@Override
	protected CommandArgument buildCommand() {
		StringBuilder sb = new StringBuilder();
		if (!uids.isEmpty()) {
			sb.append("UID FETCH ");
			sb.append(MessageSet.asString(uids));
			sb.append(" (UID ENVELOPE)");
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

		// for (IMAPResponse r : rs) {
		// logger.info("S: " + r.getPayload());
		// }

		IMAPResponse ok = rs.get(rs.size() - 1);
		if (ok.isOk()) {
			data = new ArrayList<Envelope>(uids.size());
			Iterator<IMAPResponse> it = rs.iterator();
			for (int i = 0; it.hasNext() && i < uids.size();) {
				IMAPResponse r = it.next();
				String payload = r.getPayload();
				if (!payload.contains(" FETCH")) {
					logger.warn("not a fetch: " + payload);
					continue;
				}
				int uidIdx = payload.indexOf("(UID ") + "(UID ".length();
				int endUid = payload.indexOf(' ', uidIdx);
				String uidStr = payload.substring(uidIdx, endUid);
				long uid = 0;
				try {
					uid = Long.parseLong(uidStr);
				} catch (NumberFormatException nfe) {
					logger.error("cannot parse uid for string '" + uid + "' (payload: " + payload + ")");
					continue;
				}

				String envel = payload.substring(endUid + " ENVELOPE (".length(), payload.length() - 1);

				byte[] envelData = null;
				try {
					if (r.getStreamData() != null) {
						envelData = AtomHelper.getFullResponse(envel, r.getStreamData().source().openStream());
						r.getStreamData().close();
					} else {
						envelData = AtomHelper.getFullResponse(envel, ByteSource.empty().openStream());
					}
				} catch (IOException e) {
					logger.error(e.getMessage(), e);
				}

				Envelope envelope = EnvelopeParser.parseEnvelope(envelData);
				envelope.setUid(uid);
				if (logger.isDebugEnabled()) {
					logger.info("uid: " + uid + " env.from: " + envelope.getFrom());
				}
				data.add(envelope);
				i++;
			}
		} else {
			logger.warn("error on fetch: " + ok.getPayload());
			data = Collections.emptyList();
		}
	}

}
