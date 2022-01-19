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
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.exchange.mapi.service.internal.repair;

import java.util.Set;

import com.google.common.collect.Sets;

import net.bluemind.directory.api.BaseDirEntry;
import net.bluemind.exchange.mapi.api.MapiFolderContainer;

/**
 * Those folders should not be mapped to a {@link MapiFolderContainer#TYPE}
 * container as we have a more suitable container available for them.
 *
 */
public class NonMapiFolder {

	private NonMapiFolder() {
	}

	private static final Set<String> invalidKinds = Sets.newHashSet("INBOX", "SENT_ITEMS", "DELETED_ITEMS", "DRAFTS",
			"JUNK_EMAIL", "CALENDAR", "CONTACTS", "TASKS", "TEMPLATES");

	private static final Set<String> invalidKindsMailshare = Sets.newHashSet("INBOX", "SENT_ITEMS", "DELETED_ITEMS");

	private static final Set<String> invalidKindsRoom = Sets.newHashSet("CALENDAR");

	/**
	 * @param k
	 * @return true if a mapi folder with this kind is legitimate
	 */
	public static boolean legitKind(String k, BaseDirEntry.Kind kind) {
		switch (kind) {
		case MAILSHARE:
			return invalidKindsMailshare.contains(k);
		case RESOURCE:
			return invalidKindsRoom.contains(k);
		default:
			return !invalidKinds.contains(k);
		}

	}

}
