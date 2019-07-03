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
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Future;
import org.vertx.java.core.Handler;
import org.vertx.java.core.net.NetClient;
import org.vertx.java.core.net.NetServer;
import org.vertx.java.core.net.NetSocket;
import org.vertx.java.platform.Verticle;

import com.google.common.io.Files;

import net.bluemind.metrics.registry.MetricsRegistry;
import net.bluemind.system.api.SystemState;

public class LmtpProxyVerticle extends Verticle {

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
				if (host.indexOf(':') > 0) {
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
	public void start(Future<Void> startFuture) {
		try {
			config = new LmtpConfig();

			netClient = vertx.createNetClient();

			netClient.setTCPNoDelay(true);
			netClient.setTCPKeepAlive(true);
			netClient.setUsePooledBuffers(true);
			netClient.setReuseAddress(true);

			NetServer srv = vertx.createNetServer();
			srv.setAcceptBacklog(4096);
			srv.setTCPNoDelay(true);
			srv.setTCPKeepAlive(true);
			srv.setUsePooledBuffers(true);
			srv.setReuseAddress(true);

			srv.connectHandler(onConnect());

			int port = 2400;
			// logger.info("Will bind to port " + port);
			srv.listen(port, listenHandler(startFuture));
		} catch (Exception t) {
			logger.error(t.getMessage(), t);
		}

	}

	private Handler<NetSocket> onConnect() {

		return new Handler<NetSocket>() {

			@Override
			public void handle(final NetSocket socket) {
				socket.exceptionHandler(exceptionHandler(socket));
				logger.info("connect from {}, initialize backend connection ", socket.remoteAddress());
				if (CoreStateListener.state == SystemState.CORE_STATE_RUNNING) {
					initiateProxySession(socket);
				} else {
					logger.warn("Core is not running, refusing LMTP proxy connection");
					socket.close();
				}
			}
		};

	}

	protected Handler<Throwable> exceptionHandler(NetSocket socket) {
		return new Handler<Throwable>() {

			@Override
			public void handle(Throwable throwable) {
				logger.error("error during handling socket {}", socket, throwable);
				socket.close();
			}
		};
	}

	protected void initiateProxySession(final NetSocket socket) {
		socket.pause();
		netClient.connect(LMTP_HOST.port, LMTP_HOST.host, new Handler<AsyncResult<NetSocket>>() {

			@Override
			public void handle(AsyncResult<NetSocket> event) {
				if (event.succeeded()) {
					logger.debug("connected to {}", LMTP_HOST);
					NetSocket backend = event.result();
					LmtpSessionProxy proxy = new LmtpSessionProxy(MetricsRegistry.get(), getVertx().eventBus(), socket,
							backend, config);

					proxy.start();
				} else {

					// FIXME maybe we could send a message..
					logger.error("error during connecting to lmtp backend. LMTP_HOST: {}", LMTP_HOST, event.cause());
					socket.close();
				}
			}
		});

	}

	private Handler<AsyncResult<NetServer>> listenHandler(Future<Void> start) {
		return new Handler<AsyncResult<NetServer>>() {

			@Override
			public void handle(AsyncResult<NetServer> event) {
				logger.info("listen, success: " + event.succeeded());
				if (event.succeeded()) {
					start.setResult(null);
				} else {
					start.setFailure(event.cause());
				}
			}
		};
	}
}
