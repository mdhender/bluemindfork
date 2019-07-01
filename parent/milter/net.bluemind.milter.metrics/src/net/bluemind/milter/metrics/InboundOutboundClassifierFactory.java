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
package net.bluemind.milter.metrics;

import com.netflix.spectator.api.Registry;

import net.bluemind.metrics.registry.IdFactory;
import net.bluemind.metrics.registry.MetricsRegistry;
import net.bluemind.milter.IMilterListener;
import net.bluemind.milter.IMilterListenerFactory;

public class InboundOutboundClassifierFactory implements IMilterListenerFactory {

	private final Registry registry;
	private final IdFactory idFactory;

	public InboundOutboundClassifierFactory() {
		this.registry = MetricsRegistry.get();
		this.idFactory = new IdFactory("traffic", registry, InboundOutboundClassifierFactory.class);
	}

	@Override
	public IMilterListener create() {
		return new InboundOutboundClassifier(registry, idFactory);
	}

}
