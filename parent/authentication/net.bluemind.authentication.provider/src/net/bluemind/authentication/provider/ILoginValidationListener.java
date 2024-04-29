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

package net.bluemind.authentication.provider;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.user.api.User;

public interface ILoginValidationListener {

	default void onValidLogin(IAuthProvider provider, boolean userExists, String login, String domain,
			String password) {
	}

	default void onFailedLogin(IAuthProvider provider, boolean userExists, String login, String domain,
			String password) {
	}

	/**
	 * Called by
	 * {@link net.bluemind.authentication.service.Authentication#findOrGetUser()} if
	 * user not found in database.
	 * 
	 * Return freshly created user if found and created by plugin
	 * 
	 * @param domain
	 * @param login
	 * @return freshly create user or null
	 */
	default ItemValue<User> onSu(ItemValue<Domain> domain, String login) {
		return null;
	}
}
