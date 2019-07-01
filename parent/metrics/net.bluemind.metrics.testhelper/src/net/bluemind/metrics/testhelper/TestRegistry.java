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
package net.bluemind.metrics.testhelper;

import com.netflix.spectator.api.AbstractRegistry;
import com.netflix.spectator.api.Clock;
import com.netflix.spectator.api.Counter;
import com.netflix.spectator.api.DistributionSummary;
import com.netflix.spectator.api.Gauge;
import com.netflix.spectator.api.Id;
import com.netflix.spectator.api.Timer;

public class TestRegistry extends AbstractRegistry {

	public TestRegistry() {
		super(Clock.SYSTEM);
	}

	@Override
	protected Counter newCounter(Id id) {
		return new TestCounter(this, id);
	}

	@Override
	protected DistributionSummary newDistributionSummary(Id id) {
		return new TestDistributionSummary(id);
	}

	@Override
	protected Gauge newGauge(Id id) {
		return new TestGauge(id);
	}

	@Override
	protected Gauge newMaxGauge(Id id) {
		return new TestMaxGauge(id);
	}

	@Override
	protected Timer newTimer(Id id) {
		return new TestTimer(id);
	}

}
