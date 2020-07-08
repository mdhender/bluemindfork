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
package net.bluemind.system.importation.commons;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.Test;

import net.bluemind.lib.ldap.LdapProtocol;
import net.bluemind.system.importation.commons.Parameters.Server;
import net.bluemind.system.importation.commons.Parameters.Server.Host;

public class LdapParametersServerTest {
	public class ServerTest extends Server {
		public List<Host> alternativeHosts = new ArrayList<>();

		public ServerTest(Host host, String login, String password, LdapProtocol protocol,
				boolean acceptAllCertificates) {
			super(Optional.ofNullable(host), login, password, protocol, acceptAllCertificates);
		}

		@Override
		protected List<Host> getAlternativeHosts() {
			return alternativeHosts;
		}
	}

	@Test
	public void server_serverInstance() {
		ServerTest server = new ServerTest(Host.build("hostname", 389, 1, 2), "login", "password", LdapProtocol.SSL,
				false);

		assertNotNull(server.getLdapHost());
		assertEquals(1, server.getLdapHost().size());
		assertEquals("hostname", server.getLdapHost().get(0).hostname);

		assertEquals("login", server.login);
		assertEquals("password", server.password);
		assertEquals(LdapProtocol.SSL, server.protocol);
		assertFalse(server.acceptAllCertificates);
	}

	@Test
	public void server_hostOrder() {
		ServerTest server = new ServerTest(Host.build("hostname", 389, 1, 2), "login", "password", LdapProtocol.SSL,
				false);
		server.alternativeHosts.add(Host.build("2 3 389 hostname"));

		assertNotNull(server.getLdapHost());
		assertEquals(1, server.getLdapHost().size());
		assertEquals(1, server.getLdapHost().get(0).priority);
		assertEquals(2, server.getLdapHost().get(0).weight);

		server = new ServerTest(null, "login", "password", LdapProtocol.SSL, false);
		server.alternativeHosts.add(Host.build("3 1 389 hostname4"));
		server.alternativeHosts.add(Host.build("2 1 389 hostname3"));
		server.alternativeHosts.add(Host.build("1 1 389 hostname1"));
		server.alternativeHosts.add(Host.build("2 2 389 hostname2"));

		assertNotNull(server.getLdapHost());
		assertEquals(4, server.getLdapHost().size());

		int pos = 1;
		for (Host h : server.getLdapHost()) {
			assertEquals("hostname" + pos, h.hostname);
			pos++;
		}
	}

	@Test
	public void server_equals() {
		ServerTest server1 = new ServerTest(Host.build("hostname", 389, 1, 2), "login1", "password1", LdapProtocol.SSL,
				false);
		ServerTest server2 = new ServerTest(Host.build("hostname", 389, 1, 2), "login1", "password1", LdapProtocol.SSL,
				false);

		assertEquals(server1, server2);
	}

	@Test
	public void server_notEquals() {
		ServerTest server1 = new ServerTest(Host.build("hostname1", 389, 1, 2), "login1", "password1", LdapProtocol.SSL,
				false);

		ServerTest server2 = new ServerTest(Host.build("hostname2", 389, 1, 2), "login2", "password1", LdapProtocol.SSL,
				false);
		assertNotEquals(server1, server2);

		server2 = new ServerTest(Host.build("hostname1", 389, 1, 2), "login1", "password2", LdapProtocol.SSL, false);
		assertNotEquals(server1, server2);

		server2 = new ServerTest(Host.build("hostname1", 389, 1, 2), "login1", "password1", LdapProtocol.PLAIN, false);
		assertNotEquals(server1, server2);

		server2 = new ServerTest(Host.build("hostname1", 389, 1, 2), "login1", "password1", LdapProtocol.SSL, true);
		assertNotEquals(server1, server2);

		server2 = new ServerTest(Host.build("hostname2", 389, 1, 2), "login1", "password1", LdapProtocol.SSL, false);
		assertNotEquals(server1, server2);
	}
}
