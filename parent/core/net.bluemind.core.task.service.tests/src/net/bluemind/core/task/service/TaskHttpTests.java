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

import java.util.concurrent.TimeUnit;

import org.junit.Before;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.http.ClientSideServiceProvider;
import net.bluemind.core.task.api.ITask;
import net.bluemind.lib.vertx.VertxPlatform;

public class TaskHttpTests extends TaskTests {

	@Before
	public void before() throws Exception {
		VertxPlatform.spawnBlocking(60, TimeUnit.SECONDS);
	}

	protected ITask getTask(String taskId) throws ServerFault {
		return ClientSideServiceProvider.getProvider("http://localhost:8090", null).instance(ITask.class, taskId);
	}
}
