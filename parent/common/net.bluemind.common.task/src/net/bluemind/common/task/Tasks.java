/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.common.task;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;

import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.vertx.VertxStream;
import net.bluemind.core.task.api.ITask;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus;

public class Tasks {

	private Tasks() {

	}

	/**
	 * 
	 * Tracks the tasks stream using the json log chunks.
	 * 
	 * Tasks json chunk:
	 * 
	 * <pre>
	 *  {
	 *	  "done" : 1.3666667000000001,
	 *	  "total" : 2.0,
	 *	  "message" : "Domain devenv.blue : f8de2c4a.internal (f8de2c4a.internal) : skipped",
	 *	  "end" : false
	 *	}
	 * </pre>
	 * 
	 * @param ctx
	 * @param logger
	 * @param prefix
	 * @param ref
	 * @return a {@link CompletableFuture} to track the end.
	 */
	public static CompletableFuture<TaskStatus> followStream(IServiceProvider ctx, Logger logger, String prefix,
			TaskRef ref) {
		return followStream(ctx, logger, prefix, ref, true);
	}

	public static CompletableFuture<TaskStatus> followStream(IServiceProvider ctx, Logger logger, String prefix,
			TaskRef ref, boolean enableLog) {
		Objects.requireNonNull(ref, () -> prefix + ": null taskref is not allowed");
		ITask trackApi = ctx.instance(ITask.class, ref.id);
		String logPrefix = (prefix != null && !prefix.isEmpty()) ? "[" + prefix + "] " : "";
		return new JsonStreams(logger) //
				.consume(VertxStream.read(trackApi.log()), js -> {
					if (enableLog) {
						Optional.ofNullable(js.getString("message")).map(s -> logPrefix + s).ifPresent(logger::info);
					}
				}) //
				.thenApply(v -> trackApi.status()) //
				.exceptionally(t -> trackApi.status());
	}

	public static TaskStatus follow(IServiceProvider ctx, Logger logger, TaskRef ref, String prefix,
			String errorMessage) {
		return follow(ctx, logger, true, ref, prefix, errorMessage);
	}

	public static TaskStatus follow(IServiceProvider ctx, Logger logger, boolean enableLog, TaskRef ref, String prefix,
			String errorMessage) {
		TaskStatus ts;
		do {
			ts = null;
			try {
				ts = followStream(ctx, logger, prefix, ref, enableLog).get();
			} catch (InterruptedException ie) {
				Thread.currentThread().interrupt();
			} catch (ExecutionException e) {
				if (errorMessage == null || errorMessage.isEmpty()) {
					logger.error("task {} execution exception: {}", ref, e.getMessage());
				}
			}
		} while (ts != null && !ts.state.ended);

		if ((ts == null || !ts.state.succeed) && (errorMessage != null && !errorMessage.isEmpty())) {
			logger.error(errorMessage);
		}

		return ts;
	}

}
