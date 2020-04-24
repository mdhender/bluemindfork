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
import java.util.Arrays;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import java.util.Base64;

import net.bluemind.core.api.fault.ServerFault;

public class SSHA512Hash implements Hash {
	private static final String IDENTIFIER = "{SSHA512}";
	private static final String ALGORITHM = "SHA-512";
	public static final int SALT_BYTE_SIZE = 8;

	@Override
	public String create(String plaintext) throws ServerFault {
		return create(plaintext, generateSalt());
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
		if (buf.length < SALT_BYTE_SIZE) {
			return false;
		}
		byte[] salt = Arrays.copyOfRange(buf, buf.length - SALT_BYTE_SIZE, buf.length);
		return create(plaintext, salt).equals(hash);
	}

	@Override
	public boolean matchesAlgorithm(String password) {
		return password.startsWith(IDENTIFIER);
	}

	private byte[] generateSalt() {
		SecureRandom random = new SecureRandom();
		byte[] salt = new byte[SALT_BYTE_SIZE];
		random.nextBytes(salt);
		return salt;
	}
}
