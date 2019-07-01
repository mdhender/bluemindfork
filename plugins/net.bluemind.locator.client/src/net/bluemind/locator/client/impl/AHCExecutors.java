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
package net.bluemind.locator.client.impl;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates all the necessary thread pools
 */
public class AHCExecutors {

	private static final Logger logger = LoggerFactory.getLogger(AHCExecutors.class);

	private static ExecutorService reqPool;
	private static ScheduledExecutorService reaperPool;

	static {
		init();
	}

	private static final class ThreadCreator implements ThreadFactory {

		private String name;
		private AtomicInteger count;

		public ThreadCreator(String name) {
			this.name = name;
			this.count = new AtomicInteger(0);
		}

		@Override
		public Thread newThread(Runnable r) {
			Thread ret = new Thread(r, name + " #" + count.incrementAndGet());
			ret.setDaemon(true);
			return ret;
		}
	}

	private static final synchronized void init() {
		int cores = Runtime.getRuntime().availableProcessors();
		reqPool = Executors.newFixedThreadPool(Math.max(20, 3 * cores), new ThreadCreator("lc-ahc-req"));
		reaperPool = Executors.newScheduledThreadPool(3, new ThreadCreator("lc-ahc-reaper"));
	}

	public static ExecutorService reqPool() {
		return reqPool;
	}

	public static ScheduledExecutorService reaperPool() {
		return reaperPool;
	}

	public static synchronized void reboot() throws InterruptedException {
		// AHCHelper needs restart to enable that
		shutPool(reqPool);
		shutPool(reaperPool);
		logger.info("**** POOLS shut down ****");
		init();
	}

	private static void shutPool(ExecutorService p) throws InterruptedException {
		p.shutdown();
		p.awaitTermination(1, TimeUnit.SECONDS);
	}
}
