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
package net.bluemind.core.backup.continuous.mgmt.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import net.bluemind.config.InstallationId;
import net.bluemind.core.backup.continuous.DefaultBackupStore;
import net.bluemind.core.backup.continuous.ILiveBackupStreams;
import net.bluemind.core.backup.continuous.ILiveStream;
import net.bluemind.core.backup.continuous.api.IBackupStoreFactory;
import net.bluemind.core.backup.continuous.mgmt.api.BackupSyncOptions;
import net.bluemind.core.backup.continuous.mgmt.api.IContinuousBackupMgmt;
import net.bluemind.core.backup.continuous.mgmt.service.impl.DomainSync;
import net.bluemind.core.backup.continuous.mgmt.service.impl.OrphansSync;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.service.BlockingServerTask;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.core.task.service.ITasksManager;
import net.bluemind.domain.api.Domain;
import net.bluemind.role.api.BasicRoles;

public class ContinuousBackupMgmtService implements IContinuousBackupMgmt {

	private RBACManager rbacManager;
	private BmContext context;
	private static final Logger logger = LoggerFactory.getLogger(ContinuousBackupMgmtService.class);

	public ContinuousBackupMgmtService(BmContext context) {
		this.context = context;
		this.rbacManager = RBACManager.forContext(context);
	}

	@Override
	public TaskRef syncWithStore(BackupSyncOptions opts) {
		rbacManager.check(BasicRoles.ROLE_SYSTEM_MANAGER);

		return context.provider().instance(ITasksManager.class).run(new BlockingServerTask() {

			@Override
			protected void run(IServerTaskMonitor monitor) throws Exception {
				sync(monitor, opts);

			}
		});
	}

	private void sync(IServerTaskMonitor mon, BackupSyncOptions opts) {

		IBackupStoreFactory store = DefaultBackupStore.store("sync" + InstallationId.getIdentifier());
		mon.log("Targeting store " + store);
		OrphansSync os = new OrphansSync(context);
		List<ItemValue<Domain>> domains = os.syncOrphans(store, mon);
		mon.begin(domains.size(), "Processing " + domains.size() + " domain(s)");

		ILiveBackupStreams reader = DefaultBackupStore.reader().forInstallation(InstallationId.getIdentifier());

		Map<String, ILiveStream> domUidToStream = reader.domains().stream()
				.collect(Collectors.toMap(ILiveStream::domainUid, ls -> ls));
		if (domUidToStream.isEmpty()) {
			mon.end(false, "No stream found for " + InstallationId.getIdentifier(), "FAILED", Level.ERROR);
			return;
		}

		for (ItemValue<Domain> d : domains) {
			IServerTaskMonitor domSyncMon = mon.subWork("Domain " + d.value.defaultAlias, 1);
			if (!d.value.global && domUidToStream.containsKey(d.uid)) {
				DomainSync ds = new DomainSync(context, d, opts);
				ds.sync(store, domSyncMon, domUidToStream.get(d.uid));
			}
		}
		mon.end(true, "kafka 'sync' topic(s) are ready.", "OK");
	}

}
