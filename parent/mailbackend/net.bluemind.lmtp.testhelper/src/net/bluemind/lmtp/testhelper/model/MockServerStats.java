/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.lmtp.testhelper.model;

import java.util.concurrent.atomic.AtomicInteger;

public class MockServerStats {

	private static final MockServerStats mdl = new MockServerStats();

	public static MockServerStats get() {
		return mdl;
	}

	private AtomicInteger openedCon = new AtomicInteger();
	private AtomicInteger closedCon = new AtomicInteger();

	public void reset() {
		System.out.println("RESET STATS...");
		openedCon.set(0);
		closedCon.set(0);
	}

	public void connect() {
		openedCon.incrementAndGet();
	}

	public void disconnect() {
		closedCon.incrementAndGet();
	}

	public int openConnections() {
		return openedCon.get();
	}

	public int closedConnections() {
		return closedCon.get();
	}

}
