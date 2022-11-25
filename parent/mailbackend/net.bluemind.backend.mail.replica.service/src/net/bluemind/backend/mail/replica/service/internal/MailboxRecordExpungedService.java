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
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.backend.mail.replica.api.IDbMailboxRecords;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.backend.mail.replica.api.IMailboxRecordExpunged;
import net.bluemind.backend.mail.replica.api.MailboxRecordExpunged;
import net.bluemind.backend.mail.replica.persistence.MailboxRecordExpungedStore;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.ContainersHierarchyNodeStore;
import net.bluemind.core.jdbc.JdbcAbstractStore;
import net.bluemind.core.rest.BmContext;

public class MailboxRecordExpungedService implements IMailboxRecordExpunged {

	private final BmContext context;
	private final DataSource pool;
	private final String serverUid;
	private final ContainerStore containerStore;
	private final MailboxRecordExpungedStore expungedStore;

	private static final Logger logger = LoggerFactory.getLogger(MailboxRecordExpungedService.class);

	public MailboxRecordExpungedService(BmContext context, DataSource pool, String serverUid) {
		this.context = context;
		this.pool = pool;
		this.serverUid = serverUid;
		this.containerStore = new ContainerStore(context, pool, context.getSecurityContext());
		this.expungedStore = new MailboxRecordExpungedStore(pool);
	}

	@Override
	public void delete(int days) {
		JdbcAbstractStore.doOrFail(() -> {
			logger.info("Expiring expunged messages ({} days) on server {}", days, serverUid);
			List<MailboxRecordExpunged> expiredItems;
			do {
				expiredItems = expungedStore.getExpiredItems(days);
				logger.info("Found {} message expiring to delete", expiredItems.size());

				Map<Integer, List<Long>> partitioned = expiredItems.stream()
						.collect(Collectors.groupingBy(MailboxRecordExpunged::containerId,
								Collectors.mapping(rec -> rec.imapUid, Collectors.toList())));

				partitioned.entrySet().forEach(entry -> {
					List<Long> imapUids = entry.getValue();
					Integer containerId = entry.getKey();
					try {
						Container container = containerStore.get(containerId);
						logger.info("Expiring {} messages of container {}", imapUids.size(), containerId);
						context.provider().instance(IDbMailboxRecords.class, IMailReplicaUids.uniqueId(container.uid))
								.deleteImapUids(imapUids);
					} catch (SQLException e) {
						logger.error("Error retrieving container {}: {}", containerId, e.getMessage());
					} catch (Exception e) {
						logger.error("Error cleaning up expiring messages on container {}: {}", containerId,
								e.getMessage());
					}
					try {
						logger.info("Purge {} expunged messages of queue for container {}", imapUids.size(),
								containerId);
						expungedStore.deleteExpunged(containerId, imapUids);
					} catch (Exception e) {
						logger.error("Error cleaning up expunged messages on container {}: {}", containerId,
								e.getMessage());
					}
				});
			} while (expiredItems.size() > 0);

			new ContainersHierarchyNodeStore(pool, null).removeDeletedRecords(days);

			return null;
		});
	}
}
