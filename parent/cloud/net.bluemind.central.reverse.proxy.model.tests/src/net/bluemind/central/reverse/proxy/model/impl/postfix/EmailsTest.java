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

import java.util.Optional;
import java.util.Set;

import org.junit.Test;

import net.bluemind.central.reverse.proxy.model.common.DirInfo.DirEmail;
import net.bluemind.central.reverse.proxy.model.impl.postfix.Domains.DomainAliases;
import net.bluemind.central.reverse.proxy.model.impl.postfix.Emails.Email;
import net.bluemind.central.reverse.proxy.model.impl.postfix.Emails.EmailParts;

public class EmailsTest {
	@Test
	public void emailParts() {
		assertFalse(EmailParts.fromEmail("").isPresent());
		assertFalse(EmailParts.fromEmail("invalid").isPresent());
		assertFalse(EmailParts.fromEmail("invalid@").isPresent());

		Optional<EmailParts> emailParts = EmailParts.fromEmail("left@domain");
		assertTrue(emailParts.isPresent());
		assertEquals("left", emailParts.get().left());
		assertEquals("domain", emailParts.get().domain());
		assertEquals("left@domain", emailParts.get().email());
	}

	@Test
	public void emailFromDirEmail() {
		assertNull(Email.fromDirEmail(new DirEmail("", false)));
		assertNull(Email.fromDirEmail(new DirEmail("invalid", false)));
		assertNull(Email.fromDirEmail(new DirEmail("invalid@", false)));

		Email email = Email.fromDirEmail(new DirEmail("left@domain", false));
		assertEquals("left@domain", email.getEmail());
		assertFalse(email.allAliases());

		email = Email.fromDirEmail(new DirEmail("left@domain", true));
		assertEquals("left@domain", email.getEmail());
		assertTrue(email.allAliases());
	}

	@Test
	public void emailMatch() {
		Email email = new Email(new EmailParts("left", "domain"), false);

		assertFalse(email.match(new EmailParts("unknown", "domain"), null));
		assertFalse(
				email.match(new EmailParts("unknown", "domain"), new DomainAliases("domain-uid", Set.of("domain"))));

		assertTrue(email.match(new EmailParts("left", "domain"), null));
		assertTrue(email.match(new EmailParts("left", "domain"), new DomainAliases("domain-uid", Set.of("domain"))));

		email = new Email(new EmailParts("left", "domain"), true);

		assertFalse(email.match(new EmailParts("unknown", "domain"), null));
		assertFalse(
				email.match(new EmailParts("unknown", "domain"), new DomainAliases("domain-uid", Set.of("domain"))));

		assertTrue(email.match(new EmailParts("left", "domain"), new DomainAliases("domain-uid", Set.of("domain"))));
		assertTrue(
				email.match(new EmailParts("left", "domain-uid"), new DomainAliases("domain-uid", Set.of("domain"))));
		assertFalse(email.match(new EmailParts("left", "unknown-alias"),
				new DomainAliases("domain-uid", Set.of("domain"))));
	}

	@Test
	public void emailsGetEmail() {
		Emails emails = new Emails();
		emails.update("email-uid",
				Set.of(new DirEmail("email1@domain-alias1", true), new DirEmail("email2@domain-alias1", false)));

		assertEquals("email-uid", emails.getEmail(new EmailParts("email1", "domain-alias1"),
				new DomainAliases("domain-uid", Set.of("domain-alias1", "domain-alias2"))).get().uid());
		assertEquals("email-uid", emails.getEmail(new EmailParts("email1", "domain-alias2"),
				new DomainAliases("domain-uid", Set.of("domain-alias1", "domain-alias2"))).get().uid());
		assertEquals("email-uid", emails.getEmail(new EmailParts("email1", "domain-uid"),
				new DomainAliases("domain-uid", Set.of("domain-alias1", "domain-alias2"))).get().uid());
		assertEquals("email-uid", emails.getEmail(new EmailParts("email2", "domain-alias1"),
				new DomainAliases("domain-uid", Set.of("domain-alias1", "domain-alias2"))).get().uid());

		assertFalse(emails.getEmail(new EmailParts("email2", "domain-uid"),
				new DomainAliases("domain-uid", Set.of("domain-alias1", "domain-alias2"))).isPresent());

		assertFalse(emails.getEmail(new EmailParts("unknown", "domain-alias1"),
				new DomainAliases("domain-uid", Set.of("domain-alias1", "domain-alias2"))).isPresent());
		assertFalse(emails.getEmail(new EmailParts("email1", "domain-unknown"),
				new DomainAliases("domain-uid", Set.of("domain-alias1", "domain-alias2"))).isPresent());

		assertFalse(emails.getEmail(new EmailParts("email1", "domain-alias1"),
				new DomainAliases("domain-uid", Set.of("domain2-alias1", "domain2-alias2"))).isPresent());
	}

	@Test
	public void emailsUpdate() {
		Emails emails = new Emails();
		emails.update("email-uid", Set.of(new DirEmail("email1.email-uid@domain-alias1", true)));

		assertEquals("email-uid", emails.getEmail(new EmailParts("email1.email-uid", "domain-alias1"),
				new DomainAliases("domain-uid", Set.of("domain-alias1", "domain-alias2"))).get().uid());

		emails.update("email-uid", Set.of(new DirEmail("email2.email-uid@domain-alias1", false)));

		assertEquals("email-uid", emails.getEmail(new EmailParts("email2.email-uid", "domain-alias1"),
				new DomainAliases("domain-uid", Set.of("domain-alias1", "domain-alias2"))).get().uid());
		assertFalse(emails.getEmail(new EmailParts("email1.email-uid", "domain-uid"),
				new DomainAliases("domain-uid", Set.of("domain-alias1", "domain-alias2"))).isPresent());

		emails.update("email2-uid", Set.of(new DirEmail("email.email2-uid@domain-alias1", false)));

		assertEquals("email-uid", emails.getEmail(new EmailParts("email2.email-uid", "domain-alias1"),
				new DomainAliases("domain-uid", Set.of("domain-alias1", "domain-alias2"))).get().uid());
		assertEquals("email2-uid", emails.getEmail(new EmailParts("email.email2-uid", "domain-alias1"),
				new DomainAliases("domain-uid", Set.of("domain-alias1", "domain-alias2"))).get().uid());
		assertFalse(emails.getEmail(new EmailParts("email-uid1", "domain-uid"),
				new DomainAliases("domain-uid", Set.of("domain-alias1", "domain-alias2"))).isPresent());
	}

	@Test
	public void emailsGetByUid() {
		Emails emails = new Emails();
		emails.update("email-uid1", Set.of(new DirEmail("email.email-uid1@domain-alias1", true)));
		emails.update("email-uid2", Set.of(new DirEmail("email.email-uid2@domain-alias1", true)));

		assertFalse(emails.getEmailByUid("unknown").isPresent());
		assertEquals("email-uid1", emails.getEmailByUid("email-uid1").get().uid());
	}

	@Test
	public void remove() {
		Emails emails = new Emails();
		emails.update("email-uid1", Set.of(new DirEmail("email.email-uid1@domain-alias1", true)));
		emails.update("email-uid2", Set.of(new DirEmail("email.email-uid2@domain-alias1", true)));

		emails.remove("email-uid-unknown");
		assertEquals("email-uid1", emails.getEmailByUid("email-uid1").get().uid());
		assertEquals("email-uid2", emails.getEmailByUid("email-uid2").get().uid());

		emails.remove("email-uid1");
		assertFalse("email-uid1", emails.getEmailByUid("email-uid1").isPresent());
		assertEquals("email-uid2", emails.getEmailByUid("email-uid2").get().uid());
	}
}
