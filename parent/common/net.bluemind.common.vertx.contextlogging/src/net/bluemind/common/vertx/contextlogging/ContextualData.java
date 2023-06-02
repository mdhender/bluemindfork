/*
 * Copyright 2021 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package net.bluemind.common.vertx.contextlogging;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import io.vertx.core.Vertx;
import io.vertx.core.impl.ContextInternal;

/**
 * Helper to store data in the local context.
 */
public class ContextualData {
	private ContextualData() {
	}

	/**
	 * Remove a value in the contextual data map.
	 * 
	 * @param key the key of the data in contextual data map
	 */
	public static void remove(String key) {
		Objects.requireNonNull(key);
		if (Vertx.currentContext() instanceof ContextInternal ctx) {
			contextualDataMap(ctx).remove(key);
		}
	}

	/**
	 * Remove all values in the contextual data map.
	 */
	public static void clear() {
		if (Vertx.currentContext() instanceof ContextInternal ctx) {
			contextualDataMap(ctx).clear();
		}
	}

	/**
	 * Put a value in the contextual data map.
	 *
	 * @param key   the key of the data in the contextual data map
	 * @param value the data value
	 */
	public static void put(String key, String value) {
		Objects.requireNonNull(key);
		Objects.requireNonNull(value);
		if (Vertx.currentContext() instanceof ContextInternal ctx) {
			contextualDataMap(ctx).put(key, value);
		}
	}

	/**
	 * Get a value from the contextual data map.
	 *
	 * @param key the key of the data in the contextual data map
	 * @return the value or null if absent or the method is invoked on a non Vert.x
	 *         thread
	 */
	public static String get(String key) {
		Objects.requireNonNull(key);
		if (Vertx.currentContext() instanceof ContextInternal ctx) {
			return contextualDataMap(ctx).get(key);
		}
		return null;
	}

	/**
	 * Get a value from the contextual data map.
	 *
	 * @param key          the key of the data in the contextual data map
	 * @param defaultValue the value returned when the {@code key} is not present in
	 *                     the contextual data map or the method is invoked on a non
	 *                     Vert.x thread
	 * @return the value or the {@code defaultValue} if absent or the method is
	 *         invoked on a non Vert.x thread
	 */
	public static String getOrDefault(String key, String defaultValue) {
		Objects.requireNonNull(key);
		if (Vertx.currentContext() instanceof ContextInternal ctx) {
			return contextualDataMap(ctx).getOrDefault(key, defaultValue);
		}
		return defaultValue;
	}

	/**
	 * Get all values from the contextual data map.
	 *
	 * @return the values or {@code null} if the method is invoked on a non Vert.x
	 *         thread
	 */
	public static Map<String, String> getAll() {
		if (Vertx.currentContext() instanceof ContextInternal ctx) {
			return new HashMap<>(contextualDataMap(ctx));
		}
		return Collections.emptyMap();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static ConcurrentMap<String, String> contextualDataMap(ContextInternal ctx) {
		ConcurrentMap<Object, Object> lcd = Objects.requireNonNull(ctx).localContextData();
		return (ConcurrentMap) lcd.computeIfAbsent(ContextualData.class, k -> new ConcurrentHashMap());
	}
}
