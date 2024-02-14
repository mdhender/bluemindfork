/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2024
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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoopProvider {

	private static final Logger logger = LoggerFactory.getLogger(LoopProvider.class);

	private LoopProvider() {
	}

	public static class Loop {

		private static final AtomicLong POOL_ID = new AtomicLong();
		private final AtomicLong given;
		private final AtomicLong closed;
		private final ExecutorService pool;
		private final AtomicBoolean recycle;
		private String poolId;

		private Loop() {
			this.poolId = "cq-loop-" + POOL_ID.incrementAndGet();
			this.pool = Executors.newSingleThreadExecutor(r -> new Thread(r, poolId));
			this.given = new AtomicLong(0);
			this.closed = new AtomicLong(0);
			this.recycle = new AtomicBoolean(false);
		}

		long addRef() {
			return given.incrementAndGet();
		}

		public void unRef() {
			long closures = closed.incrementAndGet();
			if (recycle.get()) {
				long usage = given.get();
				if (closures >= usage) {
					logger.info("shutdown of {}", this);
					pool.shutdown();
				}
			}

		}

		void recycle() {
			recycle.set(true);
		}

		public ExecutorService pool() {
			return pool;
		}

		@Override
		public String toString() {
			return "Loop{p: " + poolId + ", ref: " + given.get() + ", unref: " + closed.get() + ", cycle: "
					+ recycle.get() + "}";
		}

	}

	private static final AtomicReference<Loop> current = new AtomicReference<>(new Loop());

	private static final int REUSE_COUNT = 500;

	public static synchronized Loop get() {
		Loop active = current.get();

		long given = active.addRef();
		if (given >= REUSE_COUNT) {
			active.recycle();
			Loop next = new Loop();
			logger.info("recycle {}, a new loop will be used for next tasks ({})", active, next);
			current.set(next);
		}
		return active;
	}

}
