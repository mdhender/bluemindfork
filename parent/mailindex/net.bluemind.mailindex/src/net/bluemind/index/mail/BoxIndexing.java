/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
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
package net.bluemind.index.mail;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.json.JsonData;
import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.backend.mail.api.flags.MailboxItemFlag;
import net.bluemind.backend.mail.replica.api.IDbMailboxRecords;
import net.bluemind.backend.mail.replica.api.IDbReplicatedMailboxes;
import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.backend.mail.replica.indexing.IDSet;
import net.bluemind.backend.mail.replica.indexing.IMailIndexService;
import net.bluemind.backend.mail.replica.indexing.IMailIndexService.BulkOp;
import net.bluemind.backend.mail.replica.indexing.MailSummary;
import net.bluemind.backend.mail.replica.indexing.MessageFlagsHelper;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.ISortingSupport;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.SortDescriptor;
import net.bluemind.core.container.model.SortDescriptor.Direction;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.index.MailIndexActivator;
import net.bluemind.lib.elasticsearch.IndexAliasMapping;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.Mailbox.Routing;

public class BoxIndexing {

	protected final Logger logger = LoggerFactory.getLogger(BoxIndexing.class);

	private final AtomicLong counter;
	private final String domainUid;

	private final MailboxIndexingReport report;

	public BoxIndexing(String domainUid) {
		this.report = MailboxIndexingReport.create();
		this.counter = new AtomicLong();
		this.domainUid = domainUid;
	}

	public AtomicLong getCounter() {
		return counter;
	}

	public MailboxIndexingReport getReport() {
		return report;
	}

	public void resync(ItemValue<Mailbox> mailbox, IServerTaskMonitor monitor) throws ServerFault {
		monitor.begin(100, "resync index for mailbox " + mailbox.value.name + "@" + domainUid);

		MailIndexActivator.getService().repairMailbox(mailbox.uid, monitor.subWork(1));
		logger.info("consolidate mailbox {}", mailbox.value.defaultEmail());
		traverseFolders(mailbox, new IndexAction() {
			@Override
			public void run(ItemValue<Mailbox> mailbox, ItemValue<MailboxFolder> folder,
					IServerTaskMonitor indexMonitor) throws ServerFault {
				resyncSelectedFolder(mailbox, folder, indexMonitor);
			}
		}, (folders -> {
			Set<String> inIndex = MailIndexActivator.getService().getFolders(mailbox.uid);
			Set<String> inDatabase = folders.stream().map(f -> f.uid).collect(Collectors.toSet());
			Set<String> toDelete = Sets.difference(inIndex, inDatabase);
			toDelete.forEach(fId -> MailIndexActivator.getService().deleteBox(mailbox, fId));
		}), monitor.subWork(99));
	}

	private void traverseFolders(ItemValue<Mailbox> mailbox, IndexAction action,
			Consumer<List<ItemValue<MailboxFolder>>> allFoldersAction, IServerTaskMonitor monitor) throws ServerFault {
		logger.info("Traversing folders of mailbox {} type {}, routing: {}", mailbox.displayName, mailbox.value.type,
				mailbox.value.routing);
		if (mailbox.value.routing == Routing.internal && !mailbox.value.archived) {
			String ns = mailbox.value.type.nsPrefix;

			IDbReplicatedMailboxes mbService;
			try {
				mbService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
						.instance(IDbReplicatedMailboxes.class, domainUid, ns + mailbox.value.name.replace(".", "^"));
			} catch (ServerFault sf) {
				logger.error("Need repair ? {}", sf.getMessage(), sf);
				return;
			}
			List<ItemValue<MailboxFolder>> folders = mbService.all();

			if (folders.isEmpty()) {
				logger.warn("0 folders found for {}@{}", mailbox.value.name, domainUid);
			}
			monitor.begin(folders.size(), String.format("Traversing %d folders", folders.size()));
			for (ItemValue<MailboxFolder> f : folders) {
				action.run(mailbox, f, monitor.subWork(1));

			}
			allFoldersAction.accept(folders);

		} else {
			monitor.log(
					String.format("Skip mailbox indexing for mailbox %s. Mailrouting is %s. mailbox is archived ? %s",
							mailbox.value.name, mailbox.value.routing, mailbox.value.archived));
			logger.info("Skip mailbox indexing for mailbox {}. Mailrouting is {}. mailbox is archived ? {}",
					mailbox.value.name, mailbox.value.routing, mailbox.value.archived);
		}
	}

	private void resyncSelectedFolder(ItemValue<Mailbox> mailbox, ItemValue<MailboxFolder> folder,
			IServerTaskMonitor monitor) {

		logger.info("Resyncing folder {}:{} of box {}", folder.uid, folder.value.name, mailbox.uid);
		ElasticsearchClient esClient = MailIndexService.getIndexClient();
		IDbMailboxRecords mbItems = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDbMailboxRecords.class, folder.uid);

		SortDescriptor sortDescriptor = new SortDescriptor();
		SortDescriptor.Field field = new SortDescriptor.Field();
		field.column = "imap_uid";
		field.dir = Direction.Asc;
		sortDescriptor.fields = Arrays.asList(field);
		List<Long> created = ((ISortingSupport) mbItems).sortedIds(sortDescriptor);

		logger.info("Folder {}:{} containers {} created elements", folder.uid, folder.value.name, created.size());

		logger.info("Folder {}:{} deleting remaining orphans if any", folder.uid, folder.value.name);
		deleteRemainingOrphans(esClient, mailbox.uid, folder.uid);

		monitor.begin(created.size(), "Syncing " + created.size() + " message(s) in " + folder.value.name);

		List<List<Long>> partitioned = Lists.partition(created, 50);

		Set<Integer> existingDbEntries = new HashSet<>();
		AtomicInteger lowestGlobal = new AtomicInteger(Integer.MAX_VALUE);
		AtomicInteger highestGlobal = new AtomicInteger(0);

		for (List<Long> partialList : partitioned) {
			long lowestUid = Long.MAX_VALUE;
			long highestUid = Long.MIN_VALUE;
			Map<Record, Collection<MailboxItemFlag>> flagMapping = new HashMap<>();
			for (Long createdMail : partialList) {
				ItemValue<MailboxRecord> completeById = mbItems.getCompleteById(createdMail);
				long imapUid = completeById.value.imapUid;
				lowestUid = Math.min(lowestUid, imapUid);
				highestUid = Math.max(highestUid, imapUid);
				lowestGlobal.set((int) Math.min(lowestUid, lowestGlobal.get()));
				highestGlobal.set((int) Math.max(highestUid, highestGlobal.get()));
				Collection<MailboxItemFlag> systemFlags = completeById.value.flags.stream()
						.filter(item -> item.value != 0).collect(Collectors.toList());
				flagMapping.put(new Record(imapUid, completeById.uid), systemFlags);
				existingDbEntries.add((int) imapUid);
			}
			logger.info("Folder {}:{}, resyncing from {} to {}", folder.uid, folder.value.name, lowestUid, highestUid);
			resyncUidRange(mailbox, folder, (int) lowestUid, (int) highestUid, flagMapping, monitor);
			monitor.progress(partialList.size(), null);
		}

		int[] arr = IntStream.range(lowestGlobal.get(), highestGlobal.get() + 1)
				.filter(e -> !existingDbEntries.contains(e)).toArray();
		List<Integer> list = Arrays.stream(arr).boxed().collect(Collectors.toList());
		List<List<Integer>> partition = Lists.partition(list, 25000);

		for (List<Integer> part : partition) {
			Query toDelete = BoolQuery.of(bq -> bq //
					.filter(f -> f.term(t -> t.field("in").value(folder.uid))) //
					.must(m -> m.term(t -> t.field("owner").value(mailbox.uid))) //
					.filter(f -> f.terms(t -> t //
							.field("uid").terms(v -> v.value(part.stream().map(FieldValue::of).toList())))))
					._toQuery();
			delete(esClient, mailbox.uid, toDelete);
		}

		// delete unknown non-existing data in ES
		if (lowestGlobal.get() < highestGlobal.get()) {
			Query toDelete = BoolQuery.of(bq -> bq //
					.filter(f -> f.term(t -> t.field("in").value(folder.uid))) //
					.filter(f -> f.range(r -> r.field("uid").gte(JsonData.of(0)).lt(JsonData.of(lowestGlobal.get())))) //
					.filter(f -> f.range(r -> r.field("uid").gt(JsonData.of(highestGlobal.get()))))) //
					._toQuery();
			delete(esClient, mailbox.uid, toDelete);
		}
	}

	private Set<Integer> resyncUidRange(ItemValue<Mailbox> mailbox, ItemValue<MailboxFolder> f, int lowUid, int highUid,
			Map<Record, Collection<MailboxItemFlag>> flagMapping, IServerTaskMonitor monitor) {
		String set = lowUid + ":" + highUid;
		// retrieve mails summary from es
		IDSet asSet = IDSet.parse(set);
		Map<Integer, MailSummary> esSums = asMap(MailIndexActivator.getService().fetchSummary(mailbox, f, asSet));
		logger.info("Resync set [{}] in {}, DB has {} mail(s), ES has {} doc(s)", set, f.value.fullName,
				flagMapping.size(), esSums.size());
		List<MailSummary> toSync = new LinkedList<>();

		try {
			List<Record> toIndex = new LinkedList<>();
			for (Map.Entry<Record, Collection<MailboxItemFlag>> entry : flagMapping.entrySet()) {
				int imapUid = entry.getKey().imapUid.intValue();
				MailSummary esSum = esSums.remove(imapUid);
				if (esSum == null) {
					// mail not found in elasticsearch
					// index it !
					toIndex.add(entry.getKey());
				} else {
					Collection<MailboxItemFlag> imapFlags = entry.getValue();
					if (!flagsEqual(imapFlags, esSum.flags)) {
						// flags are desynchronized
						// Synchronize them !

						esSum.flags = MessageFlagsHelper.asFlags(imapFlags);
						toSync.add(esSum);
						counter.incrementAndGet();
					}
				}
			}

			IDbMailboxRecords service = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
					.instance(IDbMailboxRecords.class, f.uid);

			IMailIndexService indexService = MailIndexActivator.getService();
			List<BulkOp> operations = toIndex.stream() //
					.map(record -> service.getComplete(record.uid)) //
					.flatMap(mail -> indexService.storeMessage(f.uid, mail, mailbox.uid, true).stream()).toList();
			indexService.doBulk(operations);

			// update flags
			if (!toSync.isEmpty()) {
				MailIndexActivator.getService().syncFlags(mailbox, f, toSync);
			}
		} catch (Exception t) {
			logger.error("resyncSelectedFolder failure on " + f.displayName, t);
			monitor.log("Synchronization of folder " + f.displayName + " failed.\r\nError: " + t.getMessage()
					+ "\r\nSee /var/log/bm/mail-index.log for more infos.\r\nSkipping this folder");
		}
		return esSums.keySet();
	}

	private boolean flagsEqual(Collection<MailboxItemFlag> imapFlags, Set<String> flags) {
		return new HashSet<>(MessageFlagsHelper.asFlags(imapFlags)).equals(flags);
	}

	private Map<Integer, MailSummary> asMap(List<MailSummary> list) {
		return list.stream().collect(Collectors.toMap(sum -> sum.uid, Function.identity(), (sum1, sum2) -> {
			String summary1Flags = sum1.flags != null ? String.join(",", sum1.flags) : "";
			String summary2Flags = sum2.flags != null ? String.join(",", sum2.flags) : "";
			logger.info(
					"Found duplicate imap uid {}, summary 1 (parent:{}, flags: {}) vs. summary 2 (parent {}, flags: {}). Keeping summary 2",
					sum1.uid, sum1.parentId, summary1Flags, sum2.parentId, summary2Flags);
			return sum2;
		}, HashMap::new));
	}

	protected interface IndexAction {
		public void run(ItemValue<Mailbox> mailbox, ItemValue<MailboxFolder> folder, IServerTaskMonitor monitor)
				throws ServerFault;
	}

	private void deleteRemainingOrphans(ElasticsearchClient esClient, String mailboxUid, String folderUid) {
		Query toDelete = BoolQuery.of(bq -> bq //
				.filter(f -> f.term(t -> t.field("in").value(folderUid)))
				.must(m -> m.term(t -> t.field("owner").value(mailboxUid))) //
				.mustNot(mn -> mn.hasParent(p -> p.parentType("body").query(q -> q.matchAll(a -> a)).score(false)))
				.mustNot(mn -> mn.term(t -> t.field("body_msg_link").value("body"))))._toQuery();
		delete(esClient, mailboxUid, toDelete);
	}

	private void delete(ElasticsearchClient esClient, String mailboxUid, Query query) {
		try {
			esClient.deleteByQuery(d -> {
				String alias = IndexAliasMapping.get().getWriteAliasByMailboxUid(mailboxUid);
				return d.index(alias).query(query);
			});
		} catch (ElasticsearchException | IOException e) {
			logger.error("[es][resync] Unable to delete document in {}, query:{}", mailboxUid, query, e);
		}
	}

	@Override
	public String toString() {
		return String.format("Indexer on domain %s", domainUid);
	}

	private static class Record {
		public final Long imapUid;
		public final String uid;

		public Record(Long imapUid, String uid) {
			this.imapUid = imapUid;
			this.uid = uid;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((imapUid == null) ? 0 : imapUid.hashCode());
			result = prime * result + ((uid == null) ? 0 : uid.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Record other = (Record) obj;
			if (imapUid == null) {
				if (other.imapUid != null)
					return false;
			} else if (!imapUid.equals(other.imapUid))
				return false;
			if (uid == null) {
				if (other.uid != null)
					return false;
			} else if (!uid.equals(other.uid))
				return false;
			return true;
		}

	}

}
