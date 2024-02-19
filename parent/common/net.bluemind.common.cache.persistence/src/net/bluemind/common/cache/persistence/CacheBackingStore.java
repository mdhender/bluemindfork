/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2021
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.common.cache.persistence;

import java.io.File;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.vertx.core.json.JsonObject;

public class CacheBackingStore<V> {
	private static final Logger logger = LoggerFactory.getLogger(CacheBackingStore.class);

	private final Consumer<String> notifyUnkonwnSessionRemovalListeners;

	private final Optional<CacheEntryWriterLoader<V>> writerLoader;
	private final Cache<String, V> cache;

	public CacheBackingStore(Caffeine<Object, Object> cache, String storePath, Function<V, JsonObject> toJson,
			Function<JsonObject, V> fromJson) {
		Objects.requireNonNull(toJson);
		Objects.requireNonNull(fromJson);

		this.notifyUnkonwnSessionRemovalListeners = s -> {
		};

		this.writerLoader = getWriterLoader(storePath, toJson, fromJson);
		this.cache = writerLoader.map(wl -> cache.writer(wl).build()).orElseGet(cache::build);
	}

	public CacheBackingStore(Caffeine<Object, Object> cache, String storePath, Function<V, JsonObject> toJson,
			Function<JsonObject, V> fromJson, Consumer<String> notifyUnkonwnSessionRemovalListeners) {
		Objects.requireNonNull(toJson);
		Objects.requireNonNull(fromJson);

		this.notifyUnkonwnSessionRemovalListeners = notifyUnkonwnSessionRemovalListeners;

		this.writerLoader = getWriterLoader(storePath, toJson, fromJson);
		this.cache = writerLoader.map(wl -> cache.writer(wl).build()).orElseGet(cache::build);
	}

	private Optional<CacheEntryWriterLoader<V>> getWriterLoader(String storePath, Function<V, JsonObject> toJson,
			Function<JsonObject, V> fromJson) {
		try {
			File root = new File(storePath);
			if (!root.exists() && !root.mkdirs()) {
				logger.warn("Cache persistence disabled: unable to create {}", root.getAbsolutePath());
				return Optional.empty();
			}

			if (!root.setReadable(false, false) || !root.setReadable(true, true) || !root.setWritable(false, false)
					|| !root.setWritable(true, true) || !root.setExecutable(false, false)
					|| !root.setExecutable(true, true)) {
				logger.warn("Cache persistence disabled: unable to set perms on {}", root.getAbsolutePath());
				return Optional.empty();
			}

			return Optional.of(new CacheEntryWriterLoader<V>(storePath, toJson, fromJson));
		} catch (RuntimeException re) {
			// Sentry
			logger.error("unnown error", re);
			return Optional.empty();
		}
	}

	public V getIfPresent(String key) {
		return writerLoader.map(wl -> cache.get(key, wl::load)).orElseGet(() -> cache.getIfPresent(key));
	}

	public void put(String key, V value) {
		cache.put(key, value);
	}

	public void invalidate(String key) {
		if (cache.getIfPresent(key) == null) {
			notifyUnkonwnSessionRemovalListeners.accept(key);
			return;
		}
		cache.invalidate(key);
	}

	public Map<String, V> asMap() {
		return cache.asMap();
	}

	public Cache<String, V> getCache() {
		return cache;
	}

	public void cleanUp() {
		cache.cleanUp();
		writerLoader.ifPresent(wl -> wl.cleanUp(cache));
	}
}
