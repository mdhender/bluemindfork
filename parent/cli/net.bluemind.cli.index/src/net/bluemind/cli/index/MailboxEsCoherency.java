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

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.elasticsearch.common.Strings;
import org.elasticsearch.common.util.set.Sets;

import net.bluemind.backend.mail.replica.api.IDbByContainerReplicatedMailboxes;
import net.bluemind.backend.mail.replica.api.IDbMailboxRecords;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.directory.common.SingleOrDomainOperation;
import net.bluemind.core.container.api.Count;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemFlag;
import net.bluemind.core.container.model.ItemFlagFilter;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.lib.elasticsearch.ESearchActivator;
import net.bluemind.lib.elasticsearch.MailspoolStats;
import net.bluemind.lib.elasticsearch.MailspoolStats.FolderCount;
import net.bluemind.lib.elasticsearch.MailspoolStats.FolderCount.SampleStrategy;
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

	@Option(names = "--empty-folder", description = "Also select empty folders. Empty folders are selected only if sample size is greater than the number of folders.")
	public boolean emptyFolder = false;

	@Option(names = "--include-deleted", description = "Include message flag as deleted in the check.")
	public boolean includeDeleted = false;

	@Override
	public void synchronousDirOperation(String domainUid, ItemValue<DirEntry> de) throws Exception {
		if (Strings.isNullOrEmpty(de.value.email)) {
			return;
		}

		MailspoolStats stats = ESearchActivator.mailspoolStats();
		ctx.info("dealing with {}: {}", de.value.email, de.uid);
		long missingParentCount = stats.missingParentCount(de.uid);
		if (missingParentCount > 0) {
			ctx.warn(" - missing parent count: {}", missingParentCount);
		}

		SampleStrategy strategy = SampleStrategy.valueOfCaseInsensitive(sampleStrategy).orElse(SampleStrategy.BIGGEST);
		FolderCount.Parameters parameters = new FolderCount.Parameters(all, strategy, sampleSize, emptyFolder,
				includeDeleted);
		boolean hasDiff = stats.countByFolders(de.uid, parameters) //
				.map(countByFolders -> (all && checkMissingFolderInEs(domainUid, de, countByFolders))
						|| checkFolders(countByFolders)) //
				.orElseGet(() -> {
					ctx.error(" - Unable to get folder count for {}", de.uid);
					return false;
				});

		if (missingParentCount == 0 && !hasDiff) {
			ctx.info(" - ES mailspool index is up to date for {}", de.uid);
		}
	}

	private boolean checkMissingFolderInEs(String domainUid, ItemValue<DirEntry> de, List<FolderCount> countByFolders) {
		String subtree = IMailReplicaUids.subtreeUid(domainUid, de.value);
		IDbByContainerReplicatedMailboxes replicatedMailboxesApi = ctx.adminApi()
				.instance(IDbByContainerReplicatedMailboxes.class, subtree);
		Set<String> dbMailboxes = replicatedMailboxesApi.all().stream().map(iv -> iv.uid).collect(Collectors.toSet());
		Set<String> esMailboxes = countByFolders.stream().map(FolderCount::folderUid).collect(Collectors.toSet());
		Set<String> esMissingMailboxes = Sets.difference(dbMailboxes, esMailboxes);

		return esMissingMailboxes.stream() //
				.map(mailboxUid -> {
					IDbMailboxRecords mailboxRecordsApi = ctx.adminApi() //
							.instance(IDbMailboxRecords.class, mailboxUid);
					ItemFlagFilter filter = (includeDeleted) //
							? ItemFlagFilter.all() //
							: ItemFlagFilter.create().mustNot(ItemFlag.Deleted);
					Count dbCount = mailboxRecordsApi.count(filter);
					if (dbCount.total != 0) {
						ctx.warn(" - {}: es=not found, db={}", mailboxUid, dbCount.total);
					}
					return dbCount.total != 0;
				}).reduce(false, (diff1, diff2) -> diff1 || diff2);
	}

	private boolean checkFolders(List<FolderCount> countByFolders) {
		return countByFolders.stream()//
				.filter(folderCount -> {
					String uid = IMailReplicaUids.mboxRecords(folderCount.folderUid());
					IContainers containersApi = ctx.adminApi().instance(IContainers.class);
					ContainerDescriptor descriptor = containersApi.getIfPresent(uid);
					if (descriptor == null) {
						ctx.warn(" - {}: es={}, db=not found", folderCount.folderUid(), folderCount.count());
					}
					return (descriptor != null);
				}).map(folderCount -> {
					IDbMailboxRecords mailboxRecordsApi = ctx.adminApi() //
							.instance(IDbMailboxRecords.class, folderCount.folderUid());
					ItemFlagFilter filter = (includeDeleted) //
							? ItemFlagFilter.all() //
							: ItemFlagFilter.create().mustNot(ItemFlag.Deleted);
					Count dbCount = mailboxRecordsApi.count(filter);
					if (folderCount.count() != dbCount.total) {
						ctx.warn(" - {}: es={}, db={}", folderCount.folderUid(), folderCount.count(), dbCount.total);
					}
					return folderCount.count() != dbCount.total;
				}).reduce(false, (diff1, diff2) -> diff1 || diff2);
	}

	@Override
	public Kind[] getDirEntryKind() {
		return new Kind[] { Kind.MAILSHARE, Kind.USER, Kind.GROUP };
	}

}
