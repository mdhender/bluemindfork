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
package net.bluemind.core.sanitizer.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Arrays;

import org.junit.Test;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.sanitizer.Sanitizer;
import net.bluemind.core.tests.BmTestContext;

public class SanitizerTests {
	@Test
	public void sanitizeCreateGarbage() {
		Called u = new Called();
		u.name = "test";

		Sanitizer s = new Sanitizer(getTestBmContext());
		try {
			s.create(u);
		} catch (ServerFault e) {
			fail();
		}
		assertEquals("sanitized-test context-test", u.name);
	}

	/**
	 * @return
	 */
	private BmContext getTestBmContext() {
		return new BmTestContext(
				new SecurityContext("test", "test", Arrays.<String> asList(), Arrays.<String> asList(), null));
	}

	@Test
	public void sanitizeUpdateGarbage() {
		Called previous = new Called();
		previous.name = "previous";

		Called u = new Called();
		u.name = "test";

		Sanitizer s = new Sanitizer(getTestBmContext());
		try {
			s.update(previous, u);
		} catch (ServerFault e) {
			fail();
		}
		assertEquals("sanitized-test-previous context-test", u.name);
	}
}
