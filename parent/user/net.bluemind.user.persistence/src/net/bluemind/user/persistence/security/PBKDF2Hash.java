/* 
 * Password Hashing With PBKDF2.
 * Copyright (c) 2013, Taylor Hornby
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, 
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation 
 * and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE 
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
 * POSSIBILITY OF SUCH DAMAGE.
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

public class PBKDF2Hash implements Hash {
	public static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA1";

	public static final int SALT_BYTE_SIZE = 24;
	public static final int HASH_BYTE_SIZE = 24;

	public static final int ITERATION_INDEX = 0;
	public static final int SALT_INDEX = 1;
	public static final int PBKDF2_INDEX = 2;

	private static final Pattern pattern = Pattern.compile("^\\d+:\\S+?:\\S+?");

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
		return iterations + ":" + toHex(salt) + ":" + toHex(hash);
	}

	public static int iterations() {
		return HashConfig.get().getInt("bm.security.hash.pbkdf2.iterations");
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
