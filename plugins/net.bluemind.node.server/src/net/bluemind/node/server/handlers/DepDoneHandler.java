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
package net.bluemind.node.server.handlers;

import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

public class DepDoneHandler implements Handler<AsyncResult<String>> {

	private static final Logger logger = LoggerFactory.getLogger(DepDoneHandler.class);

	private final CountDownLatch cdl = new CountDownLatch(1);
	private String id;

	@Override
	public void handle(AsyncResult<String> ar) {
		if (ar.failed()) {
			Throwable cause = ar.cause();
			logger.error(cause.getMessage(), cause);
		}
		id = ar.result();
		logger.info("Deployement done with id: " + id);
		cdl.countDown();
	}

	public void await() throws InterruptedException {
		cdl.await();
	}

	public String id() {
		return id;
	}

}
