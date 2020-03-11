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
package net.bluemind.kafka.container;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;

import net.bluemind.kafka.configuration.IKafkaBroker;
import net.bluemind.network.utils.NetworkHelper;

public class ZkKafkaContainer extends GenericContainer<ZkKafkaContainer> implements IKafkaBroker {

	private static final Logger logger = LoggerFactory.getLogger(ZkKafkaContainer.class);

	public ZkKafkaContainer() {
		super("repository.blue-mind.loc:5001/bluemind/zk-kafka:4.1.48663");
		withExposedPorts(2181, port());
		waitingFor(new org.testcontainers.containers.wait.strategy.AbstractWaitStrategy() {

			@Override
			protected void waitUntilReady() {
				NetworkHelper nh = new NetworkHelper(inspectAddress());
				System.err.println("Waiting for " + inspectAddress() + ":" + port());
				nh.waitForListeningPort(2181, 30, TimeUnit.SECONDS);
				nh.waitForListeningPort(port(), 30, TimeUnit.SECONDS);
			}

		});
		if (logger.isDebugEnabled()) {
			withLogConsumer(new Slf4jLogConsumer(logger));
		}
	}

	public String inspectAddress() {
		return getContainerInfo().getNetworkSettings().getNetworks().get("bridge").getIpAddress();
	}

	public int defaultPartitions() {
		return 3;
	}

	public int maxReplicas() {
		return 1;
	}

	@Override
	public void start() {
		super.start();
	}

}
