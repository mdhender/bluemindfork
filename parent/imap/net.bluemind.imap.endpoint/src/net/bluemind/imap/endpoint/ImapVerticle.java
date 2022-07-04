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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.imap.endpoint;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.typesafe.config.Config;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Verticle;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;
import net.bluemind.lib.vertx.IVerticleFactory;

public class ImapVerticle extends AbstractVerticle {

	private static final Logger logger = LoggerFactory.getLogger(ImapVerticle.class);

	public static class EndpointFactory implements IVerticleFactory {

		@Override
		public boolean isWorker() {
			return false;
		}

		@Override
		public Verticle newInstance() {
			return new ImapVerticle();
		}
	}

	@Override
	public void start(Promise<Void> startPromise) throws Exception {
		Config conf = EndpointConfig.get();
		int idle = (int) conf.getDuration("imap.idle-timeout", TimeUnit.SECONDS);
		NetServerOptions opts = new NetServerOptions();
		opts.setIdleTimeout(idle).setIdleTimeoutUnit(TimeUnit.SECONDS);
		opts.setTcpFastOpen(true).setTcpNoDelay(true).setTcpQuickAck(true);
		NetServer srv = vertx.createNetServer(opts);

		srv.exceptionHandler(t -> logger.error("ImapEndpoint failure", t));

		int port = conf.getInt("imap.port");
		srv.connectHandler(ns -> ImapSession.create(vertx, ns)).listen(port, ar -> {
			if (ar.failed()) {
				logger.error("Failed to listen on port {}", port, ar.cause());
				startPromise.fail(ar.cause());
			} else {
				logger.info("Listening on port {}", port);
				startPromise.complete();
			}
		});

	}

}
