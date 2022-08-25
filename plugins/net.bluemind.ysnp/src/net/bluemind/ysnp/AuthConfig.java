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
package net.bluemind.ysnp;

public class AuthConfig {
	public final boolean expiredOk;
	public final boolean archivedOk;

	public AuthConfig(boolean expiredOk, boolean archivedOk) {
		this.expiredOk = expiredOk;
		this.archivedOk = archivedOk;
	}

	public static AuthConfig defaultConfig() {
		return new AuthConfig(false, false);
	}

	public static AuthConfig expiredOk() {
		return new AuthConfig(true, false);
	}

	public static AuthConfig archivedOk() {
		return new AuthConfig(false, true);
	}

}
