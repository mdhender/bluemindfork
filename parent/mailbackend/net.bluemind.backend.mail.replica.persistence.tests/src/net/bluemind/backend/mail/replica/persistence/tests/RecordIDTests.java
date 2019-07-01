/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2019
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
package net.bluemind.backend.mail.replica.persistence.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

import net.bluemind.backend.mail.replica.persistence.RecordID;

public class RecordIDTests {

	@Test
	public void testEquals() {
		RecordID rid1 = new RecordID(1, 2);
		RecordID rid2 = new RecordID(1, 2);
		assertEquals(rid1, rid2);
		assertEquals(rid1, rid1);
		RecordID rid3 = new RecordID(1, 3);
		assertNotEquals(rid1, rid3);
		RecordID rid4 = new RecordID(2, 2);
		assertNotEquals(rid2, rid4);
		assertNotEquals(rid3, new Object());
		assertNotEquals(rid2, rid4);

	}

}
