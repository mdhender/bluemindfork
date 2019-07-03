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
package net.bluemind.core.caches.registry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider.IServerSideServiceFactory;
import net.bluemind.eclipse.common.RunnableExtensionLoader;

public class CacheRegistry {

	public static final class Factory implements IServerSideServiceFactory<CacheRegistry> {

		@Override
		public Class<CacheRegistry> factoryClass() {
			return CacheRegistry.class;
		}

		@Override
		public CacheRegistry instance(BmContext context, String... params) throws ServerFault {
			return registry;
		}

	}

	private static final Logger logger = LoggerFactory.getLogger(CacheRegistry.class);
	private static final CacheRegistry registry = init();

	public static CacheRegistry get() {
		return registry;
	}

	private static CacheRegistry init() {
		CacheRegistry cr = new CacheRegistry();
		RunnableExtensionLoader<ICacheRegistration> loader = new RunnableExtensionLoader<>();
		List<ICacheRegistration> toAdd = loader.loadExtensions("net.bluemind.core.caches.registry", "registration",
				"reg", "impl");
		for (ICacheRegistration icr : toAdd) {
			icr.registerCaches(cr);
		}
		return cr;
	}

	private final Map<Object, Cache<?, ?>> caches;

	private CacheRegistry() {
		this.caches = new HashMap<>();
	}

	public void register(Object id, Cache<?, ?> cache) {
		this.caches.put(id, cache);
	}

	@SuppressWarnings("unchecked")
	public <K, V> Cache<K, V> get(Object id) {
		return (Cache<K, V>) caches.get(id);
	}

	public void invalidateAll() {
		for (Cache<?, ?> c : caches.values()) {
			c.invalidateAll();
		}
		logger.info("Cleared {} cache(s)", caches.size());
	}

	public void forEach(Consumer<Cache<?, ?>> action) {
		caches.values().forEach(action);
	}

}
