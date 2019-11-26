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

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.user.api.User;

public class UserValidatorTest {

	// LOGIN("^[a-z0-9][a-z0-9-._]{0,63}$"),

	@Test
	public void testCorrectLogins() throws ServerFault {
		UserValidator validator = new UserValidator();
		User user = new User();

		String l1 = "test";
		String l2 = "01test";
		String l3 = "test-test";
		String l4 = "test-.-__test";
		String l5 = "0-.-__test1";

		user.login = l1;
		validator.validateLogin(user);
		user.login = l2;
		validator.validateLogin(user);
		user.login = l3;
		validator.validateLogin(user);
		user.login = l4;
		validator.validateLogin(user);
		user.login = l5;
		validator.validateLogin(user);
	}

	@Test
	public void testInCorrectLogins() throws ServerFault {
		String l1 = "-test";
		String l2 = ".test";
		String l3 = "_test";
		String l4 = "Test";
		String l5 = "teSt";
		String l6 = "te%st";
		String l7 = "te#st";

		testFail(l1);
		testFail(l2);
		testFail(l3);
		testFail(l4);
		testFail(l5);
		testFail(l6);
		testFail(l7);
	}

	private void testFail(String login) {
		UserValidator validator = new UserValidator();
		try {
			User user = new User();
			user.login = login;
			validator.validateLogin(user);
			fail();
		} catch (Exception e) {
		}
	}
}
