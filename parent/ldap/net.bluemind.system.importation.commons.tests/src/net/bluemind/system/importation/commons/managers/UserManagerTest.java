/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2016
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.system.importation.commons.managers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.apache.directory.api.ldap.model.exception.LdapInvalidAttributeValueException;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.junit.Test;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.core.api.Email;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.mailbox.api.MailFilter;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.system.importation.commons.Parameters;
import net.bluemind.system.importation.commons.UuidMapper;
import net.bluemind.system.importation.commons.enhancer.IEntityEnhancer;
import net.bluemind.system.importation.commons.scanner.IImportLogger;
import net.bluemind.user.api.User;

public class UserManagerTest {
	private class UserManagerTestImpl extends UserManager {
		public UserManagerTestImpl(ItemValue<Domain> domain) {
			super(domain, null);
			user = ItemValue.create("" + System.nanoTime(), new User());
			user.value.login = "login-" + System.nanoTime();
			user.value.routing = Routing.internal;
			user.value.contactInfos = new VCard();
		}

		@Override
		public List<? extends UuidMapper> getUserGroupsMemberGuid(LdapConnection ldapCon) {
			return Collections.emptyList();
		}

		@Override
		public String getExternalId(IImportLogger importLogger) {
			return null;
		}

		@Override
		protected void setLoginFromDefaultAttribute(IImportLogger importLogger)
				throws LdapInvalidAttributeValueException {
		}

		@Override
		protected void manageArchived() throws LdapInvalidAttributeValueException {
		}

		@Override
		protected void setMailRouting() {
		}

		@Override
		protected List<String> getEmails() {
			return Collections.emptyList();
		}

		@Override
		protected Parameters getDirectoryParameters() {
			return null;
		}

		@Override
		protected List<IEntityEnhancer> getEntityEnhancerHooks() {
			return Collections.emptyList();
		}

		@Override
		protected void manageContactInfos() throws LdapInvalidAttributeValueException {
		}

		@Override
		protected void manageQuota(IImportLogger importLogger) throws LdapInvalidAttributeValueException {
		}
	}

	private ItemValue<Domain> getDomain() {
		Domain domain = Domain.create("domain.tld", "label", "description",
				new HashSet<>(Arrays.asList("domain-alias1.tld", "domain-alias2.tld")));
		return ItemValue.create(Item.create(domain.name, 0), domain);
	}

	@Test
	public void externalEmailsOnly() {
		List<String> emails = Arrays.asList("toto@yahoo.fr", "titi@gmail.com");

		UserManagerTestImpl umt = new UserManagerTestImpl(getDomain());
		umt.manageEmails(emails);

		assertEquals(Routing.none, umt.user.value.routing);

		assertEquals(2, umt.user.value.emails.size());
		for (Email e : umt.user.value.emails) {
			assertTrue(emails.contains(e.address));
			assertFalse(e.allAliases);

			if (e.address.equals(emails.get(0))) {
				assertTrue(e.isDefault);
			}
		}
	}

	@Test
	public void externalEmailsOnlyWithDuplicate() {
		List<String> emails = Arrays.asList("toto@yahoo.fr", "titi@gmail.com", "toto@yahoo.fr");

		UserManagerTestImpl umt = new UserManagerTestImpl(getDomain());
		umt.manageEmails(emails);

		assertEquals(Routing.none, umt.user.value.routing);

		assertEquals(2, umt.user.value.emails.size());
		for (Email e : umt.user.value.emails) {
			assertTrue(emails.contains(e.address));
			assertFalse(e.allAliases);

			if (e.address.equals(emails.get(0))) {
				assertTrue(e.isDefault);
			}
		}
	}

	@Test
	public void internalEmailsOnly() {
		List<String> emails = Arrays.asList("toto@domain.tld", "titi@domain-alias1.tld", "tata@domain-alias2.tld");

		ItemValue<Domain> domain = getDomain();

		UserManagerTestImpl umt = new UserManagerTestImpl(domain);
		umt.manageEmails(emails);

		assertEquals(Routing.internal, umt.user.value.routing);

		assertEquals(4, umt.user.value.emails.size());
		List<String> expectedEmail = new ArrayList<>(Arrays.asList(umt.user.value.login + "@" + domain.value.name));
		expectedEmail.addAll(emails);
		for (Email e : umt.user.value.emails) {
			assertTrue(expectedEmail.contains(e.address));
			assertFalse(e.allAliases);

			if (e.address.equals(emails.get(0))) {
				assertTrue(e.isDefault);
			}
		}
	}

	@Test
	public void internalEmailsOnlyWithDuplicate() {
		List<String> emails = Arrays.asList("toto@domain.tld", "titi@domain-alias1.tld", "toto@domain.tld");

		ItemValue<Domain> domain = getDomain();

		UserManagerTestImpl umt = new UserManagerTestImpl(domain);
		umt.manageEmails(emails);

		assertEquals(Routing.internal, umt.user.value.routing);

		assertEquals(3, umt.user.value.emails.size());

		List<String> exectedEmails = new ArrayList<>(Arrays.asList(umt.user.value.login + "@" + domain.value.name));
		exectedEmails.addAll(emails);
		for (Email e : umt.user.value.emails) {
			assertTrue(exectedEmails.contains(e.address));
			assertFalse(e.allAliases);

			if (e.address.equals(emails.get(0))) {
				assertTrue(e.isDefault);
			}
		}
	}

	@Test
	public void internalEmailsAllAliasesExpended() {
		List<String> emails = Arrays.asList("toto@domain.tld", "toto@domain-alias1.tld", "toto@domain-alias2.tld");

		ItemValue<Domain> domain = getDomain();

		UserManagerTestImpl umt = new UserManagerTestImpl(domain);
		umt.manageEmails(emails);

		assertEquals(Routing.internal, umt.user.value.routing);

		assertEquals(2, umt.user.value.emails.size());

		for (Email email : umt.user.value.emails) {
			if (email.address.equals(umt.user.value.login + "@" + domain.value.name)) {
				assertFalse(email.allAliases);
				assertFalse(email.isDefault);
			} else {
				assertEquals(emails.get(0), email.address);
				assertTrue(umt.user.value.emails.iterator().next().isDefault);
				assertTrue(umt.user.value.emails.iterator().next().allAliases);
			}
		}
	}

	@Test
	public void internalEmailsAllAliasesExpendedDefaultInDomainAlias() {
		List<String> emails = Arrays.asList("toto@domain-alias1.tld", "toto@domain.tld", "toto@domain-alias2.tld");

		ItemValue<Domain> domain = getDomain();

		UserManagerTestImpl umt = new UserManagerTestImpl(domain);
		umt.manageEmails(emails);

		assertEquals(Routing.internal, umt.user.value.routing);

		assertEquals(2, umt.user.value.emails.size());

		for (Email email : umt.user.value.emails) {
			if (email.address.equals(umt.user.value.login + "@" + domain.value.name)) {
				assertFalse(email.isDefault);
				assertFalse(email.allAliases);
			} else {
				assertEquals(emails.get(0), email.address);
				assertTrue(umt.user.value.emails.iterator().next().isDefault);
				assertTrue(umt.user.value.emails.iterator().next().allAliases);
			}
		}
	}

	@Test
	public void mixedEmails() {
		List<String> emails = Arrays.asList("toto@gmail.com", "toto@domain-alias1.tld", "titi@yahoo.fr");

		ItemValue<Domain> domain = getDomain();

		UserManagerTestImpl umt = new UserManagerTestImpl(domain);
		umt.manageEmails(emails);

		assertEquals(Routing.internal, umt.user.value.routing);

		assertEquals(2, umt.user.value.emails.size());

		for (Email email : umt.user.value.emails) {
			if (email.address.equals(umt.user.value.login + "@" + domain.value.name)) {
				assertFalse(email.isDefault);
				assertFalse(email.allAliases);
			} else {
				assertEquals(emails.get(1), email.address);
				assertTrue(umt.user.value.emails.iterator().next().isDefault);
				assertFalse(umt.user.value.emails.iterator().next().allAliases);
			}
		}
	}

	@Test
	public void emailWithoutDomainPart() {
		List<String> emails = Arrays.asList("toto");

		ItemValue<Domain> domain = getDomain();

		UserManagerTestImpl umt = new UserManagerTestImpl(domain);
		umt.manageEmails(emails);

		assertEquals(Routing.internal, umt.user.value.routing);

		assertEquals(2, umt.user.value.emails.size());

		for (Email email : umt.user.value.emails) {
			if (email.address.equals(umt.user.value.login + "@" + domain.value.name)) {
				assertFalse(email.isDefault);
				assertFalse(email.allAliases);
			} else {
				assertEquals(emails.get(0) + "@" + domain.value.name, email.address);
				assertTrue(umt.user.value.emails.iterator().next().isDefault);
				assertTrue(umt.user.value.emails.iterator().next().allAliases);
			}
		}
	}

	@Test
	public void internalEmailsWithAndWithoutDomainPart() {
		List<String> emails = Arrays.asList("toto@domain-alias1.tld", "titi", "toto");

		ItemValue<Domain> domain = getDomain();

		UserManagerTestImpl umt = new UserManagerTestImpl(domain);
		umt.manageEmails(emails);

		assertEquals(Routing.internal, umt.user.value.routing);

		assertEquals(3, umt.user.value.emails.size());

		for (Email email : umt.user.value.emails) {
			if (email.address.startsWith(emails.get(1) + "@")) {
				assertEquals(emails.get(1) + "@" + domain.value.name, email.address);
				assertTrue(email.allAliases);
				assertFalse(email.isDefault);
				continue;
			} else if (email.address.equals(umt.user.value.login + "@" + domain.value.name)) {
				assertFalse(email.allAliases);
				assertFalse(email.isDefault);
				continue;
			}

			assertEquals(emails.get(0), email.address);
			assertTrue(email.allAliases);
			assertTrue(email.isDefault);
		}
	}

	@Test
	public void defaultEmailNotAllAliases() {
		List<String> emails = Arrays.asList("toto@domain.tld", "titi@domain.tld", "toto@domain-alias1.tld");

		ItemValue<Domain> domain = getDomain();

		UserManagerTestImpl umt = new UserManagerTestImpl(domain);
		umt.manageEmails(emails);

		assertEquals(Routing.internal, umt.user.value.routing);

		assertEquals(4, umt.user.value.emails.size());

		for (Email email : umt.user.value.emails) {
			if (email.address.equals("toto@domain.tld")) {
				assertTrue(email.isDefault);
				assertFalse(email.allAliases);
				continue;
			} else if (email.address.equals("titi@domain.tld") || email.address.equals("toto@domain-alias1.tld")
					|| email.address.equals(umt.user.value.login + "@" + domain.value.name)) {
				assertFalse(email.isDefault);
				assertFalse(email.allAliases);
				continue;
			}

			fail("Unknown address: " + email.address);
		}
	}

	@Test
	public void getUpdatedMailFilter_userCreated() {
		UserManagerTestImpl umt = new UserManagerTestImpl(getDomain());
		umt.create = true;
		assertNotNull(umt.getUpdatedMailFilter());
		assertTrue(umt.getUpdatedMailFilter().isPresent());
	}

	@Test
	public void getUpdatedMailFilter_userUpdated() {
		UserManagerTestImpl umt = new UserManagerTestImpl(getDomain());
		umt.create = false;
		assertNotNull(umt.getUpdatedMailFilter());
		assertFalse(umt.getUpdatedMailFilter().isPresent());
	}

	@Test
	public void getUpdatedMailFilter_setExternalMailRouting() {
		UserManagerTestImpl umt = new UserManagerTestImpl(getDomain());
		umt.setExternalMailRouting();
		assertNotNull(umt.getUpdatedMailFilter());
		assertTrue(umt.getUpdatedMailFilter().isPresent());
	}

	@Test
	public void getUpdatedMailFilter_noneRouting() {
		List<String> emails = Arrays.asList("toto@yahoo.fr", "titi@gmail.com");

		UserManagerTestImpl umt = new UserManagerTestImpl(getDomain());
		umt.manageEmails(emails);

		assertEquals(Routing.none, umt.user.value.routing);
		assertNotNull(umt.getUpdatedMailFilter());
		assertTrue(umt.getUpdatedMailFilter().isPresent());
	}

	@Test
	public void update_createUserNullMailFilter() {
		UserManagerTestImpl umt = new UserManagerTestImpl(getDomain());
		umt.update(null, null);

		assertNotNull(umt.getMailFilter());
		assertTrue(umt.getUpdatedMailFilter().isPresent());
		assertEquals(umt.getUpdatedMailFilter().get(), new MailFilter());
	}

	@Test
	public void update_updateUserNullMailFilter() {
		UserManagerTestImpl umt = new UserManagerTestImpl(getDomain());
		umt.update(ItemValue.create(Item.create("test", null), new User()), null);

		assertNotNull(umt.getMailFilter());
		assertFalse(umt.getUpdatedMailFilter().isPresent());
	}

	@Test
	public void update_createUserWithMailFilter() {
		MailFilter mf = new MailFilter();
		mf.forwarding.enabled = true;
		mf.forwarding.emails.add("test@domain.tld");

		UserManagerTestImpl umt = new UserManagerTestImpl(getDomain());
		umt.update(null, mf);

		assertNotNull(umt.getMailFilter());
		assertTrue(umt.getUpdatedMailFilter().isPresent());
		assertEquals(umt.getUpdatedMailFilter().get(), new MailFilter());
	}

	@Test
	public void update_updateUserWithMailFilter() {
		MailFilter mf = new MailFilter();
		mf.forwarding.enabled = true;
		mf.forwarding.emails.add("test@domain.tld");

		UserManagerTestImpl umt = new UserManagerTestImpl(getDomain());
		umt.update(ItemValue.create(Item.create("test", null), new User()), mf);

		assertEquals(Routing.none, umt.user.value.routing);
		assertNotNull(umt.getMailFilter());
		assertTrue(umt.getUpdatedMailFilter().isPresent());
		assertFalse(umt.getUpdatedMailFilter().get().forwarding.enabled);
	}

	@Test
	public void setMailFilter_nullMailFilter() {
		UserManagerTestImpl umt = new UserManagerTestImpl(getDomain());
		umt.create = false;
		umt.setMailFilter(null);

		assertNotNull(umt.getMailFilter());
		assertFalse(umt.getUpdatedMailFilter().isPresent());
	}

	@Test
	public void setMailFilter_sameFilter() {
		UserManagerTestImpl umt = new UserManagerTestImpl(getDomain());
		umt.create = false;
		umt.setMailFilter(new MailFilter());

		assertNotNull(umt.getMailFilter());
		assertFalse(umt.getUpdatedMailFilter().isPresent());
	}

	@Test
	public void setMailFilter_updatedFilter() {
		MailFilter mf = new MailFilter();
		mf.forwarding.enabled = true;
		mf.forwarding.emails.add("test@domain.tld");

		UserManagerTestImpl umt = new UserManagerTestImpl(getDomain());
		umt.create = false;
		umt.setMailFilter(mf);

		assertNotNull(umt.getMailFilter());
		assertTrue(umt.getUpdatedMailFilter().isPresent());

		mf = new MailFilter();
		mf.forwarding.enabled = true;
		mf.forwarding.emails.add("test@domain.tld");
		assertEquals(umt.getUpdatedMailFilter().get(), mf);
	}
}
