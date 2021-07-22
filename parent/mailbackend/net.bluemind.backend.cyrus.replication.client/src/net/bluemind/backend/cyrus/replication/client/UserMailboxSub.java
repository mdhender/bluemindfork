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
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.backend.cyrus.replication.client;

public class UserMailboxSub {

	String domainUid;
	String mailboxName;
	String folderName;

	public UserMailboxSub(String domainUid, String mailboxName, String folderName) {
		this.domainUid = domainUid;
		this.mailboxName = mailboxName;
		this.folderName = folderName;
	}

	private String cyrusName() {
		return domainUid + "!user." + mailboxName.replace('.', '^')
				+ (folderName == null ? "" : "." + folderName.replace('.', '^'));
	}

	// APPLY SUB %(USERID john.wick@165890f0.internal MBOXNAME
	// 165890f0.internal!user.john^wick.Templates)
	public String applySubCommand() {
		return "APPLY SUB %(USERID " + mailboxName + "@" + domainUid + " MBOXNAME " + cyrusName() + ")\r\n";
	}

}
