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
import java.util.Collections;

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
		new TestUserEmailSanitizer(domainUid).create(new DirDomainValue<User>(domainUid, null, u1));
		assertEquals(1, u1.emails.size());
		assertEquals(1,
				u1.emails.stream().filter(e -> e.address.equals(u1.login + "@" + domainUid) && !e.allAliases).count());

		u1.emails = Arrays.asList(Email.create(u1.login + "@" + domainUid, false, false));
		new TestUserEmailSanitizer(domainUid).update(null, new DirDomainValue<User>(domainUid, null, u1));
		assertEquals(1, u1.emails.size());
		assertEquals(1,
				u1.emails.stream().filter(e -> e.address.equals(u1.login + "@" + domainUid) && !e.allAliases).count());

		u1.emails = Arrays.asList(Email.create(u1.login + "@domain.tld", false, false));
		new TestUserEmailSanitizer(domainUid).create(new DirDomainValue<User>(domainUid, null, u1));
		assertEquals(2, u1.emails.size());
		assertEquals(1,
				u1.emails.stream().filter(e -> e.address.equals(u1.login + "@" + domainUid) && !e.allAliases).count());

		u1.emails = Arrays.asList(Email.create(u1.login + "@domain.tld", false, false));
		new TestUserEmailSanitizer(domainUid).update(null, new DirDomainValue<User>(domainUid, null, u1));
		assertEquals(2, u1.emails.size());
		assertEquals(1,
				u1.emails.stream().filter(e -> e.address.equals(u1.login + "@" + domainUid) && !e.allAliases).count());

		u1.emails = Arrays.asList(Email.create(u1.login + "@domain.tld", false, true));
		new TestUserEmailSanitizer(domainUid).create(new DirDomainValue<User>(domainUid, null, u1));
		assertEquals(1, u1.emails.size());
		assertEquals(1,
				u1.emails.stream().filter(e -> e.address.split("@")[0].equals(u1.login) && e.allAliases).count());

		u1.emails = Arrays.asList(Email.create(u1.login + "@domain.tld", false, true));
		new TestUserEmailSanitizer(domainUid).update(null, new DirDomainValue<User>(domainUid, null, u1));
		assertEquals(1, u1.emails.size());
		assertEquals(1,
				u1.emails.stream().filter(e -> e.address.split("@")[0].equals(u1.login) && e.allAliases).count());

		u1.emails = Arrays.asList(Email.create(u1.login + "@" + domainUid, false, true));
		new TestUserEmailSanitizer(domainUid).create(new DirDomainValue<User>(domainUid, null, u1));
		assertEquals(1, u1.emails.size());
		assertEquals(1,
				u1.emails.stream().filter(e -> e.address.equals(u1.login + "@" + domainUid) && e.allAliases).count());

		u1.emails = Arrays.asList(Email.create(u1.login + "@" + domainUid, false, true));
		new TestUserEmailSanitizer(domainUid).update(null, new DirDomainValue<User>(domainUid, null, u1));
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
		new TestUserEmailSanitizer().create(new DirDomainValue<User>(domainUid, null, u1));
		assertEquals(1, u1.emails.size());
		assertEquals(1,
				u1.emails.stream().filter(e -> e.address.equals(u1.login + "@domain.tld") && !e.allAliases).count());

		u1.emails = Arrays.asList(Email.create(u1.login + "@domain.tld", false, false));
		new TestUserEmailSanitizer().update(null, new DirDomainValue<User>(domainUid, null, u1));
		assertEquals(1, u1.emails.size());
		assertEquals(1,
				u1.emails.stream().filter(e -> e.address.equals(u1.login + "@domain.tld") && !e.allAliases).count());

		u1.routing = Routing.none;

		u1.emails = Arrays.asList(Email.create(u1.login + "@domain.tld", false, false));
		new TestUserEmailSanitizer().create(new DirDomainValue<User>(domainUid, null, u1));
		assertEquals(2, u1.emails.size());
		assertEquals(1,
				u1.emails.stream().filter(e -> e.address.equals(u1.login + "@domain.tld") && !e.allAliases).count());
		assertEquals(1,
				u1.emails.stream().filter(e -> e.address.equals(u1.login + "@" + domainUid) && !e.allAliases).count());

		u1.emails = Arrays.asList(Email.create(u1.login + "@domain.tld", false, false));
		new TestUserEmailSanitizer().update(null, new DirDomainValue<User>(domainUid, null, u1));
		assertEquals(2, u1.emails.size());
		assertEquals(1,
				u1.emails.stream().filter(e -> e.address.equals(u1.login + "@domain.tld") && !e.allAliases).count());
		assertEquals(1,
				u1.emails.stream().filter(e -> e.address.equals(u1.login + "@" + domainUid) && !e.allAliases).count());
	}

	@Test
	public void internalDomain_login_at_internal_MustExist() {
		String internalDomain = "0123456789.internal";
		User u1 = new User();
		u1.login = "user-" + System.nanoTime();
		u1.routing = Routing.internal;

		u1.emails = Arrays.asList(Email.create(u1.login + "@domain.tld", false, false));
		new TestUserEmailSanitizer("domain.tld").create(new DirDomainValue<User>(internalDomain, null, u1));

		assertEquals(2, u1.emails.size());
		assertEquals(1, u1.emails.stream()
				.filter(e -> e.address.equals(u1.login + "@domain.tld") && !e.allAliases && e.isDefault).count());
		assertEquals(1,
				u1.emails.stream()
						.filter(e -> e.address.equals(u1.login + "@" + internalDomain) && !e.allAliases && !e.isDefault)
						.count());
	}

	@Test
	public void internalDomain_onlyOneInternalEmailShouldExist() {
		String internalDomain = "0123456789.internal";
		User u1 = new User();
		u1.login = "user-" + System.nanoTime();
		u1.routing = Routing.internal;

		u1.emails = Arrays.asList(Email.create(u1.login + "@domain.tld", false, false),
				Email.create("myOldLogin" + "@" + internalDomain, false, false));
		new TestUserEmailSanitizer("domain.tld").create(new DirDomainValue<User>(internalDomain, null, u1));

		assertEquals(2, u1.emails.size());
		assertEquals(1,
				u1.emails.stream().filter(e -> e.address.equals(u1.login + "@domain.tld") && !e.allAliases).count());
		assertEquals(1, u1.emails.stream()
				.filter(e -> e.address.equals(u1.login + "@" + internalDomain) && !e.allAliases).count());
	}

	@Test
	public void NO_internalDomain_login_at_domain_MustExist() {
		User u1 = new User();
		u1.login = "user-" + System.nanoTime();
		u1.routing = Routing.internal;

		u1.emails = Arrays.asList(Email.create("notMyLogin@domain.tld", false, false));
		new TestUserEmailSanitizer("domain.tld").create(new DirDomainValue<User>("domain.tld", null, u1));

		assertEquals(2, u1.emails.size());
		assertEquals(1,
				u1.emails.stream().filter(e -> e.address.equals(u1.login + "@domain.tld") && !e.allAliases).count());
		assertEquals(1,
				u1.emails.stream().filter(e -> e.address.equals("notMyLogin@domain.tld") && !e.allAliases).count());
	}

	@Test
	public void internalDomain_allAliases() {
		User u1 = new User();
		u1.login = "user-" + System.nanoTime();
		u1.routing = Routing.internal;

		u1.emails = Arrays.asList(Email.create(u1.login + "@domain-default.tld", true, true));
		new TestUserEmailSanitizer("domain-default.tld").create(new DirDomainValue<User>("domain.internal", null, u1));

		assertEquals(1, u1.emails.size());
		assertEquals(1, u1.emails.stream()
				.filter(e -> e.address.equals(u1.login + "@domain-default.tld") && e.allAliases).count());
	}

	@Test
	public void NO_internalDomain_allAliases() {
		User u1 = new User();
		u1.login = "user-" + System.nanoTime();
		u1.routing = Routing.internal;

		u1.emails = Arrays.asList(Email.create(u1.login + "@domain-default.tld", true, true));
		new TestUserEmailSanitizer("domain-default.tld").create(new DirDomainValue<User>("domain.tld", null, u1));

		assertEquals(1, u1.emails.size());
		assertEquals(1,
				u1.emails.stream()
						.filter(e -> e.address.equals(u1.login + "@domain-default.tld") && e.allAliases && e.isDefault)
						.count());
	}

	@Test
	public void internalDomain_userRename() {
		User u1 = new User();
		u1.login = "isrename-user-" + System.nanoTime();
		u1.routing = Routing.internal;

		u1.emails = Arrays.asList(Email.create("oldlogin@domain-default.tld", true, true));
		new TestUserEmailSanitizer("domain-default.tld").create(new DirDomainValue<User>("domain.internal", null, u1));

		assertEquals(3, u1.emails.size());
		assertEquals(1, u1.emails.stream()
				.filter(e -> e.address.equals("oldlogin@domain-default.tld") && e.allAliases && e.isDefault).count());
		assertEquals(1,
				u1.emails.stream().filter(
						e -> e.address.equals(u1.login + "@domain-default.tld") && !e.allAliases && !e.isDefault)
						.count());
		assertEquals(1, u1.emails.stream()
				.filter(e -> e.address.equals(u1.login + "@domain.internal") && !e.allAliases && !e.isDefault).count());
	}

	@Test
	public void NO_internalDomain_userRename() {
		User u1 = new User();
		u1.login = "isrename-user-" + System.nanoTime();
		u1.routing = Routing.internal;

		u1.emails = Arrays.asList(Email.create("oldlogin@domain-default.tld", true, true));
		new TestUserEmailSanitizer("domain-default.tld").create(new DirDomainValue<User>("domain.tld", null, u1));

		assertEquals(3, u1.emails.size());
		assertEquals(1, u1.emails.stream()
				.filter(e -> e.address.equals("oldlogin@domain-default.tld") && e.allAliases && e.isDefault).count());
		assertEquals(1,
				u1.emails.stream().filter(
						e -> e.address.equals(u1.login + "@domain-default.tld") && !e.allAliases && !e.isDefault)
						.count());
		assertEquals(1, u1.emails.stream()
				.filter(e -> e.address.equals(u1.login + "@domain.tld") && !e.allAliases && !e.isDefault).count());
	}

	@Test
	public void noDefault() {
		User u1 = new User();
		u1.login = "user-" + System.nanoTime();
		u1.routing = Routing.internal;

		u1.emails = Arrays.asList(Email.create(u1.login + "@domain-default.tld", false, true));
		new TestUserEmailSanitizer("domain-default.tld").create(new DirDomainValue<User>("domain.internal", null, u1));

		assertEquals(1, u1.emails.size());
		assertEquals(1,
				u1.emails.stream()
						.filter(e -> e.address.equals(u1.login + "@domain-default.tld") && e.allAliases && e.isDefault)
						.count());
	}

	@Test
	public void noDefault_noLoginMatch() {
		User u1 = new User();
		u1.login = "user-" + System.nanoTime();
		u1.routing = Routing.internal;

		u1.emails = Arrays.asList(Email.create(u1.login + ".alias@domain-default.tld", false, true));
		new TestUserEmailSanitizer("domain-default.tld").create(new DirDomainValue<User>("domain.internal", null, u1));

		assertEquals(2, u1.emails.size());
		assertEquals(1,
				u1.emails.stream().filter(
						e -> e.address.equals(u1.login + ".alias@domain-default.tld") && e.allAliases && e.isDefault)
						.count());
		assertEquals(1, u1.emails.stream()
				.filter(e -> e.address.equals(u1.login + "@domain.internal") && !e.allAliases && !e.isDefault).count());
	}

	@Test
	public void domainUidAsDefaultDomain_notAlias_keepUserAlias() {
		User u1 = new User();
		u1.login = "user-" + System.nanoTime();
		u1.routing = Routing.internal;

		u1.emails = Arrays.asList(Email.create(u1.login + ".alias@domain.internal", false, false));
		new TestUserEmailSanitizer("domain.internal", Collections.emptySet())
				.create(new DirDomainValue<User>("domain.internal", null, u1));

		assertEquals(2, u1.emails.size());
		assertEquals(1,
				u1.emails.stream().filter(
						e -> e.address.equals(u1.login + ".alias@domain.internal") && !e.allAliases && !e.isDefault)
						.count());
		assertEquals(1, u1.emails.stream()
				.filter(e -> e.address.equals(u1.login + "@domain.internal") && !e.allAliases && e.isDefault).count());

		u1.emails = Arrays.asList(Email.create(u1.login + ".alias@domain.internal", true, false));
		new TestUserEmailSanitizer("domain.internal", Collections.emptySet())
				.create(new DirDomainValue<User>("domain.internal", null, u1));

		assertEquals(2, u1.emails.size());
		assertEquals(1,
				u1.emails.stream().filter(
						e -> e.address.equals(u1.login + ".alias@domain.internal") && !e.allAliases && e.isDefault)
						.count());
		assertEquals(1, u1.emails.stream()
				.filter(e -> e.address.equals(u1.login + "@domain.internal") && !e.allAliases && !e.isDefault).count());

		u1.emails = Arrays.asList(Email.create(u1.login + ".alias@domain.internal", true, true));
		new TestUserEmailSanitizer("domain.internal", Collections.emptySet())
				.create(new DirDomainValue<User>("domain.internal", null, u1));

		assertEquals(2, u1.emails.size());
		assertEquals(1,
				u1.emails.stream().filter(
						e -> e.address.equals(u1.login + ".alias@domain.internal") && e.allAliases && e.isDefault)
						.count());
		assertEquals(1, u1.emails.stream()
				.filter(e -> e.address.equals(u1.login + "@domain.internal") && !e.allAliases && !e.isDefault).count());

		u1.emails = Arrays.asList(Email.create(u1.login + "@domain.internal", true, false),
				Email.create(u1.login + ".alias@domain.internal", false, false));
		new TestUserEmailSanitizer("domain.internal", Collections.emptySet())
				.create(new DirDomainValue<User>("domain.internal", null, u1));

		assertEquals(2, u1.emails.size());
		assertEquals(1,
				u1.emails.stream().filter(
						e -> e.address.equals(u1.login + ".alias@domain.internal") && !e.allAliases && !e.isDefault)
						.count());
		assertEquals(1, u1.emails.stream()
				.filter(e -> e.address.equals(u1.login + "@domain.internal") && !e.allAliases && e.isDefault).count());
	}
}
