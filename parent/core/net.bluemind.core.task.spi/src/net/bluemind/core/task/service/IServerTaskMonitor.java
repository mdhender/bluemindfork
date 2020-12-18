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

public interface IServerTaskMonitor {

	/**
	 * @see #subWork(String, double)
	 */
	public IServerTaskMonitor subWork(double work);

	/**
	 * Create a sub task monitor that uses a given amount of work unit from the task
	 * 
	 * @param logPrefix
	 * @param work      the total number of work units given to the sub task monitor
	 *                  task.
	 * @return a sub task monitor. Begin must be called on the return monitor
	 */
	public IServerTaskMonitor subWork(String logPrefix, double work);

	/**
	 * Notifies that the task is beginning. This must only be called once on a given
	 * monitor instance.
	 * 
	 * @param totalWork the total number of work units into which the task is been
	 *                  subdivided.
	 */
	public void begin(double totalWork, String log);

	/**
	 * Notifies that a given number of work unit of the task has been completed.
	 *
	 * @param doneWork number of work units just completed
	 * @param log
	 */
	public void progress(double doneWork, String log);

	/**
	 * Notifies that the task has been completed.
	 * 
	 * @param success
	 * @param log
	 * @param result
	 */
	public void end(boolean success, String log, String result);

	public void log(String log);
}
