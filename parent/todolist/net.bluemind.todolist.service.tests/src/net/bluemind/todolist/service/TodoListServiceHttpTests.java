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
package net.bluemind.todolist.service;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.http.ClientSideServiceProvider;
import net.bluemind.todolist.api.ITodoList;

public class TodoListServiceHttpTests extends TodoListServiceTests {

	@Override
	protected ITodoList getService(SecurityContext context) throws ServerFault {
		return ClientSideServiceProvider.getProvider("http://localhost:8090", context.getSessionId())
				.instance(ITodoList.class, container.uid);
	}

	@Override
	public void testRestoreCreate() {
		// no restore endpoint available over http
	}

	@Override
	public void testRestoreUpdate() {
		// no restore endpoint available over http
	}
}
