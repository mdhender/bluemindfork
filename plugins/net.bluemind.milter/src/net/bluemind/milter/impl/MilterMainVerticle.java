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

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Verticle;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;
import net.bluemind.lib.vertx.IVerticleFactory;

public class MilterMainVerticle extends AbstractVerticle {

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

	@Override
	public void start(Promise<Void> start) {
		NetServer srv = vertx.createNetServer(new NetServerOptions().setTcpNoDelay(true).setTcpKeepAlive(true)
				.setTcpFastOpen(true).setTcpQuickAck(true));

		srv.connectHandler(socket -> {
			MilterSession session = new MilterSession(socket);
			session.start();
		});
		srv.listen(2500, ar -> {
			if (ar.succeeded()) {
				logger.info("Milter verticle listening on {}.", 2500);
				start.complete();
			} else {
				start.fail(ar.cause());
			}
		});

	}

}
