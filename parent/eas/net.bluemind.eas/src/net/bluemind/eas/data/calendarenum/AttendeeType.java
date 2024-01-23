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
package net.bluemind.eas.data.calendarenum;

public enum AttendeeType {
	REQUIRED, // 1
	OPTIONAL, // 2
	RESOURCE; // 3

	public String asIntString() {
		switch (this) {
		case OPTIONAL:
			return "2";
		case RESOURCE:
			return "3";
		default:
		case REQUIRED:
			return "1";
		}

	}
}
