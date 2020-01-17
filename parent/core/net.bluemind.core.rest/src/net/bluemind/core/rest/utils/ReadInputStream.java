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
package net.bluemind.core.rest.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.ReadStream;

public class ReadInputStream extends InputStream {

	private BlockingDeque<ByteBuf> queue;
	private ReadStream<Buffer> inputStream;
	private AtomicBoolean paused = new AtomicBoolean(false);
	private static final int MAX_QUEUE_SIZE = 100;
	private static final int QUEUE_RESUME_SIZE = 20;

	Logger logger = LoggerFactory.getLogger(ReadInputStream.class);

	private ByteBuf currentBuff;
	public Exception exception;

	public ReadInputStream(ReadStream<Buffer> inputStream) {
		this.inputStream = inputStream;
		queue = new LinkedBlockingDeque<>();
		this.inputStream.endHandler(endHandle -> {
			queue.offerLast(Unpooled.buffer());
		});
		this.inputStream.handler(handleBuffer -> {

			ByteBuf byteBuf = handleBuffer.getByteBuf();
			queue.offerLast(byteBuf);
			checkQueueSize();
		});
		inputStream.resume();
	}

	@Override
	public int read(byte[] b) throws IOException {
		return read(b, 0, b.length);
	}

	@Override
	public int read(byte[] arr, int off, int len) throws IOException {

		ByteBuf b = currentBuffer();
		if (b == null) {
			return -1;
		}
		int r = Math.min(len, b.readableBytes());

		beforeRead(r);

		b.readBytes(arr, off, r);

		checkQueueSize();
		return r;
	}

	protected void beforeRead(int bytesToRead) throws IOException {

	}

	@Override
	public int read() throws IOException {

		ByteBuf b = currentBuffer();
		if (b == null) {
			return -1;
		}

		checkQueueSize();
		return b.readByte();

	}

	private void checkQueueSize() {
		if (this.paused.get() && queue.size() < QUEUE_RESUME_SIZE) {
			this.paused.set(false);
			inputStream.resume();
		} else {
			if (!this.paused.get() && queue.size() > MAX_QUEUE_SIZE) {
				inputStream.pause();
				this.paused.set(true);
			}
		}
	}

	private ByteBuf currentBuffer() throws IOException {

		if (currentBuff == null || currentBuff.readableBytes() == 0) {
			if (queue == null) {
				return null;
			} else {
				try {
					currentBuff = queue.poll(10000, TimeUnit.MILLISECONDS);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new IOException(e);
				}
				if (currentBuff != null) {
					currentBuff.resetReaderIndex().resetWriterIndex();
					if (currentBuff.readableBytes() == 0) {
						currentBuff = null;
						queue = null;
					}
				}
			}
		}

		return currentBuff;

	}

	@Override
	public void close() throws IOException {
		logger.info("Server is closing the connection");
		super.close();
	}

	@Override
	public int available() throws IOException {
		ByteBuf b = currentBuffer();
		if (b == null) {
			return 0;
		} else {
			return b.readableBytes();
		}
	}

}
