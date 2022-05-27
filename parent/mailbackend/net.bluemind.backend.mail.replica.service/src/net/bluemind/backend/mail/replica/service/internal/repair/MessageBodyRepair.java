/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2019
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.backend.mail.replica.service.internal.repair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.event.Level;

import com.google.common.collect.ImmutableSet;

import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.backend.mail.api.MessageBody;
import net.bluemind.backend.mail.parsing.BodyStreamProcessor;
import net.bluemind.backend.mail.replica.api.IDbMailboxRecords;
import net.bluemind.backend.mail.replica.api.IDbMessageBodies;
import net.bluemind.backend.mail.replica.api.IDbReplicatedMailboxes;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.backend.mail.replica.api.ImapBinding;
import net.bluemind.core.api.Stream;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.MaintenanceOperation;
import net.bluemind.directory.service.IDirEntryRepairSupport;
import net.bluemind.directory.service.RepairTaskMonitor;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.Mailbox.Type;

/**
 * Update message bodies having a lower version than the one specified in
 * {@link BodyStreamProcessor}. <br>
 * When an update is needed, then fetch the IMAP mail content, parse it and
 * replace the whole data in DB.
 * 
 * @see t_message_body.body_version
 * @see MessageBody#bodyVersion
 * @see BodyStreamProcessor#BODY_VERSION
 */
public class MessageBodyRepair implements IDirEntryRepairSupport {

	private static final MaintenanceOperation MAINTENANCE_OPERATION = MaintenanceOperation
			.create(IMailReplicaUids.REPAIR_MESSAGE_BODIES, "Message bodies repair");

	private BmContext bmContext;

	public MessageBodyRepair(BmContext context) {
		this.bmContext = context;
	}

	public static class RepairFactory implements IDirEntryRepairSupport.Factory {
		@Override
		public IDirEntryRepairSupport create(BmContext context) {
			return new MessageBodyRepair(context);
		}
	}

	@Override
	public Set<MaintenanceOperation> availableOperations(Kind kind) {
		if (kind == Kind.USER || kind == Kind.MAILSHARE || kind == Kind.GROUP || kind == Kind.RESOURCE) {
			return ImmutableSet.of(MAINTENANCE_OPERATION);
		} else {
			return Collections.emptySet();
		}
	}

	@Override
	public Set<InternalMaintenanceOperation> ops(Kind kind) {
		if (kind == Kind.USER || kind == Kind.MAILSHARE || kind == Kind.GROUP || kind == Kind.RESOURCE) {
			return ImmutableSet.of(new MessageBodyMaintenance(bmContext));
		} else {
			return Collections.emptySet();
		}
	}

	public static class MessageBodyMaintenance extends InternalMaintenanceOperation {

		private static final String SUCCESS = "SUCCESS";
		private static final String NOTHING_TO_DO = "NOTHING TO DO";
		private final BmContext bmContext;

		public MessageBodyMaintenance(BmContext bmContext) {
			super(MAINTENANCE_OPERATION.identifier, null, null, 1);
			this.bmContext = bmContext;
		}

		/** Check that message bodies need update. */
		@Override
		public void check(final String domainUid, final DirEntry entry, RepairTaskMonitor monitor) {
			monitor.log("Check message bodies {} {}", domainUid, entry);
			this.checkOrRepair(domainUid, entry, monitor, true);
			monitor.end();
		}

		/** Update message bodies if needed. */
		@Override
		public void repair(final String domainUid, final DirEntry entry, RepairTaskMonitor monitor) {
			monitor.log("Repair message bodies {} {}", domainUid, entry);
			this.checkOrRepair(domainUid, entry, monitor, false);
			monitor.end();
		}

		private void checkOrRepair(final String domainUid, final DirEntry entry, RepairTaskMonitor monitor,
				final boolean checkMode) {
			final IMailboxes mboxApi = bmContext.provider().instance(IMailboxes.class, domainUid);
			final ItemValue<Mailbox> mailbox = mboxApi.getComplete(entry.entryUid);
			if (mailbox == null) {
				monitor.log("{} does not have a mailbox, nothing to repair", entry);
				return;
			}
			if (mailbox.value.dataLocation == null) {
				monitor.log("{} lacks a dataLocation, can't repair", mailbox, Level.WARN);
				return;
			}

			final CyrusPartition cyrusPartition = CyrusPartition.forServerAndDomain(mailbox.value.dataLocation,
					domainUid);
			final String replicatedMailboxIdentifier = mailbox.value.type.nsPrefix
					+ mailbox.value.name.replace(".", "^");
			final IDbReplicatedMailboxes replicatedMailboxesService = ServerSideServiceProvider
					.getProvider(SecurityContext.SYSTEM)
					.instance(IDbReplicatedMailboxes.class, cyrusPartition.name, replicatedMailboxIdentifier);

			// loop aver mailbox folders
			replicatedMailboxesService.all().forEach(mailboxFolder -> {
				final List<ImapBinding> mailboxRecordsNeedingUpdate;
				try {
					final IDbMailboxRecords mailboxRecordService = ServerSideServiceProvider.getProvider(bmContext)
							.instance(IDbMailboxRecords.class, mailboxFolder.uid);
					mailboxRecordsNeedingUpdate = mailboxRecordService
							.havingBodyVersionLowerThan(BodyStreamProcessor.BODY_VERSION);
				} catch (ServerFault e) {
					monitor.notify("{} {}, can't repair", mailbox, e.getMessage());
					return;
				}

				final String mailboxReportId = String.format("%s.%s@%s", mailbox.value.name, mailboxFolder.value.name,
						domainUid);

				if (checkMode) {
					this.check(mailboxReportId, mailboxRecordsNeedingUpdate,
							(RepairTaskMonitor) monitor.subWork(mailboxReportId, 1));
				} else {
					this.repair(cyrusPartition, mailboxFolder, mailboxReportId, mailboxRecordsNeedingUpdate,
							(RepairTaskMonitor) monitor.subWork(mailboxReportId, 1), mailbox, domainUid);
				}
			});
		}

		private void repair(CyrusPartition cyrusPartition, ItemValue<MailboxFolder> mailboxFolder,
				String mailboxReportId, List<ImapBinding> mailboxRecordsNeedingUpdate, RepairTaskMonitor monitor,
				ItemValue<Mailbox> mailbox, String domainUid) {
			if (!mailboxRecordsNeedingUpdate.isEmpty()) {
				monitor.begin(mailboxRecordsNeedingUpdate.size(), "Repairing bodies for mailbox " + mailboxReportId);

				try {
					// loop over mailbox records
					BmContext ctx = bmContext;
					if (mailbox.value.type == Type.user) {
						ctx = bmContext.su(mailbox.uid, domainUid);
					}
					final IDbMailboxRecords mailboxRecordService = ServerSideServiceProvider.getProvider(ctx)
							.instance(IDbMailboxRecords.class, mailboxFolder.uid);
					final IDbMessageBodies bodiesService = ServerSideServiceProvider.getProvider(bmContext)
							.instance(IDbMessageBodies.class, cyrusPartition.name);
					final List<CompletableFuture<Void>> completableFutures = new ArrayList<>();
					int ok = 0;
					int ko = 0;
					for (final ImapBinding mailboxRecord : mailboxRecordsNeedingUpdate) {
						try {
							final Stream stream = mailboxRecordService.fetchComplete(mailboxRecord.imapUid);
							final CompletableFuture<Void> completableFuture = this
									.createBodyProcessorTask(bodiesService, stream, mailboxRecord.bodyGuid);
							completableFutures.add(completableFuture);
							ok++;
						} catch (Exception e) {
							monitor.notify("Problem with binding {}", mailboxRecord);
							ko++;
						}
						monitor.progress(1, null);
					}
					monitor.log("Finished for folder " + mailboxFolder.displayName + ", ok: " + ok + ", ko: " + ko);

					// wait - a long time - for all tasks to be completed
					final CompletableFuture<Void> globalFuture = CompletableFuture
							.allOf(completableFutures.toArray(new CompletableFuture[0]));
					globalFuture.get(1, TimeUnit.HOURS);
				} catch (Exception e) {
					monitor.notify("Message bodies update FAILURE: {}", e.getMessage());
				}
			}
		}

		private void check(String mailboxReportId, List<ImapBinding> mailboxRecordsNeedingUpdate,
				RepairTaskMonitor monitor) {
			String monitorMessage = String.format("Checking bodies for mailbox %s", mailboxReportId);
			monitor.begin(0, monitorMessage);
			if (!mailboxRecordsNeedingUpdate.isEmpty()) {
				String resultMessage = String.format("WARN %d bodies need update", mailboxRecordsNeedingUpdate.size());
				monitor.notify(resultMessage);
			}
		}

		/**
		 * Prepare a future task for parsing the IMAP message using
		 * {@link BodyStreamProcessor} and update {@link MessageBody} in DB.
		 */
		private CompletableFuture<Void> createBodyProcessorTask(final IDbMessageBodies bodiesService,
				final Stream imapMessageStream, final String messageBodyGuid) {
			return BodyStreamProcessor.processBody(imapMessageStream).thenAccept(bodyData -> {
				bodyData.body.guid = messageBodyGuid;
				bodiesService.update(bodyData.body);
			}).exceptionally(ex -> null);
		}

	}

}
