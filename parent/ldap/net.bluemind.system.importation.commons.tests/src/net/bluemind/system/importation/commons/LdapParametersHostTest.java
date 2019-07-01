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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import net.bluemind.system.importation.commons.Parameters.Server.Host;

public class LdapParametersHostTest {
	@Test
	public void host_invalidHostname() {
		try {
			Host.build(null, 0, 0, 0);
			fail("Test must thrown an excpetion");
		} catch (IllegalArgumentException iae) {
			assertEquals("Ldap hostname must be defined", iae.getMessage());
		}

		try {
			Host.build("", 0, 0, 0);
			fail("Test must thrown an excpetion");
		} catch (IllegalArgumentException iae) {
			assertEquals("Ldap hostname must be defined", iae.getMessage());
		}

		try {
			Host.build("   ", 0, 0, 0);
			fail("Test must thrown an excpetion");
		} catch (IllegalArgumentException iae) {
			assertEquals("Ldap hostname must be defined", iae.getMessage());
		}
	}

	@Test
	public void host_validHost() {
		Host h = Host.build("hostname", 1, 2, 3);
		assertEquals(h.hostname, "hostname");
		assertEquals(h.port, 1);
		assertEquals(h.priority, 2);
		assertEquals(h.weight, 3);
	}

	@Test
	public void host_invalidSrvRecord() {
		try {
			Host.build(null);
			fail("Test must thrown an excpetion");
		} catch (IllegalArgumentException iae) {
			assertEquals("srvRecord must be defined", iae.getMessage());
		}

		try {
			Host.build("");
			fail("Test must thrown an excpetion");
		} catch (IllegalArgumentException iae) {
			assertEquals("srvRecord must be defined", iae.getMessage());
		}

		try {
			Host.build("   ");
			fail("Test must thrown an excpetion");
		} catch (IllegalArgumentException iae) {
			assertEquals("srvRecord must be defined", iae.getMessage());
		}

		try {
			Host.build("1 2");
			fail("Test must thrown an excpetion");
		} catch (IllegalArgumentException iae) {
			assertEquals("Invalid srvRecord: 1 2", iae.getMessage());
		}
	}

	@Test
	public void host_validHostFromSrvRecord() {
		Host h = Host.build("1 2 3 hostname");
		assertEquals(h.hostname, "hostname");
		assertEquals(h.port, 3);
		assertEquals(h.priority, 1);
		assertEquals(h.weight, 2);
	}

	@Test
	public void host_equals() {
		Host h1 = Host.build("hostname", 1, 2, 3);
		Host h2 = Host.build("2 3 1 hostname");

		assertEquals(h1, h2);

		h2 = Host.build("4 3 1 hostname");
		assertEquals(h1, h2);

		h2 = Host.build("2 4 1 hostname");
		assertEquals(h1, h2);
	}

	@Test
	public void host_notEquals() {
		Host h1 = Host.build("hostname", 1, 2, 3);

		Host h2 = Host.build("2 3 1 hostname1");
		assertNotEquals(h1, h2);

		h2 = Host.build("2 3 4 hostname");
		assertNotEquals(h1, h2);
	}
}
