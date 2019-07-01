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
package net.bluemind.mailshare.service.internal;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.service.DirEntryHandler;
import net.bluemind.mailshare.api.IMailshare;

public class MailshareDirHandler extends DirEntryHandler {

	@Override
	public Kind kind() {
		return Kind.MAILSHARE;
	}

	@Override
	public byte[] getIcon(BmContext context, String domainUid, String uid) throws ServerFault {
		byte[] icon = context.getServiceProvider().instance(IMailshare.class, domainUid).getIcon(uid);

		if (icon == null) {
			return super.getIcon(context, domainUid, uid);
		} else {
			return icon;
		}
	}

	@Override
	public TaskRef entryDeleted(BmContext context, String domainUid, String entryUid) throws ServerFault {
		return context.getServiceProvider().instance(IMailshare.class, domainUid).delete(entryUid);
	}

}
