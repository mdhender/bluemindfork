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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.imap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import com.google.common.base.Joiner;

public class FlagsListTest {

	@Test
	public void testImportingExportingImporting() {
		Set<Flag> flags = new HashSet<>();
		flags.add(Flag.SEEN);
		flags.add(Flag.ANSWERED);
		flags.add(Flag.FLAGGED);

		FlagsList flagList = FlagsList.fromString(Joiner.on(" ").join(flags));
		assertEquals(3, flagList.size());

		Set<String> asTags = flagList.asTags();
		assertEquals(5, asTags.size());

		assertTrue(asTags.contains(Flag.SEEN.name().toLowerCase()));
		assertTrue(asTags.contains(Flag.ANSWERED.name().toLowerCase()));
		assertTrue(asTags.contains(Flag.FLAGGED.name().toLowerCase()));
		// check autoadded flags
		assertTrue(asTags.contains("read"));
		assertTrue(asTags.contains("starred"));
	}

	@Test
	public void testImportingSimpleFlags() {
		Set<String> flags = new HashSet<>();
		flags.add("seen");
		flags.add("draft");
		flags.add("flagged");

		FlagsList flagList = FlagsList.fromString(Joiner.on(" ").join(flags));
		assertEquals(3, flagList.size());

		Set<String> asTags = flagList.asTags();
		assertEquals(5, asTags.size());

		assertTrue(asTags.contains(Flag.SEEN.name().toLowerCase()));
		assertTrue(asTags.contains(Flag.DRAFT.name().toLowerCase()));
		assertTrue(asTags.contains(Flag.FLAGGED.name().toLowerCase()));
		// check autoadded flags
		assertTrue(asTags.contains("read"));
		assertTrue(asTags.contains("starred"));
	}

}
