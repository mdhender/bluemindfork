/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.retry.support;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Suppliers;

import io.netty.util.concurrent.DefaultThreadFactory;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import net.openhft.chronicle.core.io.AbstractReferenceCounted;
import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptAppender;
import net.openhft.chronicle.queue.ExcerptTailer;
import net.openhft.chronicle.queue.RollCycles;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueueBuilder;

public class RetryQueueVerticle extends AbstractVerticle {

	private static final Logger logger = LoggerFactory.getLogger(RetryQueueVerticle.class);
	private static final Executor POOL = Executors.newSingleThreadExecutor(new DefaultThreadFactory("retry-queue"));

	private final String topic;
	private final RetryProcessor rp;

	public interface RetryProcessor {

		void retry(JsonObject js) throws Exception; // NOSONAR
	}

	static {
		AbstractReferenceCounted.disableReferenceTracing();
		System.setProperty("chronicle.analytics.disable", "true");
	}

	private final Supplier<ChronicleQueue> qProv;
	private final Supplier<ExcerptAppender> append;
	private final Supplier<ExcerptTailer> tail;

	protected RetryQueueVerticle(String topic, RetryProcessor rp) {
		this.topic = topic;
		this.rp = rp;
		qProv = Suppliers.memoize(this::buildQueue);
		append = Suppliers.memoize(() -> qProv.get().acquireAppender());
		tail = Suppliers.memoize(() -> qProv.get().createTailer("retry"));
	}

	public String topic() {
		return topic;
	}

	public String address() {
		return "retry." + topic;
	}

	@Override
	public void start() throws Exception {

		POOL.execute(() -> {
			qProv.get();
			append.get();
			tail.get();
		});

		EventBus eb = vertx.eventBus();
		AtomicLong debounceTime = new AtomicLong();
		eb.consumer("retry." + topic, (Message<JsonObject> msg) -> {
			String jsStr = msg.body().encode();
			POOL.execute(() -> {
				ExcerptAppender writer = append.get();
				writer.writeText(jsStr);
				vertx.cancelTimer(debounceTime.get());
				long freshTimer = vertx.setTimer(50, tid -> {
					logger.debug("[{}] Resume processing...", topic);
					POOL.execute(this::flushRetries);
				});
				debounceTime.set(freshTimer);
			});
			msg.reply(0L);
		});
	}

	private void flushRetries() {
		ExcerptTailer reader = tail.get();
		while (true) {
			long idx = reader.index();

			String jsStr = reader.readText();
			if (jsStr == null) {
				break;
			}
			JsonObject js = new JsonObject(jsStr);
			try {
				rp.retry(js);
			} catch (Exception e) {
				reader.moveToIndex(idx);
				logger.error("[{}] Failed to process index {} {}", topic, idx, js, e);
				break;
			}
		}
	}

	private ChronicleQueue buildQueue() {
		return SingleChronicleQueueBuilder.binary("/var/cache/bm-core/retry-" + topic)//
				.readOnly(false)//
				.blockSize(64L << 14)//
				.rollCycle(RollCycles.HALF_HOURLY).storeFileListener((int cycle, File old) -> {
					try {
						Files.deleteIfExists(old.toPath());
					} catch (IOException e) {
						logger.error(e.getMessage(), e);
					}
				})//
				.build();
	}

}
