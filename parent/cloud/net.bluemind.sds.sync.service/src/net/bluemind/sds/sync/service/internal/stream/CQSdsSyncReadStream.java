/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
  *
  * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License)
  * or the CeCILL as published by CeCILL.info (version 2 of the License).
  *
  * There are special exceptions to the terms and conditions of the
  * licenses as they are applied to this program. See LICENSE.txt in
  * the directory of this program distribution.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.sds.sync.service.internal.stream;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBufUtil;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.ReadStream;
import net.bluemind.core.api.Stream;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.sds.sync.service.internal.queue.SdsSyncQueue;
import net.openhft.chronicle.queue.ExcerptTailer;

public class CQSdsSyncReadStream implements ReadStream<Buffer>, Stream {
	private static final Logger logger = LoggerFactory.getLogger(CQSdsSyncReadStream.class);
	private static final ExecutorService SSEXECUTOR = Executors
			.newSingleThreadExecutor(new DefaultThreadFactory("sds-sync-cq-stream"));
	private final SdsSyncQueue q;
	private Handler<Throwable> exceptionHandler;
	private final AtomicBoolean paused = new AtomicBoolean(true);
	private final AtomicBoolean ended = new AtomicBoolean(false);
	private final AtomicLong lastIndex = new AtomicLong(0L);
	private final AtomicLong currentIndex = new AtomicLong(0L);
	private final ExcerptTailer tailer;
	private Handler<Buffer> handler;
	private Handler<Void> endHandler;

	public CQSdsSyncReadStream() {
		this(-1L);
	}

	public CQSdsSyncReadStream(long fromIndex) {
		q = new SdsSyncQueue();
		this.tailer = getTailer(fromIndex);
	}

	private static <T> CompletableFuture<T> onCqThread(Supplier<T> r) {
		return CompletableFuture.supplyAsync(r, SSEXECUTOR).exceptionally(t -> {
			logger.error("failure on CQ thread", t);
			return null;
		});
	}

	private ExcerptTailer getTailer(long offset) {
		try {
			return onCqThread(() -> {
				ExcerptTailer t = q.createTailer();
				lastIndex.set(q.queue().lastIndex());
				if (offset > 0) {
					t.moveToIndex(offset);
				} else {
					t.toStart();
				}
				return t;
			}).get(5, TimeUnit.SECONDS);
		} catch (Exception e) { // NOSONAR
			throw ServerFault.create(ErrorCode.TIMEOUT, e);
		}
	}

	@Override
	public CQSdsSyncReadStream exceptionHandler(Handler<Throwable> handler) {
		this.exceptionHandler = handler;
		return this;
	}

	@Override
	public CQSdsSyncReadStream handler(Handler<Buffer> handler) {
		this.handler = handler;
		return this;
	}

	@Override
	public CQSdsSyncReadStream pause() {
		paused.set(true);
		return this;
	}

	private void end() {
		if (ended.compareAndSet(false, true)) {
			endHandler.handle(null);
			close();
		} else {
			logger.error("end() called but was already set", new Exception());
		}
	}

	private void read() {
		if (paused.get() || ended.get()) {
			return;
		}
		fetchPending();
		if (currentIndex.get() >= lastIndex.get()) {
			end();
		}
	}

	private JsonObject fetchOne() throws InterruptedException, ExecutionException, TimeoutException {
		return onCqThread(() -> {
			try {
				JsonObject jo = new JsonObject();
				boolean ret = tailer.readDocument(r -> r.read("sdssync").marshallable(m -> {
					String type = m.read("type").text();
					jo.put("type", type);
					if (type.equals("FHADD")) {
						jo.put("key", m.read("key").text());
					} else {
						jo.put("key", ByteBufUtil.hexDump(m.read("key").bytes()));
						jo.put("srv", m.read("srv").text());
					}
					jo.put("index", tailer.index());
				}));
				return ret ? jo : null;
			} finally {
				currentIndex.set(tailer.index());
			}
		}).get(5, TimeUnit.SECONDS);
	}

	private void fetchPending() {
		if (handler == null) {
			return;
		}
		try {
			while (!paused.get() && !ended.get()) {
				JsonObject data = fetchOne();
				if (data == null) {
					paused.set(true);
					break;
				} else {
					handler.handle(Buffer.buffer(data.encode()));
				}
			}
		} catch (Throwable e) {// NOSONAR
			if (exceptionHandler != null) {
				logger.error("error throw: ", e);
				exceptionHandler.handle(e);
				end();
			} else {
				logger.error("no exception handler for {}", e.getMessage(), e);
			}
		}

	}

	@Override
	public CQSdsSyncReadStream resume() {
		paused.set(false);
		read();
		return this;
	}

	@Override
	public CQSdsSyncReadStream fetch(long amount) {
		return this;
	}

	@Override
	public CQSdsSyncReadStream endHandler(Handler<Void> endHandler) {
		this.endHandler = endHandler;
		return this;
	}

	private void close() {
		pause();
		try {
			q.close();
		} catch (Exception e) {
			logger.error("error while closing", e);
			if (exceptionHandler != null) {
				exceptionHandler.handle(e);
			}
		}
	}
}