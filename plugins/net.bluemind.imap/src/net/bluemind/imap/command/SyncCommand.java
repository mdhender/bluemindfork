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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.bluemind.imap.Flag;
import net.bluemind.imap.FlagsList;
import net.bluemind.imap.MailboxChanges;
import net.bluemind.imap.MailboxChanges.AddedMessage;
import net.bluemind.imap.MailboxChanges.UpdatedMessage;
import net.bluemind.imap.SyncData;
import net.bluemind.imap.impl.IMAPResponse;
import net.bluemind.imap.impl.MessageSet;

public class SyncCommand extends SimpleCommand<MailboxChanges> {

	// * 1 FETCH (FLAGS (\Seen) UID 137298 MODSEQ (222110))
	private static final Pattern fetchLine = Pattern
			.compile("\\* \\d+ FETCH \\(FLAGS \\(([^\\)]*)\\) UID (\\d+) MODSEQ \\((\\d+)\\)\\)");

	// * VANISHED (EARLIER)
	// 1:137297,209933,209936,209939:210033,210039:210054,210056:210059
	private static final Pattern vanishedLine = Pattern.compile("\\* VANISHED \\(EARLIER\\) (.*)");

	// * OK [HIGHESTMODSEQ 258731] Ok
	private static final Pattern highestModseqLine = Pattern.compile("\\* OK \\[HIGHESTMODSEQ (\\d+)\\] Ok");

	// * OK [UIDNEXT 18693] Ok
	private static final Pattern uidnextLine = Pattern.compile("\\* OK \\[UIDNEXT (\\d+)\\] Ok");

	private final SyncData sd;
	private final String mailbox;

	public SyncCommand(String mailbox, SyncData sd) {
		super(cmd(mailbox, sd));
		this.sd = sd;
		this.mailbox = mailbox;

	}

	private static String cmd(String mailbox, SyncData sd) {
		String ret = "SELECT " + toUtf7(mailbox) + " (QRESYNC (" + sd.getUidvalidity() + " " + sd.getModseq()
				+ " 1:* (1 " + sd.getFirstSeenUid() + ")))";
		return ret;
	}

	@Override
	public void responseReceived(List<IMAPResponse> rs) {
		logger.info("qresync: {}", cmd(mailbox, sd));
		if (logger.isDebugEnabled()) {
			for (IMAPResponse r : rs) {
				logger.debug("qresync: " + r.getPayload());
			}
		}
		long time = System.currentTimeMillis();
		IMAPResponse ok = rs.get(rs.size() - 1);
		boolean success = ok.isOk();
		int newMail = 0;
		int vanished = 0;
		long highestModseq = sd.getModseq();
		long highestUid = sd.getLastseenUid();
		long lowestUid = Long.MAX_VALUE;
		List<AddedMessage> added = new LinkedList<MailboxChanges.AddedMessage>();
		List<UpdatedMessage> updated = new LinkedList<MailboxChanges.UpdatedMessage>();
		List<Integer> deleted = new LinkedList<Integer>();

		for (IMAPResponse r : rs) {
			String pl = r.getPayload();
			Matcher fetchMatcher = fetchLine.matcher(pl);
			if (fetchMatcher.find()) {
				newMail++;
				String flagString = fetchMatcher.group(1);
				FlagsList flags = FlagsList.fromString(flagString);
				int uid = Integer.parseInt(fetchMatcher.group(2));
				lowestUid = Math.min(lowestUid, uid);
				long modseq = Long.parseLong(fetchMatcher.group(3));
				// how would we sync a remove of the \Deleted flag with EAS...
				if (flags.contains(Flag.DELETED)) {
					deleted.add(uid);
				} else if (uid > sd.getLastseenUid()) {
					added.add(new AddedMessage(uid, modseq, flags));
				} else {
					updated.add(new UpdatedMessage(uid, modseq, flags));
				}
			} else {
				Matcher vanishedMatcher = vanishedLine.matcher(pl);
				if (vanishedMatcher.find()) {
					String setString = vanishedMatcher.group(1);
					ArrayList<Integer> set = MessageSet.asFilteredLongCollection(setString, sd.getFirstSeenUid());
					logger.debug("vanished {}", setString);
					logger.info("vanished. add {} deleted", set.size());
					vanished += set.size();
					deleted.addAll(set);
				} else {
					Matcher hmMatcher = highestModseqLine.matcher(pl);
					if (hmMatcher.find()) {
						highestModseq = Long.parseLong(hmMatcher.group(1));
					} else {
						Matcher uidNextMatcher = uidnextLine.matcher(pl);
						if (uidNextMatcher.find()) {
							highestUid = Integer.parseInt(uidNextMatcher.group(1)) - 1;
						} else {
							logger.debug("unmatched: " + r.getPayload());
						}
					}
				}
			}
		}
		time = System.currentTimeMillis() - time;
		data = new MailboxChanges();
		data.added = added;
		data.updated = updated;
		data.deleted = deleted;

		long low = Math.min(lowestUid, sd.getFirstSeenUid());
		data.lowestUid = low;

		logger.info("[seq: {} -> {}][uid: {}/{}][validity: {}] matched {} fetch and {} vanish. (parse time: {}ms)",
				sd.getModseq(), highestModseq, low, highestUid, sd.getUidvalidity(), newMail, vanished, time);

		if (success) {
			data.modseq = highestModseq;
			data.highestUid = highestUid;
			data.fetches = newMail;
			if (sd.getModseq() == 1) {
				data.vanish = 0;
				data.deleted = Collections.emptyList();
			} else {
				data.vanish = vanished;
			}
			logger.info("c: {}, u: {}, d: {}", added.size(), updated.size(), deleted.size());
		} else {
			data.modseq = sd.getModseq();
			data.highestUid = sd.getLastseenUid();
		}
	}
}
