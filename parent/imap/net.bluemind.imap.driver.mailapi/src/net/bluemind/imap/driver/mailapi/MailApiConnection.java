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
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.CharMatcher;
import com.google.common.base.MoreObjects;
import com.google.common.base.Suppliers;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.collect.Streams;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import io.netty.buffer.ByteBuf;
import io.vertx.core.Context;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.WriteStream;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.bluemind.authentication.api.AuthUser;
import net.bluemind.authentication.api.IAuthentication;
import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.backend.mail.api.IMailboxItems;
import net.bluemind.backend.mail.api.MessageBody;
import net.bluemind.backend.mail.api.flags.MailboxItemFlag;
import net.bluemind.backend.mail.replica.api.AppendTx;
import net.bluemind.backend.mail.replica.api.IDbByContainerReplicatedMailboxes;
import net.bluemind.backend.mail.replica.api.IDbMailboxRecords;
import net.bluemind.backend.mail.replica.api.IDbMessageBodies;
import net.bluemind.backend.mail.replica.api.IDbReplicatedMailboxes;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.backend.mail.replica.api.ISyncDbMailboxRecords;
import net.bluemind.backend.mail.replica.api.ImapBinding;
import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.backend.mail.replica.api.MailboxRecord.InternalFlag;
import net.bluemind.backend.mail.replica.api.MailboxReplica;
import net.bluemind.backend.mail.replica.api.MailboxReplica.Acl;
import net.bluemind.backend.mail.replica.api.WithId;
import net.bluemind.backend.mail.replica.indexing.IDSet;
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
import net.bluemind.imap.driver.mailapi.UidSearchAnalyzer.QueryBuilderResult;
import net.bluemind.imap.endpoint.EndpointRuntimeException;
import net.bluemind.imap.endpoint.driver.AppendStatus;
import net.bluemind.imap.endpoint.driver.AppendStatus.WriteStatus;
import net.bluemind.imap.endpoint.driver.CopyResult;
import net.bluemind.imap.endpoint.driver.FetchedItem;
import net.bluemind.imap.endpoint.driver.IdleToken;
import net.bluemind.imap.endpoint.driver.IdleToken.FetchToken;
import net.bluemind.imap.endpoint.driver.ImapIdSet;
import net.bluemind.imap.endpoint.driver.ImapIdSet.IdKind;
import net.bluemind.imap.endpoint.driver.ImapMailbox;
import net.bluemind.imap.endpoint.driver.ListNode;
import net.bluemind.imap.endpoint.driver.MailPart;
import net.bluemind.imap.endpoint.driver.MailboxConnection;
import net.bluemind.imap.endpoint.driver.NamespaceInfos;
import net.bluemind.imap.endpoint.driver.QuotaRoot;
import net.bluemind.imap.endpoint.driver.SelectedFolder;
import net.bluemind.imap.endpoint.driver.UpdateMode;
import net.bluemind.imap.endpoint.parsing.MailboxGlob;
import net.bluemind.lib.elasticsearch.ESearchActivator;
import net.bluemind.lib.elasticsearch.Pit;
import net.bluemind.lib.elasticsearch.Pit.PaginableSearchQueryBuilder;
import net.bluemind.lib.elasticsearch.Pit.PaginationParams;
import net.bluemind.lib.elasticsearch.exception.ElasticIndexException;
import net.bluemind.lib.jutf7.UTF7Converter;
import net.bluemind.lib.vertx.VertxContext;
import net.bluemind.mailbox.api.IMailboxAclUids;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.MailboxQuota;
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
	private static final Supplier<ElasticsearchClient> esSupplier = Suppliers.memoize(ESearchActivator::getClient);
	private static final long MAXIMUM_SEARCH_TIME = TimeUnit.SECONDS.toNanos(15);

	private Consumer activeCons;

	// DefaultFolder class is in mailbox.service bundle :'(
	private static final Set<String> USER_PROTECTED = Set.of("INBOX", "Sent", "Drafts", "Trash", "Junk", "Outbox",
			"Templates");
	private static final Set<String> SHARE_PROTECTED = Set.of("Sent", "Trash");

	public MailApiConnection(IServiceProvider userProv, IServiceProvider suProv, AuthUser me,
			SharedMap<String, String> config) {
		this.prov = userProv;
		this.suProv = suProv;
		this.me = me;
		this.myMailbox = suProv.instance(IMailboxes.class, me.domainUid).getComplete(me.uid);
		this.foldersApi = prov.instance(IDbReplicatedMailboxes.class, me.domainUid, "user." + me.value.login);
		this.subApi = prov.instance(IOwnerSubscriptions.class, me.domainUid, me.uid);
		this.sizeLimit = Integer
				.parseInt(Optional.ofNullable(config.get(SysConfKeys.message_size_limit.name())).orElse("20000000"));
		this.sharedRootPrefix = DriverConfig.get().getString(DriverConfig.SHARED_VIRTUAL_ROOT) + "/";
		this.userRootPrefix = DriverConfig.get().getString(DriverConfig.USER_VIRTUAL_ROOT) + "/";
		this.folderResolver = new FolderResolver(userProv, suProv, me, myMailbox);
	}

	@Override
	public String login() {
		return me.uid + "@" + me.domainUid;
	}

	@Override
	public String logId() {
		return me.value.defaultEmailAddress();
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(MailApiConnection.class).add("id", me.value.defaultEmailAddress()).toString();
	}

	private static final ItemFlagFilter IMAP_VISIBLE = ItemFlagFilter.create().skipExpunged();
	private static final ItemFlagFilter UNSEEN = ItemFlagFilter.create().mustNot(ItemFlag.Seen).skipExpunged();

	@Override
	public SelectedFolder select(String fName) {
		if (fName.isBlank()) {
			logger.warn("[{}] Blank folder selected", this);
			return null;
		}

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

		IDbMailboxRecords recApi = prov.instance(ISyncDbMailboxRecords.class, existing.uid);
		long exist = recApi.count(IMAP_VISIBLE).total;
		long unseen = recApi.count(UNSEEN).total;
		List<String> labels = recApi.labels();
		CyrusPartition part = CyrusPartition.forServerAndDomain(resolved.owner.value.dataLocation, me.domainUid);
		return new SelectedFolder(resolved, existing, recApi, part.name, exist, unseen, labels);
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
		} else if (toDel.mailbox.readOnly || isProtected(toDel) || hasChildren(fName)) {
			return false;
		} else {
			toDel.mailbox.foldersApi.delete(toDel.folder.uid);
			return true;
		}
	}

	private boolean hasChildren(String fName) {
		List<ListNode> children = list("", fName + "/");
		boolean ret = !children.isEmpty();
		if (ret) {
			logger.warn("[{}] Cannot delete {} because it has children ({})", this, fName, children);
		}
		return ret;
	}

	private boolean isProtected(SelectedFolder toDel) {
		boolean ret = false;
		if (toDel.mailbox.owner == myMailbox || !toDel.mailbox.owner.value.type.sharedNs) {
			ret = USER_PROTECTED.contains(toDel.folder.value.fullName);
		} else {
			String mbox = toDel.mailbox.owner.value.name;
			ret = toDel.folder.value.fullName.equals(mbox) || SHARE_PROTECTED.stream().map(s -> mbox + "/" + s)
					.anyMatch(fn -> fn.equals(toDel.folder.value.fullName));
		}
		if (ret) {
			logger.warn("[{}] Cannot delete {} because it is protected", this, toDel);
		}
		return ret;
	}

	@Override
	public List<ListNode> list(String reference, String mailboxPattern) {
		List<NamespacedFolder> withShares = fullHierarchyLoad();
		int before = withShares.size();
		Predicate<NamespacedFolder> filter = matcher(reference, mailboxPattern);
		withShares = withShares.stream().filter(filter).toList();
		int after = withShares.size();
		logger.debug("List filtered by '{}' {} folders -> {} folder(s)", mailboxPattern, before, after);

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

	private Predicate<NamespacedFolder> matcher(String reference, String mailboxPattern) {
		String sanitized = CharMatcher.is('"').removeFrom(mailboxPattern);
		if (sanitized.equalsIgnoreCase("inbox")) {
			sanitized = "INBOX";
		}
		final String san = UTF7Converter.decode(sanitized);
		Predicate<String> globPred = MailboxGlob.matcher(san);
		final String refSan = UTF7Converter.decode(reference);
		return nf -> {
			String fn = nf.fullNameWithMountpoint();
			return fn.startsWith(refSan) && globPred.test(fn);
		};
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
				break;
			}
		}
		return ln;
	}

	private List<Long> resolveIdSet(IDbMailboxRecords recApi, ImapIdSet set) {
		if (set.setStyle == IdKind.UID || set.serializedSet.equals("1:*")) {
			return recApi.imapIdSet(set.serializedSet, "");
		} else {
			List<Long> fullList = recApi.imapIdSet("1:*", "");
			int total = fullList.size();
			ListIterator<Long> iterator = IDSet.parse(set.serializedSet).iterateUid();
			List<Long> ret = new ArrayList<>(total);
			while (iterator.hasNext()) {
				int seq = iterator.next().intValue();
				int position = seq - 1;
				if (position >= 0 && position < total) {
					ret.add(fullList.get(position));
				}
			}
			return ret;
		}
	}

	@Override
	public CompletableFuture<Void> fetch(SelectedFolder selected, ImapIdSet idset, List<MailPart> fields,
			WriteStream<FetchedItem> output) {
		IDbMailboxRecords recApi = prov.instance(IDbMailboxRecords.class, selected.folder.uid);
		IDbMessageBodies bodyApi = prov.instance(IDbMessageBodies.class, selected.partition);
		IMailboxItems itemsApi = prov.instance(IMailboxItems.class, selected.folder.uid);
		Iterator<List<Long>> slice = Lists
				.partition(resolveIdSet(recApi, idset), DriverConfig.get().getInt("driver.records-mget")).iterator();
		CompletableFuture<Void> ret = new CompletableFuture<>();
		Context fetchContext = VertxContext.getOrCreateDuplicatedContext();
		FetchedItemRenderer renderer = new FetchedItemRenderer(bodyApi, recApi, itemsApi, fields);
		Map<Long, Integer> seqIndex = itemIdToSeqNum(recApi);
		fetchContext.runOnContext(
				v -> pushNext(fetchContext, seqIndex, renderer, slice, Collections.emptyIterator(), ret, output));
		return ret;
	}

	private void pushNext(Context fetchContext, Map<Long, Integer> seqIndex, FetchedItemRenderer renderer,
			Iterator<List<Long>> idSliceIterator, Iterator<WithId<MailboxRecord>> recsIterator,
			CompletableFuture<Void> ret, WriteStream<FetchedItem> output) {
		while (recsIterator.hasNext()) {
			WithId<MailboxRecord> rec = recsIterator.next();
			if (rec.value.internalFlags.contains(InternalFlag.expunged)) {
				continue;
			}

			long imapUid = rec.value.imapUid;
			FetchedItem fi = new FetchedItem();
			fi.uid = (int) imapUid;
			Integer maybeNullSeq = seqIndex.get(rec.itemId);
			if (maybeNullSeq == null) {
				logger.warn("seqindex unknown for {} using {}", rec, seqIndex);
				continue;
			}
			fi.seq = seqIndex.get(rec.itemId);
			fi.properties = renderer.renderFields(rec);
			output.write(fi);
			if (output.writeQueueFull()) {
				output.drainHandler(v -> fetchContext.runOnContext(
						w -> pushNext(fetchContext, seqIndex, renderer, idSliceIterator, recsIterator, ret, output)));
				return;
			}
		}

		if (idSliceIterator.hasNext()) {
			List<Long> slice = idSliceIterator.next();
			List<WithId<MailboxRecord>> records = renderer.recApi().slice(slice);
			fetchContext.runOnContext(
					v -> pushNext(fetchContext, seqIndex, renderer, idSliceIterator, records.iterator(), ret, output));
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
		synchronized (myMailbox) {
			if (activeCons != null) {
				activeCons.close();
				activeCons = null;
			}
		}
	}

	@Override
	public void idleMonitor(SelectedFolder selected, WriteStream<IdleToken> out) {
		notIdle();
		if (selected == null) {
			// the fucking RFC allows in authenticated state
			// https://datatracker.ietf.org/doc/html/rfc2177
			notIdle();
		} else {
			logger.info("idle monitoring on {}", selected.folder);
			IDbMailboxRecords recApi = prov.instance(IDbMailboxRecords.class, selected.folder.uid);
			String watchedUid = IMailReplicaUids.mboxRecords(selected.folder.uid);
			Context idleContext = VertxContext.getOrCreateDuplicatedContext();
			synchronized (myMailbox) {
				this.activeCons = MQ.registerConsumer(Topic.IMAP_ITEM_NOTIFICATIONS,
						msg -> idleContext.runOnContext(v -> {
							JsonObject jsMsg = msg.toJson();
							String contUid = jsMsg.getString("containerUid");

							if (watchedUid.equals(contUid)) {
								logger.info("Stuff happenned on watched folder for {} -> {}", out, msg);
								Map<Long, Integer> iidToSeq = itemIdToSeqNum(recApi);
								out.write(new IdleToken.CountToken("EXISTS", iidToSeq.size()));
								List<FetchToken> changes = jsMsg.getJsonArray("changes").stream()
										.map(JsonObject.class::cast).map(js -> js.mapTo(ImapChange.class))
										.map(ic -> ic.fetch(iidToSeq)).filter(Objects::nonNull).toList();
								Iterator<FetchToken> iter = changes.iterator();
								iteratorToStream(iter, out);
							}
						}));
			}
		}
	}

	private void iteratorToStream(Iterator<FetchToken> iter, WriteStream<IdleToken> out) {
		while (iter.hasNext()) {
			FetchToken ft = iter.next();
			out.write(ft);
			if (out.writeQueueFull()) {
				out.drainHandler(v -> iteratorToStream(iter, out));
				break;
			}
		}
	}

	private static class ImapChange {
		public long imap;
		public Set<String> flags;
		public long iid;

		public IdleToken.FetchToken fetch(Map<Long, Integer> iidToSeq) {
			Integer maybeNullSeq = iidToSeq.get(iid);
			if (maybeNullSeq == null) {
				logger.warn("seqindex unknown for {} using {}", iid, iidToSeq);
				return null;
			}

			return new FetchToken(maybeNullSeq, imap, flags);
		}
	}

	@Override
	public void close() {
		notIdle();
		prov.instance(IAuthentication.class).logout();
	}

	@Override
	public AppendStatus append(String folder, List<String> flags, Date deliveryDate, ByteBuf buffer) {
		SelectedFolder selected = select(folder);

		if (selected == null) {
			return new AppendStatus(WriteStatus.EXCEPTIONNALY_REJECTED, 0L, 0L);
		}

		IMailboxes mboxApi = suProv.instance(IMailboxes.class, me.domainUid);
		MailboxQuota mbxQuota = mboxApi.getMailboxQuota(selected.mailbox.owner.uid);
		if (mbxQuota.quota != null && mbxQuota.quota < mbxQuota.used) {
			return new AppendStatus(WriteStatus.OVERQUOTA_REJECTED, 0L, 0L);
		}

		AppendTx appendTx = selected.mailbox.foldersApi.prepareAppend(selected.folder.internalId, 1);

		@SuppressWarnings("deprecation")
		HashFunction hash = Hashing.sha1();
		String bodyGuid = hash.hashBytes(buffer.duplicate().nioBuffer()).toString();
		IDbMessageBodies bodiesApi = prov.instance(IDbMessageBodies.class, selected.partition);

		IConversationReference conversationReferenceApi = prov.instance(IConversationReference.class, me.domainUid,
				me.uid);
		Date bodyDeliveryDate = deliveryDate == null ? new Date(appendTx.internalStamp) : deliveryDate;
		bodiesApi.createWithDeliveryDate(bodyGuid, bodyDeliveryDate, VertxStream.stream(Buffer.buffer(buffer)));
		MessageBody messageBody = bodiesApi.getComplete(bodyGuid);
		Set<String> references = (messageBody.references != null) ? Sets.newHashSet(messageBody.references)
				: Sets.newHashSet();
		long conversationId = conversationReferenceApi.lookup(messageBody.messageId, references);
		logger.debug("{}: found conversationId: {}", me.value.login, conversationId);

		MailboxRecord rec = new MailboxRecord();
		rec.imapUid = appendTx.imapUid;
		rec.internalDate = bodyDeliveryDate;
		rec.messageBody = bodyGuid;
		rec.modSeq = appendTx.modSeq;
		rec.conversationId = conversationId;
		rec.flags = flags(flags);
		rec.lastUpdated = rec.internalDate;

		selected.recApi.create(appendTx.imapUid + ".", rec);
		return new AppendStatus(WriteStatus.WRITTEN, selected.folder.value.uidValidity, appendTx.imapUid);
	}

	private List<MailboxItemFlag> flags(List<String> flags) {
		return flags.stream().map(f -> {
			String fl = f.toLowerCase();
			switch (fl) {
			case "\\seen":
				return MailboxItemFlag.System.Seen.value();
			case "\\draft":
				return MailboxItemFlag.System.Draft.value();
			case "\\deleted":
				return MailboxItemFlag.System.Deleted.value();
			case "\\flagged":
				return MailboxItemFlag.System.Flagged.value();
			case "\\answered":
				return MailboxItemFlag.System.Answered.value();
			case "\\expunged":
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
	public void updateFlags(SelectedFolder selected, ImapIdSet idset, UpdateMode mode, List<String> flags) {
		IDbMailboxRecords recApi = prov.instance(ISyncDbMailboxRecords.class, selected.folder.uid);
		List<Long> toUpdate = resolveIdSet(recApi, idset);
		updateFlags(selected, toUpdate, mode, flags);
	}

	private void updateFlags(SelectedFolder selected, List<Long> toUpdate, UpdateMode mode, List<String> flags) {
		IDbMailboxRecords recApi = prov.instance(ISyncDbMailboxRecords.class, selected.folder.uid);
		for (List<Long> slice : Lists.partition(toUpdate, DriverConfig.get().getInt("driver.records-mget"))) {
			List<MailboxRecord> recs = recApi.slice(slice).stream().map(iv -> iv.value).collect(Collectors.toList()); // NOSONAR
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
		String index = "mailspool_alias_" + sel.mailbox.owner.uid;
		// Really ?!
		try {
			ESearchActivator.refreshIndex(index);
		} catch (ElasticIndexException e) {
			logger.error("Failed to refresh index '{}', result might miss some uids", index, e);
		}

		ElasticsearchClient esClient = esSupplier.get();
		try {
			List<Long> uids;
			QueryBuilderResult qbr = UidSearchAnalyzer.buildQuery(esClient, query, sel.folder.uid, me.uid);
			PaginableSearchQueryBuilder paginableSearch = s -> s.source(src -> src.fetch(false)) //
					.docvalueFields(f -> f.field("uid")) //
					.query(qbr.q());
			SortOptions sort = SortOptions.of(so -> so.field(f -> f.field("uid").order(SortOrder.Asc)));
			try (Pit<Void> pit = Pit.allocateUsingTimebudget(esClient, index, 60, MAXIMUM_SEARCH_TIME, Void.class)) {
				uids = pit.allPages(paginableSearch, PaginationParams.all(sort),
						hit -> hit.fields().get("uid").toJson().asJsonArray().getJsonNumber(0).longValue());
			}

			// Empty uids list and seq is present -> return the greatest uid (see RFC 3501)
			if (uids.isEmpty() && qbr.hasSequence()) {
				// Gets document with highest uid for keywords with sequences management
				Aggregate aggregate = esClient.search(s -> s.index(index) //
						.size(0) //
						.query(q -> q.bool(b -> b.must(m -> m.term(t -> t.field("in").value(sel.folder.uid)))))
						.aggregations("uid_max", a -> a.max(m -> m.field("uid"))), Void.class) //
						.aggregations().get("uid_max");
				long maxUid = (aggregate != null) ? (long) aggregate.max().value() : 0l;
				return Arrays.asList(maxUid);
			}
			logger.info("[{}] {} enumerate '{}' -> {} uid(s)", this, sel, query, uids.size());
			return uids;
		} catch (Exception e) {
			logger.error("[{}] unknown error: {}", this, e.getMessage(), e);
			return null; // NOSONAR: null is used for error detection
		}
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
			return true;
		}
	}

	@Override
	public boolean unsubscribe(String fName) {
		return ignoreSubRelatedCall(fName);
	}

	@Override
	public String rename(String fullName, String newName) {
		SelectedFolder toRename = select(fullName);
		if (toRename == null) {
			logger.warn("[{}] folder {} does not exists.", this, fullName);
			return null;
		}

		ItemValue<Mailbox> owner = myMailbox;
		String absoluteNewName = newName;
		if (newName.startsWith(sharedRootPrefix) || newName.startsWith(userRootPrefix)) {
			String shareParent = newName.substring(0, newName.lastIndexOf('/'));
			ImapMailbox resolvedParent = folderResolver.resolveBox(shareParent);
			owner = resolvedParent.owner;
			if (owner != null) {
				ItemValue<MailboxReplica> parentBox = resolvedParent.foldersApi
						.byReplicaName(resolvedParent.replicaName);
				absoluteNewName = shareToFullName(owner, parentBox, newName);
			}
		}

		if (owner == null || absoluteNewName == null || !owner.uid.equals(toRename.mailbox.owner.uid)) {
			logger.warn("[{}] folder {} can't be renamed to {}.", this, fullName, newName);
			return null;
		} else {
			MailboxReplica renamed = MailboxReplica.from(toRename.folder.value);
			renamed.name = null;
			renamed.parentUid = null;
			renamed.fullName = absoluteNewName;
			List<ItemValue<MailboxReplica>> children = list("", fullName + "/").stream()
					.map(ln -> select(ln.imapMountPoint).folder).toList();
			IDbReplicatedMailboxes resFolderApi = resolvedFolderApi(toRename.mailbox.owner);
			resFolderApi.update(toRename.folder.uid, renamed);
			if (!children.isEmpty()) {
				logger.info("[{}] {} child renames NEEDED", this, children.size());
				for (ItemValue<MailboxReplica> current : children) {
					logger.info("[{}] Update child folder {}", this, current.value.fullName);
					MailboxReplica copy = MailboxReplica.from(current.value);
					copy.fullName = resFolderApi.getComplete(copy.parentUid).value.fullName + "/" + copy.name;
					resFolderApi.update(current.uid, copy);
				}
			}

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

	private Map<Long, Integer> itemIdToSeqNum(IDbMailboxRecords recApi) {
		List<Long> fullList = recApi.imapIdSet("1:*", "");
		Map<Long, Integer> ret = new Long2IntOpenHashMap(2 * fullList.size());
		Iterator<Long> uidIter = fullList.iterator();
		for (int i = 1; uidIter.hasNext(); i++) {
			ret.put(uidIter.next(), i);
		}
		return ret;
	}

	@Override
	public Map<Long, Integer> sequences(SelectedFolder sel) {
		List<Long> itemIds = sel.recApi.imapIdSet("1:*", "");
		Map<Long, Integer> uidToSeq = new Long2IntOpenHashMap(2 * itemIds.size());
		Iterator<ImapBinding> bindings = sel.recApi.imapBindings(itemIds).iterator();
		for (int i = 1; bindings.hasNext(); i++) {
			uidToSeq.put(bindings.next().imapUid, i);
		}
		return uidToSeq;
	}

	@Override
	public List<Long> uidSet(SelectedFolder sel, String set, ItemFlagFilter filter) {
		List<Long> itemIds = sel.recApi.imapIdSet(set, ItemFlagFilter.toQueryString(filter));
		return sel.recApi.imapBindings(itemIds).stream().map(ib -> ib.imapUid).toList();
	}

}
