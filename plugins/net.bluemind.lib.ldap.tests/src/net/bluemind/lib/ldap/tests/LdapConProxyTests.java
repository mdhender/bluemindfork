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
package net.bluemind.lib.ldap.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;
import org.apache.directory.ldap.client.api.NoVerificationTrustManager;
import org.apache.directory.ldap.client.api.exception.InvalidConnectionException;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import net.bluemind.lib.ldap.LdapConProxy;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.pool.impl.docker.DockerContainer;
import net.bluemind.system.ldap.tests.helpers.LdapDockerTestHelper;

public class LdapConProxyTests {
	@Rule
	public TestName testName = new TestName();

	@BeforeClass
	public static void beforeClass() {
		LdapDockerTestHelper.initLdapServer();
	}

	@Test
	public void testPlainConnect() throws LdapException, IOException {
		LdapConnectionConfig config = new LdapConnectionConfig();
		config.setLdapHost(new BmConfIni().get(DockerContainer.LDAP.getName()));
		config.setLdapPort(389);

		try (LdapConProxy ldapCon = new LdapConProxy(config)) {
			assertTrue(ldapCon.connect());
			assertTrue(ldapCon.isConnected());

			ldapCon.anonymousBind();
			assertTrue(ldapCon.isAuthenticated());
		}
	}

	@Test
	public void testSSLConnectAcceptAllCertificate() throws LdapException, IOException {
		LdapConnectionConfig config = new LdapConnectionConfig();
		config.setLdapHost(new BmConfIni().get(DockerContainer.LDAP.getName()));
		config.setLdapPort(636);
		config.setUseSsl(true);
		config.setTrustManagers(new NoVerificationTrustManager());

		try (LdapConProxy ldapCon = new LdapConProxy(config)) {
			assertTrue(ldapCon.connect());
			assertTrue(ldapCon.isConnected());

			ldapCon.anonymousBind();
			assertTrue(ldapCon.isAuthenticated());
		}
	}

	@Test
	public void testSSLConnectFailIvalidCertificate() throws LdapException, IOException {
		LdapConnectionConfig config = new LdapConnectionConfig();
		config.setLdapHost(new BmConfIni().get(DockerContainer.LDAP.getHostProperty()));
		config.setLdapPort(636);
		config.setUseSsl(true);
		config.setTrustManagers(new NoVerificationTrustManager());

		try (LdapConProxy ldapCon = new LdapConProxy(config)) {
			assertTrue(ldapCon.connect());
			assertTrue(ldapCon.isConnected());

			ldapCon.anonymousBind();
		} catch (InvalidConnectionException ice) {
			assertEquals("SSL handshake failed.", ice.getMessage());
		}

		config = new LdapConnectionConfig();
		config.setLdapHost(new BmConfIni().get(DockerContainer.LDAP.getHostProperty()));
		config.setLdapPort(636);
		config.setUseSsl(true);

		try (LdapConProxy ldapCon = new LdapConProxy(config)) {
			assertTrue(ldapCon.connect());
			assertTrue(ldapCon.isConnected());

			ldapCon.anonymousBind();
		} catch (InvalidConnectionException ice) {
			assertEquals("SSL handshake failed.", ice.getMessage());
		}
	}

	@Test
	public void testTLSConnectAcceptAllCertificate() throws LdapException, IOException {
		LdapConnectionConfig config = new LdapConnectionConfig();
		config.setLdapHost(new BmConfIni().get(DockerContainer.LDAP.getHostProperty()));
		config.setLdapPort(389);
		config.setUseTls(true);
		config.setTrustManagers(new NoVerificationTrustManager());

		try (LdapConProxy ldapCon = new LdapConProxy(config)) {
			assertTrue(ldapCon.connect());
			assertTrue(ldapCon.isConnected());

			ldapCon.anonymousBind();
			assertTrue(ldapCon.isAuthenticated());
		}
	}

	@Test
	public void testTLSConnectFailIvalidCertificate() throws LdapException, IOException {
		LdapConnectionConfig config = new LdapConnectionConfig();
		config.setLdapHost(new BmConfIni().get(DockerContainer.LDAP.getHostProperty()));
		config.setLdapPort(389);
		config.setUseTls(true);
		config.setTrustManagers(new NoVerificationTrustManager());

		try (LdapConProxy ldapCon = new LdapConProxy(config)) {
			assertTrue(ldapCon.connect());
			assertTrue(ldapCon.isConnected());

			ldapCon.anonymousBind();
		} catch (InvalidConnectionException ice) {
			assertEquals("SSL handshake failed.", ice.getMessage());
		}

		config = new LdapConnectionConfig();
		config.setLdapHost(new BmConfIni().get(DockerContainer.LDAP.getHostProperty()));
		config.setLdapPort(389);
		config.setUseTls(true);

		try (LdapConProxy ldapCon = new LdapConProxy(config)) {
			assertTrue(ldapCon.connect());
			assertTrue(ldapCon.isConnected());

			ldapCon.anonymousBind();
		} catch (InvalidConnectionException ice) {
			assertEquals("SSL handshake failed.", ice.getMessage());
		}
	}
}
