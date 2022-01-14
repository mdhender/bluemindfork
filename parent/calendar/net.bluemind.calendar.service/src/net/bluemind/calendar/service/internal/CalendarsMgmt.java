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
package net.bluemind.calendar.service.internal;

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

import net.bluemind.calendar.api.CalendarDescriptor;
import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.calendar.api.ICalendarsMgmt;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.calendar.persistence.VEventIndexStore;
import net.bluemind.calendar.persistence.VEventSeriesStore;
import net.bluemind.calendar.service.IInCoreCalendarsMgmt;
import net.bluemind.config.InstallationId;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.ContainerHierarchyNode;
import net.bluemind.core.container.api.ContainerQuery;
import net.bluemind.core.container.api.IContainerManagement;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.hierarchy.hook.HierarchyIdsHints;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ContainerModifiableDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.ContainerSyncStore;
import net.bluemind.core.container.persistence.DataSourceRouter;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.sanitizer.Sanitizer;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.core.task.service.ITasksManager;
import net.bluemind.core.validator.Validator;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.directory.service.DirEntryHandlers;
import net.bluemind.lib.elasticsearch.ESearchActivator;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.server.api.IServer;

public class CalendarsMgmt implements ICalendarsMgmt, IInCoreCalendarsMgmt {
	private static final Logger logger = LoggerFactory.getLogger(CalendarsMgmt.class);
	private BmContext context;
	private RBACManager rbacManager;
	private Validator validator;
	private Sanitizer sanitizer;

	public CalendarsMgmt(BmContext context) {
		this.context = context;
		rbacManager = new RBACManager(context);
		sanitizer = new Sanitizer(context);
		validator = new Validator(context);
	}

	@Override
	public TaskRef reindexAll() throws ServerFault {
		return context.provider().instance(ITasksManager.class).run(this::reindexAll);
	}

	@Override
	public void reindexAll(IServerTaskMonitor monitor) throws Exception {
		if (!context.getSecurityContext().isDomainGlobal()) {
			throw new ServerFault("only admin0 can call this method ", ErrorCode.FORBIDDEN);
		}

		Set<String> all = getContainerUids();

		monitor.begin(all.size() + 1d, "begin calendars reindexation [" + all.size() + "]");
		ESearchActivator.resetIndex("event");
		monitor.progress(1, "Index event reseted");

		for (String uid : all) {
			IServerTaskMonitor subMonitor = monitor.subWork("calendar [" + uid + "]", 1);
			try {
				reindex(uid, subMonitor);
			} catch (ServerFault sf) {
				logger.error("Failed to reindex calendar {}: {}", uid, sf.getMessage());
				monitor.log("Failed to reindex calendar " + uid);
			}

		}
	}

	@Override
	public void reindexDomain(String domainUid, IServerTaskMonitor monitor) throws Exception {
		if (!context.getSecurityContext().isDomainAdmin(domainUid)) {
			throw new ServerFault("only admin of " + domainUid + " can call this method ", ErrorCode.FORBIDDEN);
		}

		IContainers service = context.provider().instance(IContainers.class);
		List<ContainerDescriptor> containers = service.all(ContainerQuery.type(ICalendarUids.TYPE));

		monitor.begin(containers.size(), "begin calendars reindexation [" + containers.size() + "]");
		for (ContainerDescriptor c : containers) {
			if (domainUid.equals(c.uid)) {
				IServerTaskMonitor subMonitor = monitor.subWork("calendar [" + c.uid + "]", 1);
				try {
					reindex(c.uid, subMonitor);
				} catch (ServerFault sf) {
					logger.error("Failed to reindex calendar {}: {}", c.uid, sf.getMessage());
					monitor.log("Failed to reindex calendar " + c.uid);
				}

			}
		}

	}

	@Override
	public TaskRef reindex(final String calUid) throws ServerFault {
		return context.provider().instance(ITasksManager.class).run(monitor -> reindex(calUid, monitor));
	}

	@Override
	public void reindex(String calUid, IServerTaskMonitor monitor) throws Exception {
		DataSource ds = DataSourceRouter.get(context, calUid);
		ContainerStore containerStore = new ContainerStore(context, ds, context.getSecurityContext());

		Container c = containerStore.get(calUid);
		if (c == null) {
			throw new ServerFault("no calendar with uid " + calUid, ErrorCode.NOT_FOUND);
		}

		if (!context.getSecurityContext().isDomainAdmin(c.domainUid)) {
			throw new ServerFault("only admin of " + c.domainUid + " can call this method ", ErrorCode.FORBIDDEN);
		}
		reindex(c, ds, monitor);
	}

	private void reindex(Container container, DataSource ds, IServerTaskMonitor monitor) throws ServerFault {
		VEventContainerStoreService storeService = new VEventContainerStoreService(context, ds,
				context.getSecurityContext(), container, new VEventSeriesStore(ds, container));

		VEventIndexStore indexStore = new VEventIndexStore(ESearchActivator.getClient(), container,
				DataSourceRouter.location(context, container.uid));

		logger.info("reindexing calendar {}", container.uid);
		// reinit container index
		indexStore.deleteAll();

		List<String> uids = storeService.allUids();
		monitor.begin(uids.size() + 1d, "reindexing calendar [" + container.uid + "] (size:" + uids.size() + ")");
		Lists.partition(uids, 500).forEach(subUids -> {
			List<ItemValue<VEventSeries>> values = storeService.getMultiple(subUids);
			indexStore.updates(values);
			monitor.progress(subUids.size(), "calendar [" + container.uid + "] reindexing...");
		});

		// only report one time
		monitor.progress(1, "calendar [" + container.uid + "] indexed");
		logger.info("calendar {} reindexed", container.uid);

	}

	@Override
	public void create(String uid, CalendarDescriptor descriptor) throws ServerFault {
		sanitizer.create(descriptor);
		validator.create(descriptor);
		IDirectory dir = context.provider().instance(IDirectory.class, descriptor.domainUid);
		DirEntry entry = dir.findByEntryUid(descriptor.owner);
		if (entry == null) {
			throw new ServerFault("owner " + descriptor.owner + " not found in domain " + descriptor.domainUid);
		}

		if (entry.kind == DirEntry.Kind.DOMAIN) {
			checkCanManageCalendar(descriptor, DirEntry.Kind.CALENDAR);
			DirEntryHandlers.byKind(DirEntry.Kind.CALENDAR).create(context, descriptor.domainUid,
					asDirEntry(uid, descriptor));
			// transmute owner
			descriptor.owner = uid;
		} else {
			checkCanManageCalendar(descriptor, DirEntry.Kind.USER);
		}
		if (descriptor.expectedId != null) {
			String hierUid = ContainerHierarchyNode.uidFor(uid, ICalendarUids.TYPE, descriptor.domainUid);
			HierarchyIdsHints.putHint(hierUid, descriptor.expectedId);
		}

		ContainerDescriptor cd = ContainerDescriptor.create(uid, descriptor.name, descriptor.owner, ICalendarUids.TYPE,
				descriptor.domainUid, false);
		if (descriptor.settings.getOrDefault("readonly", "false").equals("true")) {
			cd.readOnly = true;
		}

		IContainers containers = context.su().provider().instance(IContainers.class);
		containers.create(cd.uid, cd);

		if (!descriptor.settings.isEmpty()) {
			IContainerManagement containerManagement = context.provider().instance(IContainerManagement.class, cd.uid);
			containerManagement.setSettings(descriptor.settings);
		}

		DataSource ds = DataSourceRouter.get(context, cd.uid);
		ContainerStore cs = new ContainerStore(null, ds, SecurityContext.SYSTEM);
		Container container = null;
		try {
			container = cs.get(cd.uid);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}

		ContainerSyncStore syncStore = new ContainerSyncStore(ds, container);
		syncStore.initSync();

	}

	@Override
	public CalendarDescriptor getComplete(String uid) throws ServerFault {
		IContainers containers = context.provider().instance(IContainers.class);
		ContainerDescriptor cd = containers.getIfPresent(uid);
		if (cd == null) {
			return null;
		}

		if (cd.type.equals(ICalendarUids.TYPE)) {
			CalendarDescriptor ret = CalendarDescriptor.create(cd.name, cd.owner, cd.domainUid);
			if (cd.owner.equals(uid)) {
				// domain calendar
				DirEntry entry = context.su().provider().instance(IDirectory.class, cd.domainUid).findByEntryUid(uid);
				ret.orgUnitUid = entry.orgUnitUid;
			}
			ret.settings = cd.settings;
			return ret;
		} else {
			logger.warn("trying to retrieve a calendar descriptor but it's not an calendar but a {}", cd.type);
			return null;
		}
	}

	@Override
	public void update(String uid, CalendarDescriptor descriptor) throws ServerFault {
		CalendarDescriptor old = getComplete(uid);

		if (old == null) {
			throw new ServerFault("calendar " + uid + " not found", ErrorCode.NOT_FOUND);
		}

		sanitizer.update(old, descriptor);
		validator.update(old, descriptor);

		IDirectory dir = context.provider().instance(IDirectory.class, old.domainUid);
		DirEntry entry = dir.findByEntryUid(old.owner);
		if (entry == null) {
			throw new ServerFault("owner " + old.owner + " not found in domain " + old.domainUid);
		}

		if (!old.owner.equals(descriptor.owner)) {
			throw new ServerFault("trying to change calendar owner", ErrorCode.INVALID_PARAMETER);
		}

		checkCanManageCalendar(old, entry.kind);
		if (entry != null && entry.kind == DirEntry.Kind.CALENDAR) {
			DirEntryHandlers.byKind(DirEntry.Kind.CALENDAR).update(context, descriptor.domainUid,
					asDirEntry(uid, descriptor));
		}

		ContainerModifiableDescriptor cmd = new ContainerModifiableDescriptor();
		cmd.name = descriptor.name;
		context.su().provider().instance(IContainers.class).update(uid, cmd);

	}

	@Override
	public void delete(String uid) throws ServerFault {

		CalendarDescriptor descriptor = getComplete(uid);

		if (descriptor == null) {
			throw new ServerFault("calendar " + uid + " not found", ErrorCode.NOT_FOUND);
		}

		IDirectory dir = context.provider().instance(IDirectory.class, descriptor.domainUid);
		DirEntry ownerEntry = dir.findByEntryUid(descriptor.owner);
		if (ownerEntry == null) {
			logger.warn("Strange, owner {} not found in domain {}, continue delete..", descriptor.owner,
					descriptor.domainUid);
		} else {
			checkCanManageCalendar(descriptor, ownerEntry.kind);
			if (ownerEntry.kind == DirEntry.Kind.CALENDAR) {
				DirEntryHandlers.byKind(DirEntry.Kind.CALENDAR).delete(context, descriptor.domainUid, uid);
			}
		}

		DataSource ds = DataSourceRouter.get(context, uid);
		ContainerStore containerStore = new ContainerStore(context, ds, context.getSecurityContext());
		Container container;
		try {
			container = containerStore.get(uid);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}

		VEventContainerStoreService storeService = new VEventContainerStoreService(context, ds,
				context.getSecurityContext(), container, new VEventSeriesStore(ds, container));
		VEventIndexStore indexStore = new VEventIndexStore(ESearchActivator.getClient(), container,
				DataSourceRouter.location(context, container.uid));

		storeService.prepareContainerDelete();
		indexStore.deleteAll();
		context.su().provider().instance(IContainers.class).delete(uid);
	}

	private void checkCanManageCalendar(CalendarDescriptor descriptor, Kind ownerKind) throws ServerFault {
		switch (ownerKind) {
		case USER:
			if (!rbacManager.forDomain(descriptor.domainUid).forEntry(descriptor.owner).can("Manage")
					&& (!((context.getSecurityContext().getSubject().equals(descriptor.owner)
							&& context.getSecurityContext().getContainerUid().equals(descriptor.domainUid))))) {
				throw new ServerFault("cannot manage this calendar", ErrorCode.PERMISSION_DENIED);
			}
			break;
		case CALENDAR:
			rbacManager.forDomain(descriptor.domainUid).forOrgUnit(descriptor.orgUnitUid)
					.check(BasicRoles.ROLE_MANAGE_DOMAIN_CAL);
			break;
		default:
			throw new ServerFault("Invalid owner " + ownerKind, ErrorCode.INVALID_PARAMETER);
		}

	}

	private DirEntry asDirEntry(String uid, CalendarDescriptor descriptor) {
		DirEntry entry = DirEntry.create(descriptor.orgUnitUid, descriptor.domainUid + "/calendars/" + uid,
				DirEntry.Kind.CALENDAR, uid, descriptor.name, null, false, false, false);
		List<String> assignedServers = context.su().provider().instance(IServer.class, InstallationId.getIdentifier())
				.byAssignment(descriptor.domainUid, "mail/imap");
		if (!assignedServers.isEmpty()) {
			entry.dataLocation = assignedServers.get(0);
		}
		return entry;
	}

	private Set<String> getContainerUids() throws SQLException {
		Collection<DataSource> dataSources = context.getAllMailboxDataSource();
		Set<String> all = new LinkedHashSet<>();

		for (DataSource ds : dataSources) {
			ContainerStore cs = new ContainerStore(context, ds, context.getSecurityContext());
			List<Container> containers = cs.findByType(ICalendarUids.TYPE);
			all.addAll(containers.stream().map(c -> c.uid).collect(Collectors.toList()));
		}

		ContainerStore cs = new ContainerStore(context, context.getDataSource(), context.getSecurityContext());
		List<Container> containers = cs.findByType(ICalendarUids.TYPE);
		all.addAll(containers.stream().map(c -> c.uid).collect(Collectors.toList()));

		return all;
	}

}
