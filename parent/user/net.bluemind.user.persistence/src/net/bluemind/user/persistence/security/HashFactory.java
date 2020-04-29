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
	public static HashAlgorithm DEFAULT = HashAlgorithm.PBKDF2;

	public static Hash getDefault() {
		return get(DEFAULT);
	}

	public static Hash get(HashAlgorithm h) {
		switch (h) {
		case MD5:
			return new MD5Hash();
		case SSHA512:
			return new SSHA512Hash();
		case SSHA:
			return new SSHAHash();
		case SHA1:
			return new SHA1Hash();
		case PBKDF2:
			return new PBKDF2Hash();
		default:
			throw new IllegalArgumentException(String.format("Algorithm is not supported"));
		}
	}

	public static HashAlgorithm algorithm(String password) {
		if (new PBKDF2Hash().matchesAlgorithm(password)) {
			return HashAlgorithm.PBKDF2;
		} else if (new SSHA512Hash().matchesAlgorithm(password)) {
			return HashAlgorithm.SSHA512;
		} else if (new SSHAHash().matchesAlgorithm(password)) {
			return HashAlgorithm.SSHA;
		} else if (new SHA1Hash().matchesAlgorithm(password)) {
			return HashAlgorithm.SHA1;
		} else if (new MD5Hash().matchesAlgorithm(password)) {
			return HashAlgorithm.MD5;
		}
		return HashAlgorithm.UNKNOWN;
	}

	public static Hash getByPassword(String password) {
		return get(algorithm(password));
	}

	public static boolean usesDefaultAlgorithm(String password) {
		return algorithm(password) == DEFAULT;
	}
}
