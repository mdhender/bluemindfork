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
package net.bluemind.common.cache.persistence.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.github.benmanes.caffeine.cache.Caffeine;

import io.vertx.core.json.JsonObject;
import net.bluemind.common.cache.persistence.CacheBackingStore;

public class CacheEntryStoreTests {
	private String storePath;

	@Before
	public void before() {
		do {
			storePath = "/tmp/" + UUID.randomUUID();
		} while (new File(storePath).exists());
	}

	@After
	public void after() throws IOException {
		File sp = new File(storePath);
		if (sp.exists() && sp.isDirectory()) {
			Files.walk(sp.toPath()).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
		}
	}

	@Test
	public void persistenceDisabled() {
		try {
			new CacheBackingStore<String>(Caffeine.newBuilder(), storePath, null, null, null);
			fail("Test must thrown an exception");
		} catch (NullPointerException npe) {
		}

		try {
			new CacheBackingStore<String>(Caffeine.newBuilder(), storePath, this::toJson, null, null);
			fail("Test must thrown an exception");
		} catch (NullPointerException npe) {
		}
	}

	@Test
	public void persistenceEnabled() throws IOException {
		CacheBackingStore<String> sp = getTestCacheBuilder(true);
		sp.getIfPresent("key1");
		assertTrue(new File(storePath).exists());

		Files.walk(new File(storePath).toPath()).sorted(Comparator.reverseOrder()).map(Path::toFile)
				.forEach(File::delete);

		assertFalse(new File(storePath).exists());

		sp = getTestCacheBuilder(false);
		sp.getIfPresent("key1");
		assertTrue(new File(storePath).exists());
	}

	@Test
	public void loaded() {
		CacheBackingStore<String> sp = getTestCacheBuilder();
		sp.getCache().put("key1", "value1");

		sp = getTestCacheBuilder();
		assertNull(sp.getCache().getIfPresent("key1"));
		assertEquals("value1", sp.getIfPresent("key1"));
	}

	@Test
	public void ignored() {
		CacheBackingStore<String> sp = getTestCacheBuilder();
		sp.getCache().put("key1", "toIgnore");

		sp = getTestCacheBuilder();
		assertNull(sp.getCache().getIfPresent("key1"));
		assertNull(sp.getIfPresent("key1"));
	}

	private JsonObject toJson(String value) {
		return new JsonObject().put("v", value);
	}

	private String fromJson(JsonObject json) {
		return json.getString("v");
	}

	private Boolean ignore(String value) {
		return "toIgnore".equals(value);
	}

	private CacheBackingStore<String> getTestCacheBuilder() {
		return getTestCacheBuilder(true);
	}

	private CacheBackingStore<String> getTestCacheBuilder(boolean ignore) {
		return new CacheBackingStore<String>(Caffeine.newBuilder(), storePath, this::toJson, this::fromJson,
				ignore ? Optional.of(this::ignore) : Optional.empty());
	}
}
