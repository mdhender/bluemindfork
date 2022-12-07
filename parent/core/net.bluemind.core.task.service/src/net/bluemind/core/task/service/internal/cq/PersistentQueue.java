/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.core.task.service.internal.cq;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.util.concurrent.DefaultThreadFactory;
import io.vertx.core.json.JsonObject;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.task.service.internal.ISubscriber;
import net.bluemind.lib.vertx.VertxPlatform;
import net.openhft.chronicle.core.io.AbstractReferenceCounted;
import net.openhft.chronicle.queue.ExcerptAppender;
import net.openhft.chronicle.queue.ExcerptTailer;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueue;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueueBuilder;

public class PersistentQueue implements AutoCloseable {

	private static final String QUEUES_ROOT = "/var/cache/bm-core/tasks-queues";

	private static final Logger logger = LoggerFactory.getLogger(PersistentQueue.class);

	private static final ExecutorService TAIL_LOOP = Executors
			.newSingleThreadExecutor(new DefaultThreadFactory("cq-tail-loop"));

	private static final AtomicLong uid = new AtomicLong();

	static {
		AbstractReferenceCounted.disableReferenceTracing();
		System.setProperty("chronicle.analytics.disable", "true");
	}

	public static PersistentQueue createFor(String taskId) {
		File root = new File(QUEUES_ROOT);
		root.mkdirs();
		long id = uid.incrementAndGet();
		File queueDir = new File(root, taskId + "." + id);
		SingleChronicleQueue queue = SingleChronicleQueueBuilder.single(queueDir).build();
		return new PersistentQueue(taskId, id, queue);
	}

	private final SingleChronicleQueue queue;
	private final String tid;
	private final long subId;

	private final ExcerptAppender appender;

	private PersistentQueue(String tid, long id, SingleChronicleQueue queue) {
		this.tid = tid;
		this.subId = id;
		this.queue = queue;
		this.appender = onCqThread(queue::acquireAppender).join();
	}

	@Override
	public String toString() {
		return "CQ{uid: " + subId + "}";
	}

	public synchronized void put(JsonObject js) {
		if (queue.isClosed()) {
			if (logger.isWarnEnabled()) {
				logger.warn("Queue {} {} is closed, msg ({}) dropped", tid, subId, js.encode());
			}
			return;
		}
		try {
			onCqThread(() -> {
				appender.writeText(js.encode());
				return null;
			}).get(10, TimeUnit.SECONDS);
		} catch (Exception e) { // NOSONAR
			throw ServerFault.create(ErrorCode.TIMEOUT, e);
		}
	}

	private static <T> CompletableFuture<T> onCqThread(Supplier<T> r) {
		return CompletableFuture.supplyAsync(r, TAIL_LOOP).exceptionally(t -> {
			logger.error("failure on CQ thread", t);
			return null;
		});
	}

	public static class Subscriber implements ISubscriber {

		private final ExcerptTailer tail;
		private final String tid;

		private Subscriber(String tid, ExcerptTailer excerptTailer) {
			this.tid = tid;
			this.tail = excerptTailer;
		}

		public void fetchAll(Consumer<JsonObject> handler) {
			try {
				onCqThread(() -> {
					fetchAllImpl(handler);
					return null;
				}).get(20, TimeUnit.SECONDS);
			} catch (Exception e) { // NOSONAR
				throw ServerFault.create(ErrorCode.TIMEOUT, e);
			}
		}

		private void fetchAllImpl(Consumer<JsonObject> handler) {
			String cur = null;
			while (true) {
				cur = tail.readText();

				if (cur == null) {
					break;
				}
				handler.accept(new JsonObject(cur));
			}
		}

		public JsonObject fetchOne() {
			try {
				return onCqThread(this::fetchOneImpl).get(10, TimeUnit.SECONDS);
			} catch (Exception e) { // NOSONAR
				throw ServerFault.create(ErrorCode.TIMEOUT, e);
			}
		}

		private JsonObject fetchOneImpl() {
			try {
				String cur = tail.readText();
				if (cur != null) {
					return new JsonObject(cur);
				}
			} catch (Exception t) {
				logger.error("[{}] reading from queue failed", tid, t);
			}
			return null;
		}

		@Override
		public String taskId() {
			return tid;
		}

	}

	public ISubscriber subscriber(int offset) {
		CompletableFuture<ExcerptTailer> tailerRef = onCqThread(() -> {
			ExcerptTailer tailer = queue.createTailer();
			if (offset > 0) {
				long curPos = tailer.index();
				// we can do that because we don't cycle the queue
				boolean seek = tailer.moveToIndex(curPos + offset);
				if (!seek) {
					logger.debug("Failed to seek to CQ cycle {}, moving to end.", curPos + offset);
					tailer.toEnd();
				}
			}
			return tailer;
		});

		try {
			return new Subscriber(tid, tailerRef.get(10, TimeUnit.SECONDS));
		} catch (Exception e) { // NOSONAR
			throw ServerFault.create(ErrorCode.TIMEOUT, e);
		}
	}

	@Override
	public void close() {
		File toDelete = queue.file();
		appender.close();
		queue.close();
		VertxPlatform.getVertx().executeBlocking(prom -> {
			try {
				Arrays.stream(toDelete.listFiles()).forEach(File::delete);
				Files.delete(toDelete.toPath());
				logger.info("[{}] CQ deleted ({}).", tid, subId);
			} catch (IOException e) {
				logger.error("[{}] failed to delete queue dir", tid, e);
			} finally {
				prom.complete();
			}
		}, false, ar -> {
			if (ar.failed()) {
				logger.error("CQ cleanup failed", ar.cause());
			}
		});
	}

}