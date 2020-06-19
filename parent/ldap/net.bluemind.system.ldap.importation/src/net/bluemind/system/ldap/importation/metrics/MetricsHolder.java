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
package net.bluemind.system.ldap.importation.metrics;

import com.netflix.spectator.api.Clock;
import com.netflix.spectator.api.Registry;
import com.netflix.spectator.api.Timer;

import net.bluemind.metrics.registry.IdFactory;
import net.bluemind.metrics.registry.MetricsRegistry;

public class MetricsHolder {

	private static final MetricsHolder INST = new MetricsHolder();

	public static final MetricsHolder get() {
		return INST;
	}

	public final Registry registry;
	public final IdFactory idFactory;
	public final Clock clock;

	private MetricsHolder() {
		this.registry = MetricsRegistry.get();
		this.idFactory = new IdFactory("ldap.auth.service", registry, MetricsHolder.class);
		this.clock = registry.clock();
	}

	public Timer forOperation(String op) {
		return registry.timer(idFactory.name("timer", "op", op));
	}

}