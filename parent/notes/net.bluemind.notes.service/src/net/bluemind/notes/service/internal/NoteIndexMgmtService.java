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
package net.bluemind.notes.service.internal;

import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.DataSourceRouter;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.service.IServerTask;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.core.task.service.ITasksManager;
import net.bluemind.lib.elasticsearch.ESearchActivator;
import net.bluemind.notes.api.INoteIndexMgmt;
import net.bluemind.notes.api.INoteUids;
import net.bluemind.notes.api.VNote;
import net.bluemind.notes.persistence.VNoteIndexStore;
import net.bluemind.notes.persistence.VNoteStore;
import net.bluemind.notes.service.VNoteContainerStoreService;

public class NoteIndexMgmtService implements INoteIndexMgmt {

	private final Logger logger = LoggerFactory.getLogger(NoteIndexMgmtService.class);
	private final BmContext context;

	public NoteIndexMgmtService(BmContext context) {
		this.context = context;
	}

	@Override
	public TaskRef reindexAll() throws ServerFault {
		return context.provider().instance(ITasksManager.class).run(reindexAllTask());
	}

	@Override
	public TaskRef reindex(String containerUid) throws ServerFault {
		return context.provider().instance(ITasksManager.class).run(reindexTask(containerUid));
	}

	public void reindex(String containerUid, IServerTaskMonitor monitor) throws Exception {
		reindexTask(containerUid).run(monitor);
	}

	private IServerTask reindexAllTask() {
		return new IServerTask() {

			@Override
			public void run(IServerTaskMonitor monitor) throws Exception {
				if (!context.getSecurityContext().isDomainGlobal()) {
					throw new ServerFault("only admin0 can call this method ", ErrorCode.FORBIDDEN);
				}

				Set<String> all = getContainerUids();

				monitor.begin(all.size() + 1, "begin notes reindexation [" + all.size() + "]");
				ESearchActivator.resetIndex("note");
				monitor.progress(1, "Index note reseted");

				for (String uid : all) {
					IServerTaskMonitor subMonitor = monitor.subWork("notes [" + uid + "]", 1);
					try {
						reindex(uid, subMonitor);
					} catch (ServerFault sf) {
						logger.error("Failed to reindex notes {}: {}", uid, sf.getMessage());
						monitor.log("Failed to reindex notes " + uid);
					}

				}
			}
		};
	}

	private IServerTask reindexTask(final String notesUid) {
		return new IServerTask() {

			@Override
			public void run(IServerTaskMonitor monitor) throws Exception {
				DataSource ds = DataSourceRouter.get(context, notesUid);
				ContainerStore containerStore = new ContainerStore(context, ds, context.getSecurityContext());

				Container c = containerStore.get(notesUid);

				if (c == null) {
					throw new ServerFault("notes " + notesUid + " not found", ErrorCode.NOT_FOUND);
				}

				if (!context.getSecurityContext().isDomainAdmin(c.domainUid)) {
					throw new ServerFault("only admin of " + c.domainUid + " can call this method ",
							ErrorCode.FORBIDDEN);
				}

				reindex(c, monitor);
			}
		};
	}

	private void reindex(Container container, IServerTaskMonitor monitor) throws ServerFault {
		DataSource ds = DataSourceRouter.get(context, container.uid);
		VNoteContainerStoreService storeService = new VNoteContainerStoreService(context, ds,
				context.getSecurityContext(), container, new VNoteStore(ds, container));

		VNoteIndexStore indexStore = new VNoteIndexStore(ESearchActivator.getClient(), container,
				DataSourceRouter.location(context, container.uid));
		logger.info("reindexing notes {}", container.uid);
		// reinit container index
		indexStore.deleteAll();

		List<String> uids = storeService.allUids();
		monitor.begin(uids.size() + 1, "reindexing notes [" + container.uid + "] (size:" + uids.size() + ")");
		Lists.partition(uids, 500).forEach(subUids -> {
			List<ItemValue<VNote>> values = storeService.getMultiple(subUids);
			indexStore.updates(values);
			monitor.progress(subUids.size(), "notes [" + container.uid + "] reindexing...");
		});

		// only report one time
		monitor.progress(1, "notes [" + container.uid + "] indexed");
		logger.info("notes {} reindexed", container.uid);

	}

	private Set<String> getContainerUids() throws SQLException {
		Collection<DataSource> dataSources = context.getAllMailboxDataSource();
		Set<String> all = new LinkedHashSet<>();

		for (DataSource ds : dataSources) {
			ContainerStore cs = new ContainerStore(context, ds, context.getSecurityContext());
			List<Container> containers = cs.findByType(INoteUids.TYPE);
			all.addAll(containers.stream().map(c -> c.uid).collect(Collectors.toList()));
		}

		ContainerStore cs = new ContainerStore(context, context.getDataSource(), context.getSecurityContext());
		List<Container> containers = cs.findByType(INoteUids.TYPE);
		all.addAll(containers.stream().map(c -> c.uid).collect(Collectors.toList()));

		return all;
	}
}
