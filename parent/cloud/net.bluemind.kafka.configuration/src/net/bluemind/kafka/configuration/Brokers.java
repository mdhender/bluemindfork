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
package net.bluemind.kafka.configuration;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.eclipse.common.RunnableExtensionLoader;

public class Brokers {

	private static final Logger logger = LoggerFactory.getLogger(Brokers.class);

	private static final IKafkaBroker localBroker = new IKafkaBroker() {

		@Override
		public String inspectAddress() {
			return "127.0.0.1";
		}
	};

	private static final IKafkaBroker brok = plugins();

	private Brokers() {
	}

	private static IKafkaBroker plugins() {
		RunnableExtensionLoader<IBrokerFactory> r = new RunnableExtensionLoader<>();
		List<IBrokerFactory> factories = r.loadExtensionsWithPriority(StaticTopics.PLUGIN_ID, "brokers", "broker",
				"factory");
		if (factories.isEmpty()) {
			logger.error("No factories to find brokers {}", factories);
			throw new RuntimeException("no broker factory");
		}
		for (IBrokerFactory f : factories) {
			IKafkaBroker b = f.findAny();
			if (b != null) {
				logger.info("Selected {} from {}", b, f);
				return b;
			}
		}
		logger.warn("Defaulting to local kafka broker 127.0.0.1:{}", 9093);
		return localBroker;
	}

	public static IKafkaBroker locate() {
		return brok;
	}

}
