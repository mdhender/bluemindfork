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
package net.bluemind.dav.server.proto;

public enum Depth {

	// Depth = "Depth" ":" ("0" | "1" | "1,noroot" | "infinity" |
	// "infinity,noroot")

	ZERO("0"), //
	ONE("1"), //
	ONE_NOROOT("1,noroot"), //
	INFINITY("infinity"), //
	INFINITY_NOROOT("infinity,noroot"); //

	private final String str;

	private Depth(String str) {
		this.str = str;
	}

	public String toString() {
		return str;
	}

	public static final Depth fromHeader(String s) {
		if (s == null) {
			return Depth.ZERO;
		}

		switch (s) {
		case "1":
			return Depth.ONE;
		case "1,noroot":
			return Depth.ONE_NOROOT;
		case "infinity":
			return Depth.INFINITY;
		case "infinity,noroot":
			return Depth.INFINITY_NOROOT;
		default:
		case "0":
			return Depth.ZERO;
		}
	}

}
