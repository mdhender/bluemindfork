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

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
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

	protected RetryQueueVerticle(String topic, RetryProcessor rp) {
		this.topic = topic;
		this.rp = rp;
		this.persistentQueue = new RocksQueue(topic);
	}

	public String topic() {
		return topic;
	}

	public String address() {
		return "retry." + topic;
	}

	@Override
	public void start() throws Exception {

		EventBus eb = vertx.eventBus();
		AtomicLong debounceTime = new AtomicLong();
		eb.consumer("retry." + topic, (Message<JsonObject> msg) -> {
			String jsStr = msg.body().encode();
			persistentQueue.writer().write(jsStr);
			vertx.cancelTimer(debounceTime.get());
			long freshTimer = vertx.setTimer(50, tid -> {
				logger.debug("[{}] Resume processing...", topic);
				flushRetries();
			});
			debounceTime.set(freshTimer);
			msg.reply(0L);
		});

		vertx.setPeriodic(60_000, tid -> persistentQueue.compact("retry"));
	}

	private void flushRetries() {
		Tailer reader = persistentQueue.reader("retry");
		boolean stopped = false;
		while (!stopped) {
			TailRecord rec = reader.next();
			if (rec == null) {
				stopped = true;
			} else {
				try {
					rp.retry(new JsonObject(rec.payload()));
					reader.commit();
				} catch (Exception e) {
					// yeah don't commit
					logger.error("{} will be retried because of {}", rec, e.getMessage());
					stopped = true;
				}
			}
		}
	}

}
