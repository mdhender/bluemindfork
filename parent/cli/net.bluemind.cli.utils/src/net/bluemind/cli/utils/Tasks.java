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
package net.bluemind.cli.utils;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import net.bluemind.cli.cmd.api.CliContext;
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
	 * @param ref
	 * @return a {@link CompletableFuture} to track the end.
	 */
	public static CompletableFuture<TaskStatus> followStream(CliContext ctx, String prefix, TaskRef ref) {
		return followStream(ctx, prefix, ref, true);
	}

	public static CompletableFuture<TaskStatus> followStream(CliContext ctx, String prefix, TaskRef ref,
			boolean enableLog) {
		Objects.requireNonNull(ref, () -> prefix + ": null taskref is not allowed");
		ITask trackApi = ctx.infiniteRequestTimeoutAdminApi().instance(ITask.class, ref.id);
		String logPrefix = (prefix != null && !prefix.isEmpty()) ? "[" + prefix + "]" : "";
		return new JsonStreams(ctx)
				.consume(VertxStream.read(trackApi.log()),
						js -> Optional.ofNullable(js.getString("message")).map(s -> logPrefix + s).ifPresent(ctx::info))
				.thenApply(v -> trackApi.status());
	}

	public static TaskStatus follow(CliContext ctx, TaskRef ref, String errorMessage) {
		return follow(ctx, true, ref, errorMessage);
	}

	public static TaskStatus follow(CliContext ctx, boolean enableLog, TaskRef ref, String errorMessage) {
		TaskStatus ts;
		do {
			ts = null;
			try {
				ts = followStream(ctx, "", ref, enableLog).get();
			} catch (InterruptedException ie) {
				Thread.currentThread().interrupt();
			} catch (ExecutionException e) {
				if (errorMessage == null || errorMessage.isEmpty()) {
					ctx.error("task execution exception: {}", e.getMessage());
				}
			}
		} while (ts != null && !ts.state.ended);

		if ((ts == null || !ts.state.succeed) && (errorMessage != null && !errorMessage.isEmpty())) {
			ctx.error(errorMessage);
		}

		return ts;
	}

}
