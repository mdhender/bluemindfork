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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.apache.directory.ldap.client.api.LdapConnectionPool;
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
import net.bluemind.system.importation.commons.exceptions.NoLdapHostAvailableFault;
import net.bluemind.system.ldap.tests.helpers.LdapDockerTestHelper;

public class LdapPoolWrapperTests {
	@BeforeClass
	public static void beforeClass() {
		LdapDockerTestHelper.initLdapServer();
	}

	private Parameters getLdapParameters() {
		return getLdapParameters((Host) null);
	}

	private Parameters getLdapParameters(Host host) {
		return getLdapParameters(host, null);
	}

	private Parameters getLdapParameters(Host host, LdapProtocol ldapProtocol) {
		Server server = new TestServer(host, "login", "password", ldapProtocol, true);
		Directory directory = Directory.build("dc=basedn", "userfilter", "groupfilter", "extid");
		SplitDomain splitDomain = new SplitDomain(false, null);

		return Parameters.build(true, server, directory, splitDomain, Optional.of("lastupdate"));
	}

	private Parameters getLdapParameters(List<Host> hosts) {
		TestServer server = new TestServer(null, "login", "password", LdapProtocol.PLAIN, true);
		server.alternativeHosts = hosts;
		Directory directory = Directory.build("dc=basedn", "userfilter", "groupfilter", "extid");
		SplitDomain splitDomain = new SplitDomain(false, null);

		return Parameters.build(true, server, directory, splitDomain, Optional.of("lastupdate"));
	}

	@Test
	public void ldapPoolWrapper_invalidHost() {
		LdapPoolWrapper lpw = new LdapPoolWrapper(getLdapParameters(Host.build("hostname", 389, 0, 0)));

		try {
			lpw.getPool();
			fail("Test must thrown an exception");
		} catch (NoLdapHostAvailableFault nlha) {
		}
	}

	@Test
	public void ldapPoolWrapper_validHost() {
		LdapPoolWrapper lpw = new LdapPoolWrapper(
				getLdapParameters(Host.build(new BmConfIni().get(DockerContainer.LDAP.getName()), 389, 0, 0)));

		LdapConnectionPool pool = lpw.getPool();
		assertNotNull(pool);
		assertEquals(pool, lpw.getPool());
	}

	@Test
	public void ldapPoolWrapper_noHost() {
		LdapPoolWrapper lpw = new LdapPoolWrapper(getLdapParameters());

		try {
			lpw.getPool();
			fail("Test must thrown an exception");
		} catch (IllegalArgumentException iae) {
			assertEquals("At least one LDAP host must be defined!", iae.getMessage());
		}
	}

	@Test
	public void ldapPoolWrapper_multipleHosts() {
		List<Host> hosts = new ArrayList<>(Arrays.asList(Host.build("hostname", 389, 0, 0),
				Host.build(new BmConfIni().get(DockerContainer.LDAP.getName()), 389, 0, 0)));
		LdapPoolWrapper lpw = new LdapPoolWrapper(getLdapParameters(hosts));

		LdapConnectionPool pool = lpw.getPool();
		assertNotNull(pool);
		assertEquals(pool, lpw.getPool());

		assertEquals(new BmConfIni().get(DockerContainer.LDAP.getName()), lpw.ldapConnectionConfig.getLdapHost());
	}

	@Test
	public void ldapPoolWrapper_tlsFallbackToPlain() {
		LdapPoolWrapper lpw = new LdapPoolWrapper(
				getLdapParameters(Host.build("junit-ad-plugin.blue-mind.loc", 389, 0, 0), LdapProtocol.TLSPLAIN));

		LdapConnectionPool pool = lpw.getPool();
		assertNotNull(pool);
		assertEquals(pool, lpw.getPool());
	}

	@Test
	public void ldapPoolWrapper_tlsNoFallbackToPlain() {
		LdapPoolWrapper lpw = new LdapPoolWrapper(
				getLdapParameters(Host.build("junit-ad-plugin.blue-mind.loc", 389, 0, 0), LdapProtocol.TLS));

		try {
			lpw.getPool();
			fail("Test must thrown an exception");
		} catch (NoLdapHostAvailableFault nlhaf) {
		}
	}
}
