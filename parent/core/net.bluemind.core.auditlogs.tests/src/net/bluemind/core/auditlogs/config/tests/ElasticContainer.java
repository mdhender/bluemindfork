/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2023
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

package net.bluemind.core.auditlogs.config.tests;

import java.util.concurrent.TimeUnit;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.BaseConsumer;
import org.testcontainers.containers.output.OutputFrame;

import net.bluemind.network.utils.NetworkHelper;

public class ElasticContainer extends GenericContainer<ElasticContainer> {

	private static class LogConsumer extends BaseConsumer<LogConsumer> {

		@Override
		public void accept(OutputFrame t) {
			System.err.print(t.getUtf8String());
		}

	}

	public ElasticContainer() {
		super("bitnami/elasticsearch");
		withExposedPorts(9200);
		System.err.println("Hello");
//		this.getMapperPort(9200);
		withReuse(false);
		withLogConsumer(new LogConsumer());
		waitingFor(new org.testcontainers.containers.wait.strategy.AbstractWaitStrategy() {

			@Override
			protected void waitUntilReady() {
				NetworkHelper nh = new NetworkHelper(inspectAddress());
				System.err.println("Waiting for " + inspectAddress() + ":" + 9200);
				nh.waitForListeningPort(9200, 60, TimeUnit.SECONDS);
				System.err.println("Ready");
			}

		});
	}

	public String inspectAddress() {
		return getContainerInfo().getNetworkSettings().getNetworks().get("bridge").getIpAddress();
	}

	public String getMapperPort(int port) {
		return this.getMapperPort(port);
	}

}