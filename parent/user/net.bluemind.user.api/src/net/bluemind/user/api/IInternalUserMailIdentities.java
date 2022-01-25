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
package net.bluemind.user.api;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.mailbox.api.Mailbox;

public interface IInternalUserMailIdentities extends IUserMailIdentities {

	/**
	 * Create default user identity
	 * 
	 * @param mailboxItem
	 * @param dirEntry
	 * @throws ServerFault
	 */
	void createDefaultIdentity(ItemValue<Mailbox> mailboxItem, DirEntry dirEntry) throws ServerFault;
}
