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
package net.bluemind.domain.service.internal;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import net.bluemind.core.caches.registry.CacheRegistry;
import net.bluemind.core.caches.registry.ICacheRegistration;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.domain.api.Domain;

public class DomainsCache {
	public static class Registration implements ICacheRegistration {
		@Override
		public void registerCaches(CacheRegistry cr) {
			cr.register(DomainsCache.class,
					Caffeine.newBuilder().recordStats().expireAfterWrite(10, TimeUnit.MINUTES).build());
		}
	}

	private static final DomainsCache NOT_CACHED = new DomainsCache(null);

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(DomainsCache.class);

	private final Cache<String, ItemValue<Domain>> icache;

	public DomainsCache(Cache<String, ItemValue<Domain>> cache) {
		this.icache = cache;
	}

	public static DomainsCache get(BmContext context) {
		CacheRegistry cr = context.provider().instance(CacheRegistry.class);
		if (cr == null) {
			return NOT_CACHED;
		} else {
			return new DomainsCache(cr.get(DomainsCache.class));
		}

	}

	public ItemValue<Domain> getIfPresent(String uid) {
		if (icache != null) {
			return icache.getIfPresent(uid);
		} else {
			return null;
		}
	}

	public ItemValue<Domain> getDomainOrAlias(String uid) {
		if (icache != null) {
			ItemValue<Domain> ret = icache.getIfPresent(uid);
			if (ret == null) {
				return icache.getIfPresent("alias:" + uid);
			} else {
				return ret;
			}
		} else {
			return null;
		}
	}

	public void put(String uid, ItemValue<Domain> d) {
		if (icache == null) {
			return;
		}
		icache.put(uid, d);
		for (String alias : d.value.aliases) {
			icache.put("alias:" + alias, d);
		}
	}

	public void invalidate(String uid) {
		if (icache != null) {
			ItemValue<Domain> found = icache.getIfPresent(uid);
			if (found != null) {
				for (String alias : found.value.aliases) {
					icache.invalidate("alias:" + alias);
				}
			}
			icache.invalidate(uid);
		}
	}

}
