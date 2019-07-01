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

import org.vertx.java.busmods.BusModBase;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

import com.netflix.spectator.api.Counter;
import com.netflix.spectator.api.Registry;

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
public class DirNotificationsClusterProducer extends BusModBase {

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
		super.start();

		Registry reg = MetricsRegistry.get();
		IdFactory metricsId = new IdFactory("directory", reg, DirNotificationsClusterProducer.class);
		Counter sentEvents = reg.counter(metricsId.name("cluster.events"));
		Producer producer = MQ.getProducer(Topic.DIRECTORY_NOTIFICATIONS);

		Handler<Message<? extends JsonObject>> ebMessageHandler = (message) -> {
			OOPMessage cm = new OOPMessage(message.body());
			cm.putStringProperty("event", DirEventProducer.address);
			producer.send(cm);
			sentEvents.increment();
		};

		// throttle messages by domain, as we do for DomainBookVerticle
		ThrottleMessages<JsonObject> tm = new ThrottleMessages<JsonObject>((msg) -> msg.body().getString("domain"),
				ebMessageHandler, vertx, 2000);

		eb.registerHandler(DirEventProducer.address, (Message<JsonObject> msg) -> tm.handle(msg));
	}

}
