/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
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
package net.bluemind.addressbook.api.utils;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCard.Communications;
import net.bluemind.addressbook.api.VCard.Communications.Email;

public class VCardMerger {

	public static VCard merge(VCard... merges) {
		if (merges.length == 0) {
			return null;
		} else if (merges.length == 1) {
			return merges[0];
		}

		VCard ret = merges[0];
		for (int i = 1; i < merges.length; i++) {
			ret = internalMerge(ret, merges[i]);
		}
		return ret;
	}

	private static VCard internalMerge(VCard left, VCard right) {
		VCard ret = left.copy();

		for (Email email : right.communications.emails) {
			Email r = containsEmail(ret.communications, email.value);
			if (r == null) {
				ret.communications.emails.add(email);
			}
		}

		return ret;
	}

	private static Email containsEmail(Communications communications, String value) {
		for (Email email : communications.emails) {
			if (email.value.equals(value)) {
				return email;
			}
		}

		return null;
	}
}
