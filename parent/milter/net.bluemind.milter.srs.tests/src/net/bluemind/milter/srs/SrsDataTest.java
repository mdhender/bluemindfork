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
package net.bluemind.milter.srs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.junit.Test;

import net.bluemind.milter.srs.tools.SrsHash;
import net.bluemind.milter.srs.tools.SrsTimestamp;

public class SrsDataTest {
	public static final SrsHash SRSHASH = SrsHash.build("681c4953-8203-4eec-abbc-e40055ce640b").orElse(null);

	@Test
	public void fromEmail_invalidEmail() {
		assertFalse(SrsData.forEmail(SRSHASH, null).isPresent());
		assertFalse(SrsData.forEmail(SRSHASH, "").isPresent());
		assertFalse(SrsData.forEmail(SRSHASH, "invalid").isPresent());
		assertFalse(SrsData.forEmail(SRSHASH, "invalid@email").isPresent());
	}

	@Test
	public void fromEmail() {
		Optional<SrsData> srsData = SrsData.forEmail(SRSHASH, "john.doe@domain.tld");
		assertTrue(srsData.isPresent());

		SrsData sd = srsData.orElse(null);
		assertEquals("john.doe@domain.tld", sd.originalEmail());

		assertNotNull(sd.timestamp);
		assertEquals(2, sd.timestamp.length());

		assertNotNull(sd.hash);
		assertEquals(4, sd.hash.length());

		assertNotNull(sd.localPart);
		assertEquals("john.doe", sd.localPart);

		assertNotNull(sd.hostname);
		assertEquals("domain.tld", sd.hostname);

		assertTrue("SRS Email is: " + sd.srsEmail("bm.tld"),
				Pattern.matches("SRS0=.{4}=[A-Z2-7]{2}=domain.tld=john.doe@bm.tld", sd.srsEmail("bm.tld")));
	}

	@Test
	public void fromLeftPart_invalidLeftPart() {
		assertFalse(SrsData.fromLeftPart(SRSHASH, null).isPresent());
		assertFalse(SrsData.fromLeftPart(SRSHASH, "").isPresent());
		assertFalse(SrsData.fromLeftPart(SRSHASH, "INVALID=EEEE=EE=domain.tld=john.doe").isPresent());

		assertFalse(SrsData.fromLeftPart(SRSHASH, "SRS0=EEEE=EE=domain.tld").isPresent());

		// 30 days ago
		String expiredSrsTimestamp = SrsTimestamp
				.from((System.currentTimeMillis() / TimeUnit.SECONDS.toMillis(1)) - TimeUnit.DAYS.toSeconds(30));
		String expiredLeftPart = "SRS0=EEEE=" + expiredSrsTimestamp + "=domain.tld=john.doe";
		assertFalse(SrsData.fromLeftPart(SRSHASH, expiredLeftPart).isPresent());

		String invalidHashPart = "SRS0=EEEE="
				+ SrsTimestamp.from((System.currentTimeMillis() / TimeUnit.SECONDS.toMillis(1)))
				+ "=domain.tld=john.doe";
		assertFalse(SrsData.fromLeftPart(SRSHASH, invalidHashPart).isPresent());
	}

	@Test
	public void fromLeftPart() {
		String timeStamp = SrsTimestamp.from(System.currentTimeMillis() / TimeUnit.SECONDS.toMillis(1));
		String hash = SRSHASH.encode(timeStamp, "john.doe", "domain.tld");
		String leftPart = new StringBuilder().append("SRS0=").append(hash).append("=").append(timeStamp)
				.append("=domain.tld=john.doe").toString();

		Optional<SrsData> srsData = SrsData.fromLeftPart(SRSHASH, leftPart);
		assertTrue(srsData.isPresent());

		SrsData sd = srsData.orElse(null);
		assertEquals(hash, sd.hash);
		assertEquals(timeStamp, sd.timestamp);
		assertEquals("john.doe", sd.localPart);
		assertEquals("domain.tld", sd.hostname);
		assertEquals(leftPart + "@bm.tld", sd.srsEmail("bm.tld"));
		assertEquals("john.doe@domain.tld", sd.originalEmail());

		// Email with = in left part
		hash = SRSHASH.encode(timeStamp, "john=doe", "domain.tld");
		leftPart = new StringBuilder().append("SRS0=").append(hash).append("=").append(timeStamp)
				.append("=domain.tld=john=doe").toString();

		srsData = SrsData.fromLeftPart(SRSHASH, leftPart);
		assertTrue(srsData.isPresent());

		assertTrue(srsData.isPresent());

		sd = srsData.orElse(null);
		assertEquals(hash, sd.hash);
		assertEquals(timeStamp, sd.timestamp);
		assertEquals("john=doe", sd.localPart);
		assertEquals("domain.tld", sd.hostname);
		assertEquals(leftPart + "@bm.tld", sd.srsEmail("bm.tld"));
		assertEquals("john=doe@domain.tld", sd.originalEmail());
	}
}
