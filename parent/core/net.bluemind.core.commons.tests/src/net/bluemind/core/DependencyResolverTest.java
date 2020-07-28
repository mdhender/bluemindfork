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
package net.bluemind.core;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.junit.Test;

import net.bluemind.core.utils.DependencyResolver;

public class DependencyResolverTest {

	@Test
	public void testDependencyResolution() {
		List<String> elements = Arrays.asList("object1", "object2", "object3", "object4", "object5", "object6");

		List<String> ret = DependencyResolver.sortByDependencies(elements, s -> s, s -> {
			switch (s) {
			case "object1":
				return new HashSet<>(Arrays.asList("object5", "object6"));
			case "object5":
				return new HashSet<>(Arrays.asList("object2"));
			case "object6":
				return new HashSet<>(Arrays.asList("object3"));
			case "object3":
				return new HashSet<>(Arrays.asList("object2", "object4"));
			}
			return new HashSet<>();
		});

		assertEquals("object2", ret.get(0));
		assertEquals("object4", ret.get(1));
		assertEquals("object3", ret.get(2));
		assertEquals("object6", ret.get(3));
		assertEquals("object5", ret.get(4));
		assertEquals("object1", ret.get(5));

	}

}
