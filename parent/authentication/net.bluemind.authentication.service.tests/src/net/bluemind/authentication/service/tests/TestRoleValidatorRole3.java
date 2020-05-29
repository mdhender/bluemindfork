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
package net.bluemind.authentication.service.tests;

import java.util.Arrays;
import java.util.List;

import net.bluemind.authentication.service.IRoleValidator;

public class TestRoleValidatorRole3 implements IRoleValidator {

	public TestRoleValidatorRole3() {
	}

	@Override
	public boolean valid(String domain, String role) {
		return domain.startsWith("2-");
	}

	@Override
	public List<String> supportedRoles() {
		return Arrays.asList("ju-role3");
	}

}
