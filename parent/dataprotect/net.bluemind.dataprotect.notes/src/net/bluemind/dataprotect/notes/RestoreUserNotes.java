/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.dataprotect.notes;

import java.util.Arrays;
import java.util.List;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.service.ITasksManager;
import net.bluemind.dataprotect.api.DataProtectGeneration;
import net.bluemind.dataprotect.api.Restorable;
import net.bluemind.dataprotect.api.RestorableKind;
import net.bluemind.dataprotect.api.RestoreOperation;
import net.bluemind.dataprotect.notes.impl.RestoreNotesTask;
import net.bluemind.dataprotect.service.IRestoreActionProvider;

public class RestoreUserNotes implements IRestoreActionProvider {

	@Override
	public TaskRef run(RestoreOperation op, DataProtectGeneration backup, Restorable item) throws ServerFault {
		ITasksManager tsk = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(ITasksManager.class);
		return tsk.run(new RestoreNotesTask(backup, item));
	}

	@Override
	public List<RestoreOperation> operations() {
		RestoreOperation replace = new RestoreOperation();
		replace.identifier = "replace.notes";
		replace.kind = RestorableKind.USER;
		return Arrays.asList(replace);
	}

}
