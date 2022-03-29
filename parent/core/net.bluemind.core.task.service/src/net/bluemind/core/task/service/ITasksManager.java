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

import org.slf4j.Logger;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.task.api.ITask;
import net.bluemind.core.task.api.TaskRef;

/**
 * In-Core tasks service
 *
 */
public interface ITasksManager {

	/**
	 * Launch {@link IServerTask}
	 * 
	 * @param serverTask task to run
	 * @return ref to running task
	 * @throws ServerFault
	 */
	public TaskRef run(IServerTask serverTask) throws ServerFault;

	/**
	 * Launch {@link IServerTask}
	 * 
	 * @param logger     complementary logger to use with the monitor
	 * @param serverTask task to run
	 * @return ref to running task
	 * @throws ServerFault
	 */
	public TaskRef run(Logger logger, IServerTask serverTask) throws ServerFault;

	/**
	 * Launch {@link IServerTask}
	 * 
	 * @param uniqueId
	 * @param serverTask task to run
	 * @return ref to running task
	 * @throws ServerFault
	 */
	public TaskRef run(String uniqueId, IServerTask serverTask) throws ServerFault;

	/**
	 * Launch {@link IServerTask}
	 * 
	 * @param uniqueId
	 * @param logger     complementary logger to use with the monitor
	 * @param serverTask task to run
	 * @return ref to running task
	 * @throws ServerFault
	 */
	public TaskRef run(String uniqueId, Logger logger, IServerTask serverTask) throws ServerFault;

	/**
	 * Retrieve task service
	 * 
	 * @param taskId taskRef {@link TaskRef#id}
	 * @return
	 */
	public ITask getTask(String taskId);

	/**
	 * Cancel a task
	 * 
	 * @param taskId taskRef {@link TaskRef#id}
	 */
	public void cancel(String taskId);

	public boolean inTaskThread();
}
