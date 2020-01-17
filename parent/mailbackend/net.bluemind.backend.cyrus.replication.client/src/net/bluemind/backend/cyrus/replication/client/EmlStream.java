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
package net.bluemind.backend.cyrus.replication.client;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.util.internal.ThreadLocalRandom;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.ReadStream;
import net.bluemind.lib.vertx.VertxPlatform;

public class EmlStream implements ReadStream<Buffer> {

	private static final Logger logger = LoggerFactory.getLogger(EmlStream.class);

	private String partition;
	private byte[] rand;
	private Handler<Buffer> dh;
	private int remaining;
	private boolean paused;
	private Handler<Void> end;

	public EmlStream(int count, int size, String partition) {
		this.partition = partition;
		this.rand = new byte[size];
		ThreadLocalRandom.current().nextBytes(rand);
		this.remaining = count;
	}

	@Override
	public EmlStream handler(Handler<Buffer> handler) {
		this.dh = handler;
		return this;
	}

	@Override
	public EmlStream pause() {
		paused = true;
		return this;
	}

	@Override
	public EmlStream resume() {
		paused = false;
		VertxPlatform.getVertx().setTimer(1, tid -> {
			logger.info("Pump data...");
			pumpData();
		});
		return this;
	}

	private void pumpData() {
		while (remaining > 0) {
			logger.info("writing... {} to {}", remaining, dh);
			String prefix = "%{" + partition + " " + UUID.randomUUID().toString().replace("-", "") + " " + rand.length
					+ "}\r\n";
			Buffer toWrite = Buffer.buffer();
			toWrite.appendBuffer(Buffer.buffer(prefix));
			toWrite.appendBuffer(Buffer.buffer(rand));
			if (--remaining > 0) {
				toWrite.appendString(" ");
			}
			dh.handle(toWrite);
			if (paused) {
				return;
			}
		}
		end.handle(null);
	}

	@Override
	public EmlStream exceptionHandler(Handler<Throwable> handler) {
		return this;
	}

	@Override
	public EmlStream endHandler(Handler<Void> endHandler) {
		this.end = endHandler;
		return this;
	}

	@Override
	public ReadStream<Buffer> fetch(long amount) {
		return this;
	}

}
