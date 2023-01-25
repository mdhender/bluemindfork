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
package net.bluemind.imap.endpoint.imaptest.tests;

import java.util.concurrent.TimeUnit;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.BaseConsumer;
import org.testcontainers.containers.output.OutputFrame;

import net.bluemind.network.utils.NetworkHelper;

public class DovecotContainer extends GenericContainer<DovecotContainer> {

	private static class LogConsumer extends BaseConsumer<LogConsumer> {

		@Override
		public void accept(OutputFrame t) {
			System.err.print(t.getUtf8String());
		}

	}

	public DovecotContainer() {
		super("dovecot/dovecot:2.3.20");
		withExposedPorts(143, 24, 587);
		withReuse(false);
		withLogConsumer(new LogConsumer());
		waitingFor(new org.testcontainers.containers.wait.strategy.AbstractWaitStrategy() {

			@Override
			protected void waitUntilReady() {
				NetworkHelper nh = new NetworkHelper(inspectAddress());
				System.err.println("Waiting for " + inspectAddress() + ":" + 143);
				nh.waitForListeningPort(143, 30, TimeUnit.SECONDS);
			}

		});
	}

	public String inspectAddress() {
		return getContainerInfo().getNetworkSettings().getNetworks().get("bridge").getIpAddress();
	}

}
