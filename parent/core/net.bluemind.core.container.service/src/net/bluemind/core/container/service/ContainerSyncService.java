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
package net.bluemind.core.container.service;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.api.report.DiagnosticReport;
import net.bluemind.core.container.api.IContainerSync;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ContainerSyncResult;
import net.bluemind.core.container.model.ContainerSyncStatus;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.persistance.ContainerSyncStore;
import net.bluemind.core.container.persistance.DataSourceRouter;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.container.sync.SyncableContainer;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.service.IServerTask;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.core.task.service.ITasksManager;
import net.bluemind.core.utils.JsonUtils;

public class ContainerSyncService implements IContainerSync {

	private static final Logger logger = LoggerFactory.getLogger(ContainerSyncService.class);

	private BmContext context;
	private Container container;
	private ContainerSyncStore containerSyncStore;

	public ContainerSyncService(BmContext context, Container container) {
		this.context = context;
		this.container = container;
		containerSyncStore = new ContainerSyncStore(DataSourceRouter.get(context, container.uid), container);
	}

	@Override
	public TaskRef sync() throws ServerFault {
		RBACManager.forContext(context).forContainer(container).check(Verb.Manage.name());

		return context.provider().instance(ITasksManager.class).run("container-sync" + container.uid,
				new IServerTask() {

					@Override
					public void run(IServerTaskMonitor monitor) throws Exception {
						doSync(monitor);
					}

				});

	}

	protected void doSync(IServerTaskMonitor monitor) {
		monitor.begin(20, "Start synchronization...");
		DiagnosticReport report = DiagnosticReport.create();
		try {
			ContainerSyncStatus ss = containerSyncStore.getSyncStatus();
			monitor.progress(1, "syncing");
			if (ss != null) {
				SyncableContainer syncableContainer = new SyncableContainer(context.su());
				ContainerSyncResult res = syncableContainer.sync(container, ss.syncToken, monitor.subWork(18));
				if (res != null) {
					report.ok(getClass().getName(), String.format("%s sync done. created: %d, updated: %d, deleted: %d",
							container.name, res.added, res.updated, res.removed));
					containerSyncStore.setSyncStatus(res.status);
					monitor.progress(1, "save state");
				}
				monitor.end(true, null, JsonUtils.asString(res));
			} else {
				report.ko(getClass().getName(), String.format("%s is not syncable", container.name));
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			monitor.end(false, e.getMessage(), JsonUtils.asString(report));
		}
	}

	@Override
	public Date getLastSync() throws ServerFault {
		RBACManager.forContext(context).forContainer(container).check(Verb.Read.name(), Verb.Manage.name());
		ContainerSyncStatus sync = containerSyncStore.getSyncStatus();
		return sync.lastSync;
	}

}
