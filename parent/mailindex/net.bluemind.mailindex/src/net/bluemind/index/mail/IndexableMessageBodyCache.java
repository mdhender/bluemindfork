/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2019
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
package net.bluemind.index.mail;

import java.util.concurrent.TimeUnit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import net.bluemind.backend.mail.replica.indexing.IElasticSourceHolder;
import net.bluemind.backend.mail.replica.indexing.IndexedMessageBody;
import net.bluemind.core.caches.registry.CacheRegistry;
import net.bluemind.core.caches.registry.ICacheRegistration;

/**
 * 
 */
public class IndexableMessageBodyCache {
	private IndexableMessageBodyCache() {
	}

	public static final Cache<String, IndexedMessageBody> bodies = Caffeine.newBuilder().recordStats().maximumSize(256)
			.expireAfterWrite(10, TimeUnit.SECONDS).build();

	public static final Cache<String, IElasticSourceHolder> sourceHolder = Caffeine.newBuilder().recordStats()
			.maximumSize(256).expireAfterWrite(10, TimeUnit.SECONDS).build();

	public static class CacheRegistration implements ICacheRegistration {
		@Override
		public void registerCaches(CacheRegistry cr) {
			cr.register(IndexableMessageBodyCache.class, bodies);
			cr.register(IElasticSourceHolder.class, sourceHolder);
		}
	}
}
