/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2018
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

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Suppliers;
import com.google.common.collect.Lists;

import net.bluemind.backend.mail.replica.api.IDbMailboxRecords;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.backend.mail.replica.api.IReplicatedDataExpiration;
import net.bluemind.backend.mail.replica.indexing.RecordIndexActivator;
import net.bluemind.backend.mail.replica.persistence.MailboxRecordStore;
import net.bluemind.backend.mail.replica.persistence.MailboxRecordStore.MailboxRecordItemV;
import net.bluemind.backend.mail.replica.persistence.MessageBodyStore;
import net.bluemind.backend.mail.replica.service.sds.MessageBodyObjectStore;
import net.bluemind.core.container.persistence.ContainersHierarchyNodeStore;
import net.bluemind.core.jdbc.JdbcAbstractStore;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.service.BlockingServerTask;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.core.task.service.ITasksManager;

public class ReplicatedDataExpirationService implements IReplicatedDataExpiration {

	private final BmContext context;
	private final DataSource pool;
	private final String serverUid;
	private final MessageBodyStore bodyStore;
	private final Supplier<MessageBodyObjectStore> bodyObjectStore;

	private static final Logger logger = LoggerFactory.getLogger(ReplicatedDataExpirationService.class);

	public ReplicatedDataExpirationService(BmContext context, DataSource pool, String serverUid) {
		this.context = context;
		this.pool = pool;
		this.serverUid = serverUid;
		this.bodyStore = new MessageBodyStore(pool);
		this.bodyObjectStore = Suppliers.memoize(() -> new MessageBodyObjectStore(context));
	}

	@Override
	public TaskRef deleteExpired(int days) {

		return context.provider().instance(ITasksManager.class).run(m -> BlockingServerTask.run(m, monitor -> {
			monitor.begin(1, "Expiring expunged messages (" + days + " days) on server " + serverUid);
			logger.info("Expiring expunged messages ({} days) on server {}", days, serverUid);

			MailboxRecordStore store = new MailboxRecordStore(pool);
			List<MailboxRecordItemV> expiredItems = store.getExpiredItems(days);

			Map<String, List<Long>> partitioned = expiredItems.stream()
					.collect(Collectors.groupingBy(MailboxRecordItemV::containerUid,
							Collectors.mapping(rec -> rec.item().value.imapUid, Collectors.toList())));

			partitioned.entrySet().forEach(entry -> {
				String mboxUniqueId = IMailReplicaUids.uniqueId(entry.getKey());
				try {
					List<Long> imapUids = entry.getValue();
					logger.info("Expiring {} messages of mailbox {}", imapUids.size(), mboxUniqueId);
					monitor.log("Expiring " + imapUids.size() + " messages of mailbox " + mboxUniqueId);

					context.provider().instance(IDbMailboxRecords.class, mboxUniqueId).deleteImapUids(imapUids);
				} catch (Exception e) {
					logger.error("Error cleaning up {}: {}", mboxUniqueId, e.getMessage());
				}
			});

			new ContainersHierarchyNodeStore(pool, null).removeDeletedRecords(days);

			monitor.end(true, "", "");

		}));

	}

	@Override
	public void deleteOrphanMessageBodies() {

		JdbcAbstractStore.doOrFail(() -> {
			List<String> deletedOrphanBodies = bodyStore.deleteOrphanBodies();

			logger.info("Deleting {} orphan message bodies", deletedOrphanBodies.size());

			if (!deletedOrphanBodies.isEmpty()) {
				RecordIndexActivator.getIndexer().ifPresent(service -> service.deleteBodyEntries(deletedOrphanBodies));
			}
			return null;
		});
	}

	@Override
	public TaskRef deleteMessageBodiesFromObjectStore(int days) {
		return context.provider().instance(ITasksManager.class).run(m -> BlockingServerTask.run(m, monitor -> {
			long totalRemoved = 0;
			long removedRows = 0;
			monitor.begin(1, "Expiring expunged messages (" + days + " days) on server " + serverUid);
			MessageBodyObjectStore sdsStore = bodyObjectStore.get();
			Instant from = Instant.now().minusSeconds(TimeUnit.DAYS.toSeconds(days));
			do {
				List<String> guids = JdbcAbstractStore.doOrFail(() -> bodyStore.deletePurgedBodies(from, 10000));
				removedRows = guids.size();
				totalRemoved += removedRows;
				if (sdsStore != null && !guids.isEmpty()) {
					removeFromSdsStore(monitor, sdsStore, guids);
				}
			} while (removedRows > 0);
			monitor.end(true, "removed " + totalRemoved + " bodies", "");
		}));
	}

	private void removeFromSdsStore(IServerTaskMonitor monitor, MessageBodyObjectStore sdsStore, List<String> guids) {
		logger.info("Removing {} from object storage", guids.size());
		for (List<String> partitionedGuids : Lists.partition(guids, 100)) {
			monitor.log("Removing " + partitionedGuids.size() + " objects from object storage");
			try {
				sdsStore.delete(partitionedGuids);
			} catch (Exception e) {
				String guidListString = partitionedGuids.stream().collect(Collectors.joining(","));
				logger.error("sdsStore.delete() failed on guids: [{}]", guidListString, e);
				monitor.log("sdsStore.delete() failed on guids: " + guidListString);
			}
		}
	}
}
