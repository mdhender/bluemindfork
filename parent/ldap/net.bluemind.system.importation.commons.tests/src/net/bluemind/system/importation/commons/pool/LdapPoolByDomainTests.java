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
package net.bluemind.system.importation.commons.pool;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Optional;

import org.apache.directory.ldap.client.api.LdapConnection;
import org.junit.BeforeClass;
import org.junit.Test;

import net.bluemind.lib.ldap.LdapProtocol;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.pool.impl.docker.DockerContainer;
import net.bluemind.system.importation.commons.Parameters;
import net.bluemind.system.importation.commons.Parameters.Directory;
import net.bluemind.system.importation.commons.Parameters.Server;
import net.bluemind.system.importation.commons.Parameters.Server.Host;
import net.bluemind.system.importation.commons.Parameters.SplitDomain;
import net.bluemind.system.importation.commons.pool.LdapPoolByDomain.LdapConnectionContext;
import net.bluemind.system.ldap.tests.helpers.LdapDockerTestHelper;

public class LdapPoolByDomainTests {
	@BeforeClass
	public static void beforeClass() {
		LdapDockerTestHelper.initLdapServer();
	}

	private Parameters getImportLdapParameters() {
		Server server = new TestServer(Host.build(new BmConfIni().get(DockerContainer.LDAP.getName()), 389, 0, 0),
				"login", "password", LdapProtocol.PLAIN, true);
		Directory directory = Directory.build("dc=basedn", "userfilter", "groupfilter", "extid");
		SplitDomain splitDomain = new SplitDomain(false, null);

		return Parameters.build(true, server, directory, splitDomain, Optional.of("lastupdate"));
	}

	@Test
	public void ldapPoolByDomain_getConnection() throws Exception {
		LdapPoolByDomain poolByDomain = new LdapPoolByDomain();
		Parameters ldapParameters = getImportLdapParameters();

		LdapConnectionContext connCtx = poolByDomain.getConnectionContext(ldapParameters);
		assertNotNull(connCtx.ldapCon);
		assertTrue(connCtx.ldapCon.isConnected());

		assertNotNull(connCtx.ldapParameters);
		assertEquals(ldapParameters, connCtx.ldapParameters);

		assertNotNull(connCtx.ldapConnectionConfig.getLdapHost());
		assertEquals(ldapParameters.ldapServer.getLdapHost().get(0).hostname,
				connCtx.ldapConnectionConfig.getLdapHost());
	}

	@Test
	public void ldapPoolByDomain_getSameConnection() throws Exception {
		LdapPoolByDomain poolByDomain = new LdapPoolByDomain();

		LdapConnectionContext connCtx = poolByDomain.getConnectionContext(getImportLdapParameters());
		poolByDomain.releaseConnectionContext(connCtx);

		assertTrue(connCtx.ldapCon == poolByDomain.getConnectionContext(getImportLdapParameters()).ldapCon);
	}

	@Test
	public void ldapPoolByDomain_getTwoConnections() throws Exception {
		LdapPoolByDomain poolByDomain = new LdapPoolByDomain();

		LdapConnection conn1 = poolByDomain.getConnectionContext(getImportLdapParameters()).ldapCon;
		LdapConnection conn2 = poolByDomain.getConnectionContext(getImportLdapParameters()).ldapCon;

		assertFalse(conn1 == conn2);
	}

	@Test
	public void ldapPoolByDomain_resetPool() throws Exception {
		LdapPoolByDomain poolByDomain = new LdapPoolByDomain();

		LdapConnectionContext connCtx1 = poolByDomain.getConnectionContext(getImportLdapParameters());
		poolByDomain.resetPool(getImportLdapParameters());
		poolByDomain.releaseConnectionContext(connCtx1);

		LdapConnectionContext connCtx2 = poolByDomain.getConnectionContext(getImportLdapParameters());

		assertFalse(connCtx1.ldapCon == connCtx2.ldapCon);
	}
}
