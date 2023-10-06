package net.bluemind.milter.cache;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class DomainAliasCacheTest {

	@Test
	public void getDomainFromEmail() throws Exception {
		assertFalse(DomainAliasCache.getDomainFromEmail(null).isPresent());
		assertFalse(DomainAliasCache.getDomainFromEmail("").isPresent());
		assertFalse(DomainAliasCache.getDomainFromEmail("nodomain").isPresent());

		assertEquals("bm.lan", DomainAliasCache.getDomainFromEmail("john@bm.lan")
				.orElseThrow(() -> new Exception("Must found domain!")));
	}

	@Test
	public void getLeftPartFromEmail() throws Exception {
		assertFalse(DomainAliasCache.getLeftPartFromEmail(null).isPresent());
		assertFalse(DomainAliasCache.getLeftPartFromEmail("").isPresent());
		assertFalse(DomainAliasCache.getLeftPartFromEmail("@bm.lan").isPresent());

		assertEquals("john", DomainAliasCache.getLeftPartFromEmail("john@bm.lan")
				.orElseThrow(() -> new Exception("Must found left part!")));
	}

	@Test
	public void getDomain() throws Exception {
		assertNull(DomainAliasCache.getDomain(null));
	}

}
