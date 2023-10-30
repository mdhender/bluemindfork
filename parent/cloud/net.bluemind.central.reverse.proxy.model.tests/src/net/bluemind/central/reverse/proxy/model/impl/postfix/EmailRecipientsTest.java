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
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import net.bluemind.central.reverse.proxy.model.impl.postfix.EmailRecipients.Recipient;

public class EmailRecipientsTest {
	@Test
	public void unknownEmailUid() {
		EmailRecipients emailRecipients = new EmailRecipients();

		assertFalse(emailRecipients.hasRecipients("unknown"));
		assertNull(emailRecipients.getRecipients("unknown"));
	}

	@Test
	public void recipients() {
		EmailRecipients emailRecipients = new EmailRecipients();
		emailRecipients.addEmailRecipient("email-uid", "recipient1-type", "recipient1-uid");

		assertFalse(emailRecipients.hasRecipients("unknown"));
		assertNull(emailRecipients.getRecipients("unknown"));

		assertTrue(emailRecipients.hasRecipients("email-uid"));
		assertEquals(1, emailRecipients.getRecipients("email-uid").size());
		assertTrue(emailRecipients.getRecipients("email-uid")
				.contains(new Recipient("recipient1-type", "recipient1-uid")));

		emailRecipients.addEmailRecipient("email-uid", "recipient2-type", "recipient2-uid");

		assertTrue(emailRecipients.hasRecipients("email-uid"));
		assertEquals(2, emailRecipients.getRecipients("email-uid").size());
		assertTrue(emailRecipients.getRecipients("email-uid")
				.contains(new Recipient("recipient1-type", "recipient1-uid")));
		assertTrue(emailRecipients.getRecipients("email-uid")
				.contains(new Recipient("recipient2-type", "recipient2-uid")));

		emailRecipients.addEmailRecipient("email-uid", "recipient2-type", "recipient2-uid");

		assertTrue(emailRecipients.hasRecipients("email-uid"));
		assertEquals(2, emailRecipients.getRecipients("email-uid").size());
		assertTrue(emailRecipients.getRecipients("email-uid")
				.contains(new Recipient("recipient1-type", "recipient1-uid")));
		assertTrue(emailRecipients.getRecipients("email-uid")
				.contains(new Recipient("recipient2-type", "recipient2-uid")));

		emailRecipients.removeEmailRecipient("email-uid", "recipient2-type", "recipient2-uid");

		assertTrue(emailRecipients.hasRecipients("email-uid"));
		assertEquals(1, emailRecipients.getRecipients("email-uid").size());
		assertTrue(emailRecipients.getRecipients("email-uid")
				.contains(new Recipient("recipient1-type", "recipient1-uid")));

		emailRecipients.removeEmailRecipient("email-uid", "recipient-unkown-type", "recipient1-uid");

		assertTrue(emailRecipients.hasRecipients("email-uid"));
		assertEquals(1, emailRecipients.getRecipients("email-uid").size());
		assertTrue(emailRecipients.getRecipients("email-uid")
				.contains(new Recipient("recipient1-type", "recipient1-uid")));

		emailRecipients.removeEmailRecipient("email-uid", "recipient1-type", "recipient1-uid");

		assertFalse(emailRecipients.hasRecipients("email-uid"));
		assertNull(emailRecipients.getRecipients("email-uid"));

		emailRecipients.removeEmailRecipient("email-uid", "recipient1-type", "recipient1-uid");

		assertFalse(emailRecipients.hasRecipients("email-uid"));
		assertNull(emailRecipients.getRecipients("email-uid"));
	}

	@Test
	public void remove() {
		EmailRecipients emailRecipients = new EmailRecipients();
		emailRecipients.addEmailRecipient("email1-uid", "email1-recipient1-type", "email1-recipient1-uid");
		emailRecipients.addEmailRecipient("email1-uid", "email1-recipient2-type", "email1-recipient2-uid");
		emailRecipients.addEmailRecipient("email2-uid", "email2-recipient1-type", "email2-recipient1-uid");

		emailRecipients.remove("unknown");
		assertTrue(emailRecipients.hasRecipients("email1-uid"));
		assertEquals(2, emailRecipients.getRecipients("email1-uid").size());
		assertTrue(emailRecipients.getRecipients("email1-uid")
				.contains(new Recipient("email1-recipient1-type", "email1-recipient1-uid")));
		assertTrue(emailRecipients.getRecipients("email1-uid")
				.contains(new Recipient("email1-recipient2-type", "email1-recipient2-uid")));

		assertTrue(emailRecipients.hasRecipients("email2-uid"));
		assertEquals(1, emailRecipients.getRecipients("email2-uid").size());
		assertTrue(emailRecipients.getRecipients("email2-uid")
				.contains(new Recipient("email2-recipient1-type", "email2-recipient1-uid")));

		emailRecipients.remove("email1-uid");
		assertFalse(emailRecipients.hasRecipients("email1-uid"));

		assertTrue(emailRecipients.hasRecipients("email2-uid"));
		assertEquals(1, emailRecipients.getRecipients("email2-uid").size());
		assertTrue(emailRecipients.getRecipients("email2-uid")
				.contains(new Recipient("email2-recipient1-type", "email2-recipient1-uid")));
	}
}
