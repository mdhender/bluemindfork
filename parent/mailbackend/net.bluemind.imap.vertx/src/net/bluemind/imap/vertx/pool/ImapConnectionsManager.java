/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.imap.vertx.pool;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Verticle;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.eventbus.MessageProducer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.NetSocket;
import net.bluemind.lib.vertx.IVerticleFactory;

public class ImapConnectionsManager extends AbstractVerticle {

	private static Logger logger = LoggerFactory.getLogger(ImapConnectionsManager.class);

	public static class Factory implements IVerticleFactory {

		@Override
		public boolean isWorker() {
			return false;
		}

		@Override
		public Verticle newInstance() {
			return new ImapConnectionsManager();
		}

	}

	@Override
	public void start() {

		EventBus eb = vertx.eventBus();
		NetClient client = vertx.createNetClient(
				new NetClientOptions().setConnectTimeout(1000).setTcpQuickAck(true).setTcpNoDelay(true));

		eb.consumer("network.connect", (Message<JsonObject> msg) -> {
			JsonObject infos = msg.body();
			client.connect(infos.getInteger("port"), infos.getString("host"), conRes -> {
				if (conRes.failed()) {
					msg.fail(500, conRes.cause().getMessage());
				} else {
					String stream = UUID.randomUUID().toString();
					String disco = stream + ".close";
					NetSocket ns = conRes.result();
					MessageConsumer<Buffer> cons = eb.consumer(stream + ".write");
					cons.setMaxBufferedMessages(50);
					MessageProducer<Buffer> pub = eb.sender(stream + ".read");
					pub.setWriteQueueMaxSize(10);
					logger.info("EventBus streams setup on {} for {}", stream, ns);
					eb.consumer(disco, discoMsg -> {
						cons.unregister();
						pub.close();
						ns.close();
						discoMsg.reply("OK");
					});
					ns.pause();
					msg.replyAndRequest(stream, streamSetupOk -> {
						if (streamSetupOk.succeeded()) {
							ns.pipeTo(pub);
							cons.bodyStream().pipeTo(ns);
							ns.resume();
							logger.info("Event bus streaming is ok for address {}", stream);
						} else {
							logger.error("Setup error", streamSetupOk.cause());
						}
					});
				}
			});

		});

	}

}
