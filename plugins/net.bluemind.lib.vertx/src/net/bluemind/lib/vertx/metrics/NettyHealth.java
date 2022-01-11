/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.lib.vertx.metrics;

import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.spectator.api.Registry;
import com.netflix.spectator.api.patterns.PolledMeter;

import io.netty.util.internal.PlatformDependent;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Verticle;
import net.bluemind.lib.vertx.IUniqueVerticleFactory;
import net.bluemind.lib.vertx.IVerticleFactory;
import net.bluemind.metrics.registry.IdFactory;
import net.bluemind.metrics.registry.MetricsRegistry;

public class NettyHealth extends AbstractVerticle {

	public static class Factory implements IVerticleFactory, IUniqueVerticleFactory {

		@Override
		public boolean isWorker() {
			return true;
		}

		@Override
		public Verticle newInstance() {
			return new NettyHealth();
		}

	}

	private static final Logger logger = LoggerFactory.getLogger(NettyHealth.class);

	@Override
	public void start(Promise<Void> startPromise) throws Exception {
		Registry reg = MetricsRegistry.get();
		IdFactory idf = new IdFactory(reg, NettyHealth.class);
		AtomicLong nettyDirect = PolledMeter.using(reg).withId(idf.name("nettyUsedDirectMemory"))
				.monitorValue(new AtomicLong(PlatformDependent.usedDirectMemory()));
		logger.info("Used direct memory {}", nettyDirect.get());
		vertx.setPeriodic(4000, tid -> {
			long value = PlatformDependent.usedDirectMemory();
			logger.debug("Updating to {}", value);
			nettyDirect.set(value);
		});
		startPromise.complete();
	}

}
