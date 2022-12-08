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

import java.sql.SQLException;

import javax.sql.DataSource;

import org.slf4j.LoggerFactory;

import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.backend.mail.replica.api.IMailboxRecordExpunged;
import net.bluemind.backend.mail.replica.persistence.MailboxRecordExpungedStore;
import net.bluemind.backend.mail.replica.service.internal.MailboxRecordExpungedService;
import net.bluemind.backend.mail.replica.service.internal.NoopMailboxRecordExpungedService;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.DataSourceRouter;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;

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

		String folderUid = params[0];

		String uid = IMailReplicaUids.mboxRecords(folderUid);
		DataSource ds = DataSourceRouter.get(context, uid);

		try {
			ContainerStore cs = new ContainerStore(context, ds, context.getSecurityContext());
			Container recordsContainer = cs.get(uid);
			if (recordsContainer == null) {
				LoggerFactory.getLogger(this.getClass()).warn("Missing container {}", uid);
				return createNoopService();
			}

			IMailboxes mailboxesApi = context.su().provider().instance(IMailboxes.class, recordsContainer.domainUid);
			ItemValue<Mailbox> mailbox = mailboxesApi.getComplete(recordsContainer.owner);
			if (mailbox == null) {
				throw ServerFault.notFound("mailbox of " + recordsContainer.owner + " not found");
			}
			String subtreeContainerUid = IMailReplicaUids.subtreeUid(recordsContainer.domainUid, mailbox);
			Container subtreeContainer = cs.get(subtreeContainerUid);
			if (subtreeContainer == null) {
				LoggerFactory.getLogger(this.getClass()).warn("Missing subtree container {}", subtreeContainerUid);
				return createNoopService();
			}
			MailboxRecordExpungedStore recordStore = new MailboxRecordExpungedStore(ds, recordsContainer,
					subtreeContainer);
			return new MailboxRecordExpungedService(context, ds, recordStore);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	protected IMailboxRecordExpunged createNoopService() {
		return new NoopMailboxRecordExpungedService();
	}

}
