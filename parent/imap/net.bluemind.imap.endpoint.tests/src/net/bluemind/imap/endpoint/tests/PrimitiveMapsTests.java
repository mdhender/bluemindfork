/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.imap.endpoint.tests;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.Test;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;

public class PrimitiveMapsTests {

	int COUNT = 80_000;

	@Test
	public void testJdkMap() {
		Map<Long, Integer> map = new HashMap<>(2 * COUNT);
		fillMap(map);
		assertEquals(COUNT, map.size());
	}

	@Test
	public void testPrimitive() {
		Map<Long, Integer> map = new Long2IntOpenHashMap(2 * COUNT);
		fillMap(map);
		assertEquals(COUNT, map.size());
	}

	private void fillMap(Map<Long, Integer> map) {
		Random r = ThreadLocalRandom.current();
		long l = r.nextLong(1000);
		for (int i = 0; i < COUNT; i++) {
			l = l + 1 + r.nextInt(3);
			map.put(l, i);
		}
	}

}
