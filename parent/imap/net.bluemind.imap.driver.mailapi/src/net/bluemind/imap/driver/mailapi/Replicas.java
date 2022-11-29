/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.imap.driver.mailapi;

public class Replicas {

	private Replicas() {

	}

	public static int compare(String f1, String f2) {

		if (f1.equals("INBOX")) {
			return -1;
		}
		if (f2.equals("INBOX")) {
			return 1;
		}

		return f1.compareTo(f2);
	}

	public static int compareNamespaced(NamespacedFolder f1, NamespacedFolder f2) {
		if (f2.otherMailbox() && !f1.otherMailbox()) {
			return -1;
		}
		if (f1.otherMailbox() && !f2.otherMailbox()) {
			return 1;
		}
		return compare(f1.fullNameWithMountpoint(), f2.fullNameWithMountpoint());
	}

}
