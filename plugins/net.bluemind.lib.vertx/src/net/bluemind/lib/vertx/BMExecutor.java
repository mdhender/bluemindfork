/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.lib.vertx;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.spectator.api.Registry;
import com.netflix.spectator.api.Timer;

import net.bluemind.metrics.registry.IdFactory;
import net.bluemind.metrics.registry.MetricsRegistry;

public class BMExecutor {

	private static final Logger logger = LoggerFactory.getLogger(BMExecutor.class);

	public static final long DEFAULT_TIMEOUT = 20 * 1000l; // 20s

	private static final int DEFAULT_WORKER_POOL_SIZE = Math.max(Runtime.getRuntime().availableProcessors() * 2, 30);

	private final WorkerExecutorService smallTimeout;
	private final WorkerExecutorService noTimeout;
	private final Registry reg;
	private final IdFactory idFactory;
	private final Timer timer;

	public interface IHasPriority {
		int priority();
	}

	public interface BMTaskMonitor {

		public boolean alive();
	}

	private static final BMTaskMonitor DIRECT_MON = () -> true;

	public interface BMTask {
		public void run(BMTaskMonitor monitor);

		public void cancelled();

	}

	public BMExecutor(String name) {
		this(DEFAULT_WORKER_POOL_SIZE, name);
	}

	public BMExecutor(int thread, String name) {
		this.reg = MetricsRegistry.get();
		this.idFactory = new IdFactory("executor", reg, BMExecutor.class);

		this.smallTimeout = new WorkerExecutorService(name, thread, DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS);
		this.noTimeout = new WorkerExecutorService(name + "-notimeout", thread, 1, TimeUnit.DAYS);
		this.timer = reg.timer(idFactory.name(name));
	}

	public ExecutorService asExecutorService() {
		return smallTimeout;
	}

	public void execute(BMTask command) {
		long start = reg.clock().monotonicTime();
		smallTimeout.execute(() -> {
			command.run(DIRECT_MON);
			long end = reg.clock().monotonicTime();
			long elapsed = end - start;
			timer.record(elapsed, TimeUnit.NANOSECONDS);
			if (elapsed > TimeUnit.SECONDS.toNanos(1)) {
				logger.warn("{} took more than 1sec.", command);
			}
		});
	}

	public void executeDirect(BMTask command) {
		noTimeout.execute(() -> command.run(DIRECT_MON));
	}

}
