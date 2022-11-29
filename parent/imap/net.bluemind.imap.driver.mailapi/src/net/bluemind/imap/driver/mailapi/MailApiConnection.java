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
package net.bluemind.imap.driver.mailapi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.CharMatcher;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.collect.Streams;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

import io.netty.buffer.ByteBuf;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.WriteStream;
import net.bluemind.authentication.api.AuthUser;
import net.bluemind.authentication.api.IAuthentication;
import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.backend.mail.api.MessageBody;
import net.bluemind.backend.mail.api.flags.MailboxItemFlag;
import net.bluemind.backend.mail.replica.api.AppendTx;
import net.bluemind.backend.mail.replica.api.IDbByContainerReplicatedMailboxes;
import net.bluemind.backend.mail.replica.api.IDbMailboxRecords;
import net.bluemind.backend.mail.replica.api.IDbMessageBodies;
import net.bluemind.backend.mail.replica.api.IDbReplicatedMailboxes;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.backend.mail.replica.api.MailboxRecord.InternalFlag;
import net.bluemind.backend.mail.replica.api.MailboxReplica;
import net.bluemind.backend.mail.replica.api.MailboxReplica.Acl;
import net.bluemind.backend.mail.replica.api.WithId;
import net.bluemind.core.container.api.Count;
import net.bluemind.core.container.api.IOwnerSubscriptions;
import net.bluemind.core.container.model.ItemFlag;
import net.bluemind.core.container.model.ItemFlagFilter;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.vertx.VertxStream;
import net.bluemind.delivery.conversationreference.api.IConversationReference;
import net.bluemind.hornetq.client.Consumer;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.MQ.SharedMap;
import net.bluemind.hornetq.client.Topic;
import net.bluemind.imap.endpoint.EndpointRuntimeException;
import net.bluemind.imap.endpoint.driver.CopyResult;
import net.bluemind.imap.endpoint.driver.FetchedItem;
import net.bluemind.imap.endpoint.driver.IdleToken;
import net.bluemind.imap.endpoint.driver.ImapMailbox;
import net.bluemind.imap.endpoint.driver.ListNode;
import net.bluemind.imap.endpoint.driver.MailPart;
import net.bluemind.imap.endpoint.driver.MailboxConnection;
import net.bluemind.imap.endpoint.driver.NamespaceInfos;
import net.bluemind.imap.endpoint.driver.QuotaRoot;
import net.bluemind.imap.endpoint.driver.SelectedFolder;
import net.bluemind.imap.endpoint.driver.UpdateMode;
import net.bluemind.lib.jutf7.UTF7Converter;
import net.bluemind.mailbox.api.IMailboxAclUids;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.system.api.SysConfKeys;

public class MailApiConnection implements MailboxConnection {

	private static final Logger logger = LoggerFactory.getLogger(MailApiConnection.class);

	private final IServiceProvider prov;
	private final AuthUser me;
	private final IDbReplicatedMailboxes foldersApi;
	private final int sizeLimit;
	private final IOwnerSubscriptions subApi;
	private final ItemValue<Mailbox> myMailbox;
	private final IServiceProvider suProv;
	private final String sharedRootPrefix;
	private final String userRootPrefix;

	private final FolderResolver folderResolver;

	private Consumer activeCons;

	public MailApiConnection(IServiceProvider userProv, IServiceProvider suProv, AuthUser me,
			SharedMap<String, String> config) {
		this.prov = userProv;
		this.suProv = suProv;
		this.me = me;
		this.myMailbox = suProv.instance(IMailboxes.class, me.domainUid).getComplete(me.uid);
		this.foldersApi = prov.instance(IDbReplicatedMailboxes.class, me.domainUid, "user." + me.value.login);
		this.subApi = prov.instance(IOwnerSubscriptions.class, me.domainUid, me.uid);
		this.sizeLimit = Integer
				.parseInt(Optional.ofNullable(config.get(SysConfKeys.message_size_limit.name())).orElse("10000000"));
		this.sharedRootPrefix = DriverConfig.get().getString(DriverConfig.SHARED_VIRTUAL_ROOT) + "/";
		this.userRootPrefix = DriverConfig.get().getString(DriverConfig.USER_VIRTUAL_ROOT) + "/";
		this.folderResolver = new FolderResolver(userProv, suProv, me, myMailbox);
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(MailApiConnection.class).add("id", me.value.defaultEmailAddress()).toString();
	}

	@Override
	public SelectedFolder select(String fName) {
		ImapMailbox resolved = folderResolver.resolveBox(fName);
		if (resolved == null) {
			logger.warn("[{}] Mailbox resolution failed for {}", this, fName);
			return null;
		}
		if (!resolved.readable) {
			logger.warn("[{}] mailbox {} is not readable.", this, fName);
			return null;
		}
		ItemValue<MailboxReplica> existing = resolved.replica();

		if (existing == null) {
			logger.debug("[{}] folder {} not found.", this, fName);
			return null;
		}

		IDbMailboxRecords recApi = prov.instance(IDbMailboxRecords.class, existing.uid);
		long exist = recApi.count(ItemFlagFilter.all()).total;
		long unseen = recApi.count(ItemFlagFilter.create().mustNot(ItemFlag.Seen, ItemFlag.Deleted)).total;
		CyrusPartition part = CyrusPartition.forServerAndDomain(resolved.owner.value.dataLocation, me.domainUid);
		return new SelectedFolder(resolved, existing, recApi, part.name, exist, unseen);
	}

	private IDbReplicatedMailboxes resolvedFolderApi(ItemValue<Mailbox> owner) {
		return owner == myMailbox ? foldersApi
				: prov.instance(IDbByContainerReplicatedMailboxes.class,
						IMailReplicaUids.subtreeUid(me.domainUid, owner));
	}

	private String shareToFullName(ItemValue<Mailbox> owner, ItemValue<MailboxReplica> parent, String fName) {
		if (parent == null) {
			return null;
		}
		if (fName.startsWith(sharedRootPrefix)) {
			return parent.value.fullName + "/" + fName.substring(fName.lastIndexOf('/') + 1);
		}
		if (parent.internalId == 0) {
			// virtual inbox in user shares, created by virtualUserInbox
			return "INBOX/" + fName.substring(fName.lastIndexOf('/') + 1);
		} else {
			String prefix = userRootPrefix + owner.value.name + "/";
			return fName.substring(prefix.length());
		}

	}

	@Override
	public String create(String imapFolderName) {
		String fName = imapFolderName;
		ItemValue<Mailbox> owner = myMailbox;
		if (fName.startsWith(sharedRootPrefix) || fName.startsWith(userRootPrefix)) {
			String shareParent = fName.substring(0, fName.lastIndexOf('/'));
			ImapMailbox resolvedParent = folderResolver.resolveBox(shareParent);
			owner = resolvedParent.owner;
			if (owner != null) {
				ItemValue<MailboxReplica> parentBox = resolvedParent.foldersApi
						.byReplicaName(resolvedParent.replicaName);
				fName = shareToFullName(owner, parentBox, fName);
			}
		}
		if (owner == null || fName == null) {
			logger.warn("[{}] Failed to resolve mailbox to create in ({} -> {})", this, imapFolderName, fName);
			return null;
		}

		IDbReplicatedMailboxes resolvedFoldersApi = resolvedFolderApi(owner);

		ItemValue<MailboxReplica> folder = resolvedFoldersApi.byReplicaName(fName);
		if (folder != null && folder.value != null) {
			logger.warn("[{}] folder {} already exists.", this, fName);
			return null;
		} else {
			MailboxReplica mr = new MailboxReplica();
			mr.fullName = fName;
			mr.highestModSeq = 0;
			mr.xconvModSeq = 0;
			mr.lastUid = 0;
			mr.recentUid = 0;
			mr.options = "";
			mr.syncCRC = 0;
			mr.quotaRoot = null;
			mr.uidValidity = 0;
			mr.lastAppendDate = new Date();
			mr.pop3LastLogin = new Date();
			mr.recentTime = new Date();
			mr.acls = new LinkedList<>();
			mr.acls.add(Acl.create(me.uid + "@" + me.domainUid, "lrswipkxtecda"));
			mr.acls.add(Acl.create("admin0", "lrswipkxtecda"));
			mr.deleted = false;
			String pipoUid = UUID.randomUUID().toString();
			resolvedFoldersApi.create(pipoUid, mr);
		}
		return fName;
	}

	@Override
	public boolean delete(String fName) {
		SelectedFolder toDel = select(fName);

		if (toDel == null) {
			logger.warn("[{}] folder {} does not exists (delete op).", this, fName);
			return false;
		} else {
			toDel.mailbox.foldersApi.delete(toDel.folder.uid);
			return true;
		}
	}

	@Override
	public List<ListNode> list(String reference, String mailboxPattern) {
		List<NamespacedFolder> withShares = fullHierarchyLoad();

		if (mailboxPattern.equals("%")) {
			withShares = withShares.stream().filter(nf -> nf.folder().value.parentUid == null).toList();
		} else if (!(mailboxPattern.contains("%") || mailboxPattern.contains("*"))) {
			Predicate<NamespacedFolder> filter = matcher(mailboxPattern);
			withShares = withShares.stream().filter(filter).toList();
		}

		return withShares.stream().sorted(Replicas::compareNamespaced).map(this::asListNode).toList();
	}

	private List<NamespacedFolder> fullHierarchyLoad() {
		List<ItemValue<MailboxReplica>> allFolders = foldersApi.allReplicas();
		Set<String> parents = allFolders.stream().map(f -> f.value.parentUid).filter(Objects::nonNull)
				.collect(Collectors.toSet());
		List<NamespacedFolder> allMyFolders = allFolders.stream()
				.map(mr -> new NamespacedFolder(myMailbox, myMailbox, mr, parents)).toList();

		return loadSubscribedSharedFolders(allMyFolders);
	}

	private List<NamespacedFolder> loadSubscribedSharedFolders(List<NamespacedFolder> allMyFolders) {
		String myAcls = IMailboxAclUids.uidForMailbox(me.uid);
		IMailboxes mboxApi = suProv.instance(IMailboxes.class, me.domainUid);
		List<String> mboxUids = subApi.list().stream().filter(
				iv -> iv.value.containerType.equals(IMailboxAclUids.TYPE) && !iv.value.containerUid.equals(myAcls))
				.map(iv -> IMailboxAclUids.mailboxForUid(iv.value.containerUid)).toList();
		List<ItemValue<Mailbox>> toMount = mboxApi.multipleGet(mboxUids);

		List<NamespacedFolder> mboxFolders = toMount.stream().flatMap(mb -> {

			IDbReplicatedMailboxes mbFolderApi = prov.instance(IDbByContainerReplicatedMailboxes.class,
					IMailReplicaUids.subtreeUid(me.domainUid, mb));
			List<ItemValue<MailboxReplica>> sharedFolders = mbFolderApi.allReplicas();
			Set<String> sharedParents = sharedFolders.stream().map(f -> f.value.parentUid).filter(Objects::nonNull)
					.collect(Collectors.toSet());
			return sharedFolders.stream().map(sf -> new NamespacedFolder(myMailbox, mb, sf, sharedParents));
		}).toList();

		List<NamespacedFolder> withShares = Streams.concat(allMyFolders.stream(), mboxFolders.stream()).toList();
		logger.info("[{}] {} folder(s) including {} shared mailboxes", this, withShares.size(), toMount.size());
		return withShares;
	}

	private Predicate<NamespacedFolder> matcher(String mailboxPattern) {
		String sanitized = CharMatcher.is('"').removeFrom(mailboxPattern);
		if (sanitized.equalsIgnoreCase("inbox")) {
			sanitized = "INBOX";
		}
		final String san = UTF7Converter.decode(sanitized);

		return nf -> nf.fullNameWithMountpoint().contains(san);
	}

	private ListNode asListNode(NamespacedFolder f) {
		ListNode ln = new ListNode();
		ln.hasChildren = f.parents().contains(f.folder().uid);
		ln.imapMountPoint = f.fullNameWithMountpoint();
		ln.selectable = !f.virtual();
		if (!f.otherMailbox()) {
			switch (f.folder().value.fullName) {
			case "Sent":
				ln.specialUse = Collections.singletonList("\\sent");
				break;
			case "Trash":
				ln.specialUse = Collections.singletonList("\\trash");
				break;
			case "Drafts":
				ln.specialUse = Collections.singletonList("\\drafts");
				break;
			case "Junk":
				ln.specialUse = Collections.singletonList("\\junk");
				break;
			default:
				ln.specialUse = Collections.emptyList();
			}
		}
		return ln;
	}

	@Override
	public CompletableFuture<Void> fetch(SelectedFolder selected, String idset, List<MailPart> fields,
			WriteStream<FetchedItem> output) {
		IDbMailboxRecords recApi = prov.instance(IDbMailboxRecords.class, selected.folder.uid);
		IDbMessageBodies bodyApi = prov.instance(IDbMessageBodies.class, selected.partition);
		Iterator<List<Long>> slice = Lists
				.partition(recApi.imapIdSet(idset, ""), DriverConfig.get().getInt("driver.records-mget")).iterator();
		CompletableFuture<Void> ret = new CompletableFuture<>();

		pushNext(recApi, bodyApi, fields, slice, Collections.emptyIterator(), ret, output);
		return ret;
	}

	private void pushNext(IDbMailboxRecords recApi, IDbMessageBodies bodyApi, List<MailPart> fields,
			Iterator<List<Long>> idSliceIterator, Iterator<WithId<MailboxRecord>> recsIterator,
			CompletableFuture<Void> ret, WriteStream<FetchedItem> output) {
		FetchedItemRenderer renderer = new FetchedItemRenderer(bodyApi, recApi, fields);
		while (recsIterator.hasNext()) {
			WithId<MailboxRecord> rec = recsIterator.next();
			if (rec.value.internalFlags.contains(InternalFlag.expunged)) {
				continue;
			}

			long imapUid = rec.value.imapUid;
			// always increment for valid sequences
			FetchedItem fi = new FetchedItem();
			fi.uid = (int) imapUid;
			fi.seq = fi.uid;
			fi.properties = renderer.renderFields(rec);
			output.write(fi);
			if (output.writeQueueFull()) {
				output.drainHandler(v -> pushNext(recApi, bodyApi, fields, idSliceIterator, recsIterator, ret, output));
				return;
			}
		}

		if (idSliceIterator.hasNext()) {
			List<Long> slice = idSliceIterator.next();
			List<WithId<MailboxRecord>> records = recApi.slice(slice);
			pushNext(recApi, bodyApi, fields, idSliceIterator, records.iterator(), ret, output);
		} else {
			output.end(ar -> ret.complete(null));
		}
	}

	@Override
	public QuotaRoot quota(SelectedFolder sf) {
		QuotaRoot qr = new QuotaRoot();
		IMailboxes mboxApi = suProv.instance(IMailboxes.class, me.domainUid);
		qr.quota = mboxApi.getMailboxQuota(sf.mailbox.owner.uid);

		if (sf.mailbox.owner == myMailbox) {
			qr.rootName = "INBOX";
		} else if (sf.mailbox.owner.value.type.sharedNs) {
			qr.rootName = sharedRootPrefix + sf.mailbox.owner.value.name;
		} else {
			qr.rootName = userRootPrefix + sf.mailbox.owner.value.name;
		}
		return qr;
	}

	@Override
	public void notIdle() {
		if (activeCons != null) {
			activeCons.close();
		}
	}

	@Override
	public void idleMonitor(SelectedFolder selected, WriteStream<IdleToken> out) {
		logger.info("idle monitoring on {}", selected.folder);
		if (activeCons != null) {
			activeCons.close();
		}
		IDbMailboxRecords recApi = prov.instance(IDbMailboxRecords.class, selected.folder.uid);
		String watchedUid = IMailReplicaUids.mboxRecords(selected.folder.uid);
		this.activeCons = MQ.registerConsumer(Topic.MAPI_ITEM_NOTIFICATIONS, msg -> {

			String contUid = msg.getStringProperty("containerUid");

			if (watchedUid.equals(contUid)) {
				logger.info("Stuff happenned on watched folder for {} -> {}", out, msg.toJson());
				Count exist = recApi.count(ItemFlagFilter.create().mustNot(ItemFlag.Deleted));
				out.write(new IdleToken("EXISTS", (int) exist.total));

			}
		});
	}

	public void close() {
		prov.instance(IAuthentication.class).logout();
	}

	@Override
	public long append(String folder, List<String> flags, Date deliveryDate, ByteBuf buffer) {
		SelectedFolder selected = select(folder);
		if (selected == null) {
			return 0L;
		}
		AppendTx appendTx = selected.mailbox.foldersApi.prepareAppend(selected.folder.internalId, 1);

		@SuppressWarnings("deprecation")
		HashFunction hash = Hashing.sha1();
		String bodyGuid = hash.hashBytes(buffer.duplicate().nioBuffer()).toString();
		IDbMessageBodies bodiesApi = prov.instance(IDbMessageBodies.class, selected.partition);

		IConversationReference conversationReferenceApi = prov.instance(IConversationReference.class, me.domainUid,
				me.uid);
		bodiesApi.create(bodyGuid, VertxStream.stream(Buffer.buffer(buffer)));
		MessageBody messageBody = bodiesApi.getComplete(bodyGuid);
		Set<String> references = (messageBody.references != null) ? Sets.newHashSet(messageBody.references)
				: Sets.newHashSet();
		long conversationId = conversationReferenceApi.lookup(messageBody.messageId, references);
		logger.debug("{}: found conversationId: {}", me.value.login, conversationId);

		MailboxRecord rec = new MailboxRecord();
		rec.imapUid = appendTx.imapUid;
		rec.internalDate = deliveryDate == null ? new Date(appendTx.internalStamp) : deliveryDate;
		rec.messageBody = bodyGuid;
		rec.modSeq = appendTx.modSeq;
		rec.conversationId = conversationId;
		rec.flags = flags(flags);
		rec.lastUpdated = rec.internalDate;

		selected.recApi.create(appendTx.imapUid + ".", rec);
		return appendTx.imapUid;
	}

	private List<MailboxItemFlag> flags(List<String> flags) {
		return flags.stream().map(f -> {
			switch (f) {
			case "\\Seen":
				return MailboxItemFlag.System.Seen.value();
			case "\\Draft":
				return MailboxItemFlag.System.Draft.value();
			case "\\Deleted":
				return MailboxItemFlag.System.Deleted.value();
			case "\\Flagged":
				return MailboxItemFlag.System.Flagged.value();
			case "\\Answered":
				return MailboxItemFlag.System.Answered.value();
			case "\\Expunged":
				return null;
			default:
				return MailboxItemFlag.of(f, 0);
			}
		}).filter(Objects::nonNull).collect(Collectors.toList()); // NOSONAR mutated
	}

	private List<InternalFlag> internalFlags(List<String> flags) {
		return flags.stream().map(f -> {
			if (f.equalsIgnoreCase("\\Expunged")) {
				return InternalFlag.expunged;
			}
			return null;
		}).filter(Objects::nonNull).collect(Collectors.toList()); // NOSONAR mutated
	}

	@Override
	public void updateFlags(SelectedFolder selected, String idset, UpdateMode mode, List<String> flags) {
		IDbMailboxRecords recApi = prov.instance(IDbMailboxRecords.class, selected.folder.uid);
		List<Long> toUpdate = recApi.imapIdSet(idset, "");
		updateFlags(selected, toUpdate, mode, flags);
	}

	@Override
	public void updateFlags(SelectedFolder selected, List<Long> toUpdate, UpdateMode mode, List<String> flags) {
		IDbMailboxRecords recApi = prov.instance(IDbMailboxRecords.class, selected.folder.uid);
		for (List<Long> slice : Lists.partition(toUpdate, DriverConfig.get().getInt("driver.records-mget"))) {
			List<MailboxRecord> recs = recApi.multipleGetById(slice).stream().map(iv -> iv.value)
					.collect(Collectors.toList()); // NOSONAR we mutate it
			for (MailboxRecord item : recs) {
				List<MailboxItemFlag> commandFlags = flags(flags);
				List<InternalFlag> internalFlags = internalFlags(flags);
				if (internalFlags.contains(InternalFlag.expunged)) {
					commandFlags.add(MailboxItemFlag.System.Deleted.value());
				}
				updateRecFlags(item, mode, commandFlags, internalFlags);
			}
			logger.info("[{} / {}] Update slice of {} record(s)", this, selected, recs.size());
			recApi.updates(recs);
		}
	}

	private void updateRecFlags(MailboxRecord rec, UpdateMode mode, List<MailboxItemFlag> list,
			List<InternalFlag> internalList) {
		if (mode == UpdateMode.Replace) {
			rec.flags = list;
			rec.internalFlags = internalList;
		} else if (mode == UpdateMode.Add) {
			rec.flags.addAll(list);
			rec.internalFlags.addAll(internalList);
		} else if (mode == UpdateMode.Remove) {
			rec.flags.removeAll(list);
			rec.internalFlags.removeAll(internalList);
		}

	}

	@Override
	public int maxLiteralSize() {
		return sizeLimit;

	}

	@Override
	public CopyResult copyTo(SelectedFolder source, String folder, String idset) {
		SelectedFolder target = select(folder);
		if (target == null) {
			throw new EndpointRuntimeException("Folder '" + folder + "' is missing.");
		}
		IDbMailboxRecords srcRecApi = source.recApi;
		List<Long> matchingRecords = srcRecApi.imapIdSet(idset, "");

		IDbMailboxRecords tgtRecApi = target.recApi;

		boolean copyBodies = !source.partition.equals(target.partition);

		AppendTx tx = target.mailbox.foldersApi.prepareAppend(target.folder.internalId, matchingRecords.size());
		long start = tx.imapUid - (matchingRecords.size() - 1);
		long end = tx.imapUid;
		long cnt = start;
		List<MailboxRecord> toCreate = new ArrayList<>(matchingRecords.size());
		List<Long> sourceImapUid = new ArrayList<>(matchingRecords.size());

		List<String> bodiesToTransfert = new ArrayList<>(matchingRecords.size());

		for (Long recId : matchingRecords) {
			ItemValue<MailboxRecord> toCopy = srcRecApi.getCompleteById(recId);
			sourceImapUid.add(toCopy.value.imapUid);
			MailboxRecord copy = toCopy.value;
			copy.imapUid = cnt++;
			if (copyBodies) {
				bodiesToTransfert.add(toCopy.value.messageBody);
			}
			toCreate.add(copy);
		}

		if (copyBodies) {
			IDbMessageBodies sourceBodies = prov.instance(IDbMessageBodies.class, source.partition);
			IDbMessageBodies targetBodies = prov.instance(IDbMessageBodies.class, target.partition);
			List<String> toTransfer = targetBodies.missing(bodiesToTransfert);
			logger.info("Starting transfer of {} missing bodies from {} -> {}", toTransfer.size(), source.partition,
					target.partition);
			for (List<String> slice : Lists.partition(toTransfer, 50)) {
				sourceBodies.multiple(slice).forEach(targetBodies::update);
			}
		}

		tgtRecApi.updates(toCreate);
		String sourceSet = sourceImapUid.stream().mapToLong(Long::longValue).mapToObj(Long::toString)
				.collect(Collectors.joining(","));
		return new CopyResult(sourceSet, start, end, target.folder.value.uidValidity);
	}

	@Override
	public List<Long> uids(SelectedFolder sel, String query) {
		IDbMailboxRecords recApi = prov.instance(IDbMailboxRecords.class, sel.folder.uid);
		Set<String> filters = new HashSet<>();
		String lq = query.toLowerCase();
		if (lq.contains("undeleted") || lq.contains("not deleted")) {
			filters.add("-deleted");
		} else {
			if (lq.contains("deleted")) {
				filters.add("+deleted");
			}
		}
		if (lq.contains("unseen")) {
			filters.add("-seen");
		}
		String filter = filters.stream().collect(Collectors.joining(","));
		logger.info("set: 1:*, filter: {}", filter);
		List<Long> notDel = recApi.imapIdSet("1:*", filter);
		return recApi.imapBindings(notDel).stream().map(ib -> ib.imapUid).sorted().toList();
	}

	@Override
	public boolean subscribe(String fName) {
		return ignoreSubRelatedCall(fName);
	}

	private boolean ignoreSubRelatedCall(String fName) {
		ItemValue<MailboxReplica> folder = foldersApi.byReplicaName(fName);
		if (folder == null) {
			logger.warn("[{}] folder {} does not exists.", this, fName);
			return false;
		} else {
			// TODO Auto-generated method stub
			return true;
		}
	}

	@Override
	public boolean unsubscribe(String fName) {
		return ignoreSubRelatedCall(fName);
	}

	@Override
	public String rename(String fName, String newName) {
		ItemValue<MailboxReplica> folder = foldersApi.byReplicaName(fName);
		if (folder == null) {
			logger.warn("[{}] folder {} does not exists.", this, fName);
			return null;
		} else {
			MailboxReplica renamed = MailboxReplica.from(folder.value);
			renamed.fullName = newName;
			renamed.name = renamed.fullName.substring(renamed.fullName.lastIndexOf('/') + 1);
			foldersApi.update(folder.uid, renamed);
			return renamed.fullName;
		}
	}

	@Override
	public NamespaceInfos namespaces() {
		return new NamespaceInfos(userRootPrefix, sharedRootPrefix);
	}

	@Override
	public String imapAcl(SelectedFolder selected) {
		if (selected.mailbox.readOnly) {
			return net.bluemind.imap.endpoint.driver.Acl.RO;
		} else if (selected.mailbox.owner == myMailbox) {
			return net.bluemind.imap.endpoint.driver.Acl.ALL;
		} else {
			return net.bluemind.imap.endpoint.driver.Acl.RW;
		}
	}

}
