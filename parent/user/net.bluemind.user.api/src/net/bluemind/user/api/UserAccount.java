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
package net.bluemind.user.api;

import java.util.Collections;
import java.util.Map;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class UserAccount {

	@NotNull
	@Size(min = 1, max = 255)
	public String login;

	public String credentials;

	public Map<String, String> additionalSettings = Collections.emptyMap();

	public UserAccount() {

	}

	public UserAccount(UserAccount account) {
		this.login = account.login;
		this.credentials = account.credentials;
		this.additionalSettings = account.additionalSettings;
	}

	public UserAccount(String login) {
		this.login = login;
	}

}
