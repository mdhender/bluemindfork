/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
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

import java.util.Arrays;
import java.util.Collection;

import net.bluemind.imap.impl.MessageSet;

public class MessageSetTests extends IMAPTestCase {

	private void testParse(Collection<Integer> data, String expectedSet, Collection<Integer> expectedCollection) {
		String set = MessageSet.asString(data);
		assertEquals(expectedSet, set);
		assertEquals(expectedCollection, MessageSet.asLongCollection(set, data.size()));
	}

	public void testParse1() {
		testParse(Arrays.asList(1, 2, 3, 8, 9, 10, 12), "1:3,8:10,12", Arrays.asList(1, 2, 3, 8, 9, 10, 12));
	}

	public void testParse2() {
		testParse(Arrays.asList(8, 2, 3, 4, 9, 10, 12, 13), "2:4,8:10,12:13", Arrays.asList(2, 3, 4, 8, 9, 10, 12, 13));
	}

	public void testParse3() {
		testParse(Arrays.asList(1, 2), "1:2", Arrays.asList(1, 2));
	}

	public void testParse4() {
		testParse(Arrays.asList(1), "1", Arrays.asList(1));
	}

}
