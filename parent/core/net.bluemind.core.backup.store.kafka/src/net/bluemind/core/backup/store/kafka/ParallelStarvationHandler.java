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

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.RateLimiter;

import io.vertx.core.json.JsonObject;
import net.bluemind.core.backup.continuous.IRecordStarvationStrategy;
import net.bluemind.utils.ProgressPrinter;

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
	private final AtomicLong receivedRecords;
	private final Map<Integer, Long> endOffsets;
	private final Map<Integer, Long> currentOffsets;
	private final ProgressPrinter progressPrinter;
	private final RateLimiter progressRateLimit;

	private Long estimatedTotalRecords;

	public ParallelStarvationHandler(IRecordStarvationStrategy starved, int worker, Map<Integer, Long> endOffsets) {
		this.delegate = new AtomicReference<>(starved);
		this.progressRateLimit = RateLimiter.create(1);
		this.receivedRecords = new AtomicLong(0);
		this.endOffsets = endOffsets;
		this.currentOffsets = new ConcurrentHashMap<>();
		endOffsets.entrySet().stream().forEach(es -> currentOffsets.put(es.getKey(), 0L));
		this.estimatedTotalRecords = endOffsets.values().stream().reduce(0L, Long::sum);
		progressPrinter = new ProgressPrinter(estimatedTotalRecords, 500_000, 1);
		logger.info("Preparing sub with {} worker(s) and starving to {}", worker, starved);

	}

	@Override
	public void onRecordsReceived(JsonObject metas) {
		Long records = metas.getLong("records", 0L);
		if (records != 0) {
			receivedRecords.addAndGet(records);
		}
		if (progressRateLimit.tryAcquire() && logger.isInfoEnabled()) {
			logger.info("progress on {}: {}", metas.getString("topic"), progressPrinter);
		}
	}

	@Override
	public void updateOffsets(Map<Integer, Long> currentOffsets) {
		long previousTotalOffsets = this.currentOffsets.values().stream().reduce(0L, Long::sum);
		currentOffsets.forEach(this.currentOffsets::put);
		long newTotalOffsets = this.currentOffsets.values().stream().reduce(0L, Long::sum);
		progressPrinter.add(newTotalOffsets - previousTotalOffsets);
	}

	@Override
	public boolean isTopicFinished() {
		List<Entry<Integer, Long>> nonEmptyPartitions = endOffsets.entrySet().stream()
				.filter(es -> currentOffsets.get(es.getKey()) < es.getValue()).toList();
		return nonEmptyPartitions.isEmpty();
	}

	@Override
	public ExpectedBehaviour onStarvation(JsonObject infos) {
		if (isTopicFinished()) {
			IRecordStarvationStrategy del = delegate.get();
			ExpectedBehaviour result = del.onStarvation(infos);
			if (result == ExpectedBehaviour.ABORT) {
				if (del != ABORT_STRAT) {
					logger.info("{} decided to abort.", del);
				}
				delegate.set(ABORT_STRAT);
			}
			return result;
		}
		return ExpectedBehaviour.RETRY;
	}
}
