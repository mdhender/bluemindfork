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
package net.bluemind.user.api;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class ChangePassword {
	/**
	 * current password
	 * <p>
	 * if null, you need admin right to set new password
	 */
	public String currentPassword;

	public String newPassword;

	public static ChangePassword create(String newValue) {
		ChangePassword p = new ChangePassword();
		p.newPassword = newValue;
		return p;
	}

	public static ChangePassword create(String oldValue, String newValue) {
		ChangePassword p = new ChangePassword();
		p.currentPassword = oldValue;
		p.newPassword = newValue;
		return p;
	}
}
