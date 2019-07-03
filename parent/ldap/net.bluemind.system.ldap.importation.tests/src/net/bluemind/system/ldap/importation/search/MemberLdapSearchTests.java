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
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Value;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import net.bluemind.lib.ldap.LdapConProxy;
import net.bluemind.system.importation.search.LdapSearchCursor;
import net.bluemind.system.ldap.importation.internal.tools.GroupManagerImpl;
import net.bluemind.system.ldap.importation.internal.tools.LdapParameters;
import net.bluemind.system.ldap.tests.helpers.LdapDockerTestHelper;

public class MemberLdapSearchTests {
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
	public void testUserByLastModification() throws Exception {
		Thread.sleep(1500);
		String beforeDate = LdapSearchTestHelper.getDate();
		Thread.sleep(1500);

		LdapParameters ldapParameters = LdapSearchTestHelper.getLdapParameters();

		LdapSearchTestHelper.updateEntry(ldapParameters, "uid=user01," + LdapDockerTestHelper.LDAP_ROOT_DN);

		MemberLdapSearch search = new MemberLdapSearch(ldapParameters);

		List<String> logins = new ArrayList<>();
		try (LdapConProxy connection = LdapSearchTestHelper.getConnection(ldapParameters)) {
			LdapSearchCursor findUser = search.findUsersDnByLastModification(connection, Optional.of(beforeDate));

			while (findUser.next()) {
				logins.add(findUser.getEntry().get("uid").getString());
			}
		}

		assertEquals(1, logins.size());
		assertTrue(logins.contains("user01"));
	}

	@Test
	public void testfindAllGroups() throws Exception {
		LdapParameters ldapParameters = LdapSearchTestHelper.getLdapParameters();
		MemberLdapSearch search = new MemberLdapSearch(ldapParameters);

		int count = 0;

		try (LdapConProxy connection = LdapSearchTestHelper.getConnection(ldapParameters);
				LdapSearchCursor findAllGroups = search.findAllGroups(connection)) {
			while (findAllGroups.next()) {
				count++;
			}
		}

		Assert.assertEquals(3, count);
	}

	@Test
	public void testfindByGroupName() throws Exception {
		LdapParameters ldapParameters = LdapSearchTestHelper.getLdapParametersWithSplitGroup("splitgroup");
		MemberLdapSearch search = new MemberLdapSearch(ldapParameters);

		List<String> groupName = new ArrayList<>();
		List<String> groupMembers = new ArrayList<>();
		try (LdapConProxy connection = LdapSearchTestHelper.getConnection(ldapParameters);
				LdapSearchCursor findGroupsByGroupName = search.findByGroupName(connection,
						GroupManagerImpl.LDAP_MEMBER)) {
			while (findGroupsByGroupName.next()) {
				groupName.add(findGroupsByGroupName.getEntry().get("cn").getString());

				Attribute members = findGroupsByGroupName.getEntry().get(GroupManagerImpl.LDAP_MEMBER);
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
}