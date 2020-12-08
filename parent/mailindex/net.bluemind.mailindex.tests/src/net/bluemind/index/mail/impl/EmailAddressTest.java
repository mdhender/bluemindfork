/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2020
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
package net.bluemind.index.mail.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import javax.mail.internet.AddressException;

import org.junit.Test;

import net.bluemind.utils.EmailAddress;

public class EmailAddressTest {

	@Test
	public void testEmailAddress() throws AddressException {
		String address = "david@bm.lan";
		EmailAddress emailAddress = new EmailAddress(address);
		assertNull(emailAddress.getPersonal());
		assertEquals(address, emailAddress.getAddress());
	}

	@Test
	public void testDisplayNameAndEmailAddress() throws AddressException {
		String address = "David Phan, yeah <david@bm.lan>";
		EmailAddress emailAddress = new EmailAddress(address);
		assertEquals("David Phan, yeah", emailAddress.getPersonal());
		assertEquals("david@bm.lan", emailAddress.getAddress());
	}

	@Test
	public void testQuotedDisplayNameAndEmailAddress() throws AddressException {
		String address = "\"David Phan, yeah\" <david@bm.lan>";
		EmailAddress emailAddress = new EmailAddress(address);
		assertEquals("David Phan, yeah", emailAddress.getPersonal());
		assertEquals("david@bm.lan", emailAddress.getAddress());
	}

	@Test
	public void testSingleQuotedDisplayNameAndEmailAddress() throws AddressException {
		String address = "'David Phan, yeah <david@bm.lan>'";
		EmailAddress emailAddress = new EmailAddress(address);
		assertEquals("David Phan, yeah", emailAddress.getPersonal());
		assertEquals("david@bm.lan", emailAddress.getAddress());
	}

	@Test
	public void testSingleQuotedEmailAddress() throws AddressException {
		String address = "'david@bm.lan'";
		EmailAddress emailAddress = new EmailAddress(address);
		assertNull(emailAddress.getPersonal());
		assertEquals("david@bm.lan", emailAddress.getAddress());
	}

}
