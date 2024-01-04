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
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.cli.index;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.backend.mail.replica.api.IDbByContainerReplicatedMailboxes;
import net.bluemind.backend.mail.replica.api.IDbMailboxRecords;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.directory.common.SingleOrDomainOperation;
import net.bluemind.cli.utils.Tasks;
import net.bluemind.core.container.api.Count;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemFlag;
import net.bluemind.core.container.model.ItemFlagFilter;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.lib.elasticsearch.ESearchActivator;
import net.bluemind.lib.elasticsearch.MailspoolStats;
import net.bluemind.lib.elasticsearch.MailspoolStats.FolderCount;
import net.bluemind.lib.elasticsearch.MailspoolStats.FolderCount.SampleStrategy;
import net.bluemind.mailbox.api.IMailboxMgmt;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.Mailbox.Routing;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "coherency", description = "Assess mailbox record coherency between es index and database")
public class MailboxEsCoherency extends SingleOrDomainOperation {

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("index");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return MailboxEsCoherency.class;
		}

	}

	@Option(names = "--all", description = "Select all folders, except empty ones. Sample strategy is ignored.")
	public boolean all = false;

	@Option(names = "--sample-strategy", description = "Sample strategy to select the user folders sample, either 'BIGGEST' (default) or 'RANDOM'.")
	public String sampleStrategy = "BIGGEST";

	@Option(names = "--sample-size", description = "Sample size, ie number of folder to test for a given user (default 100, max 65536).")
	public int sampleSize = 100;

	@Option(names = "--delta", description = "Allowed discrepancy between ES and db counts (in percent, default 2, 0 to disable).")
	public int delta = 2;

	@Option(names = "--run-consolidate", description = "Run a 'consolidateIndex' command on mailbox with inconsistencies.")
	public boolean runConsolidate = false;

	@Option(names = "--include-archived", description = "Include archived mailbox in the check (not fixed by run consolidate).")
	public boolean includeArchived = false;

	@Option(names = "--include-deleted", description = "Include message flag as deleted in the check.")
	public boolean includeDeleted = false;

	@Option(names = "--include-es-empty", description = "Include ES empty folders in the checks. Empty folders are selected only if sample size is greater than the number of folders.")
	public boolean includeEsEmpty = false;

	@Option(names = "--report-es-oversized", description = "Report ES folders whose count is greater than the one in db")
	public boolean reportEsOversized = false;

	@Option(names = "--output-condensed", description = "Output only one line for each mailbox with inconsistencies")
	public boolean outputCondensed = false;

	@Option(names = "--output-email-only", description = "Output only the mailbox name with inconsistencies")
	public boolean outputEmailOnly = false;

	@Override
	public void synchronousDirOperation(String domainUid, ItemValue<DirEntry> de) throws Exception {
		if (Strings.isNullOrEmpty(de.value.email)) {
			return;
		}

		String displayName = displayName(de);
		ItemValue<Mailbox> mailboxItem = ctx.adminApi().instance(IMailboxes.class, domainUid).getComplete(de.uid);
		if (!includeArchived && (mailboxItem.value.routing != Routing.internal || mailboxItem.value.archived)) {
			if (!outputEmailOnly && !outputCondensed) {
				ctx.info("[{}]: skipping mailbox [routing={}, archived={}]", displayName, mailboxItem.value.routing,
						mailboxItem.value.archived);
			}
			return;
		}

		MailspoolStats stats = ESearchActivator.mailspoolStats();
		boolean aliasExists = stats.exists(de.uid);
		Report report = (!aliasExists) //
				? new Report(true, mailboxItem.value.archived)
				: reportAlias(stats, domainUid, de, mailboxItem);

		report(report, displayName(de));

		if (report.hasIncoherency() && runConsolidate) {
			IMailboxMgmt imboxesMgmt = ctx.adminApi().instance(IMailboxMgmt.class, domainUid);
			TaskRef ref = imboxesMgmt.consolidateMailbox(de.uid);
			Tasks.follow(ctx, ref, ">" + displayName,
					String.format("Failed to consolidate mailbox index of entry %s", de));
		}
	}

	private String displayName(ItemValue<DirEntry> de) {
		return (de.value.email != null && !de.value.email.isEmpty()) ? (de.value.email + " (" + de.uid + ")") : de.uid;
	}

	private Report reportAlias(MailspoolStats stats, String domainUid, ItemValue<DirEntry> de,
			ItemValue<Mailbox> mailboxItem) {
		long missingParentCount;
		try {
			missingParentCount = stats.missingParentCount(de.uid);
		} catch (ElasticsearchException | IOException e) {
			missingParentCount = 0;
			ctx.error("Unable to get the missing parent count for entry '{}'", de.uid, e);
		}

		String subtree = IMailReplicaUids.subtreeUid(domainUid, de.value);
		IDbByContainerReplicatedMailboxes replicatedMailboxesApi = ctx.adminApi()
				.instance(IDbByContainerReplicatedMailboxes.class, subtree);
		Map<String, ItemValue<MailboxFolder>> mailboxesByUid = replicatedMailboxesApi.all().stream()
				.collect(Collectors.toMap(iv -> iv.uid, iv -> iv));

		Report report = new Report(missingParentCount, mailboxItem.value.archived, mailboxesByUid);

		SampleStrategy strategy = SampleStrategy.valueOfCaseInsensitive(sampleStrategy).orElse(SampleStrategy.BIGGEST);
		FolderCount.Parameters parameters = new FolderCount.Parameters(all, strategy, sampleSize, includeEsEmpty,
				includeDeleted);
		List<FolderCount> countByFolders;
		try {
			countByFolders = stats.countByFolders(de.uid, parameters);
		} catch (ElasticsearchException | IOException e) {
			ctx.error("Unable to get the folder count for entry '{}'", de.uid, e);
			countByFolders = Collections.emptyList();
		}
		checkFolders(report, countByFolders);
		if (all) {
			checkMissingFolderInEs(report, mailboxesByUid, countByFolders);
		}

		return report;
	}

	private void report(Report report, String displayName) {
		if (!report.hasIncoherency() && !outputEmailOnly && !outputCondensed) {
			ctx.info("[{}]: ES mailspool index is up to date [archived={}]", displayName, report.isArchived);
		} else if (report.hasIncoherency()) {
			if (outputEmailOnly) {
				ctx.info("[{}]", displayName);
			} else if (outputCondensed) {
				ctx.info("[{}]: parent={} incoherency={} dbMissing={} esMissing={} missingAlias={} isArchived={}",
						displayName, report.missingParentCount, report.divergent().size(), report.missingInDb().size(),
						report.missingInEs().size(), report.missingAlias, report.isArchived);
			} else {
				if (report.isArchived) {
					ctx.warn("[{}] - mailbox is archived", displayName);
				}
				if (report.missingAlias) {
					ctx.warn("[{}] - mailbox alias is missing in ES", displayName);
				}
				if (report.missingParentCount > 0) {
					ctx.warn("[{}] - missing parent count: {}", displayName, report.missingParentCount());
				}
				report.divergent().forEach(incoherency -> {
					ctx.warn("[{}] - {} ({}): es={}, db={}", displayName, incoherency.uid(),
							report.mailboxesByUid().get(incoherency.uid()).value.fullName, incoherency.esCount(),
							incoherency.dbCount());
				});
				report.missingInDb().forEach(incoherency -> {
					ctx.warn("[{}] - {}: es={}, db=not found", displayName, incoherency.uid(), incoherency.esCount());
				});
				report.missingInEs().forEach(incoherency -> {
					ctx.warn("[{}] - {} ({}): es=not found, db={}", displayName, incoherency.uid(),
							report.mailboxesByUid().get(incoherency.uid()).value.fullName, incoherency.dbCount());
				});
			}
		}
	}

	private void checkMissingFolderInEs(Report report, Map<String, ItemValue<MailboxFolder>> mailboxesByUid,
			List<FolderCount> countByFolders) {
		Set<String> dbMailboxes = mailboxesByUid.keySet().stream().collect(Collectors.toSet());
		Set<String> esMailboxes = countByFolders.stream().map(FolderCount::folderUid).collect(Collectors.toSet());
		Set<String> esMissingMailboxes = Sets.difference(dbMailboxes, esMailboxes);

		esMissingMailboxes.stream() //
				.forEach(mailboxUid -> {
					IDbMailboxRecords mailboxRecordsApi = ctx.adminApi() //
							.instance(IDbMailboxRecords.class, mailboxUid);
					ItemFlagFilter filter = (includeDeleted) //
							? ItemFlagFilter.all() //
							: ItemFlagFilter.create().mustNot(ItemFlag.Deleted);
					Count dbCount = mailboxRecordsApi.count(filter);
					if (dbCount.total != 0) {
						report.addMissingInEs(mailboxUid, dbCount.total);
					}
				});
	}

	private void checkFolders(Report report, List<FolderCount> countByFolders) {
		countByFolders.stream()//
				.filter(folderCount -> {
					String uid = IMailReplicaUids.mboxRecords(folderCount.folderUid());
					IContainers containersApi = ctx.adminApi().instance(IContainers.class);
					ContainerDescriptor descriptor = containersApi.getIfPresent(uid);
					if (descriptor == null) {
						report.addMissingInDb(folderCount.folderUid(), folderCount.count());
					}
					return (descriptor != null);
				}).forEach(folderCount -> {
					IDbMailboxRecords mailboxRecordsApi = ctx.adminApi() //
							.instance(IDbMailboxRecords.class, folderCount.folderUid());
					ItemFlagFilter filter = (includeDeleted) //
							? ItemFlagFilter.all() //
							: ItemFlagFilter.create().mustNot(ItemFlag.Deleted);
					Count dbCount = mailboxRecordsApi.count(filter);
					double allowedDiscrepency = Math.ceil(dbCount.total * delta / 100.0);
					boolean incohenrecy = folderCount.count() < dbCount.total - allowedDiscrepency
							|| folderCount.count() > dbCount.total + allowedDiscrepency;
					if (incohenrecy && (dbCount.total > folderCount.count() || reportEsOversized)) {
						report.addDivergent(folderCount.folderUid(), folderCount.count(), dbCount.total);
					}
				});
	}

	@Override
	public Kind[] getDirEntryKind() {
		return new Kind[] { Kind.MAILSHARE, Kind.USER, Kind.GROUP };
	}

	private class Report {

		private final boolean missingAlias;
		private final long missingParentCount;
		private final boolean isArchived;
		private final List<FolderIncoherency> incoherencies = new ArrayList<>();
		private final Map<String, ItemValue<MailboxFolder>> mailboxesByUid;

		public Report(boolean missingAlias, boolean isArchived) {
			this.missingAlias = missingAlias;
			this.missingParentCount = 0;
			this.isArchived = isArchived;
			this.mailboxesByUid = new HashMap<>();
		}

		public Report(long missingParentCount, boolean isArchived,
				Map<String, ItemValue<MailboxFolder>> mailboxesByUid) {
			this.missingAlias = false;
			this.missingParentCount = missingParentCount;
			this.isArchived = isArchived;
			this.mailboxesByUid = mailboxesByUid;
		}

		public boolean missingAlias() {
			return missingAlias;
		}

		public long missingParentCount() {
			return missingParentCount;
		}

		public Map<String, ItemValue<MailboxFolder>> mailboxesByUid() {
			return mailboxesByUid;
		}

		public boolean hasIncoherency() {
			return missingAlias || missingParentCount > 0 || incoherencies.size() > 0;
		}

		public List<FolderIncoherency> missingInEs() {
			return incoherencies.stream().filter(FolderIncoherency::missingInEs).collect(Collectors.toList());
		}

		public List<FolderIncoherency> missingInDb() {
			return incoherencies.stream().filter(FolderIncoherency::missingInDb).collect(Collectors.toList());
		}

		public List<FolderIncoherency> divergent() {
			return incoherencies.stream().filter(FolderIncoherency::divergent).collect(Collectors.toList());
		}

		public void addDivergent(String uid, long esCount, long dbCount) {
			incoherencies.add(new FolderIncoherency(uid, esCount, dbCount));
		}

		public void addMissingInEs(String uid, long dbCount) {
			incoherencies.add(new FolderIncoherency(uid, -1, dbCount));
		}

		public void addMissingInDb(String uid, long esCount) {
			incoherencies.add(new FolderIncoherency(uid, esCount, -1));
		}

	}

	private class FolderIncoherency {
		private final String uid;
		private final long esCount;
		private final long dbCount;

		public FolderIncoherency(String uid, long esCount, long dbCount) {
			this.uid = uid;
			this.esCount = esCount;
			this.dbCount = dbCount;
		}

		public String uid() {
			return uid;
		}

		public long dbCount() {
			return dbCount;
		}

		public long esCount() {
			return esCount;
		}

		public boolean missingInEs() {
			return esCount == -1;
		}

		public boolean missingInDb() {
			return dbCount == -1;
		}

		public boolean divergent() {
			return dbCount != -1 && esCount != -1;
		}
	}

}
