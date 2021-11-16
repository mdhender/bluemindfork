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
package net.bluemind.eas.wbxml.builder.vertx;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import net.bluemind.eas.dto.base.DisposableByteSource;
import net.bluemind.vertx.common.LocalJsonObject;

public class ByteSourceEventProducer extends AbstractVerticle {

	public static final String REGISTER = "wbxml.bytesource.register";
	public static final String NEXT_CHUNK = "wbxml.stream.next.chunk";

	private final Map<String, InputStream> liveStreams = new ConcurrentHashMap<>();
	private final Map<String, DisposableByteSource> disposables = new ConcurrentHashMap<>();
	private final AtomicLong streamId = new AtomicLong(0);
	private final NextStreamChunkRequestHandler nextHandler = new NextStreamChunkRequestHandler();
	private static final Logger logger = LoggerFactory.getLogger(ByteSourceEventProducer.class);

	@Override
	public void start() {
		vertx.eventBus().consumer(REGISTER, (Message<LocalJsonObject<DisposableByteSource>> event) -> {
			DisposableByteSource dbs = event.body().getValue();
			String streamAddr = "stream" + streamId.incrementAndGet();
			try {
				InputStream input = dbs.source().openStream();
				liveStreams.put(streamAddr, input);
				disposables.put(streamAddr, dbs);
				if (logger.isDebugEnabled()) {
					logger.debug("{} stream {} registered {}", this, streamAddr, input);
				}
				event.reply(streamAddr);
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
			}
		});

		vertx.eventBus().consumer(NEXT_CHUNK, nextHandler);

	}

	private final class NextStreamChunkRequestHandler implements Handler<Message<String>> {

		private final LocalJsonObject<Chunk> LAST = new LocalJsonObject<>(Chunk.LAST);

		@Override
		public void handle(Message<String> event) {
			String streamId = event.body();
			logger.debug("Chunk request for stream {}", streamId);
			InputStream in = liveStreams.get(streamId);
			if (in == null) {
				logger.error("{} ************ Stream {} is unknown", this, streamId);
				event.reply(LAST);
				return;
			}
			byte[] output = new byte[65536];
			try {
				int read = in.read(output);
				if (read == -1) {
					liveStreams.remove(streamId).close();
					disposables.remove(streamId).dispose();
					event.reply(LAST);
					if (logger.isDebugEnabled()) {
						logger.debug("Sending last chunk for stream {}.", streamId);
					}
				} else {
					Chunk c = new Chunk();
					c.buf = Arrays.copyOf(output, read);
					event.reply(new LocalJsonObject<Chunk>(c));
					logger.debug("Sent {} byte(s) chunk.", read);
				}
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
			}

		}

	}

}
