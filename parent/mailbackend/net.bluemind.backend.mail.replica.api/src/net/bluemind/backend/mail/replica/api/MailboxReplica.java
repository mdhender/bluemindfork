/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.backend.mail.replica.api;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.core.api.BMApi;

/**
 * %(UNIQUEID 5596488a58661ddc MBOXNAME vagrant.vmw!user.admin LAST_UID 5
 * HIGHESTMODSEQ 40 RECENTUID 5 RECENTTIME 1483363292 LAST_APPENDDATE 1483363210
 * POP3_LAST_LOGIN 0 UIDVALIDITY 1483087324 PARTITION vagrant_vmw ACL
 * "admin@vagrant.vmw lrswipkxtecda admin0 lrswipkxtecda " OPTIONS P SYNC_CRC
 * 1009386617 RECORD ())
 * 
 */
@BMApi(version = "3")
public class MailboxReplica extends MailboxFolder {

	@BMApi(version = "3")
	public static class Acl {

		/**
		 * FIXME That would need to be stable with mailbox uid used for roots instead
		 * login or share name
		 */
		public String subject;
		public String rights;

		public static Acl create(String subject, String rights) {
			Acl acl = new Acl();
			acl.subject = subject;
			acl.rights = rights;
			return acl;
		}
	}

	public long lastUid;
	public long highestModSeq;
	public long xconvModSeq;
	public long recentUid;
	public Date recentTime;
	public Date lastAppendDate;
	public Date pop3LastLogin;
	public long uidValidity;
	public List<Acl> acls = Collections.emptyList();
	public String options;
	public long syncCRC;
	public String quotaRoot;
	public String dataLocation;

	@Override
	public String toString() {
		return "MailboxReplica{n=" + name + ", fn=" + fullName + ", parent=" + parentUid + ", lastUid=" + lastUid
				+ ", highestModSeq=" + highestModSeq + ", xconvModSeq=" + xconvModSeq + ", recentUid=" + recentUid
				+ ", recentTime=" + recentTime + ", lastAppendDate=" + lastAppendDate + ", pop3LastLogin="
				+ pop3LastLogin + ", uidValidity=" + uidValidity + ", acls=" + acls + ", options=" + options
				+ ", syncCRC=" + syncCRC + ", quotaRoot=" + quotaRoot + ", dataLocation=" + dataLocation + "}";
	}

}
