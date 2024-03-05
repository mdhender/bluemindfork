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
	/*
	 * https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.
	 * html#password-hashing-algorithms
	 * 
	 * https://tobtu.com/minimum-password-settings/
	 * 
	 * When running tests on decent machine in 2024:
	 * 
	 * Argon2 gen: 37ms / validate: 31ms
	 * 
	 * bcrypt gen: 45ms / validate: 45ms
	 * 
	 * pbkdf2sha256 600k 94ms / 90ms
	 * 
	 * pbkdf2sha1 10k 5ms / 3ms
	 */

	public static HashAlgorithm DEFAULT = HashAlgorithm.ARGON2;

	public static Hash getDefault() {
		return get(DEFAULT);
	}

	public static Hash get(HashAlgorithm h) {
		switch (h) {
		case BCRYPT:
			return new BCryptHash();
		case ARGON2:
			return new Argon2Hash();
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
		case PBKDF2SHA256:
			return new PBKDF2SHA256Hash();
		default:
			throw new IllegalArgumentException(String.format("Algorithm is not supported"));
		}
	}

	public static HashAlgorithm algorithm(String password) {
		if (new PBKDF2Hash().matchesAlgorithm(password)) {
			return HashAlgorithm.PBKDF2;
		} else if (new Argon2Hash().matchesAlgorithm(password)) {
			return HashAlgorithm.ARGON2;
		} else if (new BCryptHash().matchesAlgorithm(password)) {
			return HashAlgorithm.BCRYPT;
		} else if (new PBKDF2SHA256Hash().matchesAlgorithm(password)) {
			return HashAlgorithm.PBKDF2SHA256;
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

	public static boolean needsUpgrade(String password) {
		HashAlgorithm algo = algorithm(password);
		if (algo != DEFAULT) {
			return true;
		} else {
			Hash h = get(algo);
			return h.needsUpgrade(password);
		}
	}
}
