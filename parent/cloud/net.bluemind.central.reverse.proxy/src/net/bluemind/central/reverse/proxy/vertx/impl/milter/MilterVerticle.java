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
package net.bluemind.central.reverse.proxy.vertx.impl.milter;

import static net.bluemind.central.reverse.proxy.common.ProxyEventBusAddress.ADDRESS;
import static net.bluemind.central.reverse.proxy.common.ProxyEventBusAddress.CORE_IP;
import static net.bluemind.central.reverse.proxy.common.ProxyEventBusAddress.MODEL_READY_NAME;
import static net.bluemind.central.reverse.proxy.common.config.CrpConfig.Milter.PORT;
import static net.bluemind.central.reverse.proxy.common.config.CrpConfig.Milter.REMOTE_PORT;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.typesafe.config.Config;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;

public class MilterVerticle extends AbstractVerticle {
	private static final Logger logger = LoggerFactory.getLogger(MilterVerticle.class);

	private final Config config;
	private final int port;
	private String clientIp = "172.16.12.220";

	private NetServer server;

	private MilterHandler milterHandler;

	public MilterVerticle(Config config) {
		this.config = config;
		this.port = config.getInt(PORT);
	}

	@Override
	public void start(Promise<Void> startPromise) {
		logger.info("[milter:{}] Starting", deploymentID());

		vertx.eventBus().<JsonObject>consumer(ADDRESS).handler(event -> {
			if (MODEL_READY_NAME.equals(event.headers().get("action"))) {
				logger.info("[milter:{}] Model ready, starting verticle instance milter", deploymentID());
				startMilterServer();
			} else if (CORE_IP.equals(event.headers().get("action"))) {
				String ip = event.body().getString("ip");
				if (ip == null) {
					return;
				}

				getOrInitMilterHandler().setClientIp(ip);
			}
		});
		startPromise.complete();

	}

	private MilterHandler getOrInitMilterHandler() {
		if (milterHandler == null) {
			milterHandler = new MilterHandler(vertx, config.getInt(REMOTE_PORT));
			milterHandler.setDeploymentId(deploymentID());
			milterHandler.setClientIp(clientIp);
		}

		return milterHandler;
	}

	private void startMilterServer() {
		NetServerOptions opts = new NetServerOptions();
		opts.setIdleTimeout(1).setIdleTimeoutUnit(TimeUnit.HOURS);
		opts.setTcpFastOpen(true).setTcpNoDelay(true).setTcpQuickAck(true);

		server = vertx.createNetServer(opts);
		server.exceptionHandler(t -> logger.error("[milter:{}] failure", deploymentID(), t));

		server.connectHandler(getOrInitMilterHandler()).listen(port, "127.0.0.1")
				.onSuccess(ns -> logger.info("[milter:{}] Started on port {}", deploymentID(), config.getInt(PORT)))
				.onFailure(t -> logger.error("[milter:{}] Failed to listen on port {}", deploymentID(), port, t));
	}
}
