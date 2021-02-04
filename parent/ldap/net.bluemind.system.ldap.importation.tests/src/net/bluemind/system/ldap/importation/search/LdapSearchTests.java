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
package net.bluemind.system.ldap.importation.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.name.Dn;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.lib.ldap.GroupMemberAttribute;
import net.bluemind.lib.ldap.LdapConProxy;
import net.bluemind.system.importation.commons.exceptions.NullOrEmptySplitGroupName;
import net.bluemind.system.importation.search.PagedSearchResult;
import net.bluemind.system.importation.search.PagedSearchResult.LdapSearchException;
import net.bluemind.system.ldap.importation.internal.tools.LdapParameters;
import net.bluemind.system.ldap.tests.helpers.LdapDockerTestHelper;

public class LdapSearchTests {
	@Rule
	public TestName testName = new TestName();

	@BeforeClass
	public static void beforeClass() {
		LdapDockerTestHelper.initLdapServer();
	}

	@Before
	public void before() throws Exception {
		LdapDockerTestHelper.initLdapTree(this.getClass(), testName);
	}

	@Test
	public void testFindAllUsers() throws Exception {
		LdapParameters ldapParameters = LdapSearchTestHelper.getLdapParameters();
		LdapSearch search = new LdapSearch(ldapParameters);
		int count = 0;

		try (LdapConProxy connection = LdapSearchTestHelper.getConnection(ldapParameters);
				PagedSearchResult findAllUsers = search.findAllUsers(connection)) {
			while (findAllUsers.next()) {
				count++;
			}
		}

		Assert.assertEquals(3, count);
	}

	@Test
	public void testFindAllGroups() throws Exception {
		LdapParameters ldapParameters = LdapSearchTestHelper.getLdapParameters();
		LdapSearch search = new LdapSearch(ldapParameters);
		int count = 0;

		try (LdapConProxy connection = LdapSearchTestHelper.getConnection(ldapParameters);
				PagedSearchResult findAllGroups = search.findAllGroups(connection)) {
			while (findAllGroups.next()) {
				count++;
			}
		}

		Assert.assertEquals(3, count);
	}

	@Test
	public void testFindGroupsByLastModified() throws Exception {
		Thread.sleep(1500);
		String beforeDate = LdapSearchTestHelper.getDate();
		Thread.sleep(1500);

		LdapParameters ldapParameters = LdapSearchTestHelper.getLdapParameters();

		LdapSearchTestHelper.updateEntry(ldapParameters, "cn=grptest01," + LdapDockerTestHelper.LDAP_ROOT_DN);
		LdapSearchTestHelper.updateEntry(ldapParameters, "cn=grptest00," + LdapDockerTestHelper.LDAP_ROOT_DN);

		LdapSearch search = new LdapSearch(ldapParameters);

		List<String> cns = new ArrayList<>();
		try (LdapConProxy connection = LdapSearchTestHelper.getConnection(ldapParameters);
				PagedSearchResult findGroups = search.findGroupsDnByLastModification(connection,
						Optional.of(beforeDate))) {
			while (findGroups.next()) {
				cns.add(findGroups.getEntry().get("cn").getString());
			}
		}

		assertEquals(2, cns.size());
		assertTrue(cns.contains("grptest00"));
		assertTrue(cns.contains("grptest01"));
	}

	@Test
	public void testFindUserUUIDFromDN() throws Exception {
		LdapParameters ldapParameters = LdapSearchTestHelper.getLdapParameters();
		LdapSearch search = new LdapSearch(ldapParameters);

		try (LdapConProxy connection = LdapSearchTestHelper.getConnection(ldapParameters)) {
			Optional<Entry> entry = search.getUserUUID(connection,
					new Dn("uid=user00," + LdapDockerTestHelper.LDAP_ROOT_DN));

			Assert.assertTrue(entry.isPresent());
			Assert.assertNotNull(entry.get().get(ldapParameters.ldapDirectory.extIdAttribute).getString());
			Assert.assertFalse(
					entry.get().get(ldapParameters.ldapDirectory.extIdAttribute).getString().trim().isEmpty());
		}
	}

	@Test
	public void testFindGroupUUIDFromDN() throws Exception {
		LdapParameters ldapParameters = LdapSearchTestHelper.getLdapParameters();
		LdapSearch search = new LdapSearch(ldapParameters);

		try (LdapConProxy connection = LdapSearchTestHelper.getConnection(ldapParameters)) {
			Dn groupDn = new Dn("cn=grptest00," + LdapDockerTestHelper.LDAP_ROOT_DN);
			Optional<Entry> entry = search.getGroupFromDn(connection, groupDn);

			Assert.assertTrue(entry.isPresent());
			Assert.assertNotNull(entry.get().get(ldapParameters.ldapDirectory.extIdAttribute).getString());
			Assert.assertFalse(
					entry.get().get(ldapParameters.ldapDirectory.extIdAttribute).getString().trim().isEmpty());
		}
	}

	@Test
	public void testUserByLastModification() throws Exception {
		Thread.sleep(1500);
		String beforeDate = LdapSearchTestHelper.getDate();
		Thread.sleep(1500);

		LdapParameters ldapParameters = LdapSearchTestHelper.getLdapParameters();

		LdapSearchTestHelper.updateEntry(ldapParameters, "uid=user01," + LdapDockerTestHelper.LDAP_ROOT_DN);

		LdapSearch search = new LdapSearch(ldapParameters);

		List<String> logins = new ArrayList<>();
		try (LdapConProxy connection = LdapSearchTestHelper.getConnection(ldapParameters)) {
			PagedSearchResult findUser = search.findUsersDnByLastModification(connection, Optional.of(beforeDate));

			while (findUser.next()) {
				logins.add(findUser.getEntry().get("uid").getString());
			}
		}

		assertEquals(1, logins.size());
		assertTrue(logins.contains("user01"));
	}

	@Test
	public void testFindSplitGroup() throws Exception {
		LdapParameters ldapParameters = LdapSearchTestHelper.getLdapParametersWithSplitGroup("splitgroup");
		LdapSearch search = new LdapSearch(ldapParameters);

		List<String> groupName = new ArrayList<>();
		List<String> groupMembers = new ArrayList<>();
		try (LdapConProxy connection = LdapSearchTestHelper.getConnection(ldapParameters);
				PagedSearchResult findGroupsByGroupName = search.findSplitGroup(connection)) {
			while (findGroupsByGroupName.next()) {
				groupName.add(findGroupsByGroupName.getEntry().get("cn").getString());

				Attribute members = findGroupsByGroupName.getEntry().get(GroupMemberAttribute.member.name());
				Iterator<Value<?>> iterator = members.iterator();
				while (iterator.hasNext()) {
					String memberValue = iterator.next().getString();
					if (memberValue != null && !memberValue.trim().isEmpty()) {
						groupMembers.add(memberValue);
					}
				}
			}
		}

		assertEquals(1, groupName.size());
		assertTrue(groupName.contains("splitgroup"));

		assertEquals(1, groupMembers.size());
		assertTrue(groupMembers.contains("uid=user00,dc=local"));
	}

	@Test
	public void testFindSplitGroup_emptyOrNullName() throws Exception {
		LdapParameters ldapParameters = LdapSearchTestHelper.getLdapParametersWithSplitGroup("");
		LdapSearch search = new LdapSearch(ldapParameters);

		try (LdapConProxy connection = LdapSearchTestHelper.getConnection(ldapParameters);
				PagedSearchResult findGroupsByGroupName = search.findSplitGroup(connection)) {
			fail("Test must thrown an exception");
		} catch (NullOrEmptySplitGroupName isgn) {
		}

		ldapParameters = LdapSearchTestHelper.getLdapParametersWithSplitGroup(null);
		try (LdapConProxy connection = LdapSearchTestHelper.getConnection(ldapParameters);
				PagedSearchResult findGroupsByGroupName = search.findSplitGroup(connection)) {
			fail("Test must thrown an exception");
		} catch (NullOrEmptySplitGroupName isgn) {
		}
	}

	@Test
	public void testFindByUserLogin()
			throws ServerFault, IOException, LdapException, CursorException, LdapSearchException {
		LdapParameters ldapParameters = LdapSearchTestHelper.getLdapParametersWithSplitGroup("splitgroup");
		LdapSearch search = new LdapSearch(ldapParameters);

		try (LdapConProxy connection = LdapSearchTestHelper.getConnection(ldapParameters);
				PagedSearchResult searchResult = search.findByUserLogin(connection, "user00")) {
			assertTrue(searchResult.next());
			Entry entry = searchResult.getEntry();
			assertFalse(searchResult.next());

			assertEquals("uid=user00,dc=local", entry.getDn().toString());
			assertTrue(entry.containsAttribute(ldapParameters.ldapDirectory.extIdAttribute));
		}
	}
}
