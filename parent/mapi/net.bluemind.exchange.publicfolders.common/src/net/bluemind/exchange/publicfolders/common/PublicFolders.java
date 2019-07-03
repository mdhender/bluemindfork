/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.exchange.publicfolders.common;

import java.util.UUID;

import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;

public class PublicFolders {

	public static final String mailboxGuid(String domain) {
		return UUID.nameUUIDFromBytes(domain.getBytes()).toString();
	}

	public static final String smtpAddress(String domain) {
		return mailboxGuid(domain) + "@" + domain;
	}

	public static final String exchangeDn(String domain) {
		return "/o=Mapi/ou=" + domain + "/cn=PublicFolders/cn=" + mailboxGuid(domain);
	}

	public static DirEntry dirEntry(String domainUid) {
		return DirEntry.create(null, null, Kind.MAILSHARE, mailboxGuid(domainUid), "Public Folders Hierarchy",
				smtpAddress(domainUid), true, true, false, null);
	}

}
