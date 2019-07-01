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
package net.bluemind.dataprotect.user;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.service.ITasksManager;
import net.bluemind.dataprotect.api.DataProtectGeneration;
import net.bluemind.dataprotect.api.Restorable;
import net.bluemind.dataprotect.api.RestoreOperation;
import net.bluemind.dataprotect.service.IRestoreActionProvider;

public class RestoreUser implements IRestoreActionProvider {

	private static final Logger logger = LoggerFactory.getLogger(RestoreUser.class);

	public RestoreUser() {
	}

	@Override
	public TaskRef run(RestoreOperation op, DataProtectGeneration backup, Restorable item) throws ServerFault {
		logger.info("Restoring user {}:{}", item.entryUid, item.displayName);
		ITasksManager tsk = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(ITasksManager.class);
		return tsk.run(new RestoreUserTask(backup, item));
	}

	@Override
	public List<RestoreOperation> operations() {
		RestoreOperation replace = new RestoreOperation();
		replace.identifier = "complete.restore.user";
		return Arrays.asList(replace);
	}

}
