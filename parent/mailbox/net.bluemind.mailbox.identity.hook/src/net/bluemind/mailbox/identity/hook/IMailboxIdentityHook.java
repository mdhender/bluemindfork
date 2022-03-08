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
package net.bluemind.mailbox.identity.hook;

import net.bluemind.core.rest.BmContext;
import net.bluemind.mailbox.identity.api.Identity;

public interface IMailboxIdentityHook {

	public void onCreate(BmContext context, String domainUid, String mailboxUid, String id, Identity identity);

	public void onUpdate(BmContext context, String domainUid, String mailboxUid, String id, Identity identity);

	public void onDelete(BmContext context, String domainUid, String mailboxUid, String id, Identity previous);

}
