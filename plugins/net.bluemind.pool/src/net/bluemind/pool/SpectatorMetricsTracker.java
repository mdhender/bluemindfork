/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2024
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
package net.bluemind.pool;

import java.util.concurrent.TimeUnit;

import com.netflix.spectator.api.Counter;
import com.netflix.spectator.api.Registry;
import com.netflix.spectator.api.Tag;
import com.netflix.spectator.api.Timer;
import com.netflix.spectator.api.patterns.PolledMeter;
import com.zaxxer.hikari.metrics.IMetricsTracker;
import com.zaxxer.hikari.metrics.PoolStats;

import net.bluemind.metrics.registry.IdFactory;

public class SpectatorMetricsTracker implements IMetricsTracker {
	private final Timer connectionObtainTimer;
	private final Counter connectionTimeoutCounter;
	private final Timer connectionUsage;
	private final Timer connectionCreation;

	public SpectatorMetricsTracker(final String poolName, final PoolStats poolStats, final Registry registry,
			final IdFactory idFactory) {
		Tag poolTag = Tag.of("pool", poolName);

		connectionObtainTimer = registry.timer(idFactory.name("connections.acquire").withTag(poolTag));
		connectionTimeoutCounter = registry.counter(idFactory.name("connections.timeout").withTag(poolTag));
		connectionUsage = registry.timer(idFactory.name("connections.usage").withTag(poolTag));
		connectionCreation = registry.timer(idFactory.name("connections.creation").withTag(poolTag));
		PolledMeter.using(registry).withId(idFactory.name("connections").withTag(poolTag)).monitorValue(poolStats,
				PoolStats::getTotalConnections);
		PolledMeter.using(registry).withId(idFactory.name("connections.idle").withTag(poolTag)).monitorValue(poolStats,
				PoolStats::getIdleConnections);
		PolledMeter.using(registry).withId(idFactory.name("connections.active").withTag(poolTag))
				.monitorValue(poolStats, PoolStats::getActiveConnections);
		PolledMeter.using(registry).withId(idFactory.name("connections.pending").withTag(poolTag))
				.monitorValue(poolStats, PoolStats::getPendingThreads);
		PolledMeter.using(registry).withId(idFactory.name("connections.max").withTag(poolTag)).monitorValue(poolStats,
				PoolStats::getMaxConnections);
		PolledMeter.using(registry).withId(idFactory.name("connections.min").withTag(poolTag)).monitorValue(poolStats,
				PoolStats::getMinConnections);

	}

	@Override
	public void recordConnectionAcquiredNanos(final long elapsedAcquiredNanos) {
		connectionObtainTimer.record(elapsedAcquiredNanos, TimeUnit.NANOSECONDS);
	}

	@Override
	public void recordConnectionUsageMillis(final long elapsedBorrowedMillis) {
		connectionUsage.record(elapsedBorrowedMillis, TimeUnit.MILLISECONDS);
	}

	@Override
	public void recordConnectionTimeout() {
		connectionTimeoutCounter.increment();
	}

	@Override
	public void recordConnectionCreatedMillis(long connectionCreatedMillis) {
		connectionCreation.record(connectionCreatedMillis, TimeUnit.MILLISECONDS);
	}
}
