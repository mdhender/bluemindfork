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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class MailboxesTest {
	@Test
	public void updateGet() {
		Mailboxes mailboxes = new Mailboxes();
		mailboxes.updateMailbox("domain-uid", "mailbox1-uid", "mailbox1-name", "mailbox1-routing",
				"mailbox1-datalocation");

		assertNull(mailboxes.getMailboxByUid("unknown"));
		assertEquals("mailbox1-name", mailboxes.getMailboxByUid("mailbox1-uid").name());

		mailboxes.updateMailbox("domain-uid", "mailbox1-uid", "mailbox1-newname", "mailbox1-routing",
				"mailbox1-datalocation");

		assertNull(mailboxes.getMailboxByUid("unknown"));
		assertEquals("mailbox1-newname", mailboxes.getMailboxByUid("mailbox1-uid").name());
	}

	@Test
	public void findByName() {
		Mailboxes mailboxes = new Mailboxes();
		mailboxes.updateMailbox("domain-uid", "mailbox1-uid", "mailbox1-name", "mailbox1-routing",
				"mailbox1-datalocation");
		mailboxes.updateMailbox("domain-uid", "mailbox2-uid", "mailbox2-name", "mailbox2-routing",
				"mailbox2-datalocation");

		assertFalse(mailboxes.findAnyMailboxByName("unknown").isPresent());
		assertEquals("mailbox1-uid", mailboxes.findAnyMailboxByName("mailbox1-name").get().uid());
	}

	@Test
	public void remove() {
		Mailboxes mailboxes = new Mailboxes();
		mailboxes.updateMailbox("domain-uid", "mailbox1-uid", "mailbox1-name", "mailbox1-routing",
				"mailbox1-datalocation");
		mailboxes.updateMailbox("domain-uid", "mailbox2-uid", "mailbox2-name", "mailbox2-routing",
				"mailbox2-datalocation");

		assertEquals("mailbox1-uid", mailboxes.findAnyMailboxByName("mailbox1-name").get().uid());
		assertEquals("mailbox2-uid", mailboxes.findAnyMailboxByName("mailbox2-name").get().uid());

		mailboxes.removeMailbox("mailbox1-uid");

		assertFalse(mailboxes.findAnyMailboxByName("mailbox1-name").isPresent());
		assertEquals("mailbox2-uid", mailboxes.findAnyMailboxByName("mailbox2-name").get().uid());
	}
}
