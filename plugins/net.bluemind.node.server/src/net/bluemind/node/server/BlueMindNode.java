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
package net.bluemind.node.server;

import java.io.File;
import java.nio.channels.ClosedChannelException;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.ClientAuth;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.net.JksOptions;
import io.vertx.core.net.OpenSSLEngineOptions;
import net.bluemind.lib.vertx.RouteMatcher;
import net.bluemind.node.server.handlers.CheckFile;
import net.bluemind.node.server.handlers.DeleteFile;
import net.bluemind.node.server.handlers.Executions;
import net.bluemind.node.server.handlers.GetStatus;
import net.bluemind.node.server.handlers.Interrupt;
import net.bluemind.node.server.handlers.ListFiles;
import net.bluemind.node.server.handlers.ListMatches;
import net.bluemind.node.server.handlers.SendFile;
import net.bluemind.node.server.handlers.SubmitCommand;
import net.bluemind.node.server.handlers.WebSocketProcessHandler;
import net.bluemind.node.server.handlers.WriteFile;

public class BlueMindNode extends AbstractVerticle {

	private static final Logger logger = LoggerFactory.getLogger(BlueMindNode.class);

	private static final File serverJks = new File("/etc/bm/bm.jks");
	private static final File trustClientCert = new File("/etc/bm/nodeclient_truststore.jks");

	private HttpServer srv;

	// ugly hack to restart the server's in ssl mode
	private static List<BlueMindNode> selfRefs = new LinkedList<>();

	@Override
	public void start() {
		reconfigure();
		selfRefs.add(this);
	}

	private void reconfigure() {
		HttpServerOptions options = prepareOptions();
		this.srv = vertx.createHttpServer(prepareOptions());
		final RouteMatcher rm = createRouter(options.isSsl());
		srv.requestHandler((HttpServerRequest event) -> {
			logger.debug("{} {}...", event.method(), event.path());
			rm.handle(event);
		});
		srv.webSocketHandler(new WebSocketProcessHandler(vertx));
		logger.info("NODE is SSL: {}", options.isSsl());
		srv.exceptionHandler(t -> {
			if (t instanceof ClosedChannelException) {
				logger.debug("exceptionHandler {}", t.getMessage(), t);
			} else {
				logger.error("exceptionHandler {}", t.getMessage(), t);
			}
		});
		srv.listen(options.isSsl() ? Activator.NODE_PORT : 8021, ar -> {
			if (ar.failed()) {
				logger.error("Node failed to listen", ar.cause());
			}
		});
	}

	private HttpServerOptions prepareOptions() {
		HttpServerOptions options = new HttpServerOptions();
		options.setAcceptBacklog(1024).setReuseAddress(true);
		options.setTcpNoDelay(true);
		boolean ssl = serverJks.exists() && trustClientCert.exists();
		if (ssl) {
			options.setKeyStoreOptions(new JksOptions().setPath("/etc/bm/bm.jks").setPassword("bluemind"));
			options.setSsl(true);
			options.setTrustStoreOptions(
					new JksOptions().setPath("/etc/bm/nodeclient_truststore.jks").setPassword("password"));
			options.setClientAuth(ClientAuth.REQUIRED);
			options.setOpenSslEngineOptions(new OpenSSLEngineOptions());
			logger.info("Configured in secure mode");
		} else {
			logger.info("Unsecure mode on 8021, node can be claimed");
		}
		return options;
	}

	private static final String FS_OPS_RE = "/fs(/.*)";

	private RouteMatcher createRouter(boolean ssl) {
		RouteMatcher rm = new RouteMatcher(vertx);
		rm.post("/cmd", new SubmitCommand());
		rm.get("/cmd/:reqId", new GetStatus());
		rm.get("/cmd", new Executions());
		rm.delete("/cmd/:reqId", new Interrupt());
		rm.regex(HttpMethod.GET, FS_OPS_RE, new SendFile());
		rm.regex(HttpMethod.HEAD, FS_OPS_RE, new CheckFile());
		rm.regex(HttpMethod.PUT, FS_OPS_RE, new WriteFile());
		rm.regex(HttpMethod.DELETE, FS_OPS_RE, new DeleteFile());
		rm.regex(HttpMethod.GET, "/list(/.*)", new ListFiles());
		rm.regex(HttpMethod.GET, "/match/([^/]*)(/.*)", new ListMatches());
		rm.options("/", (HttpServerRequest event) -> {
			logger.info("{} / => OK", event.method());
			event.response().end();
		});
		if (ssl) {
			rm.options("/ping", (HttpServerRequest event) -> event.response().end());
		} else {
			plainTextPing(rm);
		}
		rm.noMatch((HttpServerRequest event) -> {
			logger.error("No match for {} {}", event.method(), event.path());
			event.response().setStatusCode(404).end();
		});
		return rm;
	}

	private void plainTextPing(RouteMatcher rm) {
		rm.options("/ping", (HttpServerRequest event) -> {
			if (serverJks.exists() && trustClientCert.exists()) {
				logger.info("Certs are here, time to secure and restart...");
				vertx.setTimer(100, tid -> {
					logger.info("Restarting all {} servers...", selfRefs.size());
					restartAllServers();
				});
				event.response().setStatusCode(201).end();
			} else {
				logger.warn("Ping on unsecure BUT certs are not there yet");
				event.response().setStatusCode(200).end();
			}
		});
	}

	private static void restartAllServers() {
		int size = selfRefs.size();
		logger.info("Will close {} unsecure servers.", size);
		for (BlueMindNode bmn : selfRefs) {
			final BlueMindNode theNode = bmn;
			theNode.srv.close();
			theNode.reconfigure();
		}
	}

	@Override
	public void stop() throws Exception {
		logger.info("Stopping {}", this);
		super.stop();
	}
}
