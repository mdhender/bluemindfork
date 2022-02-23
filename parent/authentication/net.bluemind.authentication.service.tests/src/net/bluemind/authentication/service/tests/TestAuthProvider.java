/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.authentication.service.tests;

import java.util.concurrent.atomic.AtomicBoolean;

import net.bluemind.authentication.provider.IAuthProvider;
import net.bluemind.core.api.fault.ServerFault;

public class TestAuthProvider implements IAuthProvider {
	public static final AtomicBoolean passed = new AtomicBoolean(false);

	@Override
	public int priority() {
		return 0;
	}

	@Override
	public AuthResult check(IAuthContext authContext) throws ServerFault {
		passed.set(true);
		return AuthResult.UNKNOWN;
	}

}
