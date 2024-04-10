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
		super("elasticsearch:7.17.15");
		startContainer();
	}

	public ElasticContainer(String user, String password) {
		super("elasticsearch:7.17.15");
		withEnv("ELASTIC_USERNAME", user);
		withEnv("ELASTIC_PASSWORD", password);
		withEnv("xpack.security.enabled", "true");
		withEnv("xpack.security.http.ssl.enabled", "false");
		startContainer();
	}

	private void startContainer() {
		withExposedPorts(9200);
		withReuse(false);
		withEnv("discovery.type", "single-node");
		withLogConsumer(new LogConsumer());
		waitingFor(new org.testcontainers.containers.wait.strategy.AbstractWaitStrategy() {
			@Override
			protected void waitUntilReady() {
				NetworkHelper nh = new NetworkHelper(inspectAddress());
				System.err.println("Waiting for " + inspectAddress() + ":" + 9200);
				nh.waitForListeningPort(9200, 120, TimeUnit.SECONDS);
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