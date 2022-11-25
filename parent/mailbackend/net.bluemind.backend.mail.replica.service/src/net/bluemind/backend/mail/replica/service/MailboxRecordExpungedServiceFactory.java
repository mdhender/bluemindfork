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
package net.bluemind.backend.mail.replica.service;

import javax.sql.DataSource;

import net.bluemind.backend.mail.replica.api.IMailboxRecordExpunged;
import net.bluemind.backend.mail.replica.service.internal.MailboxRecordExpungedService;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;

public class MailboxRecordExpungedServiceFactory
		implements ServerSideServiceProvider.IServerSideServiceFactory<IMailboxRecordExpunged> {

	@Override
	public Class<IMailboxRecordExpunged> factoryClass() {
		return IMailboxRecordExpunged.class;
	}

	@Override
	public IMailboxRecordExpunged instance(BmContext context, String... params) {
		if (params == null || params.length < 1) {
			throw new ServerFault("wrong number of instance parameters");
		}

		String serverUid = params[0];
		DataSource ds = context.getMailboxDataSource(serverUid);
		return new MailboxRecordExpungedService(context, ds, serverUid);
	}

}
