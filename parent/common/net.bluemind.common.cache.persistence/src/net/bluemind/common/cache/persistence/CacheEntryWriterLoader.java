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
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.benmanes.caffeine.cache.Cache;
// LC: NOTE: CacheWriter is removed in 3.x 
import com.github.benmanes.caffeine.cache.CacheWriter;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.google.common.io.ByteStreams;

import io.vertx.core.json.JsonObject;

public class CacheEntryWriterLoader<V> implements CacheWriter<String, V> {
	private static final Logger logger = LoggerFactory.getLogger(CacheEntryWriterLoader.class);

	private final String storePath;
	private final Function<V, JsonObject> toJson;
	private final Function<JsonObject, V> fromJson;
	private final Optional<Predicate<V>> ignore;

	public CacheEntryWriterLoader(String storePath, Function<V, JsonObject> toJson, Function<JsonObject, V> fromJson,
			Optional<Predicate<V>> ignore) {
		this.storePath = storePath;
		this.toJson = toJson;
		this.fromJson = fromJson;
		this.ignore = ignore;
	}

	@Override
	public void write(String key, V value) {
		if (ignore.map(f -> f.test(value)).orElse(Boolean.FALSE)) {
			return;
		}

		JsonObject jsonObject = null;

		try {
			jsonObject = toJson.apply(value);
		} catch (RuntimeException re) {
		}

		if (jsonObject == null) {
			return;
		}

		File cacheFile = getCacheFile(key);

		try (FileWriter fileWriter = new FileWriter(cacheFile)) {
			fileWriter.write(jsonObject.encode());
		} catch (IOException e) {
			if (logger.isDebugEnabled()) {
				logger.debug("Unable to write file {}", cacheFile.getAbsoluteFile(), e);
			} else {
				logger.warn("Unable to write file {}: {}", cacheFile.getAbsoluteFile(), e.getMessage());
			}
		}
	}

	@Override
	public void delete(String key, V value, RemovalCause cause) {
		delete(key);
	}

	public V load(String key) {
		if (key == null || key.isEmpty()) {
			return null;
		}
		File cacheFile = getCacheFile(key);
		if (!cacheFile.exists() || !cacheFile.canRead()) {
			return null;
		}

		try (InputStream fis = Files.newInputStream(cacheFile.toPath())) {
			return fromJson.apply(new JsonObject(new String(ByteStreams.toByteArray(fis))));
		} catch (RuntimeException | IOException re) {
			logger.error("Unable to load {}, try to remove: {}", key, cacheFile.getAbsolutePath(), re);
			cacheFile.delete();
			return null;
		}
	}

	private void delete(String key) {
		File file = getCacheFile(key);
		if (file.exists()) {
			if (!file.delete()) {
				logger.warn("Unable to delete file {}", file.getAbsoluteFile());
			}
		}
	}

	private File getCacheFile(String key) {
		return new File(storePath + "/" + key);
	}

	public void cleanUp(Cache<String, V> cache) {
		logger.info("Cleanup cache directory {}", storePath);
		Optional.ofNullable(new File(storePath).list()).map(lists -> Arrays.stream(lists))
				.ifPresent(stream -> stream.filter(sid -> cache.getIfPresent(sid) == null).forEach(this::delete));
	}
}
