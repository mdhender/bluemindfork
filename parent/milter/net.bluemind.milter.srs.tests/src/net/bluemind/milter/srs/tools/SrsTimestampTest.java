/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.milter.srs.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class SrsTimestampTest {
	@Test
	public void from() {
		// 2021-12-20
		long timestamp = 1640027114;
		assertEquals("RF", SrsTimestamp.from(timestamp));
	}

	@Test
	public void check() {
		// 30 days ago
		String expiredSrsTimestamp = SrsTimestamp
				.from((System.currentTimeMillis() / TimeUnit.SECONDS.toMillis(1)) - TimeUnit.DAYS.toSeconds(30));
		assertFalse(SrsTimestamp.check(expiredSrsTimestamp));
		assertFalse(SrsTimestamp.check(expiredSrsTimestamp.toLowerCase()));

		// 11 days ago
		expiredSrsTimestamp = SrsTimestamp
				.from((System.currentTimeMillis() / TimeUnit.SECONDS.toMillis(1)) - TimeUnit.DAYS.toSeconds(11));
		assertFalse(SrsTimestamp.check(expiredSrsTimestamp));
		assertFalse(SrsTimestamp.check(expiredSrsTimestamp.toLowerCase()));

		// 10 days ago
		String validSrsTimestamp = SrsTimestamp
				.from((System.currentTimeMillis() / TimeUnit.SECONDS.toMillis(1)) - TimeUnit.DAYS.toSeconds(10));
		assertTrue(SrsTimestamp.check(validSrsTimestamp));
		assertTrue(SrsTimestamp.check(validSrsTimestamp.toLowerCase()));

		// 5 days ago
		validSrsTimestamp = SrsTimestamp
				.from((System.currentTimeMillis() / TimeUnit.SECONDS.toMillis(1)) - TimeUnit.DAYS.toSeconds(5));
		assertTrue(SrsTimestamp.check(validSrsTimestamp));
		assertTrue(SrsTimestamp.check(validSrsTimestamp.toLowerCase()));

		// today
		validSrsTimestamp = SrsTimestamp.from((System.currentTimeMillis() / TimeUnit.SECONDS.toMillis(1)));
		assertTrue(SrsTimestamp.check(validSrsTimestamp));
		assertTrue(SrsTimestamp.check(validSrsTimestamp.toLowerCase()));
	}

	@Test
	public void check_invalidValues() {
		assertFalse(SrsTimestamp.check(null));
		assertFalse(SrsTimestamp.check(""));
		assertFalse(SrsTimestamp.check("INVALID"));
		assertFalse(SrsTimestamp.check("A9"));
		assertFalse(SrsTimestamp.check("A1"));
		assertFalse(SrsTimestamp.check("99"));
	}
}
