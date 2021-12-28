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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SrsHashTest {
	public static final String key = "681c4953-8203-4eec-abbc-e40055ce640b";

	@Test
	public void encode() {
		SrsHash srsHash = SrsHash.build(key).orElse(null);
		assertNotNull(srsHash);

		assertEquals("YYqk", srsHash.encode("RG", "toto", "domain.tld"));
	}

	@Test
	public void check() {
		SrsHash srsHash = SrsHash.build(key).orElse(null);
		assertNotNull(srsHash);

		assertTrue(srsHash.check("YYqk", "RG", "toto", "domain.tld"));
		assertTrue(srsHash.check("yyqk", "RG", "toto", "domain.tld"));

		assertFalse(srsHash.check(null, "RG", "toto", "domain.tld"));
		assertFalse(srsHash.check("", "RG", "toto", "domain.tld"));
		assertFalse(srsHash.check("inva", "RG", "toto", "domain.tld"));
		assertFalse(srsHash.check("yyqka", "RG", "toto", "domain.tld"));
	}

	@Test
	public void invalidKey() {
		assertFalse(SrsHash.build(null).isPresent());
		assertFalse(SrsHash.build("").isPresent());
	}
}
