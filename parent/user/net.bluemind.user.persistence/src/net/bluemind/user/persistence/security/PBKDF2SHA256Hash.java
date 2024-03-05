/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2024
  *
  * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License)
  * or the CeCILL as published by CeCILL.info (version 2 of the License).
  *
  * There are special exceptions to the terms and conditions of the
  * licenses as they are applied to this program. See LICENSE.txt in
  * the directory of this program distribution.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.user.persistence.security;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import net.bluemind.core.api.fault.ServerFault;

public class PBKDF2SHA256Hash implements Hash {
	public static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";

	public static final int SALT_BYTE_SIZE = 24;
	public static final int HASH_BYTE_SIZE = 24;

	public static final int ITERATION_INDEX = 1;
	public static final int SALT_INDEX = 2;
	public static final int PBKDF2_INDEX = 3;

	private static final Pattern pattern = Pattern.compile("PBKDF2SHA256:\\d+:\\S+?:\\S+?");

	@Override
	public String create(String plaintext) throws ServerFault {
		byte[] salt = generateSalt();
		byte[] hash;
		int iterations = iterations();
		try {
			hash = pbkdf2(plaintext.toCharArray(), salt, iterations, HASH_BYTE_SIZE);
		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			throw new ServerFault(e);
		}
		return "PBKDF2SHA256:" + iterations + ":" + toHex(salt) + ":" + toHex(hash);
	}

	public static int iterations() {
		return HashConfig.get().getInt("bm.security.hash.pbkdf2sha256.iterations");
	}

	private static final Cache<String, Boolean> hashCache = CacheBuilder.newBuilder()
			.expireAfterAccess(1, TimeUnit.HOURS).build();

	@Override
	public boolean validate(String plaintext, String hash) throws ServerFault {
		String cacheKey = plaintext + ":" + hash;
		try {
			return hashCache.get(cacheKey, () -> {
				try {
					String[] params = hash.split(":");
					int iterations = Integer.parseInt(params[ITERATION_INDEX]);
					byte[] salt = fromHex(params[SALT_INDEX]);
					byte[] hashed = fromHex(params[PBKDF2_INDEX]);
					byte[] testHash = pbkdf2(plaintext.toCharArray(), salt, iterations, hashed.length);
					return verify(hashed, testHash);
				} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
					throw new ServerFault(e);
				}
			});
		} catch (ExecutionException e) {
			throw new ServerFault(e);
		}
	}

	@Override
	public boolean matchesAlgorithm(String password) {
		return pattern.matcher(password).matches();
	}

	private byte[] pbkdf2(char[] password, byte[] salt, int iterations, int bytes)
			throws NoSuchAlgorithmException, InvalidKeySpecException {
		PBEKeySpec specBC = new PBEKeySpec(password, salt, iterations, bytes * 8);
		SecretKeyFactory skf = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
		return skf.generateSecret(specBC).getEncoded();
	}

	private byte[] generateSalt() {
		SecureRandom random = new SecureRandom();
		byte[] salt = new byte[SALT_BYTE_SIZE];
		random.nextBytes(salt);
		return salt;
	}

	private boolean verify(byte[] a, byte[] b) {
		int diff = a.length ^ b.length;
		for (int i = 0; i < a.length && i < b.length; i++)
			diff |= a[i] ^ b[i];
		return diff == 0;
	}

	private byte[] fromHex(String hex) {
		byte[] binary = new byte[hex.length() / 2];
		for (int i = 0; i < binary.length; i++) {
			binary[i] = (byte) Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
		}
		return binary;
	}

	private String toHex(byte[] array) {
		BigInteger bi = new BigInteger(1, array);
		String hex = bi.toString(16);
		int paddingLength = (array.length * 2) - hex.length();
		if (paddingLength > 0)
			return String.format("%0" + paddingLength + "d", 0) + hex;
		else
			return hex;
	}

	@Override
	public boolean needsUpgrade(String hash) {
		String[] params = hash.split(":");
		int iterations = Integer.parseInt(params[ITERATION_INDEX]);
		return iterations() != iterations;
	}

}