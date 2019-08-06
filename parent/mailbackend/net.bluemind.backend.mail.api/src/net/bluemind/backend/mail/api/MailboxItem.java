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
package net.bluemind.backend.mail.api;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import net.bluemind.backend.mail.api.MessageBody.Part;
import net.bluemind.core.api.BMApi;

/**
 * %(UID 3 MODSEQ 4 LAST_UPDATED 1483363360 FLAGS (\Seen) INTERNALDATE
 * 1483363360 SIZE 830 GUID 2a48b9230d2e6ad4a283d5d817bc6c01c097e3a9)
 *
 */
@BMApi(version = "3")
public class MailboxItem {

	@BMApi(version = "3")
	public static enum SystemFlag {

		/**
		 * 
		 */
		answered(1 << 0, "\\Answered"),

		/**
		 * 
		 */
		flagged(1 << 1, "\\Flagged"),

		/**
		 * 
		 */
		deleted(1 << 2, "\\Deleted"),

		/**
		 * 
		 */
		draft(1 << 3, "\\Draft"),

		/**
		 * 
		 */
		seen(1 << 4, "\\Seen");

		public final int value;
		public final String imapName;

		private SystemFlag(int v, String imap) {
			this.value = v;
			this.imapName = imap;
		}

		public static Collection<SystemFlag> of(int value) {
			EnumSet<SystemFlag> ret = EnumSet.noneOf(SystemFlag.class);
			for (SystemFlag sf : SystemFlag.values()) {
				if ((sf.value & value) == sf.value) {
					ret.add(sf);
				}
			}
			return ret;
		}

		public static int valueOf(Iterable<SystemFlag> flags) {
			int ret = 0;
			for (SystemFlag sf : flags) {
				ret |= sf.value;
			}
			return ret;
		}
	}

	/**
	 * UID of the {@link MessageBody}, guid in replication protocol
	 */
	public MessageBody body;

	public long imapUid;
	public Collection<SystemFlag> systemFlags = EnumSet.noneOf(SystemFlag.class);
	public List<String> otherFlags = Collections.emptyList();

	public static MailboxItem of(String subject, Part structure) {
		MailboxItem mi = new MailboxItem();
		mi.body = MessageBody.of(subject, structure);
		return mi;

	}

	@Override
	public String toString() {
		return "[rec imap: " + imapUid + ", body: " + Optional.ofNullable(body).map(b -> b.guid).orElse("NULL BODY")
				+ ", flags: " + systemFlags + "]";
	}
}
