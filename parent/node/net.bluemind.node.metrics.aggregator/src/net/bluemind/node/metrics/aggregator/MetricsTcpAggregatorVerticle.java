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

import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Verticle;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import net.bluemind.lib.vertx.IVerticleFactory;

public class MetricsTcpAggregatorVerticle extends AbstractVerticle {

	public static class Factory implements IVerticleFactory {

		@Override
		public boolean isWorker() {
			return false;
		}

		@Override
		public Verticle newInstance() {
			return new MetricsTcpAggregatorVerticle();
		}

	}

	public static final String METRICS_SOCK_DIR = System.getProperty(SystemProps.SOCKET_DIR_PROP,
			"/var/run/bm-metrics/");

	private static final Logger logger = LoggerFactory.getLogger(MetricsTcpAggregatorVerticle.class);

	@Override
	public void start(Promise<Void> start) {
		HttpServerOptions opts = new HttpServerOptions().setTcpNoDelay(true).setAcceptBacklog(1024)
				.setReuseAddress(true);

		HttpServer srv = vertx.createHttpServer(opts);
		srv.requestHandler(new AggregatedMetricsRequestHandler(vertx, Paths.get(METRICS_SOCK_DIR)));
		srv.listen(8019, "127.0.0.1", ar -> {
			if (ar.failed()) {
				logger.error(ar.cause().getMessage(), ar.cause());
				start.fail(ar.cause());
			} else {
				start.complete();
			}
		});

	}

}
