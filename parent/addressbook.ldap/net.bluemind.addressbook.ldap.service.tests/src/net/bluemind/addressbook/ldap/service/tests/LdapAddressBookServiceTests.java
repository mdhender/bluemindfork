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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import net.bluemind.addressbook.ldap.api.ConnectionStatus;
import net.bluemind.addressbook.ldap.api.ILdapAddressBook;
import net.bluemind.addressbook.ldap.api.LdapParameters;
import net.bluemind.addressbook.ldap.api.LdapParameters.DirectoryType;
import net.bluemind.addressbook.ldap.api.fault.LdapAddressBookErrorCode;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.pool.impl.docker.DockerContainer;
import net.bluemind.system.ldap.tests.helpers.LdapDockerTestHelper;

public class LdapAddressBookServiceTests {
	private SecurityContext admin0;
	private SecurityContext domainAdmin;
	private SecurityContext domainUser;

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

		admin0 = new SecurityContext("admin0", "admin0", Collections.<String>emptyList(),
				Arrays.asList(SecurityContext.ROLE_SYSTEM), "global");
		Sessions.get().put(admin0.getSessionId(), admin0);

		domainAdmin = new SecurityContext("domainAdmin", "domainAdmin", Collections.<String>emptyList(),
				Arrays.asList(SecurityContext.ROLE_ADMIN), "bm.lan");
		Sessions.get().put(domainAdmin.getSessionId(), domainAdmin);

		domainUser = new SecurityContext("domainUser", "domainUser", Collections.<String>emptyList(),
				Collections.<String>emptyList(), "bm.lan");
		Sessions.get().put(domainUser.getSessionId(), domainUser);

		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);

		LdapDockerTestHelper.initLdapTree(this.getClass(), testName);
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void validParameters() throws ServerFault {
		LdapParameters lp = LdapParameters.create(DirectoryType.ldap,
				new BmConfIni().get(DockerContainer.LDAP.getName()), "plain", false, "dc=local", "uid=admin,dc=local",
				"admin", "(objectClass=inetOrgPerson)", "entryUUID");

		assertTrue(getService(admin0).testConnection(lp).status);
		assertTrue(getService(domainAdmin).testConnection(lp).status);

		try {
			getService(domainUser).testConnection(lp);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals("Only admin users can test LDAP parameters", sf.getMessage());
			assertEquals(ErrorCode.FORBIDDEN, sf.getCode());
		}

		lp = LdapParameters.create(LdapParameters.DirectoryType.ldap,
				new BmConfIni().get(DockerContainer.LDAP.getName()), "plain", false, "dc=local", "uid=admin,dc=local",
				"bang", "(objectClass=inetOrgPerson)", "entryUUID");
		ConnectionStatus cs = getService(admin0).testConnection(lp);
		assertFalse(cs.status);
		assertEquals(LdapAddressBookErrorCode.INVALID_LDAP_CREDENTIAL, cs.errorCode);
		assertEquals("LDAP connection failed: INVALID_CREDENTIALS", cs.errorMsg);

		lp = LdapParameters.create(LdapParameters.DirectoryType.ldap, "WOOT", "plain", false, "dc=local",
				"uid=admin,dc=local", "bang", "(objectClass=inetOrgPerson)", "entryUUID");
		cs = getService(admin0).testConnection(lp);
		assertFalse(cs.status);
		assertEquals(LdapAddressBookErrorCode.INVALID_LDAP_HOSTNAME, cs.errorCode);
		assertEquals("Fail to connect to LDAP server", cs.errorMsg);

		lp = LdapParameters.create(LdapParameters.DirectoryType.ldap,
				new BmConfIni().get(DockerContainer.LDAP.getName()), "plain", false, "dc=invalid", "uid=admin,dc=local",
				"admin", "(objectClass=inetOrgPerson)", "entryUUID");
		cs = getService(admin0).testConnection(lp);
		assertFalse(cs.status);
		assertEquals(LdapAddressBookErrorCode.INVALID_LDAP_BASEDN, cs.errorCode);
		assertEquals("Base DN not found, check existence or set server default search base", cs.errorMsg);
	}

	protected ILdapAddressBook getService(SecurityContext sc) throws ServerFault {
		return ServerSideServiceProvider.getProvider(sc).instance(ILdapAddressBook.class);
	}

}
