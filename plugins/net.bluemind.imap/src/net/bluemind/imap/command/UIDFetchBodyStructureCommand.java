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
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;

import com.google.common.io.ByteSource;

import net.bluemind.imap.command.parser.BodyStructureParser;
import net.bluemind.imap.impl.IMAPResponse;
import net.bluemind.imap.impl.MessageSet;
import net.bluemind.imap.mime.MimeTree;
import net.bluemind.imap.mime.impl.AtomHelper;

public class UIDFetchBodyStructureCommand extends Command<Collection<MimeTree>> {

	private TreeSet<Integer> uids;

	public UIDFetchBodyStructureCommand(Collection<Integer> uid) {
		this.uids = new TreeSet<Integer>(uid);
	}

	@Override
	protected CommandArgument buildCommand() {
		String cmd = "UID FETCH " + MessageSet.asString(uids) + " (UID BODYSTRUCTURE)";
		CommandArgument args = new CommandArgument(cmd, null);
		return args;
	}

	@Override
	public void responseReceived(List<IMAPResponse> rs) {
		if (logger.isDebugEnabled()) {
			for (IMAPResponse r : rs) {
				logger.debug("ri: " + r.getPayload() + " [stream:" + (r.getStreamData() != null) + "]");
			}
		}
		IMAPResponse ok = rs.get(rs.size() - 1);
		if (ok.isOk()) {
			List<MimeTree> mts = new LinkedList<MimeTree>();
			Iterator<IMAPResponse> it = rs.iterator();
			int len = rs.size() - 1;
			for (int i = 0; i < len; i++) {
				IMAPResponse ir = it.next();
				String s = ir.getPayload();

				int bsIdx = s.indexOf(" BODYSTRUCTURE ");
				if (bsIdx == -1) {
					continue;
				}

				String bs = s.substring(bsIdx + " BODYSTRUCTURE ".length());

				if (bs.length() < 2) {
					logger.warn("strange bs response: " + s);
					continue;
				}
				if (bs.charAt(1) == '(') {
					bs = bs.substring(1);
				}
				int uidIdx = s.indexOf("(UID ");
				int uid = Integer.parseInt(s.substring(uidIdx + "(UID ".length(), bsIdx));

				byte[] bsData = null;
				try {
					if (ir.getStreamData() != null) {
						bsData = AtomHelper.getFullResponse(bs, ir.getStreamData().source().openStream());
						ir.getStreamData().close();
					} else {
						bsData = AtomHelper.getFullResponse(bs, ByteSource.empty().openStream());
					}
				} catch (IOException e) {
					logger.error(e.getMessage(), e);
				}
				BodyStructureParser bsp = new BodyStructureParser();
				try {
					MimeTree tree = bsp.parse(bsData);
					tree.setUid(uid);
					mts.add(tree);
				} catch (RuntimeException re) {
					logger.error("error parsing:\n" + new String(bsData));
					logger.error("payload was:\n" + s);
					throw re;
				}
			}
			data = mts;
		} else {
			logger.warn("bodystructure failed : " + ok.getPayload());
			data = Collections.emptyList();
		}
	}

}
