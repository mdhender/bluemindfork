/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.system.service;

import java.util.Map;

import net.bluemind.system.api.ConnectionTestStatus;
import net.bluemind.system.api.ExternalSystem;
import net.bluemind.user.api.UserAccount;

public abstract class RegisteredExternalSystem extends ExternalSystem {

	public RegisteredExternalSystem(String identifier, String description, AuthKind authKind) {
		super(identifier, description, authKind);
	}

	public RegisteredExternalSystem(String identifier, String description, AuthKind authKind,
			Map<String, String> properties) {
		super(identifier, description, authKind, properties);
	}

	public abstract byte[] getLogo();

	public abstract boolean handles(String userAccountIdentifier);

	public ConnectionTestStatus testConnection(String domain, UserAccount account) {
		return ConnectionTestStatus.NOT_SUPPORTED;
	}

}
