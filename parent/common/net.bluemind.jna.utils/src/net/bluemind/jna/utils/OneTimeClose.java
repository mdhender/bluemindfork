/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2024
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
package net.bluemind.jna.utils;

import java.util.concurrent.atomic.AtomicBoolean;

public final class OneTimeClose implements Runnable {

	private final AtomicBoolean done = new AtomicBoolean(false);
	private final Runnable delegate;
	private final String name;

	public OneTimeClose(String name, Runnable delegate) {
		this.name = name;
		this.delegate = delegate;
	}

	public String name() {
		return name;
	}

	@Override
	public void run() {
		if (done.compareAndSet(false, true)) {
			delegate.run();
		}
	}

	public boolean isClosed() {
		return done.get();
	}

}
