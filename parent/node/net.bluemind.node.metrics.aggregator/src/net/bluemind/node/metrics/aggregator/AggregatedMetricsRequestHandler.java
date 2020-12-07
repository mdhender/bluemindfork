/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.node.metrics.aggregator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.streams.Pump;

public class AggregatedMetricsRequestHandler implements Handler<HttpServerRequest> {

	private static final Logger logger = LoggerFactory.getLogger(AggregatedMetricsRequestHandler.class);

	private final Path metricsPath;
	private final HttpClient httpClient;

	public AggregatedMetricsRequestHandler(Vertx vertx, Path path) {
		this.metricsPath = path;
		HttpClientOptions opts = new HttpClientOptions();
		opts.setConnectTimeout(1000).setIdleTimeout(1000);
		this.httpClient = vertx.createHttpClient(opts);
	}

	@Override
	public void handle(HttpServerRequest event) {
		try {
			List<SocketAddress> sockets = socketPaths();
			if (logger.isDebugEnabled()) {
				logger.debug("Request: {} with sockets {}", event, sockets);
			}
			HttpServerResponse resp = event.response();
			resp.setChunked(true);
			CompletableFuture<Void> root = CompletableFuture.completedFuture(null);
			RequestOptions unixSockReqOpts = new RequestOptions().setURI("/metrics");
			// let's aggregate all the unix sockets responses in one chunked response
			for (SocketAddress sock : sockets) {
				root = root.thenCompose(prev -> {
					CompletableFuture<Void> ret = new CompletableFuture<>();
					httpClient.request(HttpMethod.GET, sock, unixSockReqOpts, clientResponse -> {
						clientResponse.exceptionHandler(t -> {
							logger.error("Resp error with sock {}", sock, t);
							ret.complete(null);
						});
						Pump pump = Pump.pump(clientResponse, resp);
						clientResponse.endHandler(v -> ret.complete(null));
						pump.start();
					}).setTimeout(1000).exceptionHandler(t -> {
						logger.error("Req error with sock {}", sock, t);
						ret.complete(null);
					}).end();
					return ret;
				});
			}
			root.whenComplete((v, ex) -> resp.end());
		} catch (Exception e) {
			endRequest(event, e);
		}
	}

	private List<SocketAddress> socketPaths() throws IOException {
		if (!Files.isDirectory(metricsPath)) {
			logger.warn("{} is not a directory", metricsPath);
			return Collections.emptyList();
		}
		try (Stream<Path> stream = Files.walk(metricsPath, 1)) {
			return stream.map(Path::toFile).filter(f -> {
				String name = f.getName();
				return !f.isDirectory() && name.startsWith("metrics") && name.endsWith(".sock");
			}).map(f -> SocketAddress.domainSocketAddress(f.getAbsolutePath())).collect(Collectors.toList());
		}
	}

	private void endRequest(HttpServerRequest event, Throwable t) {
		logger.error(t.getMessage(), t);
		HttpServerResponse resp = event.response();
		if (resp.headWritten()) {
			return;
		}
		resp.setStatusCode(503).setStatusMessage(Optional.ofNullable(t.getMessage()).orElse("failed")).end();
	}

}
