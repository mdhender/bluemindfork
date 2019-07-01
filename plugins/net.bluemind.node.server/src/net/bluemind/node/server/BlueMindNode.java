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
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.platform.Verticle;

import net.bluemind.lib.vertx.VertxPlatform;
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

public class BlueMindNode extends Verticle {

	private static final Logger logger = LoggerFactory.getLogger(BlueMindNode.class);

	private static final File serverJks = new File("/etc/bm/bm.jks");
	private static final File trustClientCert = new File("/etc/bm/nodeclient_truststore.jks");

	private HttpServer srv;

	// ugly hack to restart the server's in ssl mode
	private static List<BlueMindNode> selfRefs = new LinkedList<>();

	public BlueMindNode() {
	}

	public void start() {
		this.srv = vertx.createHttpServer();
		configureAndStart(srv);
		selfRefs.add(this);
	}

	private void configureAndStart(HttpServer srv) {
		srv.setAcceptBacklog(1024).setReuseAddress(true);
		srv.setTCPNoDelay(true);
		srv.setUsePooledBuffers(true);

		if (serverJks.exists() && trustClientCert.exists()) {
			srv.setKeyStorePath("/etc/bm/bm.jks").setKeyStorePassword("bluemind").setSSL(true);
			srv.setTrustStorePath("/etc/bm/nodeclient_truststore.jks").setTrustStorePassword("password")
					.setClientAuthRequired(true);
			logger.info("Configured in secure mode");
		} else {
			logger.info("Unsecure mode on 8021, node can be claimed");
		}

		final RouteMatcher rm = createRouter(srv);
		srv.requestHandler(new Handler<HttpServerRequest>() {

			@Override
			public void handle(HttpServerRequest event) {
				logger.debug("{} {}...", event.method(), event.path());
				rm.handle(event);
			}
		});
		srv.websocketHandler(new WebSocketProcessHandler(vertx));
		srv.listen(srv.isSSL() ? Activator.NODE_PORT : 8021);
	}

	private RouteMatcher createRouter(final HttpServer srv) {
		RouteMatcher rm = new RouteMatcher();
		rm.post("/cmd", new SubmitCommand());
		rm.get("/cmd/:reqId", new GetStatus());
		rm.get("/cmd", new Executions());
		rm.delete("/cmd/:reqId", new Interrupt());
		rm.getWithRegEx("/fs(/.*)", new SendFile());
		rm.putWithRegEx("/fs(/.*)", new WriteFile());
		rm.deleteWithRegEx("/fs(/.*)", new DeleteFile());
		rm.getWithRegEx("/list(/.*)", new ListFiles());
		rm.getWithRegEx("/match/([^/]*)(/.*)", new ListMatches());
		rm.options("/", new Handler<HttpServerRequest>() {

			@Override
			public void handle(HttpServerRequest event) {
				logger.info("OPTIONS / => OK");
				event.response().end();
			}
		});
		if (srv.isSSL()) {
			rm.options("/ping", new Handler<HttpServerRequest>() {

				@Override
				public void handle(HttpServerRequest event) {
					logger.info("PONG");
					event.response().end();
				}
			});
		} else {
			plainTextPing(rm);
		}
		rm.noMatch(new Handler<HttpServerRequest>() {

			@Override
			public void handle(HttpServerRequest event) {
				logger.error("No match for {} {}", event.method(), event.path());
				event.response().setStatusCode(404).end();
			}
		});
		return rm;
	}

	private void plainTextPing(RouteMatcher rm) {
		rm.options("/ping", new Handler<HttpServerRequest>() {

			@Override
			public void handle(HttpServerRequest event) {
				if (serverJks.exists() && trustClientCert.exists()) {
					logger.info("Certs are here, time to secure and restart...");
					vertx.setTimer(100, new Handler<Long>() {

						@Override
						public void handle(Long event) {
							logger.info("Restarting all servers...");
							restartAllServers();
						}
					});
					event.response().setStatusCode(201).end();
				} else {
					logger.warn("Ping on unsecure BUT certs are not there yet");
					event.response().setStatusCode(200).end();
				}
			}

		});
	}

	private static void restartAllServers() {
		int size = selfRefs.size();
		logger.info("Will close {} unsecure servers.", size);
		for (BlueMindNode bmn : selfRefs) {
			final BlueMindNode theNode = bmn;
			theNode.srv.close();
			theNode.srv = VertxPlatform.getVertx().createHttpServer();
			theNode.configureAndStart(theNode.srv);
		}
	}

	@Override
	public void stop() {
		logger.info("Stopping.");
		super.stop();
	}
}
