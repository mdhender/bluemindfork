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
package net.bluemind.backend.mail.replica.service.tests.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import net.bluemind.backend.mail.replica.service.internal.tools.EnvelopFrom;
import net.bluemind.core.api.Email;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.sendmail.SendmailCredentials;
import net.bluemind.domain.api.Domain;
import net.bluemind.user.api.User;

public class EnvelopFromTest {
	private String domainUid;
	private ItemValue<Domain> domain;

	@Before
	public void setup() {
		domainUid = "domain-uid";
		domain = ItemValue.create(Item.create(domainUid, 0), Domain.create("domain.internal", "domain.internal",
				"domain.internal", Set.of("domain.tld", "domain-alias.tld")));
	}

	@Test
	public void admin0() {
		String from = "admin0@domain.tld";
		assertEquals(from, new EnvelopFrom(null).getFor(SendmailCredentials.asAdmin0(), null, from));
	}

	@Test
	public void admin0_invalidHeaderFrom() {
		String from = "invalid";
		assertEquals(from, new EnvelopFrom(null).getFor(SendmailCredentials.asAdmin0(), null, from));
	}

	@Test
	public void user_DefaultEmail() {
		User user = new User();
		user.login = "login";
		user.emails = Set.of(Email.create("login@domain.tld", true, false),
				Email.create("login.alias@domain.tld", false, true));

		String headerFrom = "login@domain.tld";
		assertEquals(headerFrom, new EnvelopFrom(domain)
				.getFor(SendmailCredentials.as(user.login + "@" + domainUid, null), user, headerFrom));
	}

	@Test
	public void user_notDefaultEmail() {
		User user = new User();
		user.login = "login";
		user.emails = Set.of(Email.create("login@domain.tld", true, false),
				Email.create("login.alias@domain.tld", false, true));

		String headerFrom = "login.alias@domain.tld";
		assertEquals(headerFrom, new EnvelopFrom(domain)
				.getFor(SendmailCredentials.as(user.login + "@" + domainUid, null), user, headerFrom));
	}

	@Test
	public void user_notEmail_invalidHeaderFrom() {
		User user = new User();
		user.login = "login";
		user.emails = Set.of(Email.create("login@domain.tld", true, false),
				Email.create("login.alias@domain.tld", false, true));

		String headerFrom = "invalid";
		assertEquals(user.defaultEmailAddress(), new EnvelopFrom(domain)
				.getFor(SendmailCredentials.as(user.login + "@" + domainUid, null), user, headerFrom));
	}

	@Test
	public void user_notEmail_userDefaultEmailIsAllaliases() {
		User user = new User();
		user.login = "login";
		user.emails = Set.of(Email.create("login@domain.tld", true, true),
				Email.create("login.alias@domain.tld", false, true));

		String headerFrom = "unknown@domain.tld";
		assertEquals("login@domain.tld", new EnvelopFrom(domain)
				.getFor(SendmailCredentials.as(user.login + "@" + domainUid, null), user, headerFrom));

		headerFrom = "unknown@domain-alias.tld";
		assertEquals("login@domain-alias.tld", new EnvelopFrom(domain)
				.getFor(SendmailCredentials.as(user.login + "@" + domainUid, null), user, headerFrom));
	}

	@Test
	public void user_notEmail_userDefaultEmailNotAllaliases() {
		User user = new User();
		user.login = "login";
		user.emails = Set.of(Email.create("login@domain.tld", true, false),
				Email.create("login.alias@domain.tld", false, true));

		String headerFrom = "unknown@domain.tld";
		assertEquals("login@domain.tld", new EnvelopFrom(domain)
				.getFor(SendmailCredentials.as(user.login + "@" + domainUid, null), user, headerFrom));

		headerFrom = "unknown@domain-alias.tld";
		assertNotEquals("login@domain-alias.tld", new EnvelopFrom(domain)
				.getFor(SendmailCredentials.as(user.login + "@" + domainUid, null), user, headerFrom));
	}

	@Test
	public void user_notEmail_userNotDefaultEmail() {
		User user = new User();
		user.login = "login";
		user.emails = Set.of(Email.create("login@domain.tld", true, false),
				Email.create("login.alias@domain.tld", false, true));

		String headerFrom = "unknown@domain-alias.tld";
		assertEquals("login.alias@domain-alias.tld", new EnvelopFrom(domain)
				.getFor(SendmailCredentials.as(user.login + "@" + domainUid, null), user, headerFrom));
	}

	@Test
	public void user_notEmail_notInDomain() {
		User user = new User();
		user.login = "login";
		user.emails = Set.of(Email.create("login@domain.tld", true, false),
				Email.create("login.alias@domain.tld", false, true));

		String headerFrom = "user.alias@not-my-domain.tld";
		assertEquals("login@domain.tld", new EnvelopFrom(domain)
				.getFor(SendmailCredentials.as(user.login + "@" + domainUid, null), user, headerFrom));
	}
}
