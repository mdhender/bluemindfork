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
public class ParallelStarvationHandler {

	private static final Logger logger = LoggerFactory.getLogger(ParallelStarvationHandler.class);
	private AtomicReference<IRecordStarvationStrategy> delegate;
	private final long[] lastStarvations;
	private final boolean[] oneStarvation;
	private final IRecordStarvationStrategy[] workers;
	private final RateLimiter rateLimitLog;

	public ParallelStarvationHandler(IRecordStarvationStrategy starved, int worker) {
		this.delegate = new AtomicReference<>(starved);
		this.rateLimitLog = RateLimiter.create(0.25);
		this.oneStarvation = new boolean[worker];
		this.lastStarvations = new long[worker];
		this.workers = new IRecordStarvationStrategy[worker];
		for (int i = 0; i < worker; i++) {
			final int idx = i;

			this.workers[idx] = new IRecordStarvationStrategy() {

				@Override
				public void onRecordsReceived(JsonObject metas) {
					System.err.println("Records for " + idx);
					synchronized (lastStarvations) {
						oneStarvation[idx] = false;
					}
				}

				@Override
				public ExpectedBehaviour onStarvation(JsonObject infos) {
					long now = System.nanoTime();
					System.err.println("starved " + idx);
					synchronized (lastStarvations) {
						boolean wasStarved = oneStarvation[idx];
						oneStarvation[idx] = true;
						if (!wasStarved) {
							lastStarvations[idx] = now;
						}
						for (int j = 0; j < worker; j++) {
							if (!oneStarvation[j] || now - lastStarvations[j] < TimeUnit.SECONDS.toNanos(1)) {
								logger.info("Worker {} still getting records ({}ms ago)", j,
										TimeUnit.NANOSECONDS.toMillis(now - lastStarvations[j]));
								return ExpectedBehaviour.RETRY;
							} else {
								if (rateLimitLog.tryAcquire()) {
									logger.info("Worker {} starved.", j);
								}
							}
						}
					}
					// when the delegate decides to abort, we always abort & stop calling it
					synchronized (delegate) {
						ExpectedBehaviour result = delegate.get().onStarvation(infos);
						if (result == ExpectedBehaviour.ABORT) {
							delegate.set((JsonObject noop) -> ExpectedBehaviour.ABORT);
						}
						return result;
					}
				}

			};

		}
	}

	public IRecordStarvationStrategy forWorker(int idx) {
		return workers[idx];
	}
}
