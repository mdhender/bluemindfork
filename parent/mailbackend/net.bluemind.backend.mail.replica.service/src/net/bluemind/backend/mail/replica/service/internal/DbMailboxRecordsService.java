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
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.CharMatcher;

import io.vertx.core.json.JsonObject;
import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.backend.mail.api.MessageBody;
import net.bluemind.backend.mail.api.MessageBody.RecipientKind;
import net.bluemind.backend.mail.api.flags.MailboxItemFlag;
import net.bluemind.backend.mail.api.flags.WellKnownFlags;
import net.bluemind.backend.mail.replica.api.IDbByContainerReplicatedMailboxes;
import net.bluemind.backend.mail.replica.api.IDbMailboxRecords;
import net.bluemind.backend.mail.replica.api.IDbMessageBodies;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.backend.mail.replica.api.ISyncDbMailboxRecords;
import net.bluemind.backend.mail.replica.api.ImapBinding;
import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.backend.mail.replica.api.MailboxRecord.InternalFlag;
import net.bluemind.backend.mail.replica.api.MailboxReplicaRootDescriptor.Namespace;
import net.bluemind.backend.mail.replica.api.RawImapBinding;
import net.bluemind.backend.mail.replica.api.Weight;
import net.bluemind.backend.mail.replica.api.WithId;
import net.bluemind.backend.mail.replica.indexing.IDSet;
import net.bluemind.backend.mail.replica.indexing.IMailIndexService;
import net.bluemind.backend.mail.replica.indexing.IMailIndexService.BulkOp;
import net.bluemind.backend.mail.replica.persistence.MailboxRecordStore;
import net.bluemind.backend.mail.replica.persistence.RecordID;
import net.bluemind.backend.mail.replica.persistence.ReplicasStore;
import net.bluemind.backend.mail.replica.persistence.ReplicasStore.SubtreeLocation;
import net.bluemind.backend.mail.replica.service.internal.EmitReplicationEvents.ItemIdImapUid;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.Ack;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemFlagFilter;
import net.bluemind.core.container.model.ItemIdentifier;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.ItemVersion;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.DataSourceRouter;
import net.bluemind.core.container.service.internal.ContainerStoreService;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.system.api.SystemState;
import net.bluemind.system.state.StateContext;

public class DbMailboxRecordsService extends BaseMailboxRecordsService
		implements IDbMailboxRecords, ISyncDbMailboxRecords {

	private static final Logger logger = LoggerFactory.getLogger(DbMailboxRecordsService.class);
	private static final String TOPIC_ES_INDEXING_COUNT = "es.indexing.count";

	private Optional<ItemValue<MailboxFolder>> mboxFolder = Optional.empty();

	private final IMailIndexService indexService;

	public DbMailboxRecordsService(DataSource ds, Container cont, BmContext context, String mailboxUniqueId,
			MailboxRecordStore recordStore, ContainerStoreService<MailboxRecord> storeService,
			IMailIndexService index) {
		super(ds, cont, context, mailboxUniqueId, recordStore, storeService, new ReplicasStore(ds));
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
	public Weight weight() {
		try {
			long val = recordStore.weight();
			Weight weight = new Weight();
			weight.total = val;
			return weight;
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
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
	public ItemVersion create(String uid, MailboxRecord mail) {
		return create0(uid, null, mail);
	}

	@Override
	public Ack createById(long id, MailboxRecord mail) {
		return create0(mail.imapUid + ".", id, mail).ack();
	}

	private ItemVersion create0(String uid, Long internalId, MailboxRecord m) {
		SubtreeLocation recordsLocation = locationOrFault();
		MailboxRecord mail = fixRecordFlags(m);
		ItemVersion version = storeService.createWithId(uid, internalId, null, uid, mail);

		ItemValue<MailboxRecord> itemValue = ItemValue.create(uid, mail);
		itemValue.internalId = version.id;
		itemValue.version = version.version;
		updateIndex(Collections.singletonList(itemValue));
		if (StateContext.getState() == SystemState.CORE_STATE_RUNNING) {
			logger.debug("Sending event for created item {}", version);
			EmitReplicationEvents.recordCreated(mailboxUniqueId, version.version, version.id, mail.imapUid);
			EmitReplicationEvents.mailboxChanged(recordsLocation, container, mailboxUniqueId, version.version,
					ItemIdImapUid.arrayOf(version.id, mail), version.id);
			if (newMailNotificationCandidate(recordsLocation, itemValue)) {
				newMailNotification(itemValue);
			}
		}
		return version;
	}

	@Override
	public List<ItemIdentifier> multiCreate(List<MailboxRecord> mails) {
		SubtreeLocation recordsLocation = locationOrFault();
		List<ItemIdentifier> returned = new ArrayList<>(mails.size());
		List<ItemValue<MailboxRecord>> toIndex = new ArrayList<>(mails.size());
		ItemVersion version = null;
		MailboxRecord last = null;
		for (MailboxRecord mail : mails) {
			String uid = mail.imapUid + ".";
			version = storeService.create(uid, uid, mail);
			last = mail;

			ItemValue<MailboxRecord> itemValue = ItemValue.create(uid, mail);
			itemValue.internalId = version.id;
			itemValue.version = version.version;
			returned.add(ItemIdentifier.of(uid, version.id, version.version, version.timestamp));
			toIndex.add(itemValue);
		}
		updateIndex(toIndex);
		if (version != null) {
			EmitReplicationEvents.mailboxChanged(recordsLocation, container, mailboxUniqueId, version.version,
					ItemIdImapUid.arrayOf(version.id, last));
		}
		return returned;
	}

	private SubtreeLocation locationOrFault() {
		return optRecordsLocation.orElseThrow(() -> new ServerFault("Missing subtree location"));
	}

	private List<BulkOp> index(ItemValue<MailboxRecord> mail) {
		if (logger.isDebugEnabled()) {
			SubtreeLocation recordsLocation = locationOrFault();

			logger.debug("Indexing mail in mailbox {}:{}@{} in folder {}", mailboxUniqueId,
					recordsLocation.subtreeContainer, recordsLocation.partition, recordsLocation.boxName);
		}
		return indexService.storeMessage(mailboxUniqueId, mail, container.owner, true);
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
	public void update(String uid, MailboxRecord m) {
		SubtreeLocation recordsLocation = locationOrFault();
		MailboxRecord mail = fixRecordFlags(m);
		ItemVersion upd = storeService.update(uid, uid, mail);

		ItemValue<MailboxRecord> asItem = ItemValue.create(uid, m);
		asItem.internalId = upd.id;
		asItem.version = upd.version;
		updateIndex(Collections.singletonList(asItem));

		EmitReplicationEvents.recordUpdated(mailboxUniqueId, upd, mail);
		EmitReplicationEvents.mailboxChanged(recordsLocation, container, mailboxUniqueId, upd.version,
				ItemIdImapUid.arrayOf(upd.id, mail));
	}

	@Override
	public Ack updateById(long id, MailboxRecord m) {
		SubtreeLocation recordsLocation = locationOrFault();
		MailboxRecord mail = fixRecordFlags(m);

		ItemVersion upd = storeService.update(id, Long.toString(mail.imapUid), mail);

		ItemValue<MailboxRecord> asItem = ItemValue.create(Long.toString(mail.imapUid), m);
		asItem.internalId = upd.id;
		asItem.version = upd.version;
		updateIndex(Collections.singletonList(asItem));

		EmitReplicationEvents.recordUpdated(mailboxUniqueId, upd, mail);
		EmitReplicationEvents.mailboxChanged(recordsLocation, container, mailboxUniqueId, upd.version,
				ItemIdImapUid.arrayOf(upd.id, mail));
		return upd.ack();
	}

	@Override
	public void delete(String uid) {
		deleteIfExists(storeService.get(uid, null));
	}

	@Override
	public void deleteById(long id) {
		deleteIfExists(storeService.get(id, null));
	}

	private void deleteIfExists(ItemValue<MailboxRecord> prev) {
		if (prev != null && prev.value != null) {
			SubtreeLocation recordsLocation = locationOrFault();
			ItemVersion iv = storeService.delete(prev.uid);
			expungeIndex(Collections.singletonList(prev.value.imapUid));
			prev.value.flags.add(MailboxItemFlag.System.Deleted.value());
			EmitReplicationEvents.mailboxChanged(recordsLocation, container, mailboxUniqueId, iv.version,
					ItemIdImapUid.arrayOf(iv.id, prev.value));
		}
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
			List<RecordID> idSet = recordStore.identifiers(imapUid);
			if (idSet.isEmpty()) {
				logger.warn("No record with imap uid {}", imapUid);
				return null;
			} else {
				RecordID rec = idSet.iterator().next();
				return getCompleteById(rec.itemId);
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

	private List<MailboxRecord> fixFlags(List<MailboxRecord> toFix) {
		int len = toFix.size();
		ArrayList<MailboxRecord> ret = new ArrayList<>(len);
		for (MailboxRecord mr : toFix) {
			ret.add(fixRecordFlags(mr));
		}
		return ret;
	}

	private MailboxRecord fixRecordFlags(MailboxRecord mr) {
		ArrayList<MailboxItemFlag> mif = new ArrayList<>(mr.flags.size());
		for (MailboxItemFlag f : mr.flags) {
			mif.add(f.value == 0 ? WellKnownFlags.resolve(f.toString()) : f);
		}
		mr.flags = mif;
		return mr;
	}

	@Override
	public Ack updates(List<MailboxRecord> recs) {
		if (recs.isEmpty()) {
			return Ack.create(getVersion(), new Date());
		}

		return updatesImpl(recs);
	}

	private boolean newMailNotificationCandidate(SubtreeLocation loc, ItemValue<MailboxRecord> rec) {
		return "INBOX".equals(loc.boxName) && loc.namespace() == Namespace.users
				&& !rec.value.flags.contains(MailboxItemFlag.System.Seen.value())
				&& !rec.value.flags.contains(MailboxItemFlag.System.Deleted.value());
	}

	private Ack updatesImpl(List<MailboxRecord> recs) {
		List<MailboxRecord> records = fixFlags(recs);
		logger.info("[{}] Update with {} record(s)", mailboxUniqueId, records.size());
		long time = System.currentTimeMillis();
		List<CreateNotif> crNotifs = new LinkedList<>();
		List<UpdateNotif> upNotifs = new LinkedList<>();

		List<ItemValue<MailboxRecord>> pushToIndex = new ArrayList<>(records.size());
		List<ItemValue<MailboxRecord>> newMailNotification = new LinkedList<>();
		SubtreeLocation recordsLocation = locationOrFault();

		long contVersion = storeService.doOrFail(() -> {
			long[] uidArrays = records.stream().mapToLong(rec -> rec.imapUid).toArray();
			List<RecordID> ids = recordStore.identifiers(uidArrays);
			Map<Long, RecordID> dbByUid = ids.stream().collect(Collectors.toMap(r -> r.imapUid, r -> r));
			Map<Long, MailboxRecord> newRecsByUid = records.stream().collect(Collectors.toMap(r -> r.imapUid, r -> r));
			Map<Long, MailboxRecord> toUpdate = new HashMap<>();
			List<MailboxRecord> toCreate = new LinkedList<>();

			AtomicReference<Long> updVers = new AtomicReference<>();

			for (long createOrUpdateImapUid : uidArrays) {
				MailboxRecord touchedMailRecord = newRecsByUid.get(createOrUpdateImapUid);
				RecordID asRecID = dbByUid.get(createOrUpdateImapUid);
				if (asRecID == null) {
					toCreate.add(touchedMailRecord);
				} else {
					toUpdate.put(asRecID.itemId, touchedMailRecord);
				}
			}
			// apply the changes
			toCreate.forEach((MailboxRecord mr) -> {
				String uid = mr.imapUid + ".";
				if (mr.internalFlags.contains(InternalFlag.expunged)) {
					logger.info("Skipping create on expunged record {}", mr.imapUid);
					return;
				}

				UpsertResult upsert = upsertByUid(uid, mr);
				if (!upsert.update) {
					crNotifs.add(CreateNotif.of(upsert.version.version, upsert.version.id, mr.imapUid));
				} else {
					upNotifs.add(UpdateNotif.of(upsert.version, mr));
				}

				ItemValue<MailboxRecord> idxAndNotif = ItemValue.create(uid, mr);
				idxAndNotif.internalId = upsert.version.id;
				idxAndNotif.version = upsert.version.version;
				updVers.set(upsert.version.version);

				pushToIndex.add(idxAndNotif);
				if (newMailNotificationCandidate(recordsLocation, idxAndNotif)) {
					newMailNotification.add(idxAndNotif);
				}
			});

			AtomicInteger softDelete = new AtomicInteger();
			toUpdate.forEach((Long itemId, MailboxRecord mr) -> {
				ItemVersion upd = storeService.update(itemId, "itemId:" + itemId, mr);
				updVers.set(upd.version);

				if (mr.flags.contains(MailboxItemFlag.System.Deleted.value())) {
					softDelete.incrementAndGet();
				}

				ItemValue<MailboxRecord> asItem = ItemValue.create("dunno", mr);
				asItem.version = upd.version;
				asItem.internalId = upd.id;
				pushToIndex.add(asItem);
				upNotifs.add(UpdateNotif.of(upd, mr));
			});
			int deletes = softDelete.get();
			if (System.currentTimeMillis() - time > 500) {
				logger.info("[{}] Db CRUD op, cr: {}, upd: {}, del: {} in {}ms", mailboxUniqueId, toCreate.size(),
						toUpdate.size() - deletes, deletes, System.currentTimeMillis() - time);
			}
			return Optional.ofNullable(updVers.get()).map(Number::longValue).orElseGet(storeService::getVersion);
		});

		updateIndex(pushToIndex);
		if (!newMailNotification.isEmpty()) {
			for (ItemValue<MailboxRecord> toNotify : newMailNotification) {
				newMailNotification(toNotify);
			}
			logger.info("[{}] Notify CRUD op {}", mailboxUniqueId, newMailNotification.size());
		}

		long[] createdIds = new long[crNotifs.size()];
		ItemIdImapUid[] itemIds = new ItemIdImapUid[createdIds.length + upNotifs.size()];
		int createIdx = 0;
		for (CreateNotif create : crNotifs) {
			EmitReplicationEvents.recordCreated(mailboxUniqueId, create.version, create.itemId, create.imapUid);
			itemIds[createIdx] = new ItemIdImapUid(create.itemId, create.imapUid, Collections.emptySet());
			createdIds[createIdx++] = create.itemId;
		}
		for (UpdateNotif update : upNotifs) {
			itemIds[createIdx++] = ItemIdImapUid.of(update.itemUpdate.id, update.mr);
			EmitReplicationEvents.recordUpdated(mailboxUniqueId, update.itemUpdate, update.mr);
		}
		EmitReplicationEvents.mailboxChanged(recordsLocation, container, mailboxUniqueId, contVersion, itemIds,
				createdIds);
		return Ack.create(contVersion, null);
	}

	private void updateIndex(List<ItemValue<MailboxRecord>> pushToIndex) {
		if (!pushToIndex.isEmpty()) {
			try {
				long esTime = System.currentTimeMillis();
				List<BulkOp> operations = pushToIndex.stream().flatMap(mail -> index(mail).stream()).toList();
				indexService.doBulk(operations);
				esTime = System.currentTimeMillis() - esTime;
				if (esTime > 500) {
					logger.info("[{}] Es CRUD op, idx: {} in {}ms", mailboxUniqueId, pushToIndex.size(), esTime);
				}
			} catch (Exception e) {
				logger.error("[{}] Es CRUD op failed", mailboxUniqueId, e);
			} finally {
				VertxPlatform.eventBus().publish(TOPIC_ES_INDEXING_COUNT, pushToIndex.size());
			}
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
		js.put("internalId", Long.toString(idxAndNotif.internalId));
		if (logger.isDebugEnabled()) {
			logger.debug("HTML5 Notification attempt with {}", js.encode());
		}
		VertxPlatform.eventBus().publish(container.owner + ".notifications.mails", js);
	}

	@Override
	public void deleteImapUids(List<Long> uids) {
		SubtreeLocation recordsLocation = locationOrFault();

		logger.info("Should delete {} uid(s)", uids.size());
		long[] asArray = uids.stream().mapToLong(Long::longValue).toArray();
		AtomicLong lastVersion = new AtomicLong();
		ItemIdImapUid[] cleared = storeService.doOrFail(() -> {
			List<RecordID> itemUids = recordStore.identifiers(asArray);
			Set<String> del = Collections.singleton("\\Deleted");
			ItemIdImapUid[] purged = itemUids.stream().map(rid -> new ItemIdImapUid(rid.itemId, rid.imapUid, del))
					.toArray(ItemIdImapUid[]::new);
			itemUids.forEach(rec -> {
				ItemVersion iv = storeService.delete(rec.itemId);
				lastVersion.set(iv.version);
			});
			return purged;
		});
		expungeIndex(uids);
		EmitReplicationEvents.recordDeleted(mailboxUniqueId);
		EmitReplicationEvents.mailboxChanged(recordsLocation, container, mailboxUniqueId, lastVersion.get(), cleared);
	}

	private void expungeIndex(List<Long> uids) {
		IDSet set = IDSet.create(uids.stream().mapToInt(Long::intValue).toArray());
		ItemValue<Mailbox> box = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IMailboxes.class, container.domainUid).getComplete(container.owner);
		ItemValue<MailboxFolder> folder = getFolder();
		if (folder == null) {
			return;
		}
		indexService.expunge(box, folder, set);
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
		if (box != null) {
			indexService.deleteBox(box, folderUid);
		}
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
		IMailboxes mailboxesApi = context.su().provider().instance(IMailboxes.class, container.domainUid);
		ItemValue<Mailbox> mailbox = mailboxesApi.getComplete(container.owner);
		if (mailbox == null) {
			throw ServerFault.notFound("mailbox of " + container.owner + " not found");
		}
		String subtreeContainerUid = IMailReplicaUids.subtreeUid(container.domainUid, mailbox);
		try {
			Container subtreeContainer = cs.get(subtreeContainerUid);
			storeService.xfer(ds, c, new MailboxRecordStore(ds, c, subtreeContainer));
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	@Override
	public List<ImapBinding> havingBodyVersionLowerThan(final int version) {
		try {
			return this.recordStore.havingBodyVersionLowerThan(version);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	public List<ItemValue<MailboxRecord>> multipleGetById(List<Long> ids) {
		return storeService.getMultipleById(ids);
	}

	@Override
	public List<WithId<MailboxRecord>> slice(List<Long> itemIds) {
		try {
			return recordStore.slice(itemIds);
		} catch (SQLException e) {
			throw new ServerFault(e);
		}
	}

	@Override
	public List<RawImapBinding> imapIdSet(String set, String filter) {
		boolean validSet = CharMatcher.inRange('0', '9').or(CharMatcher.anyOf(":,*")).matchesAllOf(set);
		if (!validSet) {
			throw new ServerFault("invalid idset '" + set + "'", ErrorCode.INVALID_PARAMETER);
		}
		ItemFlagFilter itemFilter = ItemFlagFilter.fromQueryString(Optional.ofNullable(filter).orElse(""));
		try {
			return recordStore.imapIdset(set, itemFilter);
		} catch (Exception e) {
			throw ServerFault.sqlFault(e);
		}
	}

	@Override
	public MailboxRecord get(String uid) {
		return getComplete(uid).value;
	}

	@Override
	public void restore(ItemValue<MailboxRecord> item, boolean isCreate) {
		if (isCreate) {
			createById(item.internalId, item.value);
		} else {
			updateById(item.internalId, item.value);
		}
	}

	@Override
	public List<String> labels() {
		try {
			return recordStore.labels();
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

}
