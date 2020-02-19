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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.backend.mail.replica.indexing;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import net.bluemind.backend.mail.api.flags.SystemFlag.SeenFlag;
import net.bluemind.backend.mail.api.flags.MailboxItemFlag;

public class MessageFlagsHelper {

	private MessageFlagsHelper() {
	}

	private static final String UNREAD = "unread";
	private static final String UNSEEN = "unseen";

	public static Set<String> asFlags(Collection<MailboxItemFlag> imapFlags) {
		Set<String> basicSet = imapFlags.stream().filter(item -> item.isSystem)
				.map(item -> item.flag.toLowerCase().replaceAll("\\\\", "")).collect(Collectors.toSet());
		if (!basicSet.contains(new SeenFlag().flag)) {
			basicSet.add(UNREAD);
			basicSet.add(UNSEEN);
		}
		return basicSet;
	}

}
