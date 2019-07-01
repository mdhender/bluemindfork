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
package net.bluemind.core.task.service.internal;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.service.ITasksManager;
import net.bluemind.lib.vertx.VertxPlatform;

public class TasksManagerFactory implements ServerSideServiceProvider.IServerSideServiceFactory<ITasksManager> {

	private TasksManager tasksManager;

	public TasksManagerFactory() {
		this.tasksManager = new TasksManager(VertxPlatform.getVertx());
	}

	@Override
	public Class<ITasksManager> factoryClass() {
		return ITasksManager.class;
	}

	@Override
	public ITasksManager instance(BmContext context, String... params) throws ServerFault {
		// FIXME check right ?
		return tasksManager;
	}

}
