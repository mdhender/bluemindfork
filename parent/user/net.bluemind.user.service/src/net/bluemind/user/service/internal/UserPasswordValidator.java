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
package net.bluemind.user.service.internal;

import java.util.Optional;

import com.google.common.base.CharMatcher;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.user.hook.passwordvalidator.IPasswordValidator;

public class UserPasswordValidator implements IPasswordValidator {
	@Override
	public void validate(Optional<String> currentPassword, String password) throws ServerFault {
		// Create user without password
		if (password == null) {
			return;
		}

		if (password.trim().isEmpty()) {
			throw new ServerFault("Password must not be empty", ErrorCode.INVALID_PARAMETER);
		}

		if (!CharMatcher.ascii().matchesAllOf(password)) {
			throw new ServerFault("Invalid character in password", ErrorCode.INVALID_PARAMETER);
		}

		if (currentPassword.isPresent() && password.equals(currentPassword.get())) {
			throw new ServerFault("Current and new password must not be the same", ErrorCode.INVALID_PARAMETER);
		}
	}

}
