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
package net.bluemind.core.task.service;

import org.slf4j.event.Level;

public interface IServerTaskMonitor {

	/**
	 * @see #subWork(String, double)
	 */
	IServerTaskMonitor subWork(double work);

	/**
	 * Create a sub task monitor that uses a given amount of work unit from the task
	 * 
	 * @param logPrefix
	 * @param work      the total number of work units given to the sub task monitor
	 *                  task.
	 * @return a sub task monitor. Begin must be called on the return monitor
	 */
	IServerTaskMonitor subWork(String logPrefix, double work);

	/**
	 * Notifies that the task is beginning. This must only be called once on a given
	 * monitor instance.
	 * 
	 * @param totalWork the total number of work units into which the task is been
	 *                  subdivided.
	 */
	void begin(double totalWork, String log);

	/**
	 * Notifies that a given number of work unit of the task has been completed.
	 *
	 * @param doneWork number of work units just completed
	 * @param log
	 */
	void progress(double doneWork, String log);

	/**
	 * Notifies that the task has been completed.
	 * 
	 * @param success
	 * @param log
	 * @param result
	 */
	void end(boolean success, String log, String result);

	void log(String log);

	default void begin(double totalWork, String log, Level level) {
		begin(totalWork, log);
	}

	default void progress(double doneWork, String log, Level level) {
		progress(doneWork, log);
	}

	default void end(boolean success, String log, String result, Level level) {
		end(success, log, result);
	}

	default void log(String log, Level level) {
		log(log);
	}

	default void log(String log, Throwable t) {
		log(log);
	}
}
