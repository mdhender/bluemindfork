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
package net.bluemind.milter.impl;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.spectator.api.Registry;
import com.sendmail.jilter.JilterProcessor;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetSocket;
import net.bluemind.metrics.registry.IdFactory;
import net.bluemind.metrics.registry.MetricsRegistry;

public class MilterSession {

	private static final Logger logger = LoggerFactory.getLogger(MilterSession.class);
	private static final Registry registry = MetricsRegistry.get();
	private static final IdFactory idFactory = new IdFactory(MetricsRegistry.get(), MilterHandler.class);
	private final long start;
	private final NetSocket socket;
	private final JilterProcessor jp;
	private Buffer buffer;

	public MilterSession(NetSocket socket) {
		this.start = registry.clock().monotonicTime();
		this.socket = socket;
		MilterHandler handler = new MilterHandler(MLRegistry.getFactories());
		this.jp = new JilterProcessor(handler);
		buffer = Buffer.buffer();
	}

	public void start() {
		registry.counter(idFactory.name("connectionsCount")).increment();
		WritableByteChannel sink = new WritableByteChannel() {

			@Override
			public boolean isOpen() {
				return true;
			}

			@Override
			public void close() throws IOException {
				socket.close();
			}

			@Override
			public int write(ByteBuffer src) throws IOException {
				ByteBuf netty = Unpooled.wrappedBuffer(src);
				int size = netty.readableBytes();
				buffer.appendBuffer(Buffer.buffer(netty));
				doWrite();
				return size;
			}
		};

		socket.drainHandler((v) -> {
			doWrite();
		});
		socket.handler(buf -> {
			ByteBuf nettyBuffer = buf.getByteBuf();
			logger.debug("Process {}", nettyBuffer);
			ByteBuffer nioBuffer = nettyBuffer.nioBuffer();

			try {
				boolean ret = jp.process(sink, nioBuffer);
				logger.debug("processed: {}", ret);
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
				socket.close();
			}

		});
		socket.closeHandler(v -> {
			registry.timer(idFactory.name("sessionDuration")).record(registry.clock().monotonicTime() - start,
					TimeUnit.NANOSECONDS);
			logger.info("{} closed.", socket.writeHandlerID());
			stop();
		});
		logger.info("Session started for {}", socket.writeHandlerID());
	}

	public void doWrite() {
		if (!socket.writeQueueFull()) {
			Vertx.currentContext().runOnContext(v -> {
				socket.write(buffer);
				buffer = Buffer.buffer();
			});
		}
	}

	public void stop() {
		logger.info("{} stopped.", socket.writeHandlerID());
	}

}
