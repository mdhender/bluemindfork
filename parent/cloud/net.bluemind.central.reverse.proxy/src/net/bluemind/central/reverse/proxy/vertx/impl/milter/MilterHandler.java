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

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.NetSocket;

public class MilterHandler implements Handler<NetSocket> {
	private static final Logger logger = LoggerFactory.getLogger(MilterHandler.class);

	private final Vertx vertx;
	private final int clientPort;
	private String deploymentId;

	private Optional<String> clientIp = Optional.empty();

	public MilterHandler(Vertx vertx, int clientPort) {
		this.vertx = vertx;
		this.clientPort = clientPort;
	}

	public void setDeploymentId(String deploymentId) {
		this.deploymentId = deploymentId;
	}

	public void setClientIp(String clientIp) {
		logger.info("[milter:{}] set client IP to {}", deploymentId, clientIp);
		this.clientIp = Optional.of(clientIp);
	}

	@Override
	public void handle(NetSocket serverSocket) {
		serverSocket.pause();

		clientIp.ifPresentOrElse(clientIp -> connectClient(serverSocket, clientIp), () -> {
			logger.warn("[milter:{}] core server IP not set", deploymentId);
			serverSocket.close();
		});
	}

	private void connectClient(NetSocket serverSocket, String clientIp) {
		NetClientOptions netOptions = new NetClientOptions().setIdleTimeout(10).setIdleTimeoutUnit(TimeUnit.MINUTES)
				.setTcpFastOpen(true).setTcpNoDelay(true).setTcpQuickAck(true);
		NetClient netClient = vertx.createNetClient(netOptions);
		netClient.connect(clientPort, clientIp).onComplete(asyncRemoteSocket -> {
			if (asyncRemoteSocket.failed()) {
				logger.error("[milter:{}] Unable to connect to {}:{}", deploymentId, clientIp, clientPort,
						asyncRemoteSocket.cause());
				serverSocket.close();
				return;
			}

			logger.debug("[milter:{}] Proxy client connected to {}:{}", deploymentId, clientIp, clientPort);
			NetSocket clientSocket = asyncRemoteSocket.result();

			serverSocket.closeHandler(ar -> {
				netClient.close();
				clientSocket.close();
			});

			clientSocket.closeHandler(ar -> {
				netClient.close();
				serverSocket.close();
			});

			serverSocket.pipeTo(clientSocket);
			clientSocket.pipeTo(serverSocket);
			serverSocket.resume();
		});
	}
}
