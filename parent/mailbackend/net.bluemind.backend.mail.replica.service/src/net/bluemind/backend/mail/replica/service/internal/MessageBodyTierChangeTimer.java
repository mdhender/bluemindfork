/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2022
  *
  * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License)
  * or the CeCILL as published by CeCILL.info (version 2 of the License).
  *
  * There are special exceptions to the terms and conditions of the
  * licenses as they are applied to this program. See LICENSE.txt in
  * the directory of this program distribution.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.backend.mail.replica.service.internal;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.util.concurrent.DefaultThreadFactory;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Verticle;
import net.bluemind.backend.mail.replica.api.IMessageBodyTierChange;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.lib.vertx.IUniqueVerticleFactory;
import net.bluemind.lib.vertx.IVerticleFactory;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.network.topology.Topology;
import net.bluemind.server.api.TagDescriptor;

public class MessageBodyTierChangeTimer extends AbstractVerticle {
	ServerSideServiceProvider provider = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
	private static final Logger logger = LoggerFactory.getLogger(MessageBodyTierChangeTimer.class);
	private static final Executor executor = Executors.newSingleThreadExecutor(new DefaultThreadFactory("tier-change"));

	@Override
	public void start() {
		VertxPlatform.executeBlockingPeriodic(TimeUnit.HOURS.toMillis(2), this::execute);
	}

	private void execute(Long timerId) {
		executor.execute(this::doHsmJob);
	}

	private void doHsmJob() {
		long totalProcessed = 0;
		for (var backend : Topology.get().all(TagDescriptor.bm_pgsql_data.getTag())) {
			String logId = backend.displayName;
			IMessageBodyTierChange tierChangeService = provider.instance(IMessageBodyTierChange.class, backend.uid);
			int processed = 0;
			do {
				processed = tierChangeService.moveTier();
				totalProcessed += processed;
				logger.info("[{}] moved {} emails between storage tiers", logId, processed);
				if (processed >= IMessageBodyTierChange.TIER_CHANGES_PER_TICK) {
					try {
						logger.info("[{}] sleeping 1s to avoid stressing the server too much", logId);
						Thread.sleep(1_000);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				}
			} while (processed >= IMessageBodyTierChange.TIER_CHANGES_PER_TICK);
			logger.info("[{}] Moved {} emails between storage tiers in total", logId, totalProcessed);
		}
	}

	public static class Factory implements IVerticleFactory, IUniqueVerticleFactory {
		@Override
		public boolean isWorker() {
			return true;
		}

		@Override
		public Verticle newInstance() {
			return new MessageBodyTierChangeTimer();
		}
	}
}
