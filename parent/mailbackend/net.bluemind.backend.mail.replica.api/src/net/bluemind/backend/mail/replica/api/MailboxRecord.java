/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2017
 *
 * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License)
 * or the CeCILL as published by CeCILL.info (version 2 of the License).
 *
 * There are special exceptions to the terms and conditions of the
 * licenses as they are applied to this program. See LICENSE.txt in
 * the directory of this program distribution.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.backend.mail.replica.api;

import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;

import com.fasterxml.jackson.annotation.JsonGetter;

import net.bluemind.backend.mail.api.MailboxItem;
import net.bluemind.core.api.BMApi;

/**
 * %(UID 3 MODSEQ 4 LAST_UPDATED 1483363360 FLAGS (\Seen) INTERNALDATE
 * 1483363360 SIZE 830 GUID 2a48b9230d2e6ad4a283d5d817bc6c01c097e3a9 ANNOTATIONS
 * (%(ENTRY /vendor/cmu/cyrus-imapd/thrid USERID NIL VALUE 555fb6a47816a480)))
 *
 */
@BMApi(version = "3")
public class MailboxRecord extends MailboxItem {

	@BMApi(version = "3")
	public enum InternalFlag {
		needsCleanup(1 << 28, "xx"), //
		archived(1 << 29, "xx"), //
		unlinked(1 << 30, "xx"), //
		expunged(1 << 31, "\\Expunged");//

		public final int value;
		public final String imapName;

		private InternalFlag(int v, String imap) {
			this.value = v;
			this.imapName = imap;
		}

		public static Collection<InternalFlag> of(int value) {
			EnumSet<InternalFlag> ret = EnumSet.noneOf(InternalFlag.class);
			for (InternalFlag sf : InternalFlag.values()) {
				if ((sf.value & value) == sf.value) {
					ret.add(sf);
				}
			}
			return ret;
		}

		public static int valueOf(Iterable<InternalFlag> flags) {
			int ret = 0;
			for (InternalFlag sf : flags) {
				ret |= sf.value;
			}
			return ret;
		}
	}

	public Collection<InternalFlag> internalFlags = EnumSet.noneOf(InternalFlag.class);
	public String messageBody;
	public long modSeq;
	public Date internalDate;
	public Date lastUpdated;
	public Long conversationId;

	@JsonGetter(value = "conversationId")
	public String getConversationId() {
		return String.valueOf(conversationId);
	}

	@Override
	public String toString() {
		return super.toString() + "[intFl: " + internalFlags + ", thrid: " + conversationId + "]";
	}

}
