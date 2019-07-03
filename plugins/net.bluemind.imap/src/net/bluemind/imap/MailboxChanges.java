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

package net.bluemind.imap;

import java.util.ArrayList;
import java.util.List;

public final class MailboxChanges {

	public static final class AddedMessage {
		public final int uid;
		public final long modseq;
		public final FlagsList flags;

		public AddedMessage(int uid, long modseq, FlagsList flags) {
			this.uid = uid;
			this.modseq = modseq;
			this.flags = flags;
		}
	}

	public static final class UpdatedMessage {
		public final int uid;
		public final long modseq;
		public final FlagsList flags;

		public UpdatedMessage(int uid, long modseq, FlagsList flags) {
			this.uid = uid;
			this.modseq = modseq;
			this.flags = flags;
		}
	}

	public long modseq;
	public int fetches;
	public int vanish;
	public long highestUid;
	public long lowestUid;
	public List<Integer> deleted;
	public List<AddedMessage> added;
	public List<UpdatedMessage> updated;
	public List<Integer> softDeleted = new ArrayList<Integer>();

	public int size() {
		int count = 0;

		if (deleted != null) {
			count += deleted.size();
		}
		if (added != null) {
			count += added.size();
		}
		if (updated != null) {
			count += updated.size();
		}
		if (softDeleted != null) {
			count += softDeleted.size();
		}

		return count;
	}

}
