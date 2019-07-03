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
package net.bluemind.core.validator.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Arrays;

import org.junit.Test;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.core.validator.Validator;

public class ValidatorTests {
	@Test
	public void validateCreate() {
		Called u = new Called();
		u.name = "test";

		Validator s = new Validator(getTestBmContext());
		try {
			s.create(u);
		} catch (ServerFault e) {
			fail();
		}

		u.name = null;
		try {
			s.create(u);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals("null", sf.getMessage());
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}
	}

	/**
	 * @return
	 */
	private BmContext getTestBmContext() {
		return new BmTestContext(
				new SecurityContext("test", "test", Arrays.<String> asList(), Arrays.<String> asList(), null));
	}

	@Test
	public void validateUpdateGarbage() {
		Called previous = new Called();
		previous.name = "previous";

		Called u = new Called();
		u.name = "previous";

		Validator s = new Validator(getTestBmContext());
		try {
			s.update(previous, u);
		} catch (ServerFault e) {
			fail();
		}

		u.name = "updated";
		try {
			s.update(previous, u);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals("name must not change", sf.getMessage());
		}
	}
}
