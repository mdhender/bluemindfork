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
package net.bluemind.system.ldap.importation.internal.tools;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Test;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.lib.ldap.LdapConProxy;
import net.bluemind.lib.ldap.LdapProtocol;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.pool.impl.docker.DockerContainer;
import net.bluemind.system.importation.commons.exceptions.DirectoryConnectionFailed;
import net.bluemind.system.importation.commons.exceptions.InvalidDnServerFault;
import net.bluemind.system.ldap.tests.helpers.LdapDockerTestHelper;

public class LdapHelperTests {
	@BeforeClass
	public static void beforeClass() {
		LdapDockerTestHelper.initLdapServer(false);
	}

	@Test
	public void invalidHostname() throws InvalidDnServerFault, IOException {
		try (LdapConProxy con = LdapHelper.connectLdap(
				LdapParameters.build("invalid", LdapProtocol.PLAIN, "true", LdapDockerTestHelper.LDAP_ROOT_DN,
						LdapDockerTestHelper.LDAP_LOGIN_DN, LdapDockerTestHelper.LDAP_LOGIN_PWD))) {
			assertTrue(con.isConnected());
			assertFalse(con.getConfig().isUseTls());
			assertFalse(con.getConfig().isUseSsl());
			fail("Test must thrown an exception");
		} catch (DirectoryConnectionFailed dcf) {
		}
	}

	@Test
	public void invalidLogin() throws InvalidDnServerFault, IOException {
		try (LdapConProxy con = LdapHelper.connectLdap(
				LdapParameters.build(new BmConfIni().get(DockerContainer.LDAP.getName()), LdapProtocol.PLAIN, "true",
						LdapDockerTestHelper.LDAP_ROOT_DN, "invalid", LdapDockerTestHelper.LDAP_LOGIN_PWD))) {
			assertTrue(con.isConnected());
			assertFalse(con.getConfig().isUseTls());
			assertFalse(con.getConfig().isUseSsl());
			fail("Test must thrown an exception");
		} catch (DirectoryConnectionFailed dcf) {
		}
	}

	@Test
	public void invalidPassword() throws InvalidDnServerFault, IOException {
		try (LdapConProxy con = LdapHelper.connectLdap(
				LdapParameters.build(new BmConfIni().get(DockerContainer.LDAP.getName()), LdapProtocol.PLAIN, "true",
						LdapDockerTestHelper.LDAP_ROOT_DN, LdapDockerTestHelper.LDAP_LOGIN_DN, "invalid"))) {
			assertTrue(con.isConnected());
			assertFalse(con.getConfig().isUseTls());
			assertFalse(con.getConfig().isUseSsl());
			fail("Test must thrown an exception");
		} catch (DirectoryConnectionFailed dcf) {
		}
	}

	@Test
	public void testConnectNoTls() throws ServerFault, IOException {
		try (LdapConProxy con = LdapHelper
				.connectLdap(LdapParameters.build(new BmConfIni().get(DockerContainer.LDAP.getName()),
						LdapProtocol.PLAIN, "true", LdapDockerTestHelper.LDAP_ROOT_DN,
						LdapDockerTestHelper.LDAP_LOGIN_DN, LdapDockerTestHelper.LDAP_LOGIN_PWD))) {
			assertTrue(con.isConnected());
			assertFalse(con.getConfig().isUseTls());
			assertFalse(con.getConfig().isUseSsl());
		}
	}

	@Test
	public void testConnectTls() throws ServerFault, IOException {
		try (LdapConProxy con = LdapHelper
				.connectLdap(LdapParameters.build(new BmConfIni().get(DockerContainer.LDAP.getName()), LdapProtocol.TLS,
						"true", LdapDockerTestHelper.LDAP_ROOT_DN, LdapDockerTestHelper.LDAP_LOGIN_DN,
						LdapDockerTestHelper.LDAP_LOGIN_PWD))) {
			assertTrue(con.isConnected());
			assertTrue(con.getConfig().isUseTls());
			assertFalse(con.getConfig().isUseSsl());
		}
	}

	@Test
	public void testConnectSsl() throws ServerFault, IOException {
		try (LdapConProxy con = LdapHelper
				.connectLdap(LdapParameters.build(new BmConfIni().get(DockerContainer.LDAP.getName()), LdapProtocol.SSL,
						"true", LdapDockerTestHelper.LDAP_ROOT_DN, LdapDockerTestHelper.LDAP_LOGIN_DN,
						LdapDockerTestHelper.LDAP_LOGIN_PWD))) {
			assertTrue(con.isConnected());
			assertFalse(con.getConfig().isUseTls());
			assertTrue(con.getConfig().isUseSsl());
		}
	}
}
