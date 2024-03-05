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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import net.bluemind.core.api.fault.ServerFault;

public class Argon2Hash implements Hash {
	public static final int HASH_INDEX = 1;

	private static final Argon2PasswordEncoder pwEncoder = new Argon2PasswordEncoder(16, 32, 1, 1 << 15, 2);

	@Override
	public String create(String plaintext) throws ServerFault {
		return pwEncoder.encode(plaintext);
	}

	private static final Cache<String, Boolean> hashCache = CacheBuilder.newBuilder()
			.expireAfterAccess(1, TimeUnit.HOURS).build();

	@Override
	public boolean validate(String plaintext, String hash) throws ServerFault {
		String cacheKey = plaintext + ":" + hash;
		try {
			return hashCache.get(cacheKey, () -> pwEncoder.matches(plaintext, hash));
		} catch (ExecutionException e) {
			throw new ServerFault(e);
		}
	}

	@Override
	public boolean matchesAlgorithm(String password) {
		return password.startsWith("$argon2id");
	}

	@Override
	public boolean needsUpgrade(String hash) {
		return false;
	}

}
