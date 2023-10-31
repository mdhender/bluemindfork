/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.imap.endpoint.locks;

import org.slf4j.Logger;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.bluemind.imap.endpoint.ImapContext;
import net.bluemind.imap.endpoint.driver.SelectedFolder;
import net.bluemind.imap.endpoint.driver.SelectedMessage;

/**
 * A non-obvious ambiguity occurs with commands that permit an untagged EXPUNGE
 * response (commands other than FETCH, STORE, and SEARCH), since an untagged
 * EXPUNGE response can invalidate sequence numbers in a subsequent command.
 * 
 * This is not a problem for FETCH, STORE, or SEARCH commands because servers
 * are prohibited from sending EXPUNGE responses while any of those commands are
 * in progress.
 */
public interface ISequenceCheckpoint extends IFlagsCheckpoint {

	default void checkpointSequences(Logger logger, String checkpointCause, StringBuilder sb, ImapContext ctx) {
		SelectedFolder atSelectionTime = ctx.selected();
		if (atSelectionTime == null) {
			return;
		}
		SelectedFolder live = ctx.mailbox().refreshed(atSelectionTime);

		IntList expungedSequences = expungedSequences(atSelectionTime, live);
		expungedSequences.intStream().forEach((int seq) -> sb.append("* ").append(seq).append(" EXPUNGE\r\n"));
		if (!expungedSequences.isEmpty() || atSelectionTime.exist != live.exist
				|| live.notifiedContentVersion.get() > atSelectionTime.contentVersion) {
			sb.append("* ").append(live.exist).append(" EXISTS\r\n");
		}
		checkpointFlags(logger, checkpointCause, ctx, sb, live);
		ctx.selected(live);
	}

	default IntList expungedSequences(SelectedFolder f, SelectedFolder live) {
		IntList expungedSequences = new IntArrayList();
		LongSet liveItems = live.internalIdsSet();
		for (int i = f.sequences.length; i > 0; i--) {
			SelectedMessage oldMsg = f.sequences[i - 1];
			if (!liveItems.contains(oldMsg.internalId())) {
				expungedSequences.add(i);
			}
		}
		return expungedSequences;
	}

}
