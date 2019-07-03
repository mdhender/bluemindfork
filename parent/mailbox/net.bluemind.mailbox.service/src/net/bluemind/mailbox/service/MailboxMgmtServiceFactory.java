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
package net.bluemind.mailbox.service;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.mailbox.api.IMailboxMgmt;
import net.bluemind.mailbox.service.internal.MailboxMgmt;

public class MailboxMgmtServiceFactory implements ServerSideServiceProvider.IServerSideServiceFactory<IMailboxMgmt> {

	public MailboxMgmtServiceFactory() {

	}

	private IMailboxMgmt getService(BmContext context, String domainUid) throws ServerFault {
		return new MailboxMgmt(context, domainUid);
	}

	@Override
	public Class<IMailboxMgmt> factoryClass() {
		return IMailboxMgmt.class;
	}

	@Override
	public IMailboxMgmt instance(BmContext context, String... params) throws ServerFault {
		if (params == null || params.length < 1) {
			throw new ServerFault("wrong number of instance parameters");
		}
		return getService(context, params[0]);
	}
}
