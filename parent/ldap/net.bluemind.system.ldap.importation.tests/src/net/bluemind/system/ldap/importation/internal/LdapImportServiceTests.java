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
package net.bluemind.system.ldap.importation.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;

import org.apache.directory.api.ldap.model.exception.LdapException;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.pool.impl.docker.DockerContainer;
import net.bluemind.scheduledjob.scheduler.impl.JobRegistry;
import net.bluemind.system.ldap.importation.api.ILdapImport;
import net.bluemind.system.ldap.importation.api.LdapProperties;
import net.bluemind.system.ldap.tests.helpers.LdapDockerTestHelper;
import net.bluemind.system.ldap.tests.helpers.LdapDockerTestHelper.DeleteTreeException;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class LdapImportServiceTests {
	private SecurityContext admin0;
	private SecurityContext domainAdmin;
	private String domainUid;
	private SecurityContext domainUser;
	private String ldapDockerHostname;

	@Rule
	public TestName testName = new TestName();

	@BeforeClass
	public static void beforeClass() {
		LdapDockerTestHelper.initLdapServer();
	}

	@Before
	public void before() throws LdapException, DeleteTreeException, IOException {
		LdapDockerTestHelper.initLdapTree(this.getClass(), testName);
	}

	@Before
	public void setup() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());

		// Vertx vs Eclipse
		new JobRegistry();

		final CountDownLatch launched = new CountDownLatch(1);
		VertxPlatform.spawnVerticles(new Handler<AsyncResult<Void>>() {
			@Override
			public void handle(AsyncResult<Void> event) {
				launched.countDown();
			}
		});
		launched.await();

		admin0 = new SecurityContext("admin0", "admin0", Collections.<String>emptyList(),
				Arrays.asList(SecurityContext.ROLE_SYSTEM), "global");
		Sessions.get().put(admin0.getSessionId(), admin0);

		domainAdmin = new SecurityContext("domainAdmin", "domainAdmin", Collections.<String>emptyList(),
				Arrays.asList(SecurityContext.ROLE_ADMIN), domainUid);
		Sessions.get().put(domainAdmin.getSessionId(), domainAdmin);

		domainUser = new SecurityContext("domainUser", "domainUser", Collections.<String>emptyList(),
				Collections.<String>emptyList(), domainUid);
		Sessions.get().put(domainUser.getSessionId(), domainUser);

		domainUid = "ldap-import.tld";

		PopulateHelper.initGlobalVirt();
		PopulateHelper.createTestDomain(domainUid);
		PopulateHelper.domainAdmin(domainUid, domainAdmin.getSubject());

		ldapDockerHostname = new BmConfIni().get(DockerContainer.LDAP.getName());
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	protected ILdapImport getService(SecurityContext sc) throws ServerFault {
		return ServerSideServiceProvider.getProvider(sc).instance(ILdapImport.class);
	}

	protected IDomains getDomainService(SecurityContext sc) throws ServerFault {
		return ServerSideServiceProvider.getProvider(sc).instance(IDomains.class);
	}

	@Test
	public void validParameters() throws ServerFault {
		getService(admin0).testParameters(ldapDockerHostname, "plain", "false", "dc=local", "uid=admin,dc=local",
				"admin", "(objectClass=inetOrgPerson)", "(objectClass=inetOrgPerson)");

		getService(domainAdmin).testParameters(ldapDockerHostname, "plain", "false", "dc=local", "uid=admin,dc=local",
				"admin", "(objectClass=inetOrgPerson)", "(objectClass=inetOrgPerson)");

		try {
			getService(domainUser).testParameters(ldapDockerHostname, "plain", "false", "dc=local",
					"uid=admin,dc=local", "admin", "(objectClass=inetOrgPerson)", "(objectClass=inetOrgPerson)");
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals("Only admin users can test LDAP parameters", sf.getMessage());
			assertEquals(ErrorCode.FORBIDDEN, sf.getCode());
		}
	}

	@Test
	public void invalidGroupFilter() throws ServerFault {
		try {
			getService(admin0).testParameters(ldapDockerHostname, "plain", "false", "dc=local", "uid=admin,dc=local",
					"invalid", "(objectClass=inetOrgPerson)", "(invalid");
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals("Invalid LDAP group filter", sf.getMessage());
		}
	}

	@Test
	public void nullGroupFilter() throws ServerFault {
		try {
			getService(admin0).testParameters(ldapDockerHostname, "plain", "false", "dc=local", "uid=admin,dc=local",
					null, "(objectClass=inetOrgPerson)", null);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals("Invalid LDAP group filter", sf.getMessage());
		}
	}

	@Test
	public void invalidUserFilter() throws ServerFault {
		try {
			getService(admin0).testParameters(ldapDockerHostname, "plain", "false", "dc=local", "uid=admin,dc=local",
					"invalid", "(invalid", "(objectClass=inetOrgPerson)");
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals("Invalid LDAP user filter", sf.getMessage());
		}
	}

	@Test
	public void nullUserFilter() throws ServerFault {
		try {
			getService(admin0).testParameters(ldapDockerHostname, "plain", "false", "dc=local", "uid=admin,dc=local",
					null, null, "(objectClass=inetOrgPerson)");
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals("Invalid LDAP user filter", sf.getMessage());
		}
	}

	@Test
	public void invalidPassword() throws ServerFault {
		try {
			getService(admin0).testParameters(ldapDockerHostname, "plain", "false", "dc=local", "uid=admin,dc=local",
					"invalid", "(objectClass=inetOrgPerson)", "(objectClass=inetOrgPerson)");
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertTrue(sf.getMessage().startsWith("LDAP connection failed: "));
		}
	}

	@Test
	public void nullPassword() throws ServerFault {
		try {
			getService(admin0).testParameters(ldapDockerHostname, "plain", "false", "dc=local", "uid=admin,dc=local",
					null, "(objectClass=inetOrgPerson)", "(objectClass=inetOrgPerson)");
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertTrue(sf.getMessage().startsWith("LDAP connection failed: "));
		}
	}

	@Test
	public void invalidLogin() throws ServerFault {
		try {
			getService(admin0).testParameters(ldapDockerHostname, "plain", "false", "dc=local", "invalid", "admin",
					"(objectClass=inetOrgPerson)", "(objectClass=inetOrgPerson)");
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals("Invalid LDAP login", sf.getMessage());
		}
	}

	@Test
	public void nullLogin() throws ServerFault {
		getService(admin0).testParameters(ldapDockerHostname, "plain", "false", "dc=local", null, "admin",
				"(objectClass=inetOrgPerson)", "(objectClass=inetOrgPerson)");
	}

	@Test
	public void emptyBaseDn() throws ServerFault {
		getService(admin0).testParameters(ldapDockerHostname, "plain", "false", "", "uid=admin,dc=local", "admin",
				"(objectClass=inetOrgPerson)", "(objectClass=inetOrgPerson)");
	}

	@Test
	public void nullBaseDn() throws ServerFault {
		getService(admin0).testParameters(ldapDockerHostname, "plain", "false", null, "uid=admin,dc=local", "admin",
				"(objectClass=inetOrgPerson)", "(objectClass=inetOrgPerson)");
	}

	@Test
	public void invalidBaseDn() throws ServerFault {
		try {
			getService(admin0).testParameters(ldapDockerHostname, "plain", "false", "invalid", "uid=admin,dc=local",
					"admin", "(objectClass=inetOrgPerson)", "(objectClass=inetOrgPerson)");
		} catch (ServerFault sf) {
			assertEquals("Invalid LDAP base DN", sf.getMessage());
		}

		try {
			getService(admin0).testParameters(ldapDockerHostname, "plain", "false", "dc=invalid", "uid=admin,dc=local",
					"admin", "(objectClass=inetOrgPerson)", "(objectClass=inetOrgPerson)");
		} catch (ServerFault sf) {
			assertEquals("Base DN not found, check existence or set server default search base", sf.getMessage());
		}
	}

	@Test
	public void unavailableHostname() throws ServerFault {
		try {
			getService(admin0).testParameters("invalid", "plain", "false", "dc=local", "uid=admin,dc=local", "admin",
					"(objectClass=inetOrgPerson)", "(objectClass=inetOrgPerson)");
		} catch (ServerFault sf) {
			assertTrue(sf.getMessage().contains("Cannot connect to the server"));
		}
	}

	@Test
	public void invalidProtocol() throws ServerFault {
		getService(admin0).testParameters(ldapDockerHostname, "plain", "false", "dc=local", "uid=admin,dc=local",
				"admin", "(objectClass=inetOrgPerson)", "(objectClass=inetOrgPerson)");

		try {
			getService(admin0).testParameters("invalid:hostname", null, "false", "dc=local", "uid=admin,dc=local",
					"admin", "(objectClass=inetOrgPerson)", "(objectClass=inetOrgPerson)");
			fail("Test must thrown an exception");
		} catch (ServerFault e) {
			assertEquals("LDAP protocol must not be null", e.getMessage());
		}

		try {
			getService(admin0).testParameters("hostname", "invalid", "false", "dc=local", "uid=admin,dc=local", "admin",
					"(objectClass=inetOrgPerson)", "(objectClass=inetOrgPerson)");
			fail("Test must thrown an exception");
		} catch (ServerFault e) {
			assertEquals("Invalid LDAP protocol: invalid", e.getMessage());
		}
	}

	@Test
	public void invalidHostname() throws ServerFault {
		try {
			getService(admin0).testParameters(null, "plain", "false", "dc=local", "uid=admin,dc=local", "admin",
					"(objectClass=inetOrgPerson)", "(objectClass=inetOrgPerson)");
			fail("Test must thrown an exception");
		} catch (ServerFault e) {
			assertEquals("Invalid hostname", e.getMessage());
		}

		try {
			getService(admin0).testParameters("", "plain", "false", "dc=local", "uid=admin,dc=local", "admin",
					"(objectClass=inetOrgPerson)", "(objectClass=inetOrgPerson)");
			fail("Test must thrown an exception");
		} catch (ServerFault e) {
			assertEquals("Invalid hostname", e.getMessage());
		}
	}

	@Test
	public void invalidAllCertificate() throws ServerFault {
		try {
			getService(admin0).testParameters("hostname", "plain", "invalid", "dc=local", "uid=admin,dc=local", "admin",
					"(objectClass=inetOrgPerson)", "(objectClass=inetOrgPerson)");
			fail("Test must thrown an exception");
		} catch (ServerFault e) {
			assertEquals("All certificate value must be null, true or false", e.getMessage());
		}

		try {
			getService(admin0).testParameters("hostname", "plain", "", "dc=local", "uid=admin,dc=local", "admin",
					"(objectClass=inetOrgPerson)", "(objectClass=inetOrgPerson)");
			fail("Test must thrown an exception");
		} catch (ServerFault e) {
			assertEquals("All certificate value must be null, true or false", e.getMessage());
		}
	}

	@Test
	public void fullSyncNullOrEmtpyDomainUid() {
		try {
			getService(admin0).fullSync(null);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertTrue(sf.getMessage().equals("Invalid parameter") || sf.getMessage().equals("param uid is null"));
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}

		try {
			getService(admin0).fullSync("");
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertTrue(sf.getMessage().equals("Invalid parameter") || sf.getMessage().equals("param uid is null"));
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}
	}

	@Test
	public void fullSyncNotGlobalAdmin() {
		try {
			getService(domainAdmin).fullSync(domainUid);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals("Only global.virt users can start LDAP global sync", sf.getMessage());
			assertEquals(ErrorCode.FORBIDDEN, sf.getCode());
		}
	}

	@Test
	public void fullSyncInvalidDomainUid() {
		try {
			getService(admin0).fullSync("invalid");
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals("Invalid domain UID: invalid", sf.getMessage());
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}
	}

	@Test
	public void fullSyncDomainLdapNotConfigured() throws ServerFault {
		try {
			getService(admin0).fullSync(domainUid);
			fail("Test must thrown an exception");
		} catch (ServerFault rte) {
			assertEquals("LDAP import is disabled for domain: " + domainUid + " - " + domainUid, rte.getMessage());
		}
	}

	@Test
	public void fullSyncDomain() throws ServerFault, InterruptedException {
		ItemValue<Domain> domain = getDomainService(admin0).get(domainUid);
		domain.value.properties.put(LdapProperties.import_ldap_enabled.name(), Boolean.TRUE.toString());
		domain.value.properties.put(LdapProperties.import_ldap_hostname.name(), ldapDockerHostname);
		domain.value.properties.put(LdapProperties.import_ldap_base_dn.name(), "dc=local");
		domain.value.properties.put(LdapProperties.import_ldap_login_dn.name(), "uid=admin,dc=local");
		domain.value.properties.put(LdapProperties.import_ldap_password.name(), "admin");
		domain.value.properties.put(LdapProperties.import_ldap_lastupdate.name(), "lastupdate");
		getDomainService(admin0).update(domain.uid, domain.value);

		getService(admin0).fullSync(domainUid);

		domain = getDomainService(admin0).get(domainUid);
		assertFalse(domain.value.properties.containsKey(LdapProperties.import_ldap_lastupdate.name()));
	}
}
