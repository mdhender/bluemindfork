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
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.backend.mail.replica.service.internal.repair;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.slf4j.event.Level;

import com.google.common.hash.Hashing;

import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.backend.mail.replica.api.IDbByContainerReplicatedMailboxes;
import net.bluemind.backend.mail.replica.api.IDbMailboxRecords;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.backend.mail.replica.api.MailboxReplica;
import net.bluemind.core.container.api.IFlatHierarchyUids;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.persistence.DataSourceRouter;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.task.service.IServerTask;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.imap.IMAPException;
import net.bluemind.imap.ListInfo;
import net.bluemind.imap.StoreClient;
import net.bluemind.imap.TaggedResult;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.network.topology.Topology;
import net.bluemind.server.api.Server;

public class RecordsResyncTask implements IServerTask {

	private BmContext context;
	private CyrusPartition partition;
	private ItemValue<Mailbox> lookup;

	public RecordsResyncTask(BmContext context, CyrusPartition partition, ItemValue<Mailbox> lookup) {
		this.context = context;
		this.partition = partition;
		this.lookup = lookup;
	}

	@Override
	public void run(IServerTaskMonitor monitor) throws Exception {

		String loc = DataSourceRouter.location(context,
				IFlatHierarchyUids.getIdentifier(lookup.uid, partition.domainUid));
		ItemValue<Server> server = Topology.get().datalocation(loc);
		final String logName = lookup.value.defaultEmail().address;
		monitor.log("[{}] Syncing folders...", Level.INFO, logName);
		MailboxWalk walk = MailboxWalk.create(context, lookup, partition.domainUid, server.value);
		String subtreeUid = IMailReplicaUids.subtreeUid(partition.domainUid, lookup);
		IDbByContainerReplicatedMailboxes foldersApi = context.provider()
				.instance(IDbByContainerReplicatedMailboxes.class, subtreeUid);
		walk.folders((StoreClient sc, List<ListInfo> hier) -> {
			monitor.begin(2.0 * hier.size(), "[" + logName + "] Working on " + hier.size() + " folders.");

			ListInfo lastFolder = hier.stream().filter(ListInfo::isSelectable).reduce(null, (v1, v2) -> v2);
			ItemValue<MailboxReplica> lastProc = foldersApi.byReplicaName(lastFolder.getName());
			IDbMailboxRecords recs = context.provider().instance(IDbMailboxRecords.class, lastProc.uid);
			long preUpdate = recs.getVersion();

			String mboxFlag = Hashing.sipHash24().hashString(lookup.uid, StandardCharsets.UTF_8).toString();

			setFlag(monitor, logName, sc, hier, mboxFlag, true);

			// wait until the last folder version changes..
			try {
				waitFolderVersionChange(recs, preUpdate);
			} catch (InterruptedException e1) {
				Thread.currentThread().interrupt();
				monitor.log("Interrupted.");
				return;
			}

			setFlag(monitor, logName, sc, hier, mboxFlag, false);

		}, monitor);

	}

	private void setFlag(IServerTaskMonitor monitor, final String logName, StoreClient sc, List<ListInfo> hier,
			String mboxFlag, boolean add) {
		for (ListInfo f : hier) {
			if (f.isSelectable()) {
				try {
					resync(logName, sc, f, add, mboxFlag, monitor.subWork(1));
				} catch (IMAPException e) {
					monitor.log("[{}] Sync error", e);
				}
			} else {
				monitor.progress(1, null);
			}
		}
	}

	private void waitFolderVersionChange(IDbMailboxRecords recs, long preUpdate) throws InterruptedException {
		do {
			Thread.sleep(2000);
		} while (preUpdate == recs.getVersion());
	}

	private void resync(String logName, StoreClient sc, ListInfo f, boolean set, String mboxFlag,
			IServerTaskMonitor monitor) throws IMAPException {
		monitor.begin(1, "");
		if (sc.select(f.getName())) {
			TaggedResult result = sc.tagged("UID STORE 1:* " + (set ? "+" : "-") + "FLAGS.SILENT (" + mboxFlag + ")");
			monitor.log("[{}] Flag {} " + (set ? "SET" : "UNSET") + " in {} => {}", Level.INFO, logName, mboxFlag,
					f.getName(), result.isOk());
		} else {
			monitor.log("[{}] Cannot select {}", Level.WARN, logName, f.getName());
		}
		monitor.progress(1, "");
		monitor.end(true, "", null);
	}

}
