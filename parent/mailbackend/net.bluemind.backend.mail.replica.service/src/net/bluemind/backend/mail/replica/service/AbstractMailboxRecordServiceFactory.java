/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2017
 *
 * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License)
 * or the CeCILL as published by CeCILL.info (version 2 of the License).
 *
 * There are special exceptions to the terms and conditions of the
 * licenses as they are applied to this program. See LICENSE.txt in
 * the directory of this program distribution.
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
import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.backend.mail.replica.persistence.MailboxRecordStore;
import net.bluemind.backend.mail.replica.service.internal.NoopMailboxRecordService;
import net.bluemind.backend.mail.replica.service.internal.RecordsItemFlagProvider;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.DataSourceRouter;
import net.bluemind.core.container.persistence.IWeightProvider;
import net.bluemind.core.container.service.internal.ContainerStoreService;
import net.bluemind.core.container.service.internal.ContainerStoreService.IWeightSeedProvider;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;

public abstract class AbstractMailboxRecordServiceFactory<T>
		implements ServerSideServiceProvider.IServerSideServiceFactory<T> {

	private final RecordsItemFlagProvider flagsProvider;

	private final IWeightSeedProvider<MailboxRecord> recordSeedProvider;
	private final IWeightProvider toWeight;

	protected AbstractMailboxRecordServiceFactory() {
		this.flagsProvider = new RecordsItemFlagProvider();
		this.recordSeedProvider = rec -> rec.internalDate.getTime();
		this.toWeight = seed -> seed;
	}

	protected abstract T create(DataSource ds, Container cont, BmContext context, String mailboxUniqueId,
			MailboxRecordStore recordStore, ContainerStoreService<MailboxRecord> storeService);

	private T getService(BmContext context, String mailboxUniqueId) {
		String uid = IMailReplicaUids.mboxRecords(mailboxUniqueId);
		DataSource ds = DataSourceRouter.get(context, uid);
		try {
			ContainerStore cs = new ContainerStore(context, ds, context.getSecurityContext());
			Container recordsContainer = cs.get(uid);
			if (recordsContainer == null) {
				LoggerFactory.getLogger(this.getClass()).warn("Missing container {}", uid);
				return createNoopService();
			}
			MailboxRecordStore recordStore = new MailboxRecordStore(ds, recordsContainer);
			ContainerStoreService<MailboxRecord> storeService = new ContainerStoreService<>(ds,
					context.getSecurityContext(), recordsContainer, recordStore, flagsProvider, recordSeedProvider,
					toWeight);
			storeService = disableChangelogIfSystem(context, recordsContainer, storeService);
			return create(ds, recordsContainer, context, mailboxUniqueId, recordStore, storeService);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	private <W> ContainerStoreService<W> disableChangelogIfSystem(BmContext context, Container cont,
			ContainerStoreService<W> storeService) {
		try {
			DirEntry owner = context.su().provider().instance(IDirectory.class, cont.domainUid)
					.findByEntryUid(cont.owner);
			if (owner.system) {
				storeService = storeService.withoutChangelog();
			}
		} catch (Exception e) {
			// some junit might fail on missing on domains_bluemind-noid missing
		}
		return storeService;
	}

	@SuppressWarnings("unchecked")
	protected T createNoopService() {
		return (T) new NoopMailboxRecordService();
	}

	@Override
	public T instance(BmContext context, String... params) throws ServerFault {
		if (params == null || params.length < 1) {
			throw new ServerFault("wrong number of instance parameters");
		}
		String mboxUniqueId = params[0];
		return getService(context, mboxUniqueId);
	}

}
