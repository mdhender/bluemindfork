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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum AttendeeStatus {
	RESPONSE_UNKNOWN(0), // 0
	TENTATIVE(2), // 2
	ACCEPT(3), // 3
	DECLINE(4), // 4
	NOT_RESPONDED(5); // 5

	private static final Logger logger = LoggerFactory
			.getLogger(AttendeeStatus.class);
	private int intValue;

	private AttendeeStatus(int intValue) {
		this.intValue = intValue;
	}

	public static AttendeeStatus fromInt(int value) {
		switch (value) {
		case 2:
			return TENTATIVE;
		case 3:
			return ACCEPT;
		case 4:
			return DECLINE;
		case 5:
			return NOT_RESPONDED;
		case 0:
			return RESPONSE_UNKNOWN;
		default:
			logger.warn(value + " is an unknown value return null");
			return null;
		}
	}

	public String asIntString() {
		return Integer.toString(intValue);
	}
}
