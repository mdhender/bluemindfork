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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.core.task.service;

import java.util.concurrent.CompletableFuture;

public abstract class BlockingServerTask implements IServerTask {

	@Override
	public CompletableFuture<Void> execute(IServerTaskMonitor monitor) {
		try {
			run(monitor);
		} catch (Exception e) {
			return CompletableFuture.failedFuture(e);
		}
		return CompletableFuture.completedFuture(null);
	}

	protected abstract void run(IServerTaskMonitor monitor) throws Exception;

	public static CompletableFuture<Void> run(IServerTaskMonitor monitor, TaskConsumer task) {
		return new BlockingServerTask() {

			@Override
			protected void run(IServerTaskMonitor monitor) throws Exception {
				task.accept(monitor);
			}
		}.execute(monitor);
	}

	public interface TaskConsumer {
		void accept(IServerTaskMonitor monitor) throws Exception;
	}

}
