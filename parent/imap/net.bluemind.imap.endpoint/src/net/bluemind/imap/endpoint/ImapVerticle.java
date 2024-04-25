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
import io.vertx.core.Context;
import io.vertx.core.Promise;
import io.vertx.core.Verticle;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;
import net.bluemind.configfile.imap.ImapConfig;
import net.bluemind.lib.vertx.ContextNetSocket;
import net.bluemind.lib.vertx.IVerticleFactory;
import net.bluemind.lib.vertx.VertxContext;
import net.bluemind.system.api.SystemState;
import net.bluemind.system.state.StateContext;

public class ImapVerticle extends AbstractVerticle {

	private static final Logger logger = LoggerFactory.getLogger(ImapVerticle.class);
	private static final ImapMetricsHolder metricsHolder = ImapMetricsHolder.get();

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
	public void start(Promise<Void> startPromise) {
		Config conf = EndpointConfig.get();
		int idle = (int) conf.getDuration(ImapConfig.IDLE_TIMEOUT, TimeUnit.SECONDS);
		NetServerOptions opts = new NetServerOptions();
		opts.setIdleTimeout(idle).setIdleTimeoutUnit(TimeUnit.SECONDS);
		opts //
				.setTcpFastOpen(true)//
				.setTcpNoDelay(conf.getBoolean(ImapConfig.TCP_NODELAY))//
				.setTcpQuickAck(true)//
				.setTcpCork(conf.getBoolean(ImapConfig.TCP_CORK))//
		;
		opts.setRegisterWriteHandler(true);
		opts.setUseProxyProtocol(conf.getBoolean(ImapConfig.PROXY_PROTOCOL));
		NetServer srv = vertx.createNetServer(opts);

		srv.exceptionHandler(t -> logger.error("ImapEndpoint failure", t));

		int port = conf.getInt(ImapConfig.PORT);
		srv.connectHandler(ns -> {

			var curState = StateListener.state();
			var inCore = StateContext.getState();
			curState = inCore == SystemState.CORE_STATE_RUNNING ? inCore : curState;

			if (curState != SystemState.CORE_STATE_RUNNING) {
				logger.debug("Invalid state {}, rejecting for now", curState);
				vertx.setTimer(2000, tid -> ns.close());
				return;
			}

			Context context = VertxContext.getOrCreateDuplicatedContext(vertx);
			context.runOnContext(
					v -> ImapSession.create(vertx, context, new ContextNetSocket(context, ns), metricsHolder));
		}).listen(port, ar -> {
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
