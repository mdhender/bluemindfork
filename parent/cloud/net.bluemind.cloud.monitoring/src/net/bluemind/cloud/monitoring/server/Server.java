/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.cloud.monitoring.server;

import static net.bluemind.cloud.monitoring.server.MonitoringConfig.Monitoring.PORT;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.typesafe.config.Config;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import net.bluemind.central.reverse.proxy.model.common.kafka.KafkaAdminClient;

public class Server extends AbstractVerticle {

	private final Config config;
	private static final Logger logger = LoggerFactory.getLogger(Server.class);

	public Server(Config config) {
		this.config = config;
	}

	@Override
	public void start(Promise<Void> p) {
		logger.info("Starting server");
		HttpServerOptions opts = new HttpServerOptions();
		opts.setAcceptBacklog(1024);
		opts.setTcpNoDelay(true);
		opts.setTcpKeepAlive(true);
		opts.setReuseAddress(true);
		opts.setTcpFastOpen(true);

		HttpServer srv = vertx.createHttpServer(opts);

		KafkaAdminClient adminClient = KafkaAdminClient
				.create(config.getString(MonitoringConfig.Kafka.BOOTSTRAP_SERVERS));

		Handler<HttpServerRequest> requestHandler = MonitoringRouter.create(vertx, config, adminClient);
		srv.requestHandler(requestHandler).listen(config.getInt(PORT), new Handler<AsyncResult<HttpServer>>() {

			@Override
			public void handle(AsyncResult<HttpServer> event) {
				if (event.succeeded()) {
					logger.info("{} Listening", this);
				} else {
					p.fail(event.cause());
				}
			}
		});
		p.complete();
	}

}
