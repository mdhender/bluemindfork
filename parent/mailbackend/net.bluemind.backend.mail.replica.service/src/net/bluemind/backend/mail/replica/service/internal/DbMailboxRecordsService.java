/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.backend.mail.replica.service.internal;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.json.JsonObject;
import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.backend.mail.api.MailboxItem.SystemFlag;
import net.bluemind.backend.mail.api.MessageBody;
import net.bluemind.backend.mail.api.MessageBody.RecipientKind;
import net.bluemind.backend.mail.replica.api.IDbByContainerReplicatedMailboxes;
import net.bluemind.backend.mail.replica.api.IDbMailboxRecords;
import net.bluemind.backend.mail.replica.api.IDbMessageBodies;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.backend.mail.replica.api.ImapBinding;
import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.backend.mail.replica.api.MailboxRecord.InternalFlag;
import net.bluemind.backend.mail.replica.api.MailboxReplicaRootDescriptor.Namespace;
import net.bluemind.backend.mail.replica.indexing.IDSet;
import net.bluemind.backend.mail.replica.indexing.IMailIndexService;
import net.bluemind.backend.mail.replica.indexing.IMailIndexService.BulkOperation;
import net.bluemind.backend.mail.replica.persistence.MailboxRecordStore;
import net.bluemind.backend.mail.replica.persistence.RecordID;
import net.bluemind.backend.mail.replica.persistence.ReplicasStore;
import net.bluemind.backend.mail.replica.persistence.ReplicasStore.SubtreeLocation;
import net.bluemind.backend.mail.replica.service.internal.BodyInternalIdCache.ExpectedId;
import net.bluemind.backend.mail.replica.service.internal.BodyInternalIdCache.VanishedBody;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.ItemVersion;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.DataSourceRouter;
import net.bluemind.core.container.service.internal.ContainerStoreService;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.service.ITasksManager;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;

public class DbMailboxRecordsService extends BaseMailboxRecordsService implements IDbMailboxRecords {

	private static final Logger logger = LoggerFactory.getLogger(DbMailboxRecordsService.class);

	private Optional<ItemValue<MailboxFolder>> mboxFolder = Optional.empty();

	private final IMailIndexService indexService;

	public DbMailboxRecordsService(DataSource ds, Container cont, BmContext context, String mailboxUniqueId,
			MailboxRecordStore recordStore, ContainerStoreService<MailboxRecord> storeService,
			IMailIndexService index) {
		super(cont, context, mailboxUniqueId, recordStore, storeService, new ReplicasStore(ds));
		if (ds == context.getDataSource()) {
			throw new ServerFault("Service is invoked with directory datasource for " + cont.uid + ".");
		}
		this.indexService = index;
	}

	@Override
	public ItemValue<MailboxRecord> getComplete(String uid) {
		return storeService.get(uid, null);
	}

	@Override
	public ItemValue<MailboxRecord> getCompleteById(long id) {
		return storeService.get(id, null);
	}

	@Override
	public List<ImapBinding> imapBindings(List<Long> itemIds) {
		try {
			return recordStore.bindings(itemIds);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	@Override
	public void create(String uid, MailboxRecord mail) {
		SubtreeLocation recordsLocation = locationOrFault();

		ExpectedId knownInternalId = BodyInternalIdCache.expectedRecordId(container.owner, mail.messageBody);
		ItemVersion version = null;
		boolean isUpdate = false;
		if (knownInternalId != null) {
			logger.info("************************ Create from replication with a preset id {}", knownInternalId);
			if (knownInternalId.updateOfBody == null) {
				version = storeService.createWithId(uid, knownInternalId.id, null, uid, mail);
			} else {
				logger.info("********** UPDATE by id to point record to new message body");
				version = storeService.update(knownInternalId.id, uid, mail);
				isUpdate = true;
			}
			BodyInternalIdCache.invalidateBody(mail.messageBody);
		} else {
			version = storeService.create(uid, uid, mail);
		}
		if (!isUpdate) {
			logger.info("Sending event for created item {}v{}", version.id, version);
			EmitReplicationEvents.recordCreated(mailboxUniqueId, version.version, version.id, mail.imapUid);
			EmitReplicationEvents.mailboxChanged(recordsLocation, container, mailboxUniqueId, version.version,
					new long[] { version.id }, version.id);
		} else {
			logger.info("Sending event for replaced item {}v{}", version.id, version);
			EmitReplicationEvents.recordUpdated(mailboxUniqueId, version, mail);
			EmitReplicationEvents.mailboxChanged(recordsLocation, container, mailboxUniqueId, version.version,
					new long[] { version.id });
		}
	}

	private SubtreeLocation locationOrFault() {
		return optRecordsLocation.orElseThrow(() -> new ServerFault("Missing subtree location"));
	}

	private void index(ItemValue<MailboxRecord> mail, Optional<BulkOperation> op) {
		if (logger.isDebugEnabled()) {
			SubtreeLocation recordsLocation = locationOrFault();

			logger.debug("Indexing mail in mailbox {}:{}@{} in folder {}", mailboxUniqueId,
					recordsLocation.subtreeContainer, recordsLocation.partition, recordsLocation.boxName);
		}
		indexService.storeMessage(mailboxUniqueId, mail, container.owner, op);
	}

	private ItemValue<MailboxFolder> getFolder() {

		if (!mboxFolder.isPresent()) {
			try {
				SubtreeLocation recordsLocation = locationOrFault();

				IDbByContainerReplicatedMailboxes foldersApi = context.provider()
						.instance(IDbByContainerReplicatedMailboxes.class, recordsLocation.subtreeContainer);
				mboxFolder = Optional.of(foldersApi.getComplete(mailboxUniqueId));
			} catch (ServerFault sf) {
				logger.error("Fail to fetch folder {}", mailboxUniqueId, sf);
				return null;
			}
		}

		return mboxFolder.get();

	}

	@Override
	public void update(String uid, MailboxRecord mail) {
		SubtreeLocation recordsLocation = locationOrFault();
		ItemVersion upd = storeService.update(uid, uid, mail);
		EmitReplicationEvents.recordUpdated(mailboxUniqueId, upd, mail);

		EmitReplicationEvents.mailboxChanged(recordsLocation, container, mailboxUniqueId, upd.version,
				new long[] { upd.id });
	}

	@Override
	public void delete(String uid) {
		SubtreeLocation recordsLocation = locationOrFault();

		ItemVersion iv = storeService.delete(uid);
		EmitReplicationEvents.mailboxChanged(recordsLocation, container, mailboxUniqueId, iv.version,
				new long[] { iv.id });
	}

	@Override
	public List<ItemValue<MailboxRecord>> all() {
		return storeService.all();
	}

	private static class CreateNotif {

		long version;
		long itemId;
		long imapUid;

		public static CreateNotif of(long v, long id, long imapUid) {
			CreateNotif cn = new CreateNotif();
			cn.version = v;
			cn.itemId = id;
			cn.imapUid = imapUid;
			return cn;
		}
	}

	private static class UpdateNotif {
		ItemVersion itemUpdate;
		MailboxRecord mr;

		public static UpdateNotif of(ItemVersion upd, MailboxRecord r) {
			UpdateNotif cn = new UpdateNotif();
			cn.itemUpdate = upd;
			cn.mr = r;
			return cn;
		}
	}

	@Override
	public ItemValue<MailboxRecord> getCompleteByImapUid(long imapUid) {
		try {
			Set<RecordID> idSet = recordStore.identifiers(new long[] { imapUid });
			if (idSet.isEmpty()) {
				logger.warn("No record with imap uid {}", imapUid);
				return null;
			} else {
				RecordID rec = idSet.iterator().next();
				return getComplete(rec.itemUid);
			}
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	private static class UpsertResult {
		final ItemVersion version;
		final boolean update;

		public UpsertResult(ItemVersion v, boolean b) {
			this.version = v;
			this.update = b;
		}

		public static UpsertResult create(ItemVersion v) {
			return new UpsertResult(v, false);
		}

		public static UpsertResult update(ItemVersion v) {
			return new UpsertResult(v, true);
		}
	}

	private UpsertResult upsertByUid(String uid, MailboxRecord mr) {
		try {
			return UpsertResult.create(storeService.create(uid, uid, mr));
		} catch (ServerFault sf) {
			logger.warn("create failed: {}, trying update of {}", sf.getMessage(), uid);
			return UpsertResult.update(storeService.update(uid, uid, mr));
		}
	}

	private UpsertResult upsertById(String uid, long id, MailboxRecord mr) {
		try {
			return UpsertResult.create(storeService.createWithId(uid, id, null, uid, mr));
		} catch (ServerFault sf) {
			logger.warn("createById failed: {}, trying updateById of uid: {}, id: {}", sf.getMessage(), uid, id);
			return UpsertResult.update(storeService.update(id, uid, mr));
		}
	}

	@Override
	public void updates(List<MailboxRecord> records) {
		logger.info("[{}] Update with {} record(s)", mailboxUniqueId, records.size());
		long time = System.currentTimeMillis();
		List<CreateNotif> crNotifs = new LinkedList<>();
		List<UpdateNotif> upNotifs = new LinkedList<>();

		List<ItemValue<MailboxRecord>> pushToIndex = new ArrayList<>(records.size());
		List<ItemValue<MailboxRecord>> newMailNotification = new LinkedList<>();
		SubtreeLocation recordsLocation = locationOrFault();

		long contVersion = storeService.doOrFail(() -> {
			long[] uidArrays = records.stream().mapToLong(rec -> rec.imapUid).toArray();
			Set<RecordID> ids = recordStore.identifiers(uidArrays);
			Map<Long, RecordID> dbByUid = ids.stream().collect(Collectors.toMap(r -> r.imapUid, r -> r));
			Set<RecordID> toUpdateRecords = records.stream().map(mr -> new RecordID(mr.imapUid, mr.modSeq))
					.collect(Collectors.toSet());
			Map<Long, MailboxRecord> newRecsByUid = records.stream().collect(Collectors.toMap(r -> r.imapUid, r -> r));
			Map<String, MailboxRecord> toUpdate = new HashMap<>();
			List<MailboxRecord> toCreate = new LinkedList<>();

			for (RecordID createOrUpdate : toUpdateRecords) {
				MailboxRecord touchedMailRecord = newRecsByUid.get(createOrUpdate.imapUid);
				RecordID asRecID = dbByUid.get(createOrUpdate.imapUid);
				if (asRecID == null) {
					toCreate.add(touchedMailRecord);
				} else {
					toUpdate.put(asRecID.itemUid, touchedMailRecord);
				}
			}
			// apply the changes
			toCreate.forEach((MailboxRecord mr) -> {
				if (mr.internalFlags.contains(InternalFlag.expunged)) {
					logger.debug("Skipping create on expunged record");
					return;
				}
				VanishedBody vanished = BodyInternalIdCache.vanishedBody(container.owner, mr.messageBody);
				if (vanished != null) {
					logger.info("Don't touch {} {} as it vanished", mr.imapUid, mr.messageBody);
					expungeIndex(Arrays.asList(mr.imapUid));
					upNotifs.add(UpdateNotif.of(vanished.version, mr));
					return;
				}

				String uid = mr.imapUid + ".";
				UpsertResult upsert = null;

				Long expId = GuidExpectedIdCache
						.expectedId(IMailReplicaUids.uniqueId(container.uid) + ":" + mr.messageBody);

				if (expId != null) {
					upsert = UpsertResult.create(storeService.createWithId(uid, expId, null, uid, mr));
					GuidExpectedIdCache.invalidate(IMailReplicaUids.uniqueId(container.uid) + ":" + mr.messageBody);
				} else {
					ExpectedId knownInternalId = BodyInternalIdCache.expectedRecordId(container.owner, mr.messageBody);
					if (knownInternalId == null) {
						upsert = upsertByUid(uid, mr);
					} else {
						logger.info("Create directly with the right id {} from replication.", knownInternalId);
						if (knownInternalId.updateOfBody == null) {
							upsert = upsertById(uid, knownInternalId.id, mr);
						} else {
							try {
								logger.info("Update record {} to point to a different body {}", knownInternalId,
										mr.messageBody);
								upsert = UpsertResult.update(storeService.update(knownInternalId.id, uid, mr));
								BodyInternalIdCache.vanishedBody(container.owner,
										knownInternalId.updateOfBody).version = upsert.version;

							} catch (ServerFault sf) {
								logger.warn("[{}] Update of {} failed: {}", container.uid, knownInternalId.id,
										sf.getMessage());
								try {
									upsert = UpsertResult
											.create(storeService.createWithId(uid, knownInternalId.id, null, uid, mr));
								} catch (ServerFault refault) {
									logger.warn("byId global failure: {}", refault.getMessage());
									upsert = upsertByUid(uid, mr);
								}
							}
						}
						BodyInternalIdCache.invalidateBody(mr.messageBody);
					}
				}
				if (!upsert.update) {
					crNotifs.add(CreateNotif.of(upsert.version.version, upsert.version.id, mr.imapUid));
				} else {
					upNotifs.add(UpdateNotif.of(upsert.version, mr));
				}

				ItemValue<MailboxRecord> idxAndNotif = ItemValue.create(uid, mr);
				idxAndNotif.internalId = upsert.version.id;
				idxAndNotif.version = upsert.version.version;

				pushToIndex.add(idxAndNotif);
				if ("INBOX".equals(recordsLocation.boxName) && recordsLocation.namespace() == Namespace.users
						&& !idxAndNotif.value.systemFlags.contains(SystemFlag.seen)
						&& !idxAndNotif.value.systemFlags.contains(SystemFlag.deleted)) {
					newMailNotification.add(idxAndNotif);
				}
			});

			AtomicInteger softDelete = new AtomicInteger();
			toUpdate.forEach((String uid, MailboxRecord mr) -> {
				VanishedBody vanished = BodyInternalIdCache.vanishedBody(container.owner, mr.messageBody);
				if (vanished != null) {
					logger.info("Using version from vanished item {} and the old imap uid", vanished);
					expungeIndex(Arrays.asList(mr.imapUid));
					upNotifs.add(UpdateNotif.of(vanished.version, mr));
				} else {
					ItemVersion upd = storeService.update(uid, uid, mr);
					if (mr.systemFlags.contains(SystemFlag.deleted)) {
						softDelete.incrementAndGet();
					}

					ItemValue<MailboxRecord> asItem = ItemValue.create(uid, mr);
					asItem.version = upd.version;
					asItem.internalId = upd.id;
					pushToIndex.add(asItem);
					upNotifs.add(UpdateNotif.of(upd, mr));
				}
			});
			int deletes = softDelete.get();
			logger.info("[{}] Db CRUD op, cr: {}, upd: {}, del: {} in {}ms", mailboxUniqueId, toCreate.size(),
					toUpdate.size() - deletes, deletes, System.currentTimeMillis() - time);
			return storeService.getVersion();
		});
		updateIndex(contVersion, pushToIndex);
		if (!newMailNotification.isEmpty()) {
			for (ItemValue<MailboxRecord> toNotify : newMailNotification) {
				newMailNotification(toNotify);
			}
			logger.info("[{}] Notify CRUD op {}", mailboxUniqueId, newMailNotification.size());
		}

		long[] createdIds = new long[crNotifs.size()];
		long[] itemIds = new long[createdIds.length + upNotifs.size()];
		int createIdx = 0;
		for (CreateNotif create : crNotifs) {
			EmitReplicationEvents.recordCreated(mailboxUniqueId, create.version, create.itemId, create.imapUid);
			itemIds[createIdx] = create.itemId;
			createdIds[createIdx++] = create.itemId;
		}
		for (UpdateNotif update : upNotifs) {
			itemIds[createIdx++] = update.itemUpdate.id;
			EmitReplicationEvents.recordUpdated(mailboxUniqueId, update.itemUpdate, update.mr);
		}
		EmitReplicationEvents.mailboxChanged(recordsLocation, container, mailboxUniqueId, contVersion, itemIds,
				createdIds);
	}

	private void updateIndex(long contVersion, List<ItemValue<MailboxRecord>> pushToIndex) {
		if (!pushToIndex.isEmpty()) {
			ITasksManager runner = context.provider().instance(ITasksManager.class);
			runner.run(mailboxUniqueId + "-" + contVersion + "-" + System.nanoTime(), monitor -> {
				long esTime = System.currentTimeMillis();
				Optional<BulkOperation> bulkOp = Optional.of(indexService.startBulk());
				for (ItemValue<MailboxRecord> forIndex : pushToIndex) {
					index(forIndex, bulkOp);
				}
				bulkOp.ifPresent(bul -> bul.commit(false));
				esTime = System.currentTimeMillis() - esTime;
				logger.info("[{}] Es CRUD op, idx: {} in {}ms", mailboxUniqueId, pushToIndex.size(), esTime);
			});
		}
	}

	private void newMailNotification(ItemValue<MailboxRecord> idxAndNotif) {
		if (idxAndNotif.value.body == null) {
			String partition = CyrusPartition.forServerAndDomain(DataSourceRouter.location(context, container.uid),
					container.domainUid).name;
			IDbMessageBodies bodiesApi = context.provider().instance(IDbMessageBodies.class, partition);
			MessageBody body = bodiesApi.getComplete(idxAndNotif.value.messageBody);
			if (body == null) {
				logger.error("Fail to send notification, no body for message {}", idxAndNotif.value.messageBody);
				return;
			}
			idxAndNotif.value.body = body;
		}

		String from = idxAndNotif.value.body.recipients.stream().filter(r -> r.kind == RecipientKind.Originator)
				.findFirst().map(Object::toString).orElse("??");
		JsonObject js = new JsonObject();
		js.put("title", from).put("body", idxAndNotif.value.body.subject);
		js.put("uid", Long.toString(idxAndNotif.value.imapUid));
		if (logger.isDebugEnabled()) {
			logger.debug("HTML5 Notification attempt with {}", js.encode());
		}
		VertxPlatform.eventBus().send(container.owner + ".notifications.mails", js);
	}

	@Override
	public void deleteImapUids(List<Long> uids) {
		SubtreeLocation recordsLocation = locationOrFault();

		logger.info("Should delete {} uid(s)", uids.size());
		long[] asArray = uids.stream().mapToLong(Long::longValue).toArray();
		AtomicLong lastVersion = new AtomicLong();
		storeService.doOrFail(() -> {
			Set<RecordID> itemUids = recordStore.identifiers(asArray);
			itemUids.forEach(rec -> {
				ItemVersion iv = storeService.delete(rec.itemUid);
				lastVersion.set(iv.version);

			});
			return null;
		});
		expungeIndex(uids);
		EmitReplicationEvents.recordDeleted(mailboxUniqueId);
		EmitReplicationEvents.mailboxChanged(recordsLocation, container, mailboxUniqueId, lastVersion.get(), asArray);
	}

	private void expungeIndex(List<Long> uids) {
		IDSet set = IDSet.create(uids.stream().mapToInt(Long::intValue).toArray());
		ItemValue<Mailbox> box = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IMailboxes.class, container.domainUid).getComplete(container.owner);
		ItemValue<MailboxFolder> folder = getFolder();
		if (folder == null) {
			return;
		}
		indexService.expunge(null, box, folder, set);
	}

	@Override
	public void deleteAll() {
		storeService.deleteAll();
	}

	@Override
	public void prepareContainerDelete() {
		String folderUid = IMailReplicaUids.uniqueId(container.uid);
		ItemValue<Mailbox> box = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IMailboxes.class, container.domainUid).getComplete(container.owner);
		indexService.deleteBox(null, box, folderUid);
		storeService.prepareContainerDelete();
	}

	@Override
	public void xfer(String serverUid) {

		DataSource ds = context.getMailboxDataSource(serverUid);
		ContainerStore cs = new ContainerStore(null, ds, context.getSecurityContext());
		Container c;
		try {
			c = cs.get(container.uid);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}

		storeService.xfer(ds, c, new MailboxRecordStore(ds, c));

	}

	@Override
	public List<ImapBinding> havingBodyVersionLowerThan(final int version) {
		try {
			return this.recordStore.havingBodyVersionLowerThan(version);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

}
