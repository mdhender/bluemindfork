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
import java.util.List;
import java.util.Optional;

import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.name.Dn;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import net.bluemind.lib.ldap.LdapConProxy;
import net.bluemind.system.importation.search.LdapSearchCursor;
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
		LdapSearch search = new LdapSearch(ldapParameters, new LdapGroupSearchFilter(), new LdapUserSearchFilter());
		int count = 0;

		try (LdapConProxy connection = LdapSearchTestHelper.getConnection(ldapParameters);
				LdapSearchCursor findAllUsers = search.findAllUsers(connection)) {
			while (findAllUsers.next()) {
				count++;
			}
		}

		Assert.assertEquals(3, count);
	}

	@Test
	public void testFindAllGroups() throws Exception {
		LdapParameters ldapParameters = LdapSearchTestHelper.getLdapParameters();
		LdapSearch search = new LdapSearch(ldapParameters, new LdapGroupSearchFilter(), new LdapUserSearchFilter());
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
	public void testFindGroupsByLastModified() throws Exception {
		Thread.sleep(1500);
		String beforeDate = LdapSearchTestHelper.getDate();
		Thread.sleep(1500);

		LdapParameters ldapParameters = LdapSearchTestHelper.getLdapParameters();

		LdapSearchTestHelper.updateEntry(ldapParameters, "cn=grptest01," + LdapDockerTestHelper.LDAP_ROOT_DN);
		LdapSearchTestHelper.updateEntry(ldapParameters, "cn=grptest00," + LdapDockerTestHelper.LDAP_ROOT_DN);

		LdapSearch search = new LdapSearch(ldapParameters, new LdapGroupSearchFilter(), new LdapUserSearchFilter());

		List<String> cns = new ArrayList<>();
		try (LdapConProxy connection = LdapSearchTestHelper.getConnection(ldapParameters);
				LdapSearchCursor findGroups = search.findGroupsDnByLastModification(connection,
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
		LdapSearch search = new LdapSearch(ldapParameters, new LdapGroupSearchFilter(), new LdapUserSearchFilter());

		try (LdapConProxy connection = LdapSearchTestHelper.getConnection(ldapParameters)) {
			LdapSearchCursor findUser = search.getUserUUID(connection,
					new Dn("uid=user00," + LdapDockerTestHelper.LDAP_ROOT_DN));

			Assert.assertTrue(findUser.next());
			Entry entry = findUser.getEntry();
			Assert.assertNotNull(entry.get(ldapParameters.ldapDirectory.extIdAttribute).getString());
			Assert.assertFalse(entry.get(ldapParameters.ldapDirectory.extIdAttribute).getString().trim().isEmpty());
		}
	}

	@Test
	public void testFindGroupUUIDFromDN() throws Exception {
		LdapParameters ldapParameters = LdapSearchTestHelper.getLdapParameters();
		LdapSearch search = new LdapSearch(ldapParameters, new LdapGroupSearchFilter(), new LdapUserSearchFilter());

		try (LdapConProxy connection = LdapSearchTestHelper.getConnection(ldapParameters)) {
			Dn groupDn = new Dn("cn=grptest00," + LdapDockerTestHelper.LDAP_ROOT_DN);
			LdapSearchCursor findGroupName = search.getGroupUUID(connection, groupDn);

			Assert.assertTrue(findGroupName.next());
			Entry entry = findGroupName.getEntry();
			Assert.assertNotNull(entry.get(ldapParameters.ldapDirectory.extIdAttribute).getString());
			Assert.assertFalse(entry.get(ldapParameters.ldapDirectory.extIdAttribute).getString().trim().isEmpty());
		}
	}
}
