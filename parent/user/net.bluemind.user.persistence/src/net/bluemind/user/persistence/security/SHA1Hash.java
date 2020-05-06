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

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import net.bluemind.core.api.fault.ServerFault;

public class SHA1Hash implements Hash {
	private static final String ALGORITHM = "SHA1";
	
	@Override
	public String create(String plaintext) throws ServerFault {
		throw new ServerFault("Don't use SHA1, use PBKDF2 instead please");
	}

	@Override
	public boolean validate(String plaintext, String hash) throws ServerFault {
		MessageDigest md;
		int prefixlen = 6;
		if (hash.startsWith("{SHA}")) {
			prefixlen = 5;
		}
		if (hash.length() < prefixlen) {
			return false;
		}
		try {
			md = MessageDigest.getInstance(ALGORITHM);
	        md.update(plaintext.getBytes("UTF-8"));
	        String sha1hash = new String(Base64.getEncoder().encode(md.digest()), "UTF-8");
			return sha1hash.equals(hash.substring(prefixlen));
		} catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
		}
		return false;
	}

	@Override
	public boolean matchesAlgorithm(String password) {
		return password.startsWith("{SHA}") || password.startsWith("{SHA1}");
	}
}
