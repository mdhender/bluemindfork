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
package net.bluemind.user.persistence.security;

public class HashFactory {

	public static Hash getDefault() {
		return new PBKDF2Hash();
	}

	public static Hash getByName(String name) {
		switch (name) {
		case "MD5":
			return new MD5Hash();
		case "PBKDF2":
			return new PBKDF2Hash();
		default:
			throw new IllegalArgumentException(String.format("Algorithm is not supported"));
		}
	}

	public static Hash getByPassword(String password) {
		return getByName(algorithm(password));
	}

	public static String algorithm(String password) {
		if (new PBKDF2Hash().matchesAlgorithm(password)) {
			return "PBKDF2";
		} else if (new MD5Hash().matchesAlgorithm(password)) {
			return "MD5";
		}
		return "unknown";
	}

	public static String getDefaultName() {
		return "PBKDF2";
	}

	public static boolean usesDefaultAlgorithm(String password) {
		return algorithm(password).equals(getDefaultName());
	}
}
