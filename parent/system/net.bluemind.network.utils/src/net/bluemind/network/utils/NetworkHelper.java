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
package net.bluemind.network.utils;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;

public class NetworkHelper {

	private static final Logger logger = LoggerFactory.getLogger(NetworkHelper.class);
	private String address;

	public NetworkHelper(String address) {
		this.address = address;
	}

	public void waitForListeningPort(int port, long delay, TimeUnit unit) {
		logger.info("Checking port {}:{}", address, port);

		long exitAt = System.nanoTime() + unit.toNanos(delay);
		long time = 0;
		InetSocketAddress sockAddress = new InetSocketAddress(address, port);
		while ((time = System.nanoTime()) < exitAt) {
			try (Socket socket = new Socket()) {
				socket.connect(sockAddress, 100);
				break;
			} catch (Exception ste) {
				// that's ok
			}
			try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
			}
		}
		if (time > exitAt) {
			throw new ServerFault("Port " + address + ":" + port + " is not listening");
		}

	}

}
