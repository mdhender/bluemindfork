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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

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
	private final MailboxRecordExpungedStore expungedStore;

	private static final Logger logger = LoggerFactory.getLogger(MailboxRecordExpungedService.class);

	public MailboxRecordExpungedService(BmContext context, MailboxRecordExpungedStore store) {
		this.context = context;
		this.expungedStore = store;
	}

	@Override
	public void delete(long itemId) {
		multipleDelete(List.of(itemId));
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

	@Override
	public void multipleDelete(List<Long> itemIds) {
		JdbcAbstractStore.doOrFail(() -> {
			Map<Integer, List<Long>> imapUidsByContainerUid = itemIds.stream().map(itemId -> get(itemId))
					.collect(Collectors.groupingBy(data -> data.containerId,
							Collectors.mapping(MailboxRecordExpunged::imapUid, Collectors.toList())));
			for (Entry<Integer, List<Long>> entry : imapUidsByContainerUid.entrySet()) {
				Integer containerId = entry.getKey();
				List<Long> imapUids = entry.getValue();

				try {
					context.provider()
							.instance(IDbMailboxRecords.class,
									IMailReplicaUids.uniqueId(expungedStore.getContainerUid()))
							.deleteImapUids(imapUids);
					logger.info("Purge expunged message {} of queue for container {}", imapUids.toString(),
							containerId);
					expungedStore.deleteExpunged(containerId, imapUids);
				} catch (Exception e) {
					logger.error("Error cleaning up message {} on container {}: {}", imapUids.toString(), containerId,
							e.getMessage());
				}
			}

			return null;
		});

	}
}
