/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.core.container.persistence;

import java.util.List;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import net.bluemind.core.caches.registry.CacheRegistry;
import net.bluemind.core.caches.registry.ICacheRegistration;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.rest.BmContext;

public class AclCache {

	public static class Registration implements ICacheRegistration {

		@Override
		public void registerCaches(CacheRegistry cr) {
			cr.register(AclCache.class, CacheBuilder.newBuilder().build());
		}
	}

	private final Cache<String, List<AccessControlEntry>> cache;

	public AclCache(Cache<String, List<AccessControlEntry>> c) {
		this.cache = c;
	}

	public static AclCache get(BmContext context) {
		if (context == null || context.provider().instance(CacheRegistry.class) == null) {
			return new AclCache(null);
		} else {
			return new AclCache(context.provider().instance(CacheRegistry.class).get(AclCache.class));
		}
	}

	public List<AccessControlEntry> getIfPresent(String uid) {
		if (cache != null) {
			return cache.getIfPresent(uid);
		} else {
			return null;
		}

	}

	public void put(String uid, List<AccessControlEntry> c) {
		if (cache != null) {
			cache.put(uid, c);
		}
	}

	public void invalidate(String uid) {
		if (cache != null) {
			cache.invalidate(uid);
		}
	}

}
