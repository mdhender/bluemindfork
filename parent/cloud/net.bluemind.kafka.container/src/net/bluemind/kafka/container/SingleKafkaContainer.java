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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.kafka.container;

import net.bluemind.kafka.configuration.IBrokerFactory;
import net.bluemind.kafka.configuration.IKafkaBroker;

public class SingleKafkaContainer {

	private static final ZkKafkaContainer zk = startSingle();

	public static ZkKafkaContainer get() {
		return zk;
	}

	private static ZkKafkaContainer startSingle() {
		ZkKafkaContainer zk = new ZkKafkaContainer();
		zk.start();
		return zk;
	}

	private SingleKafkaContainer() {

	}

	public static class DockerBF implements IBrokerFactory {

		@Override
		public IKafkaBroker findAny() {
			return SingleKafkaContainer.get();
		}

	}
}
