/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2019
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
package net.bluemind.network.topology.producer;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import net.bluemind.config.InstallationId;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.Producer;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.network.topology.Topology;
import net.bluemind.network.topology.dto.TopologyPayload;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.server.hook.DefaultServerHook;

public class TopologyProducerHook extends DefaultServerHook {

	private static final Logger logger = LoggerFactory.getLogger(TopologyProducerHook.class);

	private final Vertx vertx;
	private Optional<Long> timerId;
	private final CompletableFuture<Producer> producerPromise;

	public TopologyProducerHook() {
		this.vertx = VertxPlatform.getVertx();
		this.timerId = Optional.empty();
		producerPromise = MQ.init().thenApply(v -> {
			Producer prod = MQ.registerProducer("topology.updates");
			MQ.registerConsumer("topology.requests", msg -> {
				String origin = msg.getStringProperty("origin");
				logger.info("Topology update requested from {}", origin);
				start();
			});
			return prod;
		});
		this.vertx.eventBus().consumer("topology.internal.startup", msg -> {
			logger.info("Topology startup event: {}", msg.body());
			start();
		});
	}

	private void start() {
		queueUpdate(ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM));
	}

	private synchronized void queueUpdate(IServiceProvider sp) {
		timerId.ifPresent(vertx::cancelTimer);
		timerId = Optional.of(vertx.setTimer(50, tid -> doUpdate(sp)));
	}

	private void doUpdate(IServiceProvider sp) {
		final long time = System.nanoTime();
		IServer serversApi = sp.instance(IServer.class, InstallationId.getIdentifier());
		List<ItemValue<Server>> allNodes = serversApi.allComplete();
		Topology.update(allNodes);
		producerPromise.thenAccept(prod -> {
			JsonObject toSend = new JsonObject(JsonUtils.asString(TopologyPayload.of(allNodes)));
			prod.send(toSend);
			long elapsed = System.nanoTime() - time;
			logger.info("Topology update with {} node(s) in {}ms.", allNodes.size(),
					TimeUnit.NANOSECONDS.toMillis(elapsed));
		});
	}

	@Override
	public void onServerCreated(BmContext context, ItemValue<Server> item) {
		queueUpdate(context.provider());
	}

	@Override
	public void onServerUpdated(BmContext context, ItemValue<Server> previousValue, Server value) {
		queueUpdate(context.provider());
	}

	@Override
	public void onServerDeleted(BmContext context, ItemValue<Server> itemValue) {
		queueUpdate(context.provider());
	}

}
