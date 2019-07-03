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
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.backend.cyrus.replication.server.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.regex.Matcher;

import org.junit.Test;

import net.bluemind.backend.cyrus.replication.server.utils.Patterns;

public class PatternsTests {

	@Test
	public void testMessageHeader() {
		String header = " %{ex2016_vmw deadbeef 1234}";
		Matcher matcher = Patterns.APPLY_MSG_HEADER.matcher(header);
		assertTrue(header + " does not match " + Patterns.APPLY_MSG_HEADER, matcher.matches());
		assertEquals("ex2016_vmw", matcher.group(1));
		assertEquals("deadbeef", matcher.group(2));
		assertEquals("1234", matcher.group(3));
	}

}
