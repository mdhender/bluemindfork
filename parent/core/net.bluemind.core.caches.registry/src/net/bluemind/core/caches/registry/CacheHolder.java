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
package net.bluemind.core.caches.registry;

import java.util.Optional;

import com.google.common.base.MoreObjects;
import com.google.common.cache.Cache;

public class CacheHolder<K, V> {

	protected final Optional<Cache<K, V>> cache;

	/**
	 * Holds a nullable cache on some key-values
	 * 
	 * @param c might be null
	 */
	protected CacheHolder(Cache<K, V> c) {
		this.cache = Optional.ofNullable(c);
	}

	public static <K, V> CacheHolder<K, V> of(Cache<K, V> c) {
		return new CacheHolder<K, V>(c);
	}

	public V getIfPresent(K k) {
		return cache.map(c -> c.getIfPresent(k)).orElse(null);
	}

	public void put(K k, V v) {
		cache.ifPresent(c -> c.put(k, v));
	}

	public void invalidate(K key) {
		cache.ifPresent(c -> c.invalidate(key));
	}

	public String toString() {
		return MoreObjects.toStringHelper(getClass())//
				.add("cache", cache.orElse(null))//
				.toString();
	}

}
