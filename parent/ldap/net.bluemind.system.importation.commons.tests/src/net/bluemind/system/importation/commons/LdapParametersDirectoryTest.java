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

import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.junit.Test;

import net.bluemind.system.importation.commons.Parameters.Directory;
import net.bluemind.system.importation.commons.exceptions.InvalidDnServerFault;

public class LdapParametersDirectoryTest {
	@Test
	public void directory_Instance() throws LdapInvalidDnException {
		Directory directory = Directory.build("cn=local", "userfilter", "groupfilter", "extidattr");

		assertEquals("cn=local", directory.baseDn.getName());
		assertEquals("userfilter", directory.userFilter);
		assertEquals("groupfilter", directory.groupFilter);
		assertEquals("extidattr", directory.extIdAttribute);
	}

	@Test
	public void directory_equals() throws LdapInvalidDnException {
		Directory directory1 = Directory.build("cn=local", "userfilter", "groupfilter", "extidattr");
		Directory directory2 = Directory.build("cn=local", "userfilter", "groupfilter", "extidattr");

		assertEquals(directory1, directory2);
	}

	@Test
	public void directory_notEquals() throws LdapInvalidDnException {
		Directory directory1 = Directory.build("cn=local", "userfilter1", "groupfilter1", "extidattr1");

		Directory directory2 = Directory.build("dc=local", "userfilter1", "groupfilter1", "extidattr1");
		assertNotEquals(directory1, directory2);

		directory2 = Directory.build("cn=local", "userfilter2", "groupfilter1", "extidattr1");
		assertNotEquals(directory1, directory2);

		directory2 = Directory.build("cn=local", "userfilter1", "groupfilter2", "extidattr1");
		assertNotEquals(directory1, directory2);

		directory2 = Directory.build("cn=local", "userfilter1", "groupfilter1", "extidattr2");
		assertNotEquals(directory1, directory2);
	}

	@Test
	public void directory_invalidDn() {
		try {
			Directory.build("invalid dn", "userfilter1", "groupfilter1", "extidattr1");
			fail("Test must thrown an exception");
		} catch (InvalidDnServerFault lide) {
		}
	}
}
