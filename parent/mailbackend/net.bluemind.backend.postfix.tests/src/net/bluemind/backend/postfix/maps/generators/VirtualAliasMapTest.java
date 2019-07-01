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
package net.bluemind.backend.postfix.maps.generators;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import net.bluemind.backend.postfix.internal.maps.MapRow;
import net.bluemind.backend.postfix.internal.maps.generators.VirtualAliasMap;
import net.bluemind.mailbox.api.Mailbox.Type;

public class VirtualAliasMapTest {
	@Test
	public void generateMap_emptyMapRow() {
		String map = new VirtualAliasMap(Collections.emptyList()).generateMap();
		assertEquals("", map);
	}

	@Test
	public void generateMap_noEmail() {
		MapRow mapRow = new MapRow(null, 0, null, null, null, null, null, null);

		String map = new VirtualAliasMap(Arrays.asList(mapRow)).generateMap();
		assertEquals("", map);
	}

	@Test
	public void generateMap_withEmail_nullRecipient() {
		MapRow mapRow = new MapRow(null, 0, null, null, null, null, null, null);
		mapRow.emails.add("test@domain.tld");

		String map = new VirtualAliasMap(Arrays.asList(mapRow)).generateMap();
		assertEquals("", map);
	}

	@Test
	public void generateMap_withEmail_emptyRecipient() {
		MapRow mapRow = new MapRow(null, 0, null, Type.user, null, null, "", null);
		mapRow.emails.add("test@domain.tld");
		mapRow.expandRecipients(Collections.emptyMap());

		String map = new VirtualAliasMap(Arrays.asList(mapRow)).generateMap();
		assertEquals("", map);
	}

	@Test
	public void generateMap_validRow() {
		MapRow mapRow = new MapRow(null, 0, null, Type.user, null, null, "mailboxname@domain.tld", null);
		mapRow.emails.add("test.alias1@domain.tld");
		mapRow.emails.add("test.alias2@domain.tld");
		mapRow.expandRecipients(Collections.emptyMap());

		String map = new VirtualAliasMap(Arrays.asList(mapRow)).generateMap();
		assertTrue(map.contains("test.alias1@domain.tld mailboxname@domain.tld\n"));
		assertTrue(map.contains("test.alias2@domain.tld mailboxname@domain.tld\n"));
		assertEquals(2, map.split("\n").length);
	}
}
