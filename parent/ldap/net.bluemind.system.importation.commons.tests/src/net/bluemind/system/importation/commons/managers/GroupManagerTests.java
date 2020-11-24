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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapInvalidAttributeValueException;
import org.junit.Test;

import net.bluemind.core.api.Email;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.group.api.Group;
import net.bluemind.lib.ldap.GroupMemberAttribute;
import net.bluemind.system.importation.commons.Parameters;
import net.bluemind.system.importation.commons.enhancer.IEntityEnhancer;
import net.bluemind.system.importation.commons.scanner.IImportLogger;
import net.bluemind.system.ldap.tests.helpers.LdifHelper;

public class GroupManagerTests {
	private class GroupManagerTestImpl extends GroupManager {
		public GroupManagerTestImpl(ItemValue<Domain> domain) {
			super(domain, null);
			group = ItemValue.create("" + System.nanoTime(), new Group());
			group.value.name = "group-" + System.nanoTime();
		}

		public GroupManagerTestImpl(ItemValue<Domain> domain, Entry entry) {
			super(domain, entry);
			group = ItemValue.create("" + System.nanoTime(), new Group());
			group.value.name = "group-" + System.nanoTime();
		}

		@Override
		public String getExternalId(IImportLogger importLogger) {
			return null;
		}

		@Override
		protected String getNameFromDefaultAttribute(IImportLogger importLogger) {
			return "";
		}

		@Override
		protected void manageInfos() throws LdapInvalidAttributeValueException {
		}

		@Override
		protected List<String> getEmails() {
			return null;
		}

		@Override
		protected List<IEntityEnhancer> getEntityEnhancerHooks() {
			return null;
		}

		@Override
		protected Parameters getDirectoryParameters() {
			return null;
		}

		@Override
		protected Set<String> getRangedGroupMembers() {
			return Collections.emptySet();
		}

		@Override
		protected boolean isSplitDomainNestedGroup() {
			return false;
		}
	}

	private ItemValue<Domain> getDomain() {
		Domain domain = Domain.create("domain.tld", "label", "desc",
				new HashSet<>(Arrays.asList("domain-alias1.tld", "domain-alias2.tld")));
		return ItemValue.create(Item.create(domain.name, 0), domain);
	}

	@Test
	public void externalEmailsOnly() {
		List<String> emails = Arrays.asList("toto@yahoo.fr", "titi@gmail.com");

		GroupManagerTestImpl gmt = new GroupManagerTestImpl(getDomain());
		gmt.manageEmails(emails);

		assertNotNull(gmt.group.value.emails);
		assertEquals(0, gmt.group.value.emails.size());
	}

	@Test
	public void internalEmailsOnly() {
		List<String> emails = Arrays.asList("toto@domain.tld", "titi@domain-alias1.tld", "tata@domain-alias2.tld");

		GroupManagerTestImpl gmt = new GroupManagerTestImpl(getDomain());
		gmt.manageEmails(emails);

		assertEquals(3, gmt.group.value.emails.size());
		for (Email e : gmt.group.value.emails) {
			assertTrue(emails.contains(e.address));
			assertFalse(e.allAliases);

			if (e.address.equals(emails.get(0))) {
				assertTrue(e.isDefault);
			}
		}
	}

	@Test
	public void internalEmailsOnlyWithDuplicate() {
		List<String> emails = Arrays.asList("toto@domain.tld", "titi@domain-alias1.tld", "toto@domain.tld");

		GroupManagerTestImpl gmt = new GroupManagerTestImpl(getDomain());
		gmt.manageEmails(emails);

		assertEquals(2, gmt.group.value.emails.size());
		for (Email e : gmt.group.value.emails) {
			assertTrue(emails.contains(e.address));
			assertFalse(e.allAliases);

			if (e.address.equals(emails.get(0))) {
				assertTrue(e.isDefault);
			}
		}
	}

	@Test
	public void internalEmailsAllAliasesExpended() {
		List<String> emails = Arrays.asList("toto@domain.tld", "toto@domain-alias1.tld", "toto@domain-alias2.tld");

		GroupManagerTestImpl gmt = new GroupManagerTestImpl(getDomain());
		gmt.manageEmails(emails);

		assertEquals(1, gmt.group.value.emails.size());
		assertEquals(emails.get(0), gmt.group.value.emails.iterator().next().address);
		assertTrue(gmt.group.value.emails.iterator().next().isDefault);
		assertTrue(gmt.group.value.emails.iterator().next().allAliases);
	}

	@Test
	public void internalEmailsAllAliasesExpendedDefaultInDomainAlias() {
		List<String> emails = Arrays.asList("toto@domain-alias1.tld", "toto@domain.tld", "toto@domain-alias2.tld");

		GroupManagerTestImpl gmt = new GroupManagerTestImpl(getDomain());
		gmt.manageEmails(emails);

		assertEquals(1, gmt.group.value.emails.size());
		assertEquals(emails.get(0), gmt.group.value.emails.iterator().next().address);
		assertTrue(gmt.group.value.emails.iterator().next().isDefault);
		assertTrue(gmt.group.value.emails.iterator().next().allAliases);
	}

	@Test
	public void mixedEmails() {
		List<String> emails = Arrays.asList("toto@gmail.com", "toto@domain-alias1.tld", "titi@yahoo.fr");

		GroupManagerTestImpl gmt = new GroupManagerTestImpl(getDomain());
		gmt.manageEmails(emails);

		assertEquals(1, gmt.group.value.emails.size());
		assertEquals(emails.get(1), gmt.group.value.emails.iterator().next().address);
		assertTrue(gmt.group.value.emails.iterator().next().isDefault);
		assertFalse(gmt.group.value.emails.iterator().next().allAliases);
	}

	@Test
	public void emailWithoutDomainPart() {
		List<String> emails = Arrays.asList("toto");

		GroupManagerTestImpl gmt = new GroupManagerTestImpl(getDomain());
		gmt.manageEmails(emails);

		assertEquals(1, gmt.group.value.emails.size());
		assertEquals(emails.get(0) + "@domain.tld", gmt.group.value.emails.iterator().next().address);
		assertTrue(gmt.group.value.emails.iterator().next().allAliases);
		assertTrue(gmt.group.value.emails.iterator().next().isDefault);

	}

	@Test
	public void internalEmailsWithAndWithoutDomainPart() {
		List<String> emails = Arrays.asList("toto@domain-alias1.tld", "titi");

		GroupManagerTestImpl gmt = new GroupManagerTestImpl(getDomain());
		gmt.manageEmails(emails);

		assertEquals(2, gmt.group.value.emails.size());

		for (Email email : gmt.group.value.emails) {
			switch (email.address) {
			case "toto@domain-alias1.tld":
				assertFalse(email.allAliases);
				assertTrue(email.isDefault);
				break;

			case "titi" + "@domain.tld":
				assertTrue(email.allAliases);
				assertFalse(email.isDefault);
				break;

			default:
				fail("Unknown email: " + email.address);
			}
		}
	}

	@Test
	public void getGroupMembers() {
		List<Entry> entities = LdifHelper.loadLdif(this.getClass(),
				"/resources/commons/managers/GroupManagerTest/getGroupMembers.ldif");
		assertEquals(1, entities.size());

		Set<String> members = new GroupManagerTestImpl(getDomain(), entities.get(0))
				.getGroupMembers(GroupMemberAttribute.member);
		assertEquals(2, members.size());
	}
}
