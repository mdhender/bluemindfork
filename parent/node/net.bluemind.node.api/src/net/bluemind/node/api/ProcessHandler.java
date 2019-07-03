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
package net.bluemind.node.api;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import net.bluemind.core.api.fault.ServerFault;

public interface ProcessHandler {

	void log(String l);

	void completed(int exitCode);

	void starting(String taskRef);

	public static class NoOutBlockingHandler implements ProcessHandler {

		private CompletableFuture<Integer> promise;

		public NoOutBlockingHandler() {
			this.promise = new CompletableFuture<>();
		}

		@Override
		public void log(String l) {
		}

		@Override
		public void completed(int exitCode) {
			promise.complete(exitCode);
		}

		public int get(long t, TimeUnit unit) {
			try {
				return promise.get(t, unit);
			} catch (Exception e) {
				throw new ServerFault(e);
			}
		}

		@Override
		public void starting(String taskRef) {
		}

	}

	public static class BlockingHandler implements ProcessHandler {

		private CompletableFuture<ExitList> promise;
		private ExitList exitList;

		public BlockingHandler() {
			this.promise = new CompletableFuture<>();
			this.exitList = new ExitList();

		}

		@Override
		public void log(String l) {
			exitList.add(l);
		}

		@Override
		public void completed(int exitCode) {
			exitList.setExitCode(exitCode);
			promise.complete(exitList);
		}

		public ExitList get(long t, TimeUnit unit) {
			try {
				return promise.get(t, unit);
			} catch (Exception e) {
				throw new ServerFault(e);
			}
		}

		@Override
		public void starting(String taskRef) {
		}

	}

}
