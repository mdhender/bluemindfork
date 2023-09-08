/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.filehosting.filesystem.service.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import java.security.SecureRandom;

import org.junit.Test;

import net.bluemind.filehosting.api.ID;

public class IDTest {

	@Test
	public void testIDGeneration() {

		for (int i = 0; i < 10; i++) {
			String id = ID.generate();
			assertTrue(ID.isUUID(id));
		}

		SecureRandom random = new SecureRandom();
		for (int i = 0; i < 10; i++) {
			String id = new BigInteger(130, random).toString(39);
			assertFalse(ID.isUUID(id));
		}

	}

	@Test
	public void testIDExtraction() {

		String id = ID.generate();
		String url = String.format("http://localhost:8090/api/filehosting/%s/_complete", id);

		assertEquals(id, ID.extract(url));

	}

}
