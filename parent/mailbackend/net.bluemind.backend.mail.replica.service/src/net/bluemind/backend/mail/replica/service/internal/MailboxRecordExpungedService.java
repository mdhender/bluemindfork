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
package net.bluemind.backend.mail.replica.service.internal;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.backend.mail.replica.api.IDbMailboxRecords;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.backend.mail.replica.api.IMailboxRecordExpunged;
import net.bluemind.backend.mail.replica.api.MailboxRecordExpunged;
import net.bluemind.backend.mail.replica.persistence.MailboxRecordExpungedStore;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.Count;
import net.bluemind.core.container.model.ItemFlagFilter;
import net.bluemind.core.jdbc.JdbcAbstractStore;
import net.bluemind.core.rest.BmContext;

public class MailboxRecordExpungedService implements IMailboxRecordExpunged {

	private final BmContext context;
	private final DataSource pool;
	private final MailboxRecordExpungedStore expungedStore;

	private static final Logger logger = LoggerFactory.getLogger(MailboxRecordExpungedService.class);

	public MailboxRecordExpungedService(BmContext context, DataSource pool, MailboxRecordExpungedStore store) {
		this.context = context;
		this.pool = pool;
		this.expungedStore = store;
	}

	@Override
	public void delete(long itemId) {
		JdbcAbstractStore.doOrFail(() -> {
			MailboxRecordExpunged record = get(itemId);
			Long imapUid = record.imapUid;
			Integer containerId = record.containerId;
			try {
				context.provider()
						.instance(IDbMailboxRecords.class, IMailReplicaUids.uniqueId(expungedStore.getContainerUid()))
						.deleteImapUids(Arrays.asList(imapUid));
			} catch (Exception e) {
				logger.error("Error cleaning up message {} on container {}: {}", imapUid, containerId, e.getMessage());
			}
			try {
				logger.info("Purge expunged message {} of queue for container {}", imapUid, containerId);
				expungedStore.deleteExpunged(containerId, Arrays.asList(imapUid));
			} catch (Exception e) {
				logger.error("Error cleaning up message {} on container {}: {}", imapUid, containerId, e.getMessage());
			}

			return null;
		});
	}

	@Override
	public List<MailboxRecordExpunged> fetch() {
		try {
			return expungedStore.fetch();
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	@Override
	public Count count(ItemFlagFilter filter) {
		try {
			return Count.of(expungedStore.count());
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	@Override
	public MailboxRecordExpunged get(long itemId) {
		try {
			return expungedStore.get(itemId);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}
}
