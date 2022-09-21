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
package net.bluemind.addressbook.service.internal;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.ReadStream;
import net.bluemind.addressbook.api.AddressBookDescriptor;
import net.bluemind.addressbook.api.IAddressBookUids;
import net.bluemind.addressbook.api.IAddressBooksMgmt;
import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.persistence.VCardIndexStore;
import net.bluemind.addressbook.persistence.VCardStore;
import net.bluemind.addressbook.service.IInCoreAddressBooksMgmt;
import net.bluemind.config.InstallationId;
import net.bluemind.core.api.Stream;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.ContainerHierarchyNode;
import net.bluemind.core.container.api.ContainerQuery;
import net.bluemind.core.container.api.IContainerManagement;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.api.IRestoreCrudSupport;
import net.bluemind.core.container.hierarchy.hook.HierarchyIdsHints;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ContainerModifiableDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.ContainerSyncStore;
import net.bluemind.core.container.persistence.DataSourceRouter;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.base.GenericJsonObjectWriteStream;
import net.bluemind.core.rest.base.GenericStream;
import net.bluemind.core.rest.vertx.VertxStream;
import net.bluemind.core.sanitizer.Sanitizer;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.service.BlockingServerTask;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.core.task.service.ITasksManager;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.core.validator.Validator;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.DirEntryQuery;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.directory.service.DirEntryHandlers;
import net.bluemind.lib.elasticsearch.ESearchActivator;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.server.api.IServer;

public class AddressBooksMgmt
		implements IAddressBooksMgmt, IInCoreAddressBooksMgmt, IRestoreCrudSupport<AddressBookDescriptor> {
	private static final Logger logger = LoggerFactory.getLogger(AddressBooksMgmt.class);
	private BmContext context;
	private RBACManager rbacManager;
	private Validator validator;
	private Sanitizer sanitizer;

	public AddressBooksMgmt(BmContext context) {
		this.context = context;
		rbacManager = new RBACManager(context);

		sanitizer = new Sanitizer(context);
		validator = new Validator(context);
	}

	@Override
	public TaskRef reindexAll() throws ServerFault {
		return context.provider().instance(ITasksManager.class).run(new BlockingServerTask() {

			@Override
			protected void run(IServerTaskMonitor monitor) throws Exception {
				reindexAll();
			}
		});
	}

	@Override
	public TaskRef reindexDomain(final String domainUid) throws ServerFault {
		return context.provider().instance(ITasksManager.class).run(m -> BlockingServerTask.run(m, monitor -> {
			reindexDomain(domainUid, monitor);
		}));
	}

	@Override
	public TaskRef reindex(final String bookUid) throws ServerFault {
		return context.provider().instance(ITasksManager.class).run(m -> BlockingServerTask.run(m, monitor -> {
			reindex(bookUid, monitor);
		}));
	}

	@Override
	public void reindex(String bookUid, IServerTaskMonitor monitor) throws Exception {
		DataSource ds = DataSourceRouter.get(context, bookUid);
		ContainerStore containerStore = new ContainerStore(context, ds, context.getSecurityContext());
		Container c = containerStore.get(bookUid);
		if (c == null) {
			throw new ServerFault("addresbook " + bookUid + " not found", ErrorCode.NOT_FOUND);
		}

		if (!context.getSecurityContext().isDomainAdmin(c.domainUid)) {
			throw new ServerFault("only admin of " + c.domainUid + " can call this method ", ErrorCode.FORBIDDEN);
		}

		reindex(c, monitor);
	}

	private void reindex(Container container, IServerTaskMonitor monitor) throws ServerFault {
		DataSource ds = DataSourceRouter.get(context, container.uid);
		VCardIndexStore indexStore = new VCardIndexStore(ESearchActivator.getClient(), container,
				DataSourceRouter.location(context, container.uid));

		VCardContainerStoreService storeService = new VCardContainerStoreService(context, ds,
				context.getSecurityContext(), container, new VCardStore(ds, container), indexStore);

		logger.info("reindexing addressbook {}", container.uid);
		// reinit container index
		indexStore.deleteAll();

		List<String> uids = storeService.allUids();
		monitor.begin(uids.size() + 1d, "reindexing addressbook [" + container.uid + "] (size:" + uids.size() + ")");

		Lists.partition(uids, 500).forEach(subUids -> {
			List<ItemValue<VCard>> values = storeService.getMultiple(subUids);
			indexStore.updates(values);
			monitor.progress(subUids.size(), "addressbook [" + container.uid + "] reindexing...");
		});

		monitor.progress(1, "addressbook [" + container.uid + "] reindexed");
		logger.info("addressbook {} reindexed", container.uid);
	}

	@Override
	public void reindexAll(IServerTaskMonitor monitor) throws Exception {
		if (!context.getSecurityContext().isDomainGlobal()) {
			throw new ServerFault("only admin0 can call this method ", ErrorCode.FORBIDDEN);
		}

		Set<String> all = getContainerUids();

		monitor.begin(all.size() + 1d, "begin addressbooks reindexation [" + all.size() + "]");
		ESearchActivator.resetIndex("contact");
		monitor.progress(1, "Index contact reseted");

		for (String uid : all) {
			IServerTaskMonitor subMonitor = monitor.subWork("addressbook [" + uid + "]", 1);
			try {
				reindex(uid, subMonitor);
			} catch (ServerFault sf) {
				logger.error("Failed to reindex AB {}: {}", uid, sf.getMessage());
				monitor.log("Failed to reindex AB " + uid);
			}

		}

	}

	@Override
	public void reindexDomain(String domainUid, IServerTaskMonitor monitor) throws Exception {
		if (!context.getSecurityContext().isDomainAdmin(domainUid)) {
			throw new ServerFault("only admin of " + domainUid + " can call this method ", ErrorCode.FORBIDDEN);
		}

		IContainers service = context.provider().instance(IContainers.class);
		List<ContainerDescriptor> containers = service.all(ContainerQuery.type(IAddressBookUids.TYPE));

		monitor.begin(containers.size(), "begin addressbooks reindexation [" + containers.size() + "]");
		for (ContainerDescriptor c : containers) {
			if (domainUid == null || domainUid.equals(c.domainUid)) {
				IServerTaskMonitor subMonitor = monitor.subWork("addressbook [" + c.uid + "]", 1);
				try {
					reindex(c.uid, subMonitor);
				} catch (ServerFault sf) {
					logger.error("Failed to reindex AB {}: {}", c.uid, sf.getMessage());
					monitor.log("Failed to reindex AB " + c.uid);
				}
			}
		}
	}

	@Override
	public Stream backup(String abUid, Long since) throws ServerFault {

		DataSource ds = DataSourceRouter.get(context, abUid);
		ContainerStore containerStore = new ContainerStore(context, ds, context.getSecurityContext());

		Container container;
		try {
			container = containerStore.get(abUid);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}

		if (container == null) {
			throw new ServerFault("addresbook " + abUid + " not found", ErrorCode.NOT_FOUND);
		}

		if (!context.getSecurityContext().isDomainAdmin(container.domainUid)) {
			throw new ServerFault("only admin of " + container.domainUid + " can call this method ",
					ErrorCode.FORBIDDEN);
		}

		VCardContainerStoreService storeService = new VCardContainerStoreService(context, ds,
				context.getSecurityContext(), container, new VCardStore(ds, container), new VCardIndexStore(
						ESearchActivator.getClient(), container, DataSourceRouter.location(context, container.uid)));

		ContainerChangeset<String> changeset = storeService.changeset(since, Long.MAX_VALUE);

		return backupStream(changeset, storeService);
	}

	private Stream backupStream(ContainerChangeset<String> changeset, final VCardContainerStoreService storeService) {

		final Iterator<String> deleted = changeset.deleted.iterator();
		final Iterator<String> created = changeset.created.iterator();
		final Iterator<String> updated = changeset.updated.iterator();
		GenericStream<ChangesetItem> stream = new GenericStream<IAddressBooksMgmt.ChangesetItem>() {

			protected StreamState<IAddressBooksMgmt.ChangesetItem> next() throws Exception {
				if (deleted.hasNext()) {
					String uid = deleted.next();
					ChangesetItem e = new IAddressBooksMgmt.ChangesetItem();
					e.item = new ItemValue<VCard>();
					e.item.uid = uid;
					return StreamState.data(e);
				} else if (created.hasNext()) {
					String uid = created.next();
					ItemValue<VCard> item = storeService.get(uid, null);
					ChangesetItem e = new IAddressBooksMgmt.ChangesetItem();
					e.item = item;
					return StreamState.data(e);
				} else if (updated.hasNext()) {
					String uid = updated.next();
					ItemValue<VCard> item = storeService.get(uid, null);
					ChangesetItem e = new IAddressBooksMgmt.ChangesetItem();
					e.item = item;
					return StreamState.data(e);
				} else {
					return StreamState.end();
				}

			}

			@Override
			protected Buffer serialize(ChangesetItem n) throws Exception {
				return Buffer.buffer(JsonUtils.asString(n));
			}
		};
		return VertxStream.stream(stream);

	}

	@Override
	public void restore(String abUid, Stream restoreStream, boolean resetBeforeRestore) throws ServerFault {

		DataSource ds = DataSourceRouter.get(context, abUid);
		ContainerStore containerStore = new ContainerStore(context, ds, context.getSecurityContext());

		Container container;
		try {
			container = containerStore.get(abUid);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}

		if (container == null) {
			throw new ServerFault("addresbook " + abUid + " not found", ErrorCode.NOT_FOUND);
		}

		if (!context.getSecurityContext().isDomainAdmin(container.domainUid)) {
			throw new ServerFault("only admin of " + container.domainUid + " can call this method ",
					ErrorCode.FORBIDDEN);
		}

		final VCardContainerStoreService storeService = new VCardContainerStoreService(context, ds,
				context.getSecurityContext(), container, new VCardStore(ds, container), new VCardIndexStore(
						ESearchActivator.getClient(), container, DataSourceRouter.location(context, container.uid)));

		if (resetBeforeRestore) {
			storeService.deleteAll();
		}

		ReadStream<Buffer> s = VertxStream.read(restoreStream);
		GenericJsonObjectWriteStream<ChangesetItem> stream = new GenericJsonObjectWriteStream<ChangesetItem>(
				IAddressBooksMgmt.ChangesetItem.class) {

			@Override
			protected void next(ChangesetItem value) throws Exception {
				if (value.item.value == null) {
					// delete
					storeService.delete(value.item.uid);
				} else {
					storeService.create(value.item.uid, value.item.displayName, value.item.value);

				}

			}
		};
		CompletableFuture<Void> v = new CompletableFuture<>();
		s.pipeTo(stream, ar -> {
			if (ar.succeeded()) {
				v.complete(null);
			} else {
				v.completeExceptionally(ar.cause());
			}
		});

		try {
			v.get(1, TimeUnit.MINUTES);
		} catch (Exception e) {
			throw new ServerFault(e);
		}

	}

	@Override
	public void delete(String uid) throws ServerFault {

		AddressBookDescriptor descriptor = getComplete(uid);

		if (descriptor == null) {
			throw new ServerFault("addressbook " + uid + " not found", ErrorCode.NOT_FOUND);
		}

		IDirectory dir = context.provider().instance(IDirectory.class, descriptor.domainUid);
		DirEntry ownerEntry = dir.findByEntryUid(descriptor.owner);
		if (ownerEntry == null) {
			logger.warn("Strange, owner {} not found in domain {}, continue delete..", descriptor.owner,
					descriptor.domainUid);
		} else {
			checkCanManageBook(descriptor, ownerEntry.kind);
			if (ownerEntry.kind == DirEntry.Kind.ADDRESSBOOK) {
				DirEntryHandlers.byKind(DirEntry.Kind.ADDRESSBOOK).delete(context, descriptor.domainUid, uid);
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

		VCardContainerStoreService storeService = new VCardContainerStoreService(context, ds,
				context.getSecurityContext(), container, new VCardStore(ds, container), new VCardIndexStore(
						ESearchActivator.getClient(), container, DataSourceRouter.location(context, container.uid)));

		storeService.prepareContainerDelete();
		context.su().provider().instance(IContainers.class).delete(uid);
	}

	@Override
	public AddressBookDescriptor getComplete(String uid) throws ServerFault {
		IContainers containers = context.provider().instance(IContainers.class);
		ContainerDescriptor cd = containers.getIfPresent(uid);
		if (cd == null) {
			return null;
		}

		if (cd.type.equals(IAddressBookUids.TYPE)) {
			AddressBookDescriptor ret = AddressBookDescriptor.create(cd.name, cd.owner, cd.domainUid, cd.settings);
			if (cd.owner.equals(uid)) {
				// domain addressbook
				DirEntry entry = context.su().provider().instance(IDirectory.class, cd.domainUid).findByEntryUid(uid);
				if (entry != null) {
					ret.orgUnitUid = entry.orgUnitUid;
				}
			}
			return ret;
		}

		logger.warn("trying to retrieve a domain addressbook descriptor but it's not an addressbook but a {}", cd.type);
		return null;
	}

	@Override
	public void create(String uid, AddressBookDescriptor descriptor, boolean isDefault) throws ServerFault {
		ItemValue<AddressBookDescriptor> item = ItemValue.create(uid, descriptor);
		create(item, isDefault);
	}

	private void create(ItemValue<AddressBookDescriptor> item, boolean isDefault) throws ServerFault {
		AddressBookDescriptor descriptor = item.value;
		String uid = item.uid;
		sanitizer.create(descriptor);
		validator.create(descriptor);

		IDirectory dir = context.provider().instance(IDirectory.class, descriptor.domainUid);
		DirEntry entry = dir.findByEntryUid(descriptor.owner);
		if (entry == null) {
			throw new ServerFault("owner " + descriptor.owner + " not found in domain " + descriptor.domainUid);
		}

		if (entry.kind == DirEntry.Kind.DOMAIN) {
			checkCanManageBook(descriptor, DirEntry.Kind.ADDRESSBOOK);
			checkDomainAbDoesNotExist(descriptor, dir);

			ItemValue<DirEntry> dirEntryItem = ItemValue.create(item.item(), asDirEntry(uid, descriptor));
			DirEntryHandlers.byKind(DirEntry.Kind.ADDRESSBOOK).create(context, descriptor.domainUid, dirEntryItem);
			descriptor.owner = uid;
			// transmute owner
		} else {
			checkCanManageBook(descriptor, entry.kind);
		}
		if (descriptor.expectedId != null) {
			String hierUid = ContainerHierarchyNode.uidFor(uid, IAddressBookUids.TYPE, descriptor.domainUid);
			HierarchyIdsHints.putHint(hierUid, descriptor.expectedId);
		}

		ContainerDescriptor abContainerDescriptor = ContainerDescriptor.create(uid, descriptor.name, descriptor.owner,
				IAddressBookUids.TYPE, descriptor.domainUid, isDefault);

		abContainerDescriptor.readOnly = Boolean.parseBoolean(descriptor.settings.getOrDefault("readonly", "false"));

		IContainers containers = context.su().provider().instance(IContainers.class);
		containers.create(abContainerDescriptor.uid, abContainerDescriptor);

		if (!descriptor.settings.isEmpty()) {
			IContainerManagement containerManagement = context.provider().instance(IContainerManagement.class, uid);
			containerManagement.setSettings(descriptor.settings);
		}

		DataSource ds = DataSourceRouter.get(context, abContainerDescriptor.uid);
		ContainerStore cs = new ContainerStore(null, ds, SecurityContext.SYSTEM);
		Container container = null;
		try {
			container = cs.get(abContainerDescriptor.uid);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}

		ContainerSyncStore syncStore = new ContainerSyncStore(ds, container);
		syncStore.initSync();

	}

	private void checkDomainAbDoesNotExist(AddressBookDescriptor descriptor, IDirectory dir) {
		DirEntryQuery query = DirEntryQuery.filterName(descriptor.name);
		query.kindsFilter = Arrays.asList(Kind.ADDRESSBOOK);
		query.systemFilter = false;

		if (dir.search(query).total > 0) {
			throw new ServerFault("addressbook " + descriptor.name + " already exists", ErrorCode.ALREADY_EXISTS);
		}
	}

	@Override
	public void update(String uid, AddressBookDescriptor descriptor) throws ServerFault {
		ItemValue<AddressBookDescriptor> item = ItemValue.create(uid, descriptor);
		update(item);
	}

	private void update(ItemValue<AddressBookDescriptor> item) throws ServerFault {
		String uid = item.uid;
		AddressBookDescriptor descriptor = item.value;
		AddressBookDescriptor old = getComplete(uid);

		if (old == null) {
			throw new ServerFault("addressbook " + uid + " not found", ErrorCode.NOT_FOUND);
		}

		sanitizer.update(old, descriptor);
		validator.update(old, descriptor);

		IDirectory dir = context.provider().instance(IDirectory.class, old.domainUid);
		DirEntry entry = dir.findByEntryUid(old.owner);
		if (entry == null) {
			throw new ServerFault("owner " + old.owner + " not found in domain " + old.domainUid);
		}

		if (!old.owner.equals(descriptor.owner)) {
			throw new ServerFault("trying to change addressbook owner", ErrorCode.INVALID_PARAMETER);
		}

		checkCanManageBook(old, entry.kind);
		if (entry.kind == DirEntry.Kind.ADDRESSBOOK) {
			ItemValue<DirEntry> dirEntryItem = ItemValue.create(item.item(), asDirEntry(uid, descriptor));
			DirEntryHandlers.byKind(DirEntry.Kind.ADDRESSBOOK).update(context, descriptor.domainUid, dirEntryItem);
		}

		ContainerModifiableDescriptor cmd = new ContainerModifiableDescriptor();
		cmd.name = descriptor.name;
		context.su().provider().instance(IContainers.class).update(uid, cmd);

	}

	private void checkCanManageBook(AddressBookDescriptor descriptor, Kind ownerKind) throws ServerFault {

		switch (ownerKind) {
		case USER:
			if (!rbacManager.forDomain(descriptor.domainUid).forEntry(descriptor.owner).can("Manage")
					&& !(context.getSecurityContext().getSubject().equals(descriptor.owner)
							&& context.getSecurityContext().getContainerUid().equals(descriptor.domainUid))) {
				throw new ServerFault("cannot manage this addressbook", ErrorCode.PERMISSION_DENIED);
			}
			break;
		case ADDRESSBOOK:
			rbacManager.forDomain(descriptor.domainUid).forOrgUnit(descriptor.orgUnitUid)
					.check(BasicRoles.ROLE_MANAGE_DOMAIN_AB);
			break;
		default:
			throw new ServerFault("Invalid owner " + ownerKind, ErrorCode.INVALID_PARAMETER);
		}

	}

	private DirEntry asDirEntry(String uid, AddressBookDescriptor descriptor) {
		DirEntry entry = DirEntry.create(descriptor.orgUnitUid, descriptor.domainUid + "/addressbooks/" + uid,
				DirEntry.Kind.ADDRESSBOOK, uid, descriptor.name, null, false, descriptor.system, false);
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
			List<Container> containers = cs.findByType(IAddressBookUids.TYPE);
			all.addAll(containers.stream().map(c -> c.uid).collect(Collectors.toList()));
		}

		ContainerStore cs = new ContainerStore(context, context.getDataSource(), context.getSecurityContext());
		List<Container> containers = cs.findByType(IAddressBookUids.TYPE);
		all.addAll(containers.stream().map(c -> c.uid).collect(Collectors.toList()));

		return all;
	}

	@Override
	public AddressBookDescriptor get(String uid) {
		return getComplete(uid);
	}

	@Override
	public void restore(ItemValue<AddressBookDescriptor> item, boolean isCreate) {
		if (isCreate) {
			create(item, false);
		} else {
			update(item);
		}
	}

}
