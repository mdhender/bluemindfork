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
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.ReadStream;
import net.bluemind.core.api.fault.ServerFault;

public class ReadInputStream extends InputStream {

	private static final Logger logger = LoggerFactory.getLogger(ReadInputStream.class);
	private static final int MAX_QUEUE_SIZE = 100;
	private static final int QUEUE_RESUME_SIZE = 20;

	private final BlockingDeque<ByteBufInputStream> queue;
	private final ReadStream<Buffer> inputStream;
	private AtomicBoolean paused = new AtomicBoolean(false);

	private ByteBufInputStream currentBuff;
	public final AtomicReference<IOException> exception = new AtomicReference<>();
	private AtomicBoolean ended = new AtomicBoolean();

	public ReadInputStream(ReadStream<Buffer> inputStream) {
		this.inputStream = inputStream;
		queue = new LinkedBlockingDeque<>();
		this.inputStream.endHandler(endHandle -> ended.set(true));
		this.inputStream.handler(handleBuffer -> {
			if (exception.get() != null) {
				// beforeRead might have set/thrown an exception (eg. SizeLimitedReadStream),
				// the vertx stream needs to be cut...
				// HttpServerRequestImpl:114 seems to miss
				// pending.exceptionHandler(context::reportException)
				throw new ServerFault(exception.get());
			}
			ByteBuf byteBuf = handleBuffer.getByteBuf();
			queue.offerLast(new ByteBufInputStream(byteBuf));
			checkQueueSize();
		});
		this.inputStream.exceptionHandler(ex -> {
			exception.set(ex instanceof IOException ? (IOException) ex : new IOException(ex)); // NOSONAR 4.8 compat
			ended.set(true);
		});
		inputStream.resume();
		logger.debug("created {}", this);
	}

	@Override
	public int read(byte[] b) throws IOException {
		return read(b, 0, b.length);
	}

	private void failIfStreamFailed() throws IOException {
		IOException ex = exception.get();
		if (ex != null) {
			throw ex;
		}
	}

	@Override
	public int read(byte[] arr, int off, int len) throws IOException {
		failIfStreamFailed();
		InputStream b = currentStream();
		if (b == null) {
			return -1;
		}
		int qty = Math.min(len, b.available());
		beforeRead(qty);
		int ret = b.read(arr, off, len);
		if (ret == -1 && !ended.get()) {
			ret = 0;
		}
		return ret;
	}

	protected void beforeRead(@SuppressWarnings("unused") int bytesToRead) throws IOException {
		// override if needed
	}

	@Override
	public int read() throws IOException {
		failIfStreamFailed();
		InputStream b = currentStream();
		if (b == null) {
			return -1;
		}
		beforeRead(1);
		int ret = b.read();
		if (ret == -1 && !ended.get()) {
			ret = 0;
		}
		return ret;

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

	private InputStream currentStream() throws IOException {
		if (currentBuff == null || currentBuff.available() == 0) {
			try {
				do {
					currentBuff = queue.poll(10, TimeUnit.MILLISECONDS);
					checkQueueSize();
				} while (currentBuff == null && !ended.get());
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}

		return currentBuff;

	}

	@Override
	public void close() throws IOException {
		// that's fine
	}

	@Override
	public int available() throws IOException {
		return queue.isEmpty() ? 0 : currentStream().available();
	}

}
