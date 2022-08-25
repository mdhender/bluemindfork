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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.user.service.internal;

public class PasswordInfo {

	public final boolean passwordOk;
	public final boolean passwordUpdateNeeded;
	public final String userUid;

	public PasswordInfo(boolean passwordOk, boolean passwordUpdateNeeded, String userUid) {
		this.passwordOk = passwordOk;
		this.passwordUpdateNeeded = passwordUpdateNeeded;
		this.userUid = userUid;
	}

}
