/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2020
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

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.netflix.spectator.api.Registry;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Verticle;
import net.bluemind.lib.vertx.IUniqueVerticleFactory;
import net.bluemind.lib.vertx.IVerticleFactory;
import net.bluemind.metrics.registry.IdFactory;
import net.bluemind.metrics.registry.MetricsRegistry;

public class CacheRegistryStatisticsVerticle extends AbstractVerticle {
	private static final Logger logger = LoggerFactory.getLogger(CacheRegistryStatisticsVerticle.class);
	private long cacheStatsTimer = 0;
	private static final Registry registry = MetricsRegistry.get();
	private static final IdFactory idFactory = new IdFactory("cache", MetricsRegistry.get(),
			CacheRegistryStatisticsVerticle.class);

	public static class Factory implements IVerticleFactory, IUniqueVerticleFactory {
		@Override
		public boolean isWorker() {
			return true;
		}

		@Override
		public Verticle newInstance() {
			return new CacheRegistryStatisticsVerticle();
		}
	}

	@Override
	public void start() {
		cacheStatsTimer = vertx.setPeriodic(30_000, i -> {
			for (Map.Entry<String, Cache<?, ?>> entry : CacheRegistry.get().getAll().entrySet()) {
				Cache<?, ?> cache = entry.getValue();
				CacheStats stats = cache.stats();
				String key = entry.getKey();
				if (stats != null) {
					registry.gauge(idFactory.name("hitcount").withTag("name", key)).set(stats.hitCount());
					registry.gauge(idFactory.name("misscount").withTag("name", key)).set(stats.missCount());
					registry.gauge(idFactory.name("evictioncount").withTag("name", key)).set(stats.evictionCount());
					registry.gauge(idFactory.name("requestcount").withTag("name", key)).set(stats.requestCount());
					registry.gauge(idFactory.name("size").withTag("name", key)).set(cache.estimatedSize());
				}
			}
		});
	}

	@Override
	public void stop() {
		if (cacheStatsTimer != 0) {
			vertx.cancelTimer(cacheStatsTimer);
		}
	}

}
