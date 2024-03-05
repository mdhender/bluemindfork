/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.user.persistence.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import net.bluemind.user.persistence.security.HashAlgorithm;
import net.bluemind.user.persistence.security.HashFactory;
import net.bluemind.user.persistence.security.PBKDF2SHA256Hash;

public class PBKDF2SHA256Tests {

	@Test
	public void testGenerate() {
		int total = 0;
		for (int i = 0; i < 20; i++) {
			long ts = System.currentTimeMillis();
			String hash = HashFactory.get(HashAlgorithm.PBKDF2SHA256).create("this is password");
			long done = System.currentTimeMillis() - ts;
			total += done;
			assertEquals(HashAlgorithm.valueOf("PBKDF2SHA256"), HashFactory.algorithm(hash));
		}

		System.err.println(
				PBKDF2SHA256Hash.iterations() + " iterations, 20 runs. Generate average time: " + (total / 20) + " ms");
	}

	@Test
	public void testValidate() {
		int total = 0;
		String hash = HashFactory.get(HashAlgorithm.PBKDF2SHA256).create("this is password");

		for (int i = 0; i < 200; i++) {
			long ts = System.currentTimeMillis();
			HashFactory.get(HashAlgorithm.PBKDF2SHA256).validate("this is password" + i, hash);
			long done = System.currentTimeMillis() - ts;
			total += done;
			// System.err.println(PBKDF2Hash.iterations() + " iterations.
			// Hash validated in " + done + " ms");
		}

		System.err.println(PBKDF2SHA256Hash.iterations() + " iterations, 200 runs. Validate average time: "
				+ (total / 200) + " ms");
	}
}
