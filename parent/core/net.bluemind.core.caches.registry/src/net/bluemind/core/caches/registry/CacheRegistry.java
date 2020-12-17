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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.benmanes.caffeine.cache.Cache;

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
		public CacheRegistry instance(BmContext context, String... params) {
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

	private final Map<String, Cache<?, ?>> caches;
	private final List<Cache<?, ?>> readonlyCaches;

	private CacheRegistry() {
		caches = new HashMap<>();
		readonlyCaches = new ArrayList<>();
	}

	public void register(Class<?> idKlass, Cache<?, ?> cache) {
		register(idKlass.getName(), cache);
	}

	public void register(String id, Cache<?, ?> cache) {
		if (caches.containsKey(id)) {
			logger.error("registring duplicated cache identifier: {}", id);
		} else {
			caches.put(id, cache);
		}
	}

	// Read only caches will not be touched by invalidate
	public void registerReadOnly(Class<?> idKlass, Cache<?, ?> cache) {
		readonlyCaches.add(cache);
		register(idKlass, cache);
	}

	public void registerReadOnly(String id, Cache<?, ?> cache) {
		readonlyCaches.add(cache);
		register(id, cache);
	}

	@SuppressWarnings("unchecked")
	public <K, V> Cache<K, V> get(String id) {
		return (Cache<K, V>) caches.get(id);
	}

	@SuppressWarnings("unchecked")
	public <K, V> Cache<K, V> get(Class<?> idKlass) {
		return (Cache<K, V>) caches.get(idKlass.getName());
	}

	public void invalidateAll() {
		for (Cache<?, ?> c : caches.values()) {
			if (!readonlyCaches.contains(c)) {
				c.invalidateAll();
			}
		}
		logger.info("Cleared {} cache(s)", caches.size() - readonlyCaches.size());
	}

	public void forEach(Consumer<Cache<?, ?>> action) {
		caches.values().forEach(action);
	}

	public Map<String, Cache<?, ?>> getAll() {
		return caches;
	}

}
