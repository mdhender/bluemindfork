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
package net.bluemind.milter.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Future;
import org.vertx.java.core.net.NetServer;
import org.vertx.java.platform.Verticle;

import net.bluemind.lib.vertx.IVerticleFactory;

public class MilterMainVerticle extends Verticle {

	private static final Logger logger = LoggerFactory.getLogger(MilterMainVerticle.class);

	public static class Factory implements IVerticleFactory {

		@Override
		public boolean isWorker() {
			return false;
		}

		@Override
		public Verticle newInstance() {
			return new MilterMainVerticle();
		}

	}

	public void start(Future<Void> start) {
		NetServer srv = vertx.createNetServer();
		srv.setUsePooledBuffers(true).setTCPNoDelay(true).setTCPKeepAlive(true);

		srv.connectHandler(socket -> {
			MilterSession session = new MilterSession(vertx, socket);
			session.start();
		});
		srv.listen(2500, ar -> {
			if (ar.succeeded()) {
				logger.info("Milter verticle listening.");
				start.setResult(null);
			} else {
				start.setFailure(ar.cause());
			}
		});

	}

}
