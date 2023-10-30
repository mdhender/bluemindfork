/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
  *
  * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License)
  * or the CeCILL as published by CeCILL.info (version 2 of the License).
  *
  * There are special exceptions to the terms and conditions of the
  * licenses as they are applied to this program. See LICENSE.txt in
  * the directory of this program distribution.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.central.reverse.proxy.vertx.impl.postfix;

import static net.bluemind.central.reverse.proxy.common.ProxyEventBusAddress.ADDRESS;
import static net.bluemind.central.reverse.proxy.common.ProxyEventBusAddress.MODEL_READY_NAME;
import static net.bluemind.central.reverse.proxy.common.config.CrpConfig.PostfixMaps.PORT;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.typesafe.config.Config;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;
import io.vertx.core.parsetools.RecordParser;

public class PostfixMapsVerticle extends AbstractVerticle {
	private static final Logger logger = LoggerFactory.getLogger(PostfixMapsVerticle.class);

	private final Config config;

	public PostfixMapsVerticle(Config config) {
		this.config = config;
	}

	@Override
	public void start(Promise<Void> startPromise) {
		logger.info("[postfix-maps:{}] Starting", deploymentID());

		vertx.eventBus().<JsonObject>consumer(ADDRESS).handler(event -> {
			if (MODEL_READY_NAME.equals(event.headers().get("action"))) {
				logger.info("[postfix-maps:{}] Model ready, starting verticle instance postfix-maps", deploymentID());
				startPostfixMaps(startPromise);
				logger.info("[postfix-maps:{}] Started on port {}", deploymentID(), config.getInt(PORT));
			}
		});
		startPromise.complete();
	}

	private void startPostfixMaps(Promise<Void> startPromise) {
		NetServerOptions opts = new NetServerOptions();
		opts.setIdleTimeout(1).setIdleTimeoutUnit(TimeUnit.HOURS);
		opts.setTcpFastOpen(true).setTcpNoDelay(true).setTcpQuickAck(true);
		opts.setRegisterWriteHandler(true);

		NetServer server = vertx.createNetServer(opts);
		server.exceptionHandler(t -> logger.error("[postfix-maps:{}] failure", deploymentID(), t));

		int port = config.getInt(PORT);

		server.connectHandler(socket -> {
			PostfixMapsHandler postfixMapsHandler = new PostfixMapsHandler(vertx, socket);
			socket.handler(RecordParser.newDelimited(",", postfixMapsHandler));
		}).listen(port, "127.0.0.1")
				.onFailure(t -> logger.error("[postfix-maps:{}] Failed to listen on port {}", deploymentID(), port, t));
	}
}
