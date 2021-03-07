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
package net.bluemind.imap.vertx.connection;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.eventbus.MessageProducer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.ReadStream;
import io.vertx.core.streams.WriteStream;
import net.bluemind.imap.vertx.IConnectionSupport;
import net.bluemind.lib.vertx.Result;

public class EventBusConnectionSupport implements IConnectionSupport {

	private final EventBus eb;

	private final Vertx vertx;

	private static final DeliveryOptions delOpts = new DeliveryOptions().setSendTimeout(2000);

	public EventBusConnectionSupport(Vertx vertx) {
		this.vertx = vertx;
		this.eb = vertx.eventBus();
	}

	@Override
	public Vertx vertx() {
		return vertx;
	}

	@Override
	public void connect(int port, String host, Handler<AsyncResult<INetworkCon>> futureCon) {

		JsonObject infos = new JsonObject().put("host", host).put("port", port);
		eb.request("network.connect", infos, delOpts, (AsyncResult<Message<String>> conRes) -> {
			if (conRes.failed()) {
				futureCon.handle(Result.fail(conRes.cause()));
			} else {
				Message<String> forAck = conRes.result();
				String streamAddr = forAck.body();
				MessageConsumer<Buffer> readCons = eb.consumer(streamAddr + ".read");
				readCons.setMaxBufferedMessages(50);
				MessageProducer<Buffer> writeProd = eb.sender(streamAddr + ".write");
				writeProd.setWriteQueueMaxSize(10);
				INetworkCon nc = new INetworkCon() {

					@Override
					public WriteStream<Buffer> write() {
						return writeProd;
					}

					@Override
					public ReadStream<Buffer> read() {
						return readCons.bodyStream();
					}

					@Override
					public void close(Handler<AsyncResult<Void>> h) {
						eb.request(streamAddr + ".close", "close", delOpts, ar -> {
							h.handle(Result.success());
						});
					}

				};
				futureCon.handle(Result.success(nc));
				forAck.reply("OK");
			}
		});

	}

}
