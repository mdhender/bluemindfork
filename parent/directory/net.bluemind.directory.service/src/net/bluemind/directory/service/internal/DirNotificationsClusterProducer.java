/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.directory.service.internal;

import com.netflix.spectator.api.Counter;
import com.netflix.spectator.api.Registry;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.Verticle;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import net.bluemind.directory.service.DirEventProducer;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.OOPMessage;
import net.bluemind.hornetq.client.Producer;
import net.bluemind.hornetq.client.Topic;
import net.bluemind.lib.vertx.IUniqueVerticleFactory;
import net.bluemind.lib.vertx.IVerticleFactory;
import net.bluemind.lib.vertx.utils.ThrottleMessages;
import net.bluemind.metrics.registry.IdFactory;
import net.bluemind.metrics.registry.MetricsRegistry;

/**
 * Forwards dir changed events to the cluster.
 * 
 * Known users include ysnp & milter for some cache invalidation purposes.
 *
 */
public class DirNotificationsClusterProducer extends AbstractVerticle {

	public static class Factory implements IVerticleFactory, IUniqueVerticleFactory {

		@Override
		public boolean isWorker() {
			return true;
		}

		@Override
		public Verticle newInstance() {
			return new DirNotificationsClusterProducer();
		}

	}

	public void start() {

		Registry reg = MetricsRegistry.get();
		IdFactory metricsId = new IdFactory("directory", reg, DirNotificationsClusterProducer.class);
		Counter sentEvents = reg.counter(metricsId.name("cluster.events"));
		Producer producer = MQ.getProducer(Topic.DIRECTORY_NOTIFICATIONS);

		Handler<Message<JsonObject>> ebMessageHandler = (message) -> {
			OOPMessage cm = new OOPMessage(message.body());
			cm.putStringProperty("event", DirEventProducer.address);
			producer.send(cm);
			sentEvents.increment();
		};

		// throttle messages by domain, as we do for DomainBookVerticle
		ThrottleMessages<JsonObject> tm = new ThrottleMessages<>((msg) -> msg.body().getString("domain"),
				ebMessageHandler, vertx, 2000);

		vertx.eventBus().consumer(DirEventProducer.address, (Message<JsonObject> msg) -> tm.handle(msg));
	}

}
