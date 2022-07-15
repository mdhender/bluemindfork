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

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

import io.netty.buffer.ByteBuf;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.WriteStream;
import net.bluemind.authentication.api.AuthUser;
import net.bluemind.authentication.api.IAuthentication;
import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.backend.mail.api.flags.MailboxItemFlag;
import net.bluemind.backend.mail.replica.api.AppendTx;
import net.bluemind.backend.mail.replica.api.IDbMailboxRecords;
import net.bluemind.backend.mail.replica.api.IDbMessageBodies;
import net.bluemind.backend.mail.replica.api.IDbReplicatedMailboxes;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.backend.mail.replica.api.MailboxReplica;
import net.bluemind.core.container.api.Count;
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.container.model.ItemFlag;
import net.bluemind.core.container.model.ItemFlagFilter;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.ItemVersion;
import net.bluemind.core.rest.http.ClientSideServiceProvider;
import net.bluemind.core.rest.vertx.VertxStream;
import net.bluemind.hornetq.client.Consumer;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.Topic;
import net.bluemind.imap.endpoint.driver.FetchedItem;
import net.bluemind.imap.endpoint.driver.IdleToken;
import net.bluemind.imap.endpoint.driver.ListNode;
import net.bluemind.imap.endpoint.driver.MailPart;
import net.bluemind.imap.endpoint.driver.MailboxConnection;
import net.bluemind.imap.endpoint.driver.SelectedFolder;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.MailboxQuota;

public class MailApiConnection implements MailboxConnection {

	private static final Logger logger = LoggerFactory.getLogger(MailApiConnection.class);

	private final ClientSideServiceProvider prov;
	private final AuthUser me;

	private final IDbReplicatedMailboxes foldersApi;

	private Consumer activeCons;

	public MailApiConnection(ClientSideServiceProvider userProv, AuthUser me) {
		this.prov = userProv;
		this.me = me;
		this.foldersApi = prov.instance(IDbReplicatedMailboxes.class, me.domainUid, "user." + me.value.login);
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(MailApiConnection.class).add("id", me.value.defaultEmailAddress()).toString();
	}

	@Override
	public SelectedFolder select(String fName) {

		ItemValue<MailboxReplica> existing = foldersApi.byReplicaName(fName);
		if (existing == null) {
			logger.warn("[{}] folder {} not found.", this, fName);
			return null;
		}
		IDbMailboxRecords recApi = prov.instance(IDbMailboxRecords.class, existing.uid);
		long exist = recApi.count(ItemFlagFilter.all()).total;
		long unseen = recApi.count(ItemFlagFilter.create().mustNot(ItemFlag.Seen, ItemFlag.Deleted)).total;
		return new SelectedFolder(existing, exist, unseen);
	}

	@Override
	public List<ListNode> list(String reference, String mailboxPattern) {
		List<ItemValue<MailboxReplica>> allFolders = foldersApi.allReplicas();
		Set<String> parents = allFolders.stream().map(f -> f.value.parentUid).filter(Objects::nonNull)
				.collect(Collectors.toSet());

		if (mailboxPattern.equals("%")) {
			allFolders = allFolders.stream().filter(f -> f.value.parentUid == null).collect(Collectors.toList());
		}

		Collections.sort(allFolders, (f1, f2) -> {
			if (f1.value.fullName.equals("INBOX")) {
				return -1;
			}
			if (f2.value.fullName.equals("INBOX")) {
				return 1;
			}
			return f1.value.fullName.compareTo(f2.value.fullName);
		});

		return allFolders.stream().map(f -> asListNode(parents, f)).collect(Collectors.toList());
	}

	private ListNode asListNode(Set<String> parents, ItemValue<MailboxReplica> f) {
		ListNode ln = new ListNode();
		ln.hasChildren = parents.contains(f.uid);
		ln.replica = f;
		switch (f.value.name) {
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
		return ln;
	}

	private static final ItemFlagFilter NOT_DELETED = ItemFlagFilter.create().mustNot(ItemFlag.Deleted);

	@Override
	public CompletableFuture<Void> fetch(SelectedFolder selected, String idset, List<MailPart> fields,
			WriteStream<FetchedItem> output) {
		IDbMailboxRecords recApi = prov.instance(IDbMailboxRecords.class, selected.folder.uid);

		ContainerChangeset<ItemVersion> cs = recApi.filteredChangesetById(0L, NOT_DELETED);
		List<ItemVersion> ids = Lists.newArrayList(Iterables.concat(cs.created, cs.updated));
		Collections.sort(ids, (iv1, iv2) -> Long.compare(iv1.id, iv2.id));
		Map<Long, Integer> itemIdToSeq = new HashMap<>();
		IntStream.rangeClosed(1, ids.size()).forEach(idx -> itemIdToSeq.put(ids.get(idx - 1).id, idx));

		Iterator<List<Long>> slice = Lists.partition(recApi.imapIdSet(idset, "-deleted"), 500).iterator();
		CompletableFuture<Void> ret = new CompletableFuture<>();

		pushNext(recApi, selected, fields, slice, Collections.emptyIterator(), itemIdToSeq, ret, output);
		return ret;
	}

	private void pushNext(IDbMailboxRecords recApi, SelectedFolder selected, List<MailPart> fields,
			Iterator<List<Long>> idSliceIterator, Iterator<ItemValue<MailboxRecord>> recsIterator,
			Map<Long, Integer> itemIdToSeq, CompletableFuture<Void> ret, WriteStream<FetchedItem> output) {
		FetchedItemRenderer renderer = new FetchedItemRenderer(prov, recApi, selected, fields);
		while (recsIterator.hasNext()) {
			ItemValue<MailboxRecord> rec = recsIterator.next();
			long imapUid = rec.value.imapUid;
			// always increment for valid sequences
			FetchedItem fi = new FetchedItem();
			fi.seq = itemIdToSeq.get(rec.internalId);
			fi.uid = (int) imapUid;
			fi.properties = renderer.renderFields(rec);
			output.write(fi);
			if (output.writeQueueFull()) {
				output.drainHandler(v -> pushNext(recApi, selected, fields, idSliceIterator, recsIterator, itemIdToSeq,
						ret, output));
				return;
			}
		}

		if (idSliceIterator.hasNext()) {
			List<Long> slice = idSliceIterator.next();
			List<ItemValue<MailboxRecord>> records = recApi.multipleGetById(slice);
			pushNext(recApi, selected, fields, idSliceIterator, records.iterator(), itemIdToSeq, ret, output);
		} else {
			output.end(ar -> ret.complete(null));
		}
	}

	@Override
	public MailboxQuota quota() {
		IMailboxes mboxApi = prov.instance(IMailboxes.class, me.domainUid);
		return mboxApi.getMailboxQuota(me.uid);
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
				logger.info("Stuff happenned on watched folder {}", msg.toJson());
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
		AppendTx appendTx = foldersApi.prepareAppend(selected.folder.internalId);

		@SuppressWarnings("deprecation")
		HashFunction hash = Hashing.sha1();
		String bodyGuid = hash.hashBytes(buffer.duplicate().nioBuffer()).toString();
		String partition = CyrusPartition.forServerAndDomain(me.value.dataLocation, me.domainUid).name;
		System.err.println("Deliver " + bodyGuid + " to " + partition);
		IDbMessageBodies bodiesApi = prov.instance(IDbMessageBodies.class, partition);
		bodiesApi.create(bodyGuid, VertxStream.stream(Buffer.buffer(buffer)));

		MailboxRecord rec = new MailboxRecord();
		rec.imapUid = appendTx.imapUid;
		rec.internalDate = new Date(appendTx.internalStamp);
		rec.messageBody = bodyGuid;
		rec.modSeq = appendTx.modSeq;
		rec.conversationId = System.nanoTime();
		rec.flags = flags(flags);
		rec.lastUpdated = rec.internalDate;
		rec.internalFlags = Collections.emptyList();

		IDbMailboxRecords recApi = prov.instance(IDbMailboxRecords.class, selected.folder.uid);
		recApi.create(appendTx.imapUid + ".", rec);
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
			default:
				return null;
			}
		}).filter(Objects::nonNull).collect(Collectors.toList());
	}

}
