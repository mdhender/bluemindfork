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
package net.bluemind.xivo.common;

import java.util.Arrays;

public enum PhoneStatus {

	ONHOLD(16), //
	RINGING(8), //
	UNAVAILABLE(4), //
	BUSY_AND_RINGING(9), //
	AVAILABLE(0), //
	CALLING(1), //
	BUSY(2), //
	DEACTIVATED(-1), //
	UNEXISTING(-2), //
	ERROR(-99); //

	private final int code;

	private PhoneStatus(int numStatus) {
		this.code = numStatus;
	}

	public int code() {
		return code;
	}

	public static PhoneStatus fromCode(int code) {
		return Arrays.stream(values()).filter(v -> code == v.code).findFirst().orElse(PhoneStatus.UNEXISTING);

	}

}
