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
package net.bluemind.maintenance.elastic;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
import net.bluemind.lib.elasticsearch.ESearchActivator;
import net.bluemind.mailbox.api.IMailboxMgmt;
import net.bluemind.mailbox.api.SimpleShardStats;
import net.bluemind.maintenance.IMaintenanceScript;
import net.bluemind.system.api.hot.upgrade.HotUpgradeTask;
import net.bluemind.system.api.hot.upgrade.HotUpgradeTaskExecutionMode;
import net.bluemind.system.api.hot.upgrade.HotUpgradeTaskStatus;
import net.bluemind.system.api.hot.upgrade.IInternalHotUpgrade;

public class IndexRollover implements IMaintenanceScript {

	@Override
	public void run(IServerTaskMonitor monitor) {
		// we have mailspool_12 ... mailspool_32
		// let's roll all mailspool_12 content to mailspool_33 then delete it

		monitor.begin(1, "Preparing index rollover");
		ServerSideServiceProvider prov = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);

		IMailboxMgmt mboxMgmt = prov.instance(IMailboxMgmt.class, "global.virt");
		List<SimpleShardStats> existing = mboxMgmt.getLiteStats();
		if (existing.isEmpty()) {
			monitor.end(true, "No shards found", "404");
			return;
		}

		int targetIndexNumber = 1;
		int sourceIndexNumber = Integer.MAX_VALUE;
		SimpleShardStats src = null;
		for (SimpleShardStats ss : existing) {
			monitor.log("Inspect {} with {} aliases", ss.indexName, ss.mailboxes.size());
			int idxId = Integer.parseInt(ss.indexName.substring("mailspool_".length()));
			targetIndexNumber = Math.max(targetIndexNumber, idxId);
			sourceIndexNumber = Math.min(sourceIndexNumber, idxId);
			if (sourceIndexNumber == idxId) {
				src = ss;
			}
		}

		if (src == null) {
			monitor.log("Leaving because source index is missing");
			monitor.end(true, "No source index", "404");
			return;
		}

		String dest = "mailspool_" + (targetIndexNumber + 1);

		monitor.log("Moving " + src.mailboxes.size() + " aliases in " + src.indexName + " to " + dest);

		ESearchActivator.putMeta(src.indexName, ESearchActivator.BM_MAINTENANCE_STATE_META_KEY, "drainingInProgress");

		Date toReg = new Date();
		IDomains domApi = prov.instance(IDomains.class);
		List<ItemValue<Domain>> knownDomains = domApi.all().stream().filter(d -> !d.value.global)
				.collect(Collectors.toList());
		IInternalHotUpgrade hpService = prov.instance(IInternalHotUpgrade.class);
		int queued = 0;
		Iterator<String> mboxIterator = src.mailboxes.iterator();
		while (mboxIterator.hasNext()) {
			String mailboxUid = mboxIterator.next();
			String domainUid = lookupDomain(prov, knownDomains, mailboxUid);
			if (domainUid == null) {
				continue;
			}
			boolean deleteSource = !mboxIterator.hasNext();
			HotUpgradeTask moveAlias = createTask(toReg, src.indexName, dest, mailboxUid, domainUid, deleteSource);
			if (deleteSource) {
				monitor.log("Index " + src.indexName + " should be drained. Prepare for deleting it.");
			}
			try {
				hpService.create(moveAlias);
				queued++;
			} catch (Exception e) {
				monitor.log("Error registering hot upgrade", e);
			}
		}
		monitor.end(true, "Queued " + queued + " aliases move.", "OK");
	}

	private String lookupDomain(ServerSideServiceProvider prov, List<ItemValue<Domain>> knownDomains,
			String mailboxUid) {
		if (knownDomains.size() == 1) {
			return knownDomains.get(0).uid;
		}
		for (ItemValue<Domain> knownDomain : knownDomains) {
			IDirectory dirApi = prov.instance(IDirectory.class, knownDomain.uid);
			DirEntry result = dirApi.findByEntryUid(mailboxUid);
			if (result != null) {
				return knownDomain.uid;
			}
		}
		return null;
	}

	@Override
	public String name() {
		return "esIndexRollover";
	}

	public static HotUpgradeTask createTask(Date date, String sourceIndex, String targetIndex, String mailboxUid,
			String domainUid, boolean tryRemove) {
		Map<String, Object> operationParams = new HashMap<>();
		operationParams.put("mailboxUid", mailboxUid);
		operationParams.put("domainUid", domainUid);
		operationParams.put("sourceIndex", sourceIndex);
		operationParams.put("targetIndex", targetIndex);
		operationParams.put("tryRemove", Boolean.toString(tryRemove));
		HotUpgradeTask task = new HotUpgradeTask();
		task.operation = ElasticAliasMoveOperation.OP_NAME;
		task.setParameters(operationParams);
		task.status = HotUpgradeTaskStatus.PLANNED;
		task.executionMode = HotUpgradeTaskExecutionMode.JOB;
		task.failure = 0;
		task.createdAt = date;
		task.updatedAt = date;
		return task;
	}

}
