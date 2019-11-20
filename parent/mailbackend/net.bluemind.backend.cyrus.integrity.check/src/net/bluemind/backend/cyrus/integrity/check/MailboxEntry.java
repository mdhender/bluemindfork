/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2019
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
package net.bluemind.backend.cyrus.integrity.check;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import net.bluemind.mailbox.api.Mailbox.Type;

public class MailboxEntry {

	public final Type mboxType;
	public final String name;
	public final String domain;

	public MailboxEntry(Type mailboxType, String name, String domain) {
		this.mboxType = mailboxType;
		this.name = name;
		this.domain = domain;
	}

	public Stream<String> filesystemPrefixes() {
		String letter = ShardingLetter.letter(name);
		Set<String> prefs = new HashSet<>(128);
		for (char c = 'a'; c <= 'z'; c++) {
			prefs.add(Character.toString(c));
		}
		String intName = name.replace('.', '^');
		if (mboxType.sharedNs) {
			for (char c = 'a'; c <= 'z'; c++) {
				prefs.add(c + "/" + intName);
			}
		} else {
			String pf = letter + "/user";
			prefs.add(pf);
			prefs.add(pf + "/" + intName);
		}
		return prefs.stream();

	}

}
