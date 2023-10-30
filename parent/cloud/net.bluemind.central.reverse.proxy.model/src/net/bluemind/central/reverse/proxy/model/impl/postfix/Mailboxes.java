/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
  *
  * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License)
  * or the CeCILL as published by CeCILL.info (version 2 of the License).
  *
  * There are special exceptions to the terms and conditions of the
  * licenses as they are applied to this program. See LICENSE.txt in
  * the directory of this program distribution.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.central.reverse.proxy.model.impl.postfix;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Mailboxes {
	public record Mailbox(String domainUid, String uid, String name, String routing, String dataLocationUid) {
	}

	private Map<String, Mailbox> mailboxes = new HashMap<>();

	public void updateMailbox(String domainUid, String uid, String name, String routing, String dataLocationUid) {
		mailboxes.put(uid, new Mailbox(domainUid, uid, name, routing, dataLocationUid));
	}

	public void removeMailbox(String uid) {
		mailboxes.remove(uid);
	}

	public Mailbox getMailboxByUid(String mailboxUid) {
		return mailboxes.get(mailboxUid);
	}

	public Optional<Mailbox> findAnyMailboxByName(String mailboxName) {
		return mailboxes.values().stream().filter(m -> m.name.equals(mailboxName)).findAny();
	}
}
