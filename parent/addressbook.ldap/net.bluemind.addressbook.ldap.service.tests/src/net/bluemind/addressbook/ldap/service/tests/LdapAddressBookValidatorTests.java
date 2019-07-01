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
package net.bluemind.addressbook.ldap.service.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import net.bluemind.addressbook.api.AddressBookDescriptor;
import net.bluemind.addressbook.ldap.service.internal.LdapAddressbookValidator;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.pool.impl.docker.DockerContainer;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.system.ldap.tests.helpers.LdapDockerTestHelper;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class LdapAddressBookValidatorTests {
	private String domainUid = "bm.lan";

	@Rule
	public TestName testName = new TestName();

	@BeforeClass
	public static void beforeClass() {
		LdapDockerTestHelper.initLdapServer();
	}

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		
		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());
		PopulateHelper.initGlobalVirt();
		PopulateHelper.createTestDomain(domainUid);

		LdapDockerTestHelper.initLdapTree(this.getClass(), testName);
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testRole() {
		BmTestContext ctx = BmTestContext.contextWithSession("testRole", "testRole", domainUid);
		LdapAddressbookValidator validator = new LdapAddressbookValidator(ctx);

		Map<String, String> settings = new HashMap<String, String>();
		settings.put("type", "ldap");
		settings.put("hostname", new BmConfIni().get(DockerContainer.LDAP.getName()));
		settings.put("protocol", "plain");
		settings.put("allCertificate", "false");
		settings.put("baseDn", "dc=local");
		settings.put("loginDn", "uid=admin,dc=local");
		settings.put("loginPw", "admin");
		settings.put("userFilter", "(objectClass=inetOrgPerson)");
		AddressBookDescriptor bookDescriptor = AddressBookDescriptor.create("test", domainUid, domainUid, settings);

		try {
			validator.create(bookDescriptor);
			fail("should fail because of manageDomainLDAPAB role");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.PERMISSION_DENIED, sf.getCode());
		}

		ctx = BmTestContext.contextWithSession("testRole", "testRole", domainUid,
				BasicRoles.ROLE_MANAGE_DOMAIN_LDAP_AB);
		validator = new LdapAddressbookValidator(ctx);
		try {
			validator.create(bookDescriptor);
		} catch (ServerFault sf) {
			fail(sf.getMessage());
		}
	}
}
