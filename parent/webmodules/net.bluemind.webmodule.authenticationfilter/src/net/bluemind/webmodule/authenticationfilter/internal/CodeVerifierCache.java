/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.webmodule.authenticationfilter.internal;

import java.util.concurrent.TimeUnit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

public class CodeVerifierCache {

	private static final Cache<String, String> cache = CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.HOURS)
			.build();

	private CodeVerifierCache() {

	}

	public static String verify(String key) {
		return cache.getIfPresent(key);
	}

	public static void put(String key, String codeVerifier) {
		cache.put(key, codeVerifier);

	}

	public static void invalidate(String key) {
		cache.invalidate(key);
	}

}
