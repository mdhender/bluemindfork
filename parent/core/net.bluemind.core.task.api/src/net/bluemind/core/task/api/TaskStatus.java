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
package net.bluemind.core.task.api;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class TaskStatus {

	@BMApi(version = "3")
	public enum State {
		NotStarted(false, false), InProgress(false, false), InError(false, true), Success(true, true);

		public final boolean ended;
		public final boolean succeed;

		private State(boolean succeed, boolean ended) {
			this.ended = ended;
			this.succeed = succeed;
		}

		public static State status(boolean success, boolean end) {
			if (end && success) {
				return Success;
			} else if (end) {
				return InError;
			} else {
				return InProgress;
			}
		}
	}

	public double steps;
	public double progress;

	public String lastLogEntry;

	public State state = State.NotStarted;

	public String result;

	public static TaskStatus create(double steps, double progress, String lastLogEntry, State status, String result) {

		TaskStatus ret = new TaskStatus();
		ret.steps = steps;
		ret.progress = progress;
		ret.lastLogEntry = lastLogEntry;
		ret.state = status;
		ret.result = result;

		return ret;
	}

	@Override
	public String toString() {
		return "Status{st: " + state + ", lastLog: " + lastLogEntry + ", res: " + result + "}";
	}
}
