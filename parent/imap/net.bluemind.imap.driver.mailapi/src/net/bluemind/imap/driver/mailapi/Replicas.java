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

import net.bluemind.backend.mail.replica.api.MailboxReplica;
import net.bluemind.core.container.model.ItemValue;

public class Replicas {

	private Replicas() {

	}

	public static int compare(ItemValue<MailboxReplica> f1, ItemValue<MailboxReplica> f2) {
		if (f1.value.fullName.equals("INBOX")) {
			return -1;
		}
		if (f2.value.fullName.equals("INBOX")) {
			return 1;
		}
		return f1.value.fullName.compareTo(f2.value.fullName);
	}

}
