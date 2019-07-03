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
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.dataprotect.todolist;

import java.util.Arrays;
import java.util.List;

import com.google.common.collect.ImmutableMap;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.service.ITasksManager;
import net.bluemind.dataprotect.api.DataProtectGeneration;
import net.bluemind.dataprotect.api.Restorable;
import net.bluemind.dataprotect.api.RestorableKind;
import net.bluemind.dataprotect.api.RestoreOperation;
import net.bluemind.dataprotect.service.IRestoreActionProvider;
import net.bluemind.dataprotect.todolist.impl.SendUserTodolistsICSTasks;

public class SendUserTodolistsICS implements IRestoreActionProvider {

	@Override
	public TaskRef run(RestoreOperation op, DataProtectGeneration backup, Restorable item) throws ServerFault {
		ITasksManager tsk = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(ITasksManager.class);
		return tsk.run(new SendUserTodolistsICSTasks(backup, item));
	}

	@Override
	public List<RestoreOperation> operations() {
		RestoreOperation op = new RestoreOperation();
		op.identifier = "send.todolist.ics";
		op.translations = ImmutableMap.of("en", "Send todolists ICS export by mail", "fr",
				"Envoyer des exports ICS des listes de tâches par mail");
		op.kind = RestorableKind.USER;
		return Arrays.asList(op);
	}

}
