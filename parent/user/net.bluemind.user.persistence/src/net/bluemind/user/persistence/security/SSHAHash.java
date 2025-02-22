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
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */

/*
 * Hash format: {IDENTIFIER}base64(hash(plaintext + salt))salt with 8 bytes random salt
 */

package net.bluemind.user.persistence.security;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

import net.bluemind.core.api.fault.ServerFault;

public class SSHAHash implements Hash {
	private static final String IDENTIFIER = "{SSHA}";
	private static final String ALGORITHM = "SHA1";
	public static final int SALT_BYTE_SIZE = 8;

	@Override
	public String create(String plaintext) throws ServerFault {
		return create(plaintext, generateSalt(SALT_BYTE_SIZE));
	}

	public String create(String plaintext, byte[] salt) throws ServerFault {
		MessageDigest md;
		try {
			md = MessageDigest.getInstance(ALGORITHM);
		} catch (NoSuchAlgorithmException e) {
			throw new ServerFault(e);
		}
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			md.update(plaintext.getBytes("UTF-8"));
			md.update(salt);
			baos.write(md.digest());
			baos.write(salt);
			return IDENTIFIER + new String(Base64.getEncoder().encode(baos.toByteArray()));
		} catch (IOException e) {
			throw new ServerFault(e);
		}
	}

	@Override
	public boolean validate(String plaintext, String hash) throws ServerFault {
		if (hash.length() < IDENTIFIER.length()) {
			return false;
		}
		byte[] buf = Base64.getDecoder().decode(hash.substring(IDENTIFIER.length()));
		int saltsize = buf.length < 28 ? 4 : SALT_BYTE_SIZE;
		if (buf.length < saltsize) {
			return false;
		}
		byte[] salt = Arrays.copyOfRange(buf, buf.length - saltsize, buf.length);
		return create(plaintext, salt).equals(hash);
	}

	@Override
	public boolean matchesAlgorithm(String password) {
		return password.startsWith(IDENTIFIER);
	}

	private byte[] generateSalt(int size) {
		SecureRandom random = new SecureRandom();
		byte[] salt = new byte[size];
		random.nextBytes(salt);
		return salt;
	}
}
