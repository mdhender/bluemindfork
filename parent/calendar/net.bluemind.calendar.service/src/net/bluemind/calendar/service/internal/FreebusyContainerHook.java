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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.calendar.service.internal;

import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.calendar.api.IFreebusyMgmt;
import net.bluemind.calendar.api.IFreebusyUids;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.hooks.ContainersHookAdapter;
import net.bluemind.core.container.hooks.IContainersHook;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.rest.BmContext;
import net.bluemind.directory.api.BaseDirEntry;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;

public class FreebusyContainerHook extends ContainersHookAdapter implements IContainersHook {

	@Override
	public void onContainerDeleted(BmContext ctx, ContainerDescriptor cd) throws ServerFault {
		if (cd.type.equals(ICalendarUids.TYPE)) {
			IDirectory dir = ctx.provider().instance(IDirectory.class, cd.domainUid);
			DirEntry ownerEntry = dir.findByEntryUid(cd.owner);
			if (ownerEntry != null && ownerEntry.kind == BaseDirEntry.Kind.USER) {
				ctx.provider().instance(IFreebusyMgmt.class, IFreebusyUids.getFreebusyContainerUid(ownerEntry.entryUid))
						.remove(cd.uid);
			}
		}
	}

}
