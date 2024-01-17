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
package net.bluemind.eas.dto.settings;

public enum OofState {

	DISABLED(0),

	GLOBAL(1),

	TIME_BASED(2);

	private final String xmlValue;

	private OofState(int intValue) {
		this.xmlValue = Integer.toString(intValue);
	}

	public String xmlValue() {
		return xmlValue;
	}

	public static OofState fromXml(String s) {
		switch (s) {
		case "0":
			return OofState.DISABLED;
		case "1":
			return OofState.GLOBAL;
		case "2":
			return TIME_BASED;
		default:
			return null;
		}
	}

}
