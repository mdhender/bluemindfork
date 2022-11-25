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

import java.nio.channels.ClosedChannelException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import net.bluemind.lib.vertx.RouteMatcher;
import net.bluemind.node.server.handlers.CheckFile;
import net.bluemind.node.server.handlers.DeleteFile;
import net.bluemind.node.server.handlers.Executions;
import net.bluemind.node.server.handlers.GetStatus;
import net.bluemind.node.server.handlers.Interrupt;
import net.bluemind.node.server.handlers.ListFiles;
import net.bluemind.node.server.handlers.ListMatches;
import net.bluemind.node.server.handlers.MakeDirs;
import net.bluemind.node.server.handlers.MoveFile;
import net.bluemind.node.server.handlers.SendFile;
import net.bluemind.node.server.handlers.SubmitCommand;
import net.bluemind.node.server.handlers.WebSocketProcessHandler;
import net.bluemind.node.server.handlers.WriteFile;

public abstract class BlueMindNode extends AbstractVerticle {

	protected static final Logger logger = LoggerFactory.getLogger(BlueMindNode.class);

	private HttpServer srv;

	@Override
	public void start() {
		configure();
	}

	protected abstract int getPort();

	protected abstract void options(HttpServerOptions options);

	protected abstract void router(RouteMatcher rm);

	private void configure() {
		HttpServerOptions options = prepareOptions();
		this.srv = vertx.createHttpServer(prepareOptions());
		final RouteMatcher rm = createRouter(options.isSsl());
		srv.requestHandler((HttpServerRequest event) -> {
			logger.debug("{} {}...", event.method(), event.path());
			rm.handle(event);
		});
		srv.webSocketHandler(new WebSocketProcessHandler(vertx));
		srv.exceptionHandler(t -> {
			if (t instanceof ClosedChannelException) {
				logger.debug("exceptionHandler {}", t.getMessage(), t);
			} else {
				logger.error("exceptionHandler {}", t.getMessage(), t);
			}
		});
		srv.listen(getPort(), ar -> {
			if (ar.failed()) {
				logger.error("Node failed to listen", ar.cause());
			}
		});
	}

	private HttpServerOptions prepareOptions() {
		HttpServerOptions options = new HttpServerOptions();
		options.setAcceptBacklog(1024).setReuseAddress(true);
		options.setTcpNoDelay(true);
		options(options);
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
		rm.post("/move", new MoveFile());
		rm.post("/mkdirs", new MakeDirs());
		rm.regex(HttpMethod.GET, "/match/([^/]*)(/.*)", new ListMatches());
		rm.options("/", (HttpServerRequest event) -> {
			logger.info("{} / => OK", event.method());
			event.response().end();
		});
		router(rm);
		rm.noMatch((HttpServerRequest event) -> {
			logger.error("No match for {} {}", event.method(), event.path());
			event.response().setStatusCode(404).end();
		});
		return rm;
	}

	@Override
	public void stop() throws Exception {
		logger.info("Stopping {}", this);
		super.stop();
	}
}
