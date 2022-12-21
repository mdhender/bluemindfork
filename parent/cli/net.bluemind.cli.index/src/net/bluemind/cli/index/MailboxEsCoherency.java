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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.elasticsearch.common.Strings;
import org.elasticsearch.common.util.set.Sets;

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

		String displayName = (de.value.email != null && !de.value.email.isEmpty())
				? (de.value.email + " (" + de.uid + ")")
				: de.uid;

		String subtree = IMailReplicaUids.subtreeUid(domainUid, de.value);
		IDbByContainerReplicatedMailboxes replicatedMailboxesApi = ctx.adminApi()
				.instance(IDbByContainerReplicatedMailboxes.class, subtree);
		Map<String, ItemValue<MailboxFolder>> mailboxesByUid = replicatedMailboxesApi.all().stream()
				.collect(Collectors.toMap(iv -> iv.uid, iv -> iv));

		MailspoolStats stats = ESearchActivator.mailspoolStats();
		long missingParentCount = stats.missingParentCount(de.uid);
		Report report = new Report(missingParentCount);

		SampleStrategy strategy = SampleStrategy.valueOfCaseInsensitive(sampleStrategy).orElse(SampleStrategy.BIGGEST);
		FolderCount.Parameters parameters = new FolderCount.Parameters(all, strategy, sampleSize, includeEsEmpty,
				includeDeleted);
		stats.countByFolders(de.uid, parameters) //
				.ifPresent(countByFolders -> {
					checkFolders(report, countByFolders);
					if (all) {
						checkMissingFolderInEs(report, mailboxesByUid, countByFolders);
					}
				});

		report(report, displayName, mailboxesByUid);

		if (report.hasIncoherency() && runConsolidate) {
			IMailboxMgmt imboxesMgmt = ctx.adminApi().instance(IMailboxMgmt.class, domainUid);
			TaskRef ref = imboxesMgmt.consolidateMailbox(de.uid);
			Tasks.follow(ctx, ref, ">" + displayName,
					String.format("Failed to consolidate mailbox index of entry %s", de));
		}
	}

	private void report(Report report, String displayName, Map<String, ItemValue<MailboxFolder>> mailboxesByUid) {
		if (!report.hasIncoherency() && !outputEmailOnly && !outputCondensed) {
			ctx.info("[{}]: ES mailspool index is up to date", displayName);
		} else if (report.hasIncoherency()) {
			if (outputEmailOnly) {
				ctx.info("[{}]", displayName);
			} else if (outputCondensed) {
				ctx.info("[{}]: parent={} incoherency={} dbMissing={} esMissing={}", displayName,
						report.missingParentCount, report.divergent().size(), report.missingInDb().size(),
						report.missingInEs().size());
			} else {
				if (report.missingParentCount > 0) {
					ctx.warn("[{}] - missing parent count: {}", displayName, report.missingParentCount());
				}
				report.divergent().forEach(incoherency -> {
					ctx.warn("[{}] - {} ({}): es={}, db={}", displayName, incoherency.uid(),
							mailboxesByUid.get(incoherency.uid()).value.fullName, incoherency.esCount(),
							incoherency.dbCount());
				});
				report.missingInDb().forEach(incoherency -> {
					ctx.warn("[{}] - {}: es={}, db=not found", incoherency.uid(), incoherency.esCount());
				});
				report.missingInEs().forEach(incoherency -> {
					ctx.warn("[{}] - {} ({}): es=not found, db={}", incoherency.uid(),
							mailboxesByUid.get(incoherency.uid()).value.fullName, incoherency.dbCount());
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
					double allowedDiscrepency = dbCount.total * delta / 100.0;
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

		private final long missingParentCount;
		private final List<FolderIncoherency> incoherencies = new ArrayList<>();

		public Report(long missingParentCount) {
			this.missingParentCount = missingParentCount;
		}

		public long missingParentCount() {
			return missingParentCount;
		}

		public boolean hasIncoherency() {
			return missingParentCount > 0 || incoherencies.size() > 0;
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
