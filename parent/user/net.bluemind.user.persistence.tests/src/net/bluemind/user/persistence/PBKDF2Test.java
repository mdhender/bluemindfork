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
package net.bluemind.user.persistence;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import net.bluemind.user.persistence.security.HashAlgorithm;
import net.bluemind.user.persistence.security.HashFactory;
import net.bluemind.user.persistence.security.PBKDF2Hash;

public class PBKDF2Test {

	@Test
	public void testGenerate() {
		int total = 0;
		for (int i = 0; i < 20; i++) {
			long ts = System.currentTimeMillis();
			String hash = HashFactory.getDefault().create("this is password");
			long done = System.currentTimeMillis() - ts;
			total += done;
			// System.err.println(PBKDF2Hash.PBKDF2_ITERATIONS + " iterations.
			// Hash generated in " + done + " ms");
			assertEquals(HashAlgorithm.valueOf("PBKDF2"), HashFactory.algorithm(hash));
		}

		System.err.println(
				PBKDF2Hash.PBKDF2_ITERATIONS + " iterations, 20 runs. Generate average time: " + (total / 20) + " ms");
	}

	@Test
	public void testValidate() {
		int total = 0;
		String hash = HashFactory.getDefault().create("this is password");

		for (int i = 0; i < 200; i++) {
			long ts = System.currentTimeMillis();
			HashFactory.getDefault().validate("this is password", hash);
			long done = System.currentTimeMillis() - ts;
			total += done;
			// System.err.println(PBKDF2Hash.PBKDF2_ITERATIONS + " iterations.
			// Hash validated in " + done + " ms");
		}

		System.err.println(
				PBKDF2Hash.PBKDF2_ITERATIONS + " iterations, 200 runs. Validate average time: " + (total / 20) + " ms");
	}
}
