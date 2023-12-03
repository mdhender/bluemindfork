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

import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.RateLimiter;
import com.netflix.spectator.api.Counter;
import com.netflix.spectator.api.Timer;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import net.bluemind.metrics.registry.IdFactory;
import net.bluemind.metrics.registry.MetricsRegistry;
import net.bluemind.retry.support.rocks.RocksQueue;
import net.bluemind.retry.support.rocks.RocksQueue.TailRecord;
import net.bluemind.retry.support.rocks.RocksQueue.Tailer;

public class RetryQueueVerticle extends AbstractVerticle {

	private static final Logger logger = LoggerFactory.getLogger(RetryQueueVerticle.class);

	private final String topic;
	private final RetryProcessor rp;

	public interface RetryProcessor {

		void retry(JsonObject js) throws Exception; // NOSONAR
	}

	private final RocksQueue persistentQueue;
	private final Counter writeCounter;
	private final RateLimiter limitFlushes;

	private MessageConsumer<JsonObject> cons;

	private Future<String> depFuture;

	private static final String TOPIC_TAG = "topic";
	private static final String RETRY_ID = "retry";

	protected RetryQueueVerticle(String topic, RetryProcessor rp) {
		this.topic = topic;
		this.rp = rp;
		this.persistentQueue = new RocksQueue(topic);
		var reg = MetricsRegistry.get();
		var idFactory = new IdFactory(RETRY_ID, reg, RetryRequester.class);
		this.writeCounter = reg.counter(idFactory.name("writes", TOPIC_TAG, topic));

		this.limitFlushes = RateLimiter.create(1.0);
	}

	public String topic() {
		return topic;
	}

	public String address() {
		return "retry." + topic;
	}

	@Override
	public void start() throws Exception {
		this.depFuture = vertx.deployVerticle(() -> new FlushCompanionVerticle(persistentQueue, rp, topic),
				new DeploymentOptions().setInstances(1).setWorker(true));

		EventBus eb = vertx.eventBus();
		final String flushTrigger = "retry.flush." + topic;
		final String flushPayload = "F";
		AtomicLong debounce = new AtomicLong();
		this.cons = eb.consumer("retry." + topic, (Message<JsonObject> msg) -> {
			String jsStr = msg.body().encode();
			persistentQueue.writer().write(jsStr);
			writeCounter.increment();
			logger.debug("WRITE {}", topic);
			vertx.cancelTimer(debounce.get());
			if (limitFlushes.tryAcquire()) {
				eb.send(flushTrigger, flushPayload);
			} else {
				debounce.set(vertx.setTimer(250, tid -> eb.send(flushTrigger, flushPayload)));
			}
			msg.reply(0L);
		});

	}

	@Override
	public void stop() throws Exception {
		cons.unregister();
		if (depFuture.succeeded()) {
			vertx.undeploy(depFuture.result());
		}
		super.stop();
	}

	private static class FlushCompanionVerticle extends AbstractVerticle {
		private static final Logger logger = LoggerFactory.getLogger(FlushCompanionVerticle.class);
		private static final String RETRY_TAILER = "retry";

		private final RocksQueue persistentQueue;
		private final Timer flushesTimer;
		private final Counter flushedItemsCounter;
		private final String topic;
		private final RetryProcessor rp;
		private MessageConsumer<?> cons;
		private long periodic;

		public FlushCompanionVerticle(RocksQueue persistentQueue, RetryProcessor rp, String topic) {
			this.persistentQueue = persistentQueue;
			this.rp = rp;
			var reg = MetricsRegistry.get();
			IdFactory idFactory = new IdFactory(RETRY_ID, reg, RetryRequester.class);
			this.flushesTimer = reg.timer(idFactory.name("flushes", TOPIC_TAG, topic));
			this.flushedItemsCounter = reg.counter(idFactory.name("flushedItems", TOPIC_TAG, topic));
			this.topic = topic;
		}

		@Override
		public void start() throws Exception {
			this.cons = vertx.eventBus().consumer("retry.flush." + topic, msg -> {
				try {
					long procs = flushesTimer.record(this::flushRetries);
					flushedItemsCounter.increment(procs);
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
				}
			});

			this.periodic = vertx.setPeriodic(60_000, tid -> vertx.executeBlocking(() -> {
				persistentQueue.compact(RETRY_TAILER);
				return null;
			}, true));
		}

		@Override
		public void stop() throws Exception {
			vertx.cancelTimer(periodic);
			cons.unregister();
			super.stop();
		}

		private long flushRetries() {
			Tailer reader = persistentQueue.reader(RETRY_TAILER);
			boolean stopped = false;
			long processed = 0;
			while (!stopped) {
				TailRecord rec = reader.next();
				if (rec == null) {
					stopped = true;
				} else {
					try {
						rp.retry(new JsonObject(rec.payload()));
						processed++;
						reader.commit();
					} catch (Exception e) {
						// yeah don't commit
						logger.error("{} will be retried because of {}", rec, e.getMessage());
						stopped = true;
					}
				}
			}
			logger.debug("FLUSH {} ({})", topic, processed);
			return processed;
		}

	}

}
