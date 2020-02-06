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
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.lib.vertx;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Vertx;

/**
 * Helper inspired by vertx 3 to run blocking code from an event loop context.
 * 
 * @deprecated use
 *             {@link Vertx#executeBlocking(io.vertx.core.Handler, io.vertx.core.Handler)}
 *             instead
 *
 */
public class BlockingCode {

	private static final Logger logger = LoggerFactory.getLogger(BlockingCode.class);

	private final Vertx vertx;

	private BlockingCode(Vertx v) {
		this.vertx = v;
	}

	/**
	 * Provide your own executor where the blocking code will run.
	 * 
	 * @deprecated the executor is not used anymore
	 * @param pool
	 * @return
	 */
	public BlockingCode withExecutor(ExecutorService pool) {
		return this;
	}

	/**
	 * Creates a BlockingCode executor to use in a vertx event loop.
	 * 
	 * DISCLAIMER: Do not call that with {@link VertxPlatform#getVertx()} but with
	 * the vertx instance from your verticle
	 * 
	 * @param v
	 * @return
	 */
	public static BlockingCode forVertx(Vertx v) {
		return new BlockingCode(v);
	}

	/**
	 * Returns a future that will execute its followers in the vertx context
	 * associated with this instance
	 * 
	 * @param supplier
	 * @return a future for with the result of the given supplier
	 */
	public <T> CompletableFuture<T> run(Supplier<T> supplier) {

		CompletableFuture<T> result = new CompletableFuture<>();
		vertx.executeBlocking(prom -> {
			try {
				T r = supplier.get();
				prom.complete(result);
				result.complete(r);
			} catch (Exception e) {
				prom.fail(e);
				result.completeExceptionally(e);
			}
		}, true, ar -> {

		});
		return result;
	}

}
