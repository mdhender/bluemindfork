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
package net.bluemind.user.service.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Optional;

import org.junit.Test;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;

public class UserPasswordValidatorTest {
	@Test
	public void validPassword() {
		UserPasswordValidator validator = new UserPasswordValidator();

		validator.validate(Optional.empty(), null);
		validator.validate(Optional.empty(), "test");
		validator.validate(Optional.empty(), "aA45-!");
	}

	@Test
	public void invalidPassword() {
		testFail("   ");
		testFail("Aé_");
	}

	private void testFail(String password) {
		UserPasswordValidator validator = new UserPasswordValidator();

		try {
			validator.validate(Optional.empty(), password);
			fail();
		} catch (Exception e) {
		}
	}

	@Test
	public void mustNotBeSame() {
		UserPasswordValidator validator = new UserPasswordValidator();

		try {
			validator.validate(Optional.of("same"), "same");
			fail();
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}
	}
}
