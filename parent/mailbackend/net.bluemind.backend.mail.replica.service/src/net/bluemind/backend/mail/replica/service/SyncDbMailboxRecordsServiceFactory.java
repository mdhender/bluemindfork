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

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.backend.mail.replica.api.ISyncDbMailboxRecords;
import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.backend.mail.replica.indexing.IMailIndexService;
import net.bluemind.backend.mail.replica.indexing.NoopMailIndexService;
import net.bluemind.backend.mail.replica.indexing.RecordIndexActivator;
import net.bluemind.backend.mail.replica.persistence.MailboxRecordStore;
import net.bluemind.backend.mail.replica.service.internal.DbMailboxRecordsService;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.service.internal.ContainerStoreService;
import net.bluemind.core.rest.BmContext;

public class SyncDbMailboxRecordsServiceFactory extends AbstractMailboxRecordServiceFactory<ISyncDbMailboxRecords> {
	private static final Logger logger = LoggerFactory.getLogger(SyncDbMailboxRecordsServiceFactory.class);
	private static final IMailIndexService NOOP = new NoopMailIndexService();

	public SyncDbMailboxRecordsServiceFactory() {
		// OK
	}

	@Override
	public Class<ISyncDbMailboxRecords> factoryClass() {
		return ISyncDbMailboxRecords.class;
	}

	@Override
	protected ISyncDbMailboxRecords create(DataSource ds, Container cont, BmContext context, String mailboxUniqueId,
			MailboxRecordStore recordStore, ContainerStoreService<MailboxRecord> storeService) {
		return new DbMailboxRecordsService(ds, cont, context, mailboxUniqueId, recordStore, storeService,
				RecordIndexActivator.getIndexer().orElse(NOOP));
	}

}
