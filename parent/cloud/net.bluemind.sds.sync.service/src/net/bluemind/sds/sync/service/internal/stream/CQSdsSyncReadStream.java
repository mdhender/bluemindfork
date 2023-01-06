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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBufUtil;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.ReadStream;
import net.bluemind.core.api.Stream;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.sds.sync.service.internal.queue.SdsSyncQueue;
import net.openhft.chronicle.queue.ExcerptTailer;

public class CQSdsSyncReadStream implements ReadStream<JsonObject>, Stream {
	private static final Logger logger = LoggerFactory.getLogger(CQSdsSyncReadStream.class);
	private static final ExecutorService SSEXECUTOR = Executors
			.newSingleThreadExecutor(new DefaultThreadFactory("sds-sync-cq-stream"));
	private final SdsSyncQueue q;
	private Handler<Throwable> exceptionHandler;
	private boolean paused = true;
	private Handler<JsonObject> handler;
	private Handler<Void> endHandler;
	private final long fromIndex;

	public CQSdsSyncReadStream() {
		this(-1L);
	}

	public CQSdsSyncReadStream(long fromIndex) {
		q = new SdsSyncQueue();
		this.fromIndex = fromIndex;
	}

	private static <T> CompletableFuture<T> onCqThread(Supplier<T> r) {
		return CompletableFuture.supplyAsync(r, SSEXECUTOR).exceptionally(t -> {
			logger.error("failure on CQ thread", t);
			return null;
		});
	}

	@Override
	public CQSdsSyncReadStream exceptionHandler(Handler<Throwable> handler) {
		this.exceptionHandler = handler;
		return this;
	}

	@Override
	public CQSdsSyncReadStream handler(Handler<JsonObject> handler) {
		this.handler = handler;
		return this;
	}

	@Override
	public CQSdsSyncReadStream pause() {
		paused = true;
		return this;
	}

	private synchronized void read(Context ctx) {
		if (handler == null) {
			return;
		}
		try (ExcerptTailer tailer = q.createTailer()) {
			long lastIndex = tailer.toEnd().index();
			if (fromIndex > 0) {
				tailer.moveToIndex(fromIndex);
			} else {
				tailer.toStart();
			}
			while (!paused && tailer.index() < lastIndex) {
				tailer.readDocument(r -> r.read("sdssync").marshallable(m -> {
					JsonObject jo = new JsonObject();
					String type = m.read("type").text();
					jo.put("type", type);
					if (type.equals("FHADD")) {
						jo.put("key", m.read("key").text());
					} else {
						jo.put("key", ByteBufUtil.hexDump(m.read("key").bytes()));
						jo.put("srv", m.read("srv").text());
					}
					jo.put("index", tailer.index());
					ctx.runOnContext(v -> handler.handle(jo));
				}));
			}
			close();
			if (endHandler != null) {
				ctx.runOnContext(v -> endHandler.handle(null));
			}
		} catch (Exception e) { // NOSONAR
			if (exceptionHandler != null) {
				ctx.runOnContext(v -> exceptionHandler.handle(e));
			}
		}
	}

	@Override
	public CQSdsSyncReadStream resume() {
		paused = false;
		Vertx vertx = VertxPlatform.getVertx();
		Context vertxContext = vertx.getOrCreateContext();
		onCqThread(() -> {
			read(vertxContext);
			return null;
		});
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