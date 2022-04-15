/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2020
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
package net.bluemind.backend.mail.api.flags;

import java.util.List;

import net.bluemind.backend.mail.api.MailboxItem;
import net.bluemind.core.api.BMApi;

/**
 * Update one flag on multiple {@link MailboxItem}
 */
@BMApi(version = "3")
public class ConversationFlagUpdate {

	/**
	 * {@link MailboxItem} item identifiers
	 */
	public List<String> conversationUids;

	/**
	 * {@link MailboxItemFlag} to update
	 */
	public MailboxItemFlag mailboxItemFlag;

}
