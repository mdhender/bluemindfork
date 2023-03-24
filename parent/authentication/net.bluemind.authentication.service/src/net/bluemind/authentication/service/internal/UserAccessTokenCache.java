/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.authentication.service.internal;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;

import net.bluemind.core.caches.registry.CacheRegistry;
import net.bluemind.core.caches.registry.ICacheRegistration;
import net.bluemind.core.context.UserAccessToken;
import net.bluemind.core.rest.BmContext;

public class UserAccessTokenCache {

	private static final Logger logger = LoggerFactory.getLogger(UserAccessTokenCache.class);

	public static class Registration implements ICacheRegistration {
		@Override
		public void registerCaches(CacheRegistry cr) {
			logger.debug("Registered UserAccessToken cache");
			cr.register(UserAccessTokenCache.class,
					Caffeine.newBuilder().recordStats().expireAfter(new Expiry<String, UserAccessToken>() {
						@Override
						public long expireAfterCreate(String key, UserAccessToken value, long currentTime) {
							return expiryTime(value, currentTime);
						}

						@Override
						public long expireAfterUpdate(String key, UserAccessToken value, long currentTime,
								long currentDuration) {
							return expiryTime(value, currentTime);
						}

						@Override
						public long expireAfterRead(String key, UserAccessToken value, long currentTime,
								long currentDuration) {
							return currentDuration;
						}

						private long expiryTime(UserAccessToken value, long currentTime) {
							LocalDateTime now = LocalDateTime.now();
							LocalDateTime expiration = value.expiryTime.toInstant().atZone(ZoneId.of("UTC"))
									.toLocalDateTime();

							return ChronoUnit.NANOS.between(now, expiration);
						}
					}).build());
		}
	}

	private final Cache<String, UserAccessToken> cache;

	public UserAccessTokenCache(Cache<String, UserAccessToken> c) {
		this.cache = c;
	}

	public static UserAccessTokenCache get(BmContext context) {
		if (context == null || context.provider().instance(CacheRegistry.class) == null) {
			return new UserAccessTokenCache(null);
		} else {
			return new UserAccessTokenCache(
					context.provider().instance(CacheRegistry.class).get(UserAccessTokenCache.class));
		}
	}

	public UserAccessToken getIfPresent(String domainUid, String userUid, String systemIdentifier) {
		if (cache != null) {
			return cache.getIfPresent(id(domainUid, userUid, systemIdentifier));
		} else {
			return null;
		}

	}

	public void put(String domainUid, String userUid, String systemIdentifier, UserAccessToken c) {
		if (cache != null) {
			cache.put(id(domainUid, userUid, systemIdentifier), c);
		}
	}

	public void invalidate(String domainUid, String userUid, String systemIdentifier) {
		if (cache != null) {
			cache.invalidate(id(domainUid, userUid, systemIdentifier));
		}
	}

	private String id(String domainUid, String userUid, String systemIdentifier) {
		return domainUid + "#" + userUid + "#" + systemIdentifier;
	}

}
