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
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.directory.api.ldap.model.entry.Entry;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import net.bluemind.lib.ldap.LdapConProxy;
import net.bluemind.scheduledjob.api.JobExitStatus;
import net.bluemind.system.importation.commons.UuidMapper;
import net.bluemind.system.importation.commons.scanner.ImportLogger;
import net.bluemind.system.importation.commons.scanner.RepportStatus;
import net.bluemind.system.importation.search.PagedSearchResult;
import net.bluemind.system.ldap.importation.internal.tools.LdapParameters;
import net.bluemind.system.ldap.tests.helpers.LdapDockerTestHelper;

public class MemberOfLdapSearchTests {
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
	public void testFindUsersByLastModification() throws Exception {
		Thread.sleep(1500);
		String beforeDate = LdapSearchTestHelper.getDate();
		Thread.sleep(1500);

		LdapParameters ldapParameters = LdapSearchTestHelper.getLdapParameters();

		LdapSearchTestHelper.updateEntry(ldapParameters, "uid=user01," + LdapDockerTestHelper.LDAP_ROOT_DN);

		MemberOfLdapSearch search = new MemberOfLdapSearch(getImportLogger(), ldapParameters);

		List<String> logins = new ArrayList<>();
		try (LdapConProxy connection = LdapSearchTestHelper.getConnection(ldapParameters);
				PagedSearchResult findUsers = search.findUsersDnByLastModification(connection,
						Optional.of(beforeDate))) {
			while (findUsers.next()) {
				logins.add(findUsers.getEntry().get("uid").getString());
			}

			assertEquals(1, logins.size());
			assertTrue(logins.contains("user01"));
		}
	}

	private ImportLogger getImportLogger() {
		return new ImportLogger(Optional.empty(), Optional.empty(), Optional.of(new RepportStatus()));
	}

	@Test
	public void testUserByLogin() throws Exception {
		LdapParameters ldapParameters = LdapSearchTestHelper.getLdapParameters();
		ImportLogger importLogger = getImportLogger();

		MemberOfLdapSearch search = new MemberOfLdapSearch(importLogger, ldapParameters);

		try (LdapConProxy connection = LdapSearchTestHelper.getConnection(ldapParameters)) {
			PagedSearchResult cursor = search.findAllUsers(connection);
			Entry entry = null;
			while (cursor.next()) {
				if (entry != null) {
					fail("Must found only one user!");
				}

				entry = cursor.getEntry();
			}

			List<UuidMapper> memberUIs = search.getUserGroupsByMemberUuid(connection, ldapParameters, entry);
			Assert.assertEquals(2, memberUIs.size());
			Assert.assertEquals(JobExitStatus.SUCCESS, importLogger.repportStatus.get().getJobStatus());
		}
	}

	@Test
	public void testUserByLoginInvalidExternalId() throws Exception {
		LdapParameters ldapParameters = LdapSearchTestHelper.getLdapParameters("invalid", null, null, null);
		ImportLogger importLogger = getImportLogger();

		MemberOfLdapSearch search = new MemberOfLdapSearch(importLogger, ldapParameters);

		try (LdapConProxy connection = LdapSearchTestHelper.getConnection(ldapParameters)) {
			PagedSearchResult cursor = search.findAllUsers(connection);
			Entry entry = null;
			while (cursor.next()) {
				if (entry != null) {
					fail("Must found only one user!");
				}

				entry = cursor.getEntry();
			}

			List<UuidMapper> memberUIs = search.getUserGroupsByMemberUuid(connection, ldapParameters, entry);
			Assert.assertEquals(0, memberUIs.size());
			Assert.assertEquals(JobExitStatus.COMPLETED_WITH_WARNINGS, importLogger.repportStatus.get().getJobStatus());
		}
	}
}
