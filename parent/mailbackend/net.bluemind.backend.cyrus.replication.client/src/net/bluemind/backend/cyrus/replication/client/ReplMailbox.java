/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.backend.cyrus.replication.client;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.imap.Acl;

/**
 * 
 * APPLY MAILBOX %(UNIQUEID b86c4589-1aff-4ba6-9dfe-7dd8f8369b19 MBOXNAME
 * 5c50b085.internal!user.john^bang SYNC_CRC 0 SYNC_CRC_ANNOT 0 LAST_UID 0
 * HIGHESTMODSEQ 5 RECENTUID 0 RECENTTIME 0 LAST_APPENDDATE 0 POP3_LAST_LOGIN 0
 * POP3_SHOW_AFTER 0 UIDVALIDITY 1626984989 PARTITION
 * bm-master__5c50b085_internal ACL
 * "84711B0F-ABE4-4861-8774-EC9E26F16A13@5c50b085.internal lrswipkxtecda admin0
 * lrswipkxtecda " OPTIONS PS RECORD ())
 * 
 * 
 * APPLY MAILBOX %(UNIQUEID e1002f88-620b-4f59-bd2e-f929aa0bcc40 MBOXNAME
 * 5c50b085.internal!user.john^bang.Sent SYNC_CRC 0 SYNC_CRC_ANNOT 0 LAST_UID 0
 * HIGHESTMODSEQ 3 RECENTUID 0 RECENTTIME 0 LAST_APPENDDATE 0 POP3_LAST_LOGIN 0
 * POP3_SHOW_AFTER 0 UIDVALIDITY 1626984990 PARTITION
 * bm-master__5c50b085_internal ACL
 * "84711B0F-ABE4-4861-8774-EC9E26F16A13@5c50b085.internal lrswipkxtecda admin0
 * lrswipkxtecda " OPTIONS PS ANNOTATIONS (%(ENTRY /specialuse USERID
 * john.bang@5c50b085.internal VALUE {5+}\Sent)) RECORD ())
 *
 */
public class ReplMailbox {

	private String uniqueId;
	private boolean user = true;
	private String domainUid;
	private String mailboxUid;
	private String mailboxName;
	private boolean root;
	private String folderName;
	private CyrusPartition partition;
	private String specialUse;
	private String aclString;

	private long lastUid;
	private long highestModSeq;
	private Date lastAppendDate;
	private long uidValidity;

	private ReplMailbox(String uniqueId, boolean user, String domainUid, String mailboxUid, String mailboxName,
			boolean root, String folderName, CyrusPartition partition, String specialUse, String aclString,
			long lastUid, long highestModSeq, Date lastAppendDate, long uidValidity) {
		this.uniqueId = uniqueId;
		this.user = user;
		this.domainUid = domainUid;
		this.mailboxUid = mailboxUid;
		this.mailboxName = mailboxName;
		this.root = root;
		this.folderName = folderName;
		this.partition = partition;
		this.specialUse = specialUse;
		this.aclString = aclString;
		this.lastUid = lastUid;
		this.highestModSeq = highestModSeq;
		this.lastAppendDate = lastAppendDate;
		this.uidValidity = uidValidity;
	}

	public String applyMailboxCommand() {
		return applyMailboxCommand("");
	}

	public String applyMailboxCommand(String recordsArray) {
		long lastAppendTime = (lastAppendDate != null) ? lastAppendDate.getTime() / 1000 : 0;
		String cyrus = cyrusName();
		StringBuilder sb = new StringBuilder();
		sb.append("APPLY MAILBOX %(");
		sb.append("UNIQUEID ").append(uniqueId).append(' ');
		sb.append("MBOXNAME \"").append(cyrus).append("\" ");
		sb.append("SYNC_CRC 0 SYNC_CRC_ANNOT 0 LAST_UID ").append(lastUid).append(' ');
		sb.append("HIGHESTMODSEQ ").append(highestModSeq).append(' ');
		sb.append("RECENTUID 0 RECENTTIME 0 LAST_APPENDDATE ").append(lastAppendTime).append(' ');
		sb.append("POP3_LAST_LOGIN 0 POP3_SHOW_AFTER 0 ");
		sb.append("XCONVMODSEQ ").append(5).append(' ');
		sb.append("UIDVALIDITY 1626984990 ");
		sb.append("PARTITION ").append(partition.name).append(' ');
		sb.append("ACL \"").append(aclString).append("\" ");
		sb.append("OPTIONS PS ");
		if (specialUse != null) {
			sb.append("ANNOTATIONS (%(ENTRY /specialuse USERID ").append(mailboxName).append('@').append(domainUid)
					.append(' ');
			sb.append("VALUE {").append(specialUse.length()).append("+}\r\n").append(specialUse).append(")) ");
		}
		sb.append("RECORD (").append(recordsArray).append("))\r\n");
		return sb.toString();
	}

	private String cyrusName() {
		if (user) {
			if (root) {
				return domainUid + "!user." + mailboxName.replace('.', '^');
			} else {
				return domainUid + "!user." + mailboxName.replace('.', '^') + "."
						+ folderName.replace('.', '^').replace('/', '.');
			}
		} else {
			return domainUid + "!" + folderName.replace('.', '^').replace('/', '.');
		}
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		private boolean user = true;
		private String domainUid;
		private String mailboxUid;
		private String mailboxName;
		private boolean root;
		private String folderName;
		private CyrusPartition partition;
		private String specialUse;
		private String uniqueId;
		private Map<String, Acl> acls = new LinkedHashMap<>();

		private long lastUid = 0;
		private long highestModSeq = 5;
		private Date lastAppendDate = null;
		private long uidValidity = 1626984990;

		public Builder domainUid(String domainUid) {
			this.domainUid = domainUid;
			return this;
		}

		public Builder mailboxUid(String mailboxUid) {
			this.mailboxUid = mailboxUid;
			return this;
		}

		public Builder mailboxName(String mailboxName) {
			this.mailboxName = mailboxName;
			return this;
		}

		public Builder specialUse(String use) {
			this.specialUse = use;
			return this;
		}

		public Builder partition(CyrusPartition cp) {
			this.partition = cp;
			return this;
		}

		public Builder root() {
			this.root = true;
			return this;
		}

		public Builder folderName(String folderName) {
			this.folderName = folderName;
			return this;
		}

		public Builder sharedNs() {
			this.user = false;
			return this;
		}

		public Builder acl(String subject, Acl acl) {
			acls.put(subject, acl);
			return this;
		}

		public Builder uniqueId(UUID uuid) {
			this.uniqueId = uuid.toString();
			return this;
		}

		public Builder uniqueId(String uuid) {
			this.uniqueId = uuid;
			return this;
		}

		public Builder lastUid(long lastUid) {
			this.lastUid = lastUid;
			return this;
		}

		public Builder highestModSeq(long highestModSeq) {
			this.highestModSeq = highestModSeq;
			return this;
		}

		public Builder lastAppendDate(Date lastAppendDate) {
			this.lastAppendDate = lastAppendDate;
			return this;
		}

		public Builder uidValidity(long uidValidity) {
			this.uidValidity = uidValidity;
			return this;
		}

		public ReplMailbox build() {
			// check names...
			if (uniqueId == null) {
				throw new SyncClientException("uniqueId is null");
			}

			if (user && root && !"INBOX".equals(folderName)) {
				throw new SyncClientException("root but not INBOX");
			} else if (!user && root && !mailboxName.equals(folderName)) {
				throw new SyncClientException("shared root but " + mailboxName + " != " + folderName);
			} else if (!user && !root && !folderName.startsWith(mailboxName + "/")) {
				throw new SyncClientException(
						"shared child " + folderName + " does not start with " + mailboxName + "/");
			}

			StringBuilder aclString = new StringBuilder();
			for (Entry<String, Acl> e : acls.entrySet()) {
				aclString.append(e.getKey()).append('\t').append(e.getValue().toString()).append('\t');
			}

			return new ReplMailbox(uniqueId, user, domainUid, mailboxUid, mailboxName, root, folderName, partition,
					specialUse, aclString.toString(), lastUid, highestModSeq, lastAppendDate, uidValidity);
		}
	}

}
