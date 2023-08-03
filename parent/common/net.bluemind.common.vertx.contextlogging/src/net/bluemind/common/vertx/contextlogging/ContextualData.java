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
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.impl.ContextInternal;

/**
 * Helper to store data in the local context.
 */
public class ContextualData {
	private ContextualData() {
	}

	private static boolean isDuplicatedContext(Context context) {
		// Do not use Assert.checkNotNullParam with type io.vertx.core.Context as it is
		// likely
		// to trigger a performance issue via JDK-8180450.
		// Identified via https://github.com/franz1981/type-pollution-agent
		// So we cast to ContextInternal first:
		ContextInternal actual = (ContextInternal) context;
		Objects.requireNonNull(actual);
		return actual.isDuplicate();
	}

	private static Context ensureDuplicatedContext() {
		Context current = Vertx.currentContext();
		if (current == null || !isDuplicatedContext(current)) {
			throw new UnsupportedOperationException("Access to Context Locals are forbidden from a 'root' context  as "
					+ "it can leak data between unrelated processing. Make sure the method runs on a 'duplicated' (local)"
					+ " Context");
		}
		return current;
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
		contextualDataMap((ContextInternal) ensureDuplicatedContext()).put(key, value);
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
		return contextualDataMap((ContextInternal) ensureDuplicatedContext()).get(key);
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
		Context current = Vertx.currentContext();
		if (isDuplicatedContext(current)) {
			return contextualDataMap((ContextInternal) ensureDuplicatedContext()).getOrDefault(key, defaultValue);
		} else {
			return null;
		}
	}

	/**
	 * Get all values from the contextual data map
	 * 
	 * @return readonly copy vue of all the keys and values
	 */

	public static Map<String, String> getAll() {
		Context current = Vertx.currentContext();
		if (current == null || !isDuplicatedContext(current)) {
			return Collections.emptyMap();
		} else {
			return Collections.unmodifiableMap(contextualDataMap((ContextInternal) current));
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static ConcurrentMap<String, String> contextualDataMap(ContextInternal ctx) {
		ConcurrentMap<Object, Object> lcd = Objects.requireNonNull(ctx).localContextData();
		return (ConcurrentMap) lcd.computeIfAbsent(ContextualData.class, k -> new ConcurrentHashMap());
	}
}
