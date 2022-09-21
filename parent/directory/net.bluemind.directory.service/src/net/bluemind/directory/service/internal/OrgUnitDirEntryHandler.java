/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.directory.service.internal;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.service.BlockingServerTask;
import net.bluemind.core.task.service.ITasksManager;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IOrgUnits;
import net.bluemind.directory.api.OrgUnit;
import net.bluemind.directory.service.DirEntryHandler;

public class OrgUnitDirEntryHandler extends DirEntryHandler {

	@Override
	public Kind kind() {
		return DirEntry.Kind.ORG_UNIT;
	}

	@Override
	public byte[] getIcon(BmContext context, String domainUid, String uid) throws ServerFault {
		return super.getIcon(context, domainUid, uid);
	}

	@Override
	public TaskRef entryDeleted(BmContext context, String domainUid, String entryUid) throws ServerFault {

		return context.provider().instance(ITasksManager.class).run(m -> BlockingServerTask.run(m, monitor -> {
			IOrgUnits service = context.getServiceProvider().instance(IOrgUnits.class, domainUid);
			deleteHierarchy(service, entryUid);
		}));

	}

	private void deleteHierarchy(IOrgUnits service, String ou) {
		for (ItemValue<OrgUnit> child : service.getChildren(ou)) {
			deleteHierarchy(service, child.uid);
		}
		service.delete(ou);
	}

}
