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

import net.bluemind.user.persistence.security.HashAlgorithm;
import net.bluemind.user.persistence.security.SSHA512Hash;
import net.bluemind.user.persistence.security.HashFactory;

public class SSHA512Test {
	@Test
	public void testNoMatch() {
		String h = "{SHA512}invalid";
		assertFalse(HashFactory.get(HashAlgorithm.SSHA512).matchesAlgorithm(h));
	}

	@Test
	public void testMatch() {
		String h = "{SSHA512}invalid";
		assertTrue(HashFactory.get(HashAlgorithm.SSHA512).matchesAlgorithm(h));
	}

	@Test
	public void testCreate() {
		byte[] salt = {49,50,51,52,53,54,55,56};
		String hashed = new SSHA512Hash().create("azerty", salt);
		assertTrue(hashed.startsWith("{SSHA512}"));
		assertEquals(hashed, "{SSHA512}jpKr28kIZS8Co3aUodLOzvSbL5EGqPvr6m7DpICv9TCatJZV8BwAaOTRkJSndNUhaTvTPALhx5qT/S4I+GgJZTEyMzQ1Njc4");
	}

	@Test
	public void testValidate() {
		SSHA512Hash h = new SSHA512Hash();
		String hashed = h.create("azerty");
		assertTrue(h.validate("azerty", hashed));
	}

	@Test
	public void testValidateBadHash() {
		SSHA512Hash h = new SSHA512Hash();
		assertFalse(h.validate("azerty", ""));
		assertFalse(h.validate("azerty", "{SSHA512}"));
	}
}
