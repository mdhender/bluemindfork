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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.junit.Test;

import net.bluemind.lib.ldap.LdapProtocol;
import net.bluemind.system.importation.commons.Parameters.Directory;
import net.bluemind.system.importation.commons.Parameters.Server;
import net.bluemind.system.importation.commons.Parameters.Server.Host;
import net.bluemind.system.importation.commons.Parameters.SplitDomain;

public class LdapParametersTest {
	public class ServerTest extends Server {
		public List<Host> alternativeHosts = new ArrayList<>();

		public ServerTest(Host host, String login, String password, LdapProtocol protocol,
				boolean acceptAllCertificates) {
			super(Optional.of(host), login, password, protocol, acceptAllCertificates);
		}

		@Override
		protected List<Host> getAlternativeHosts() {
			return alternativeHosts;
		}
	}

	@Test
	public void ldapParameter_disabled() throws LdapInvalidDnException {
		Parameters ldapParameters = Parameters.build(false,
				new ServerTest(Host.build("hostname", 389, 1, 2), "login", "password", LdapProtocol.PLAIN, true),
				Directory.build("dc=local", "userfilter", "groupfilter", "extidattr"),
				new SplitDomain(true, "relaymailboxgroup"), Optional.of("lastupdate"));

		assertFalse(ldapParameters.enabled);
		assertNotNull(ldapParameters.ldapServer);
		assertNotNull(ldapParameters.ldapDirectory);
		assertNotNull(ldapParameters.splitDomain);
		assertTrue(ldapParameters.lastUpdate.isPresent());

		ldapParameters = Parameters.disabled();
		assertFalse(ldapParameters.enabled);
		assertNull(ldapParameters.ldapServer);
		assertNull(ldapParameters.ldapDirectory);
		assertNull(ldapParameters.splitDomain);
		assertFalse(ldapParameters.lastUpdate.isPresent());
	}

	@Test
	public void ldapParameter_equals() throws LdapInvalidDnException {
		Parameters ldapParameters1 = Parameters.build(false,
				new ServerTest(Host.build("hostname1", 389, 1, 2), "login1", "password1", LdapProtocol.PLAIN, true),
				Directory.build("dc=local1", "userfilter1", "groupfilter1", "extidattr1"),
				new SplitDomain(true, "relaymailboxgroup1"), Optional.of("lastupdate1"));
		Parameters ldapParameters2 = Parameters.build(false,
				new ServerTest(Host.build("hostname1", 389, 1, 2), "login1", "password1", LdapProtocol.PLAIN, true),
				Directory.build("dc=local1", "userfilter1", "groupfilter1", "extidattr1"),
				new SplitDomain(true, "relaymailboxgroup1"), Optional.of("lastupdate1"));

		assertEquals(ldapParameters1, ldapParameters2);

		ldapParameters2 = Parameters.build(false,
				new ServerTest(Host.build("hostname1", 389, 1, 2), "login1", "password1", LdapProtocol.PLAIN, true),
				Directory.build("dc=local1", "userfilter1", "groupfilter1", "extidattr1"),
				new SplitDomain(true, "relaymailboxgroup1"), Optional.of("lastupdate2"));
		assertEquals(ldapParameters1, ldapParameters2);
	}

	@Test
	public void server_notEquals() throws LdapInvalidDnException {
		Parameters ldapParameters1 = Parameters.build(false,
				new ServerTest(Host.build("hostname1", 389, 1, 2), "login1", "password1", LdapProtocol.PLAIN, true),
				Directory.build("dc=local1", "userfilter1", "groupfilter1", "extidattr1"),
				new SplitDomain(true, "relaymailboxgroup1"), Optional.of("lastupdate1"));

		Parameters ldapParameters2 = Parameters.build(false,
				new ServerTest(Host.build("hostname2", 389, 1, 2), "login2", "password2", LdapProtocol.PLAIN, true),
				Directory.build("dc=local2", "userfilter2", "groupfilter2", "extidattr2"),
				new SplitDomain(true, "relaymailboxgroup2"), Optional.of("lastupdate2"));
		assertNotEquals(ldapParameters1, ldapParameters2);

		ldapParameters2 = Parameters.build(false,
				new ServerTest(Host.build("hostname2", 389, 1, 2), "login1", "password1", LdapProtocol.PLAIN, true),
				Directory.build("dc=local1", "userfilter1", "groupfilter1", "extidattr1"),
				new SplitDomain(true, "relaymailboxgroup1"), Optional.of("lastupdate1"));
		assertNotEquals(ldapParameters1, ldapParameters2);

		ldapParameters2 = Parameters.build(false,
				new ServerTest(Host.build("hostname1", 389, 1, 2), "login1", "password1", LdapProtocol.PLAIN, true),
				Directory.build("dc=local2", "userfilter1", "groupfilter1", "extidattr1"),
				new SplitDomain(true, "relaymailboxgroup1"), Optional.of("lastupdate1"));
		assertNotEquals(ldapParameters1, ldapParameters2);

		ldapParameters2 = Parameters.build(false,
				new ServerTest(Host.build("hostname1", 389, 1, 2), "login1", "password1", LdapProtocol.PLAIN, true),
				Directory.build("dc=local1", "userfilter1", "groupfilter1", "extidattr1"),
				new SplitDomain(false, "relaymailboxgroup1"), Optional.of("lastupdate1"));
		assertNotEquals(ldapParameters1, ldapParameters2);
	}

	@Test
	public void ldapParameter_enabled() throws LdapInvalidDnException {
		Parameters ldapParameters = Parameters.build(true,
				new ServerTest(Host.build("hostname", 389, 1, 2), "login", "password", LdapProtocol.PLAIN, true),
				Directory.build("dc=local", "userfilter", "groupfilter", "extidattr"),
				new SplitDomain(true, "relaymailboxgroup"), Optional.of("lastupdate"));

		assertTrue(ldapParameters.enabled);
		assertEquals(new ServerTest(Host.build("hostname", 389, 1, 2), "login", "password", LdapProtocol.PLAIN, true),
				ldapParameters.ldapServer);
		assertEquals(Directory.build("dc=local", "userfilter", "groupfilter", "extidattr"),
				ldapParameters.ldapDirectory);
		assertEquals(new SplitDomain(true, "relaymailboxgroup"), ldapParameters.splitDomain);
		assertEquals("lastupdate", ldapParameters.lastUpdate.get());

		ldapParameters = Parameters.build(true,
				new ServerTest(Host.build("hostname", 389, 1, 2), "login", "password", LdapProtocol.PLAIN, true),
				Directory.build("dc=local", "userfilter", "groupfilter", "extidattr"),
				new SplitDomain(true, "relaymailboxgroup"), Optional.empty());
		assertFalse(ldapParameters.lastUpdate.isPresent());
	}

	@Test
	public void ldapParameter_nullServer() throws LdapInvalidDnException {
		try {
			Parameters.build(true, null, Directory.build("dc=local", "userfilter", "groupfilter", "extidattr"),
					new SplitDomain(true, "relaymailboxgroup"), Optional.of("lastupdate"));

			fail("Test must thrown an exception");
		} catch (IllegalArgumentException iae) {
			assertEquals("ldapServer must not be null", iae.getMessage());
		}
	}

	@Test
	public void ldapParameter_nullDirectory() throws LdapInvalidDnException {
		try {
			Parameters.build(true,
					new ServerTest(Host.build("hostname", 389, 1, 2), "login", "password", LdapProtocol.PLAIN, true),
					null, new SplitDomain(true, "relaymailboxgroup"), Optional.of("lastupdate"));

			fail("Test must thrown an exception");
		} catch (IllegalArgumentException iae) {
			assertEquals("ldapDirectory must not be null", iae.getMessage());
		}
	}

	@Test
	public void ldapParameter_nullSplitDomain() throws LdapInvalidDnException {
		try {
			Parameters.build(true,
					new ServerTest(Host.build("hostname", 389, 1, 2), "login", "password", LdapProtocol.PLAIN, true),
					Directory.build("dc=local", "userfilter", "groupfilter", "extidattr"), null,
					Optional.of("lastupdate"));

			fail("Test must thrown an exception");
		} catch (IllegalArgumentException iae) {
			assertEquals("splitDomain must not be null", iae.getMessage());
		}
	}

	@Test
	public void ldapParameter_emptyLastUpdate() throws LdapInvalidDnException {
		try {
			Parameters.build(true,
					new ServerTest(Host.build("hostname", 389, 1, 2), "login", "password", LdapProtocol.PLAIN, true),
					Directory.build("dc=local", "userfilter", "groupfilter", "extidattr"),
					new SplitDomain(true, "relaymailboxgroup"), Optional.of(""));

			fail("Test must thrown an exception");
		} catch (IllegalArgumentException iae) {
			assertEquals("lastUpdate must not be empty", iae.getMessage());
		}

		try {
			Parameters.build(true,
					new ServerTest(Host.build("hostname", 389, 1, 2), "login", "password", LdapProtocol.PLAIN, true),
					Directory.build("dc=local", "userfilter", "groupfilter", "extidattr"),
					new SplitDomain(true, "relaymailboxgroup"), Optional.of("   "));

			fail("Test must thrown an exception");
		} catch (IllegalArgumentException iae) {
			assertEquals("lastUpdate must not be empty", iae.getMessage());
		}
	}
}
