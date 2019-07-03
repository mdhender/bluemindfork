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
package net.bluemind.backend.mail.replica.persistence.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import net.bluemind.backend.mail.replica.api.utils.UidRanges;
import net.bluemind.backend.mail.replica.api.utils.UidRanges.UidRange;

public class UidRangesTests {

	@Test
	public void testSingleDigit() {
		List<UidRange> ranges = UidRanges.from("142");
		System.out.println(ranges);
		assertEquals(1, ranges.size());
		UidRange range = ranges.get(0);
		assertEquals(range.lowBound, range.highBound);
		assertEquals(142, range.lowBound);
	}

	@Test
	public void testOneRange() {
		List<UidRange> ranges = UidRanges.from("12:42");
		System.out.println(ranges);
		assertEquals(1, ranges.size());
		UidRange range = ranges.get(0);
		assertEquals(12, range.lowBound);
		assertEquals(42, range.highBound);
	}

	@Test
	public void testUnbound() {
		List<UidRange> ranges = UidRanges.from("12:*");
		System.out.println(ranges);
		assertEquals(1, ranges.size());
		UidRange range = ranges.get(0);
		assertEquals(12, range.lowBound);
		assertEquals(Long.MAX_VALUE, range.highBound);
	}

	@Test
	public void testNone() {
		List<UidRange> ranges = UidRanges.from("");
		System.out.println(ranges);
		assertEquals(0, ranges.size());
	}

	@Test
	public void testTwoUids() {
		List<UidRange> ranges = UidRanges.from("12,42");
		System.out.println(ranges);
		assertEquals(2, ranges.size());
		UidRange range12 = ranges.get(0);
		UidRange range42 = ranges.get(1);
		assertEquals(12, range12.lowBound);
		assertEquals(42, range42.lowBound);
	}

	@Test
	public void testComplex() {
		List<UidRange> ranges = UidRanges.from("1,5,8:14,16:*");
		System.out.println(ranges);
		assertEquals(4, ranges.size());
		UidRange range = ranges.get(0);
		assertEquals(1, range.lowBound);
		UidRange range8To14 = ranges.get(2);
		assertEquals(8, range8To14.lowBound);
		assertEquals(14, range8To14.highBound);

		assertTrue(UidRanges.contains(ranges, 1));
		assertTrue(UidRanges.contains(ranges, 5));
		assertFalse(UidRanges.contains(ranges, 6));
		assertTrue(UidRanges.contains(ranges, 8));
		assertTrue(UidRanges.contains(ranges, 9));
		assertTrue(UidRanges.contains(ranges, 14));
		assertFalse(UidRanges.contains(ranges, 15));
		assertTrue(UidRanges.contains(ranges, 42));

	}

}
