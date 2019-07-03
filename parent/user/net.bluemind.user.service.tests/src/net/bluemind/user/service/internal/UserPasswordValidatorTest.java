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

import static org.junit.Assert.fail;

import org.junit.Test;

public class UserPasswordValidatorTest {
	@Test
	public void validPassword() {
		UserPasswordValidator validator = new UserPasswordValidator();

		validator.validate(null);
		validator.validate("test");
		validator.validate("aA45-!");
	}

	@Test
	public void invalidPassword() {
		testFail("   ");
		testFail("Aé_");
	}

	private void testFail(String password) {
		UserPasswordValidator validator = new UserPasswordValidator();

		try {
			validator.validate(password);
			fail();
		} catch (Exception e) {
		}
	}
}
