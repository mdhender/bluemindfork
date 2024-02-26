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

import com.netflix.spectator.api.Registry;
import com.zaxxer.hikari.metrics.IMetricsTracker;
import com.zaxxer.hikari.metrics.MetricsTrackerFactory;
import com.zaxxer.hikari.metrics.PoolStats;

import net.bluemind.metrics.registry.IdFactory;
import net.bluemind.metrics.registry.MetricsRegistry;

public class SpectatorMetricsTrackerFactory implements MetricsTrackerFactory {
	private final Registry registry;
	private final IdFactory idFactory;

	public SpectatorMetricsTrackerFactory() {
		registry = MetricsRegistry.get();
		idFactory = new IdFactory("db.pool", registry, SpectatorMetricsTrackerFactory.class);
	}

	@Override
	public IMetricsTracker create(String poolName, PoolStats poolStats) {
		return new SpectatorMetricsTracker(poolName, poolStats, registry, idFactory);
	}
}
