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
package net.bluemind.user.service.internal;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

import net.bluemind.core.api.Email;
import net.bluemind.directory.service.DirDomainValue;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.user.api.User;

public class UserEmailSanitizerTest {
	private static final String domainUid = "domain-" + System.nanoTime() + ".test";

	@Test
	public void loginAtDomainIsAnEmail() {
		User u1 = new User();
		u1.login = "user-" + System.nanoTime();
		u1.routing = Routing.internal;

		u1.emails = Arrays.asList(Email.create(u1.login + "@" + domainUid, false, false));
		new UserEmailSanitizer().create(new DirDomainValue<User>(domainUid, null, u1));
		assertEquals(1, u1.emails.size());
		assertEquals(1,
				u1.emails.stream().filter(e -> e.address.equals(u1.login + "@" + domainUid) && !e.allAliases).count());

		u1.emails = Arrays.asList(Email.create(u1.login + "@" + domainUid, false, false));
		new UserEmailSanitizer().update(null, new DirDomainValue<User>(domainUid, null, u1));
		assertEquals(1, u1.emails.size());
		assertEquals(1,
				u1.emails.stream().filter(e -> e.address.equals(u1.login + "@" + domainUid) && !e.allAliases).count());

		u1.emails = Arrays.asList(Email.create(u1.login + "@domain.tld", false, false));
		new UserEmailSanitizer().create(new DirDomainValue<User>(domainUid, null, u1));
		assertEquals(2, u1.emails.size());
		assertEquals(1,
				u1.emails.stream().filter(e -> e.address.equals(u1.login + "@" + domainUid) && !e.allAliases).count());

		u1.emails = Arrays.asList(Email.create(u1.login + "@domain.tld", false, false));
		new UserEmailSanitizer().update(null, new DirDomainValue<User>(domainUid, null, u1));
		assertEquals(2, u1.emails.size());
		assertEquals(1,
				u1.emails.stream().filter(e -> e.address.equals(u1.login + "@" + domainUid) && !e.allAliases).count());

		u1.emails = Arrays.asList(Email.create(u1.login + "@domain.tld", false, true));
		new UserEmailSanitizer().create(new DirDomainValue<User>(domainUid, null, u1));
		assertEquals(1, u1.emails.size());
		assertEquals(1,
				u1.emails.stream().filter(e -> e.address.split("@")[0].equals(u1.login) && e.allAliases).count());

		u1.emails = Arrays.asList(Email.create(u1.login + "@domain.tld", false, true));
		new UserEmailSanitizer().update(null, new DirDomainValue<User>(domainUid, null, u1));
		assertEquals(1, u1.emails.size());
		assertEquals(1,
				u1.emails.stream().filter(e -> e.address.split("@")[0].equals(u1.login) && e.allAliases).count());

		u1.emails = Arrays.asList(Email.create(u1.login + "@" + domainUid, false, true));
		new UserEmailSanitizer().create(new DirDomainValue<User>(domainUid, null, u1));
		assertEquals(1, u1.emails.size());
		assertEquals(1,
				u1.emails.stream().filter(e -> e.address.equals(u1.login + "@" + domainUid) && e.allAliases).count());

		u1.emails = Arrays.asList(Email.create(u1.login + "@" + domainUid, false, true));
		new UserEmailSanitizer().update(null, new DirDomainValue<User>(domainUid, null, u1));
		assertEquals(1, u1.emails.size());
		assertEquals(1,
				u1.emails.stream().filter(e -> e.address.equals(u1.login + "@" + domainUid) && e.allAliases).count());
	}

	@Test
	public void notInternal() {
		User u1 = new User();
		u1.login = "user-" + System.nanoTime();
		u1.routing = Routing.external;

		u1.emails = Arrays.asList(Email.create(u1.login + "@domain.tld", false, false));
		new UserEmailSanitizer().create(new DirDomainValue<User>(domainUid, null, u1));
		assertEquals(1, u1.emails.size());
		assertEquals(1,
				u1.emails.stream().filter(e -> e.address.equals(u1.login + "@domain.tld") && !e.allAliases).count());

		u1.emails = Arrays.asList(Email.create(u1.login + "@domain.tld", false, false));
		new UserEmailSanitizer().update(null, new DirDomainValue<User>(domainUid, null, u1));
		assertEquals(1, u1.emails.size());
		assertEquals(1,
				u1.emails.stream().filter(e -> e.address.equals(u1.login + "@domain.tld") && !e.allAliases).count());

		u1.routing = Routing.none;

		u1.emails = Arrays.asList(Email.create(u1.login + "@domain.tld", false, false));
		new UserEmailSanitizer().create(new DirDomainValue<User>(domainUid, null, u1));
		assertEquals(1, u1.emails.size());
		assertEquals(1,
				u1.emails.stream().filter(e -> e.address.equals(u1.login + "@domain.tld") && !e.allAliases).count());

		u1.emails = Arrays.asList(Email.create(u1.login + "@domain.tld", false, false));
		new UserEmailSanitizer().update(null, new DirDomainValue<User>(domainUid, null, u1));
		assertEquals(1, u1.emails.size());
		assertEquals(1,
				u1.emails.stream().filter(e -> e.address.equals(u1.login + "@domain.tld") && !e.allAliases).count());
	}
}
