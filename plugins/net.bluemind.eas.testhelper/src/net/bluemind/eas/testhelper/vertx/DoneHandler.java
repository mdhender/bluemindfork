/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
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
package net.bluemind.eas.testhelper.vertx;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;

public class DoneHandler<T> implements Handler<AsyncResult<T>> {

	private final CountDownLatch cdl;
	private Set<T> value;

	public DoneHandler(int steps) {
		cdl = new CountDownLatch(steps);
		value = new HashSet<>();
	}

	@Override
	public void handle(AsyncResult<T> event) {
		if (event.succeeded()) {
			T result = event.result();
			if (result != null) {
				value.add(event.result());
			}
			cdl.countDown();

		} else {
			event.cause().printStackTrace();
		}
	}

	public Set<T> waitForIt() {
		Wait.forIt(cdl);
		return value;
	}

}
