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

import net.bluemind.core.api.ParametersValidator;
import net.bluemind.core.api.Regex;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.validator.IValidator;
import net.bluemind.user.api.User;

public class UserValidator implements IValidator<User> {

	@Override
	public void create(User obj) throws ServerFault {
		validate(obj);
	}

	@Override
	public void update(User oldValue, User newValue) throws ServerFault {
		validate(newValue);
	}

	public void validate(User obj) throws ServerFault {
		ParametersValidator.notNull(obj);
		ParametersValidator.notNullAndNotEmpty(obj.login);
		ParametersValidator.notNull(obj.routing);
		if (!Regex.LOGIN.validate(obj.login)) {
			throw new ServerFault("Login is invalid", ErrorCode.INVALID_PARAMETER);
		}
	}
}
