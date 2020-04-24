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
import static org.junit.Assert.fail;

import org.junit.Test;

import net.bluemind.user.persistence.security.SHA1Hash;
import net.bluemind.user.persistence.security.HashFactory;
import net.bluemind.core.api.fault.ServerFault;

public class SHA1Test {
	@Test
	public void testNoMatch() {
		String h = "{SHA256}invalid";
		assertFalse(HashFactory.getByName("SHA1").matchesAlgorithm(h));
	}

	@Test
	public void testMatch() {
		assertTrue(HashFactory.getByName("SHA1").matchesAlgorithm("{SHA}valid"));
		assertTrue(HashFactory.getByName("SHA1").matchesAlgorithm("{SHA1}valid"));
	}

	@Test
	public void testCreateMustFail() {
		try {
			HashFactory.getByName("SHA1").create("XXX");
			fail();
		} catch (ServerFault e) {
		}
	}

	@Test
	public void testValidateSHA() {
		SHA1Hash h = new SHA1Hash();
		/* Check if the performer name matches the hash */
		assertTrue(h.validate("Wvyy Xnffvql", "{SHA}56b6vcI6nhMRGGgA+duAyzuSQ8M="));
		assertTrue(h.validate("Wvyy Xnffvql", "{SHA1}56b6vcI6nhMRGGgA+duAyzuSQ8M="));
	}
}
