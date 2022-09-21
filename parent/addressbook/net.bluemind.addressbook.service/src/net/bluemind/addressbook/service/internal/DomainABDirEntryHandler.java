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
package net.bluemind.addressbook.service.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.addressbook.api.IAddressBooksMgmt;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.service.BlockingServerTask;
import net.bluemind.core.task.service.ITasksManager;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.service.DirEntryHandler;

public class DomainABDirEntryHandler extends DirEntryHandler {
	private static final Logger logger = LoggerFactory.getLogger(DomainABDirEntryHandler.class);

	@Override
	public Kind kind() {
		return Kind.ADDRESSBOOK;
	}

	@Override
	public TaskRef entryDeleted(BmContext context, String domainUid, String entryUid) throws ServerFault {

		return context.provider().instance(ITasksManager.class).run(m -> BlockingServerTask.run(m, monitor -> {
			try {
				context.provider().instance(IAddressBooksMgmt.class, domainUid).delete(entryUid);
			} catch (ServerFault e) {
				if (e.getCode() == ErrorCode.NOT_FOUND) {
					logger.warn("domianbook {}@{} not found, continue..", entryUid, domainUid);
					monitor.end(true, "domianbook " + entryUid + "@" + domainUid + " not found, continue..", "[]");
				} else {
					monitor.end(false, e.getMessage(), "[]");
				}
			}

			monitor.end(true, String.format("domainbook %s@%s deleted ", entryUid, domainUid), "[]");
		}));
	}

}
