/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.pop3.endpoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.typesafe.config.Config;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Verticle;
import net.bluemind.lib.vertx.IVerticleFactory;

public class Pop3Verticle extends AbstractVerticle {

	public static class Pop3Factory implements IVerticleFactory {

		@Override
		public boolean isWorker() {
			return false;
		}

		@Override
		public Verticle newInstance() {
			return new Pop3Verticle();
		}

	}

	private static final Logger logger = LoggerFactory.getLogger(Pop3Verticle.class);

	@Override
	public void start(Promise<Void> startPromise) throws Exception {

		Config conf = Pop3Config.get();

		int port = conf.getInt("pop3.port");

		logger.info("pop3 started");
		vertx.createNetServer().connectHandler(socket -> {
			Pop3Session session = new Pop3Session(vertx, socket);
			session.start();
		}).listen(port, ar -> {
			if (ar.failed()) {
				logger.error("Problem", ar.cause());
				startPromise.fail(ar.cause());
			} else {
				logger.info("{} listening", ar.result());
				startPromise.complete();
			}
		});

	}

}
