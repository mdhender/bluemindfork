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
package net.bluemind.lmtp.impl;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Files;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;
import io.vertx.core.net.NetSocket;
import net.bluemind.metrics.registry.MetricsRegistry;
import net.bluemind.system.api.SystemState;

public class LmtpProxyVerticle extends AbstractVerticle {

	private static final Logger logger = LoggerFactory.getLogger(LmtpProxyVerticle.class);

	private static class HostPort {

		public final String host;
		public final int port;

		public HostPort(String host, int port) {
			this.host = host;
			this.port = port;
		}

	}

	private static final HostPort LMTP_HOST = cyrusLmtpHost();

	private static final HostPort cyrusLmtpHost() {
		File f = new File(System.getProperty("user.home") + "/lmtpd.debug");
		String host = "127.0.0.1";
		int port = 24;
		if (f.exists()) {
			try {
				host = Files.asCharSource(f, StandardCharsets.US_ASCII).readFirstLine();
				if (host.indexOf(':') >= 0) {
					String[] splitted = host.split(":");
					host = splitted[0];
					port = Integer.parseInt(splitted[1]);
				}
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
			}
		}
		logger.info("Will proxy to {}:{}", host, port);
		return new HostPort(host, port);
	}

	private NetClient netClient;

	private LmtpConfig config;

	@Override
	public void start(Promise<Void> startFuture) {
		try {
			config = new LmtpConfig();

			netClient = vertx.createNetClient(
					new NetClientOptions().setTcpNoDelay(true).setReuseAddress(true).setTcpKeepAlive(true));

			NetServer srv = vertx.createNetServer(new NetServerOptions().setTcpNoDelay(true).setReuseAddress(true)
					.setTcpKeepAlive(true).setAcceptBacklog(4096));

			srv.connectHandler(onConnect());

			int port = 2400;
			srv.listen(port, listenHandler(startFuture));
		} catch (Exception t) {
			logger.error(t.getMessage(), t);
		}

	}

	private Handler<NetSocket> onConnect() {
		return (NetSocket socket) -> {
			socket.exceptionHandler(exceptionHandler(socket));
			logger.info("connect from {}, initialize backend connection ", socket.remoteAddress());
			if (CoreStateListener.state == SystemState.CORE_STATE_RUNNING) {
				initiateProxySession(socket);
			} else {
				logger.warn("Core is not running, refusing LMTP proxy connection");
				socket.close();
			}
		};

	}

	protected Handler<Throwable> exceptionHandler(NetSocket socket) {
		return (Throwable throwable) -> {
			logger.error("error during handling socket {}", socket, throwable);
			socket.close();
		};
	}

	protected void initiateProxySession(final NetSocket socket) {
		socket.pause();
		netClient.connect(LMTP_HOST.port, LMTP_HOST.host, (AsyncResult<NetSocket> event) -> {
			if (event.succeeded()) {
				logger.debug("connected to {}", LMTP_HOST);
				NetSocket backend = event.result();
				LmtpSessionProxy proxy = new LmtpSessionProxy(MetricsRegistry.get(), vertx.eventBus(), socket, backend,
						config);

				proxy.start();
			} else {
				logger.error("error during connecting to lmtp backend. LMTP_HOST: {}", LMTP_HOST, event.cause());
				socket.close();
			}
		});

	}

	private Handler<AsyncResult<NetServer>> listenHandler(Promise<Void> start) {
		return (AsyncResult<NetServer> event) -> {
			logger.info("listen, success: {}", event.succeeded());
			if (event.succeeded()) {
				start.complete(null);

			} else {
				start.fail(event.cause());
			}
		};
	}
}
