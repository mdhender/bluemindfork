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
package net.bluemind.core.backup.store.kafka;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.RateLimiter;

import io.vertx.core.json.JsonObject;
import net.bluemind.core.backup.continuous.IRecordStarvationStrategy;

/**
 * Create {@link IRecordStarvationStrategy} "workers" for dealing with
 * starvation in multiple consumers.
 * 
 * We will trigger the parent {@link IRecordStarvationStrategy} only if all
 * workers starved in the last second.
 *
 */
public class ParallelStarvationHandler implements IRecordStarvationStrategy {

	private static final IRecordStarvationStrategy ABORT_STRAT = new IRecordStarvationStrategy() {

		@Override
		public ExpectedBehaviour onStarvation(JsonObject infos) {
			return ExpectedBehaviour.ABORT;
		}

		@Override
		public String toString() {
			return "ABORT_START";
		}

	};

	private static final Logger logger = LoggerFactory.getLogger(ParallelStarvationHandler.class);
	private final AtomicReference<IRecordStarvationStrategy> delegate;
	private final AtomicLong lastRec;
	private final AtomicLong lastStarv;
	private RateLimiter logRateLimit;

	public ParallelStarvationHandler(IRecordStarvationStrategy starved, int worker) {
		this.delegate = new AtomicReference<>(starved);
		this.lastRec = new AtomicLong(System.nanoTime());
		this.lastStarv = new AtomicLong();
		this.logRateLimit = RateLimiter.create(0.25);

	}

	@Override
	public void onRecordsReceived(JsonObject metas) {
		lastRec.set(System.nanoTime());
	}

	@Override
	public ExpectedBehaviour onStarvation(JsonObject infos) {
		lastStarv.set(System.nanoTime());
		long deltaNanos = lastStarv.get() - lastRec.get();
		if (logRateLimit.tryAcquire()) {
			logger.info("Delta between lastStarvation & lastRecord is {}ms.",
					TimeUnit.NANOSECONDS.toMillis(deltaNanos));
		}
		if (deltaNanos > TimeUnit.SECONDS.toNanos(1)) {
			logger.info("Calling into parent delegate {} (delta {}ms)", delegate.get(),
					TimeUnit.NANOSECONDS.toMillis(deltaNanos));
			// when the delegate decides to abort, we always abort & stop calling it
			synchronized (delegate) {
				IRecordStarvationStrategy del = delegate.get();
				ExpectedBehaviour result = del.onStarvation(infos);
				if (result == ExpectedBehaviour.ABORT) {
					if (del != ABORT_STRAT) {
						logger.info("{} decided to abort.", delegate.get());
					}
					delegate.set(ABORT_STRAT);
				}
				return result;
			}
		} else {
			return ExpectedBehaviour.RETRY;
		}
	}
}
