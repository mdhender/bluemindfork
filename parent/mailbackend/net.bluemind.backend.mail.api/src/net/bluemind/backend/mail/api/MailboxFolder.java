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
package net.bluemind.backend.mail.api;

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
public class MailboxFolder {

	public String name;
	public String fullName;
	public String parentUid;
	public boolean deleted;

	public String toString() {
		return "MailboxFolder{n: " + name + ", fn: " + fullName + ", pUid: " + parentUid + "}";
	}

	public static MailboxFolder of(String fullName) {
		MailboxFolder f = new MailboxFolder();
		f.fullName = fullName;
		if (fullName.contains("/")) {
			f.name = fullName.substring(fullName.lastIndexOf('/') + 1);
		} else {
			f.name = fullName;
		}
		return f;
	}

}
