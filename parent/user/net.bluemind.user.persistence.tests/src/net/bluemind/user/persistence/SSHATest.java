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
package net.bluemind.user.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

import net.bluemind.user.persistence.security.SSHAHash;
import net.bluemind.user.persistence.security.HashFactory;

public class SSHATest {
	@Test
	public void testNoMatch() {
		String h = "{SHA}invalid";
		assertFalse(HashFactory.getByName("SSHA").matchesAlgorithm(h));
	}

	@Test
	public void testMatch() {
		String h = "{SSHA}invalid";
		assertTrue(HashFactory.getByName("SSHA").matchesAlgorithm(h));
	}

	@Test
	public void testCreate() {
		byte[] salt = {49,50,51,52,53,54,55,56};
		String hashed = new SSHAHash().create("azerty", salt);
		assertTrue(hashed.startsWith("{SSHA}"));
		assertEquals(hashed, "{SSHA}GxTb7Ris2d1iiu2oUgecQ9JbOnYxMjM0NTY3OA==");
	}

	@Test
	public void testValidate() {
		SSHAHash h = new SSHAHash();
		String hashed = h.create("azerty");
		assertTrue(h.validate("azerty", hashed));
	}

	@Test
	public void testValidateBadHash() {
		SSHAHash h = new SSHAHash();
		assertFalse(h.validate("azerty", ""));
		assertFalse(h.validate("azerty", "{SSHA}"));
	}
}
