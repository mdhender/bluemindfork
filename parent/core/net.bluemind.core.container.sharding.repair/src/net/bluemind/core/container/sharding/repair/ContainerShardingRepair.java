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
package net.bluemind.core.container.sharding.repair;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import com.github.benmanes.caffeine.cache.Cache;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.core.caches.registry.CacheRegistry;
import net.bluemind.core.container.api.IDataShardSupport;
import net.bluemind.core.container.api.IFlatHierarchyUids;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.DataSourceRouter;
import net.bluemind.core.container.sharding.Sharding;
import net.bluemind.core.jdbc.JdbcAbstractStore;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.MaintenanceOperation;
import net.bluemind.directory.service.IDirEntryRepairSupport;
import net.bluemind.directory.service.IInCoreDirectory;
import net.bluemind.directory.service.RepairTaskMonitor;
import net.bluemind.directory.xfer.ContainerToIDataShardSupport;
import net.bluemind.directory.xfer.ContainerXfer;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.network.topology.Topology;

public class ContainerShardingRepair implements IDirEntryRepairSupport {

	public static final String REPAIR_OP_ID = "containers.sharding.location";

	private final BmContext context;

	public static final MaintenanceOperation containerOp = MaintenanceOperation.create(
			ContainerShardingRepair.REPAIR_OP_ID,
			"Check and move data inserted on the wrong shard regarding to the direntry location");

	public static class Factory implements IDirEntryRepairSupport.Factory {
		@Override
		public IDirEntryRepairSupport create(BmContext context) {
			return new ContainerShardingRepair(context);
		}
	}

	public ContainerShardingRepair(BmContext context) {
		this.context = context;
	}

	private static class ContainerWithDataSource {
		private final DataSource dataSource;
		private final Container container;
		private final DataSource routerDataSource;
		private final BmContext context;

		private final ItemValue<Mailbox> mailbox;
		private final String domainUid;
		private final DirEntry dirEntry;

		public ContainerWithDataSource(BmContext context, DataSource dataSource, Container container, String domainUid,
				DirEntry dirEntry, ItemValue<Mailbox> mailbox) {
			this.context = context;
			this.dataSource = dataSource;
			this.container = container;
			this.dirEntry = dirEntry;
			this.domainUid = domainUid;
			this.mailbox = mailbox;
			this.routerDataSource = DataSourceRouter.get(context, container.uid);
		}

		public Container getContainer() {
			return container;
		}

		public String toString() {
			return "Container<type=" + container.type + " name=" + container.name + " uid=" + container.uid + ">@"
					+ dataSource;
		}

		public DataSource getDataSource() {
			return dataSource;
		}

		public String getLocation() {
			return context.dataSourceLocation(dataSource);
		}

		public String getRouterLocation() {
			return context.dataSourceLocation(routerDataSource);
		}

		public IDataShardSupport getDataShardService(String location) {
			return ContainerToIDataShardSupport.getService(ServerSideServiceProvider.getProvider(context), container,
					domainUid, dirEntry.entryUid, location, mailbox);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ContainerWithDataSource other = (ContainerWithDataSource) obj;
			Container otherContainer = other.getContainer();
			return container.equals(otherContainer) && dataSource.equals(other.getDataSource());
		}
	}

	private static class ContainerMaintenance extends InternalMaintenanceOperation {
		private final BmContext context;
		private final IServiceProvider sp;
		private final ContainerStore directoryContainerStore;

		private final List<String> nonTransferableData = Lists.newArrayList(//
				IMailReplicaUids.MAILBOX_RECORDS, // synced by replication
				IMailReplicaUids.REPLICATED_MBOXES, // synced by replication
				IMailReplicaUids.REPLICATED_CONVERSATIONS, // synced by replication
				IFlatHierarchyUids.TYPE // synced by replication
		);
		private String dirEntryLocation; // NOSONAR: used in lambda, and not final

		public ContainerMaintenance(BmContext context) {
			super(containerOp.identifier, IMailReplicaUids.REPAIR_SUBTREE_OP, null, 1);
			this.context = context;
			sp = ServerSideServiceProvider.getProvider(context);
			directoryContainerStore = new ContainerStore(context, context.getDataSource(),
					context.getSecurityContext());
		}

		public boolean isXferable(Container c) {
			return !nonTransferableData.contains(c.type);
		}

		public List<Consumer<Boolean>> getRepairActions(String domainUid, DirEntry dirEntry,
				RepairTaskMonitor monitor) {
			List<Consumer<Boolean>> ops = new ArrayList<>();
			boolean needFlushCacheGlobal = false;

			if (shouldSkip(dirEntry)) {
				return ops;
			}
			dirEntryLocation = dirEntry.dataLocation;
			ItemValue<Mailbox> mailbox = sp.instance(IMailboxes.class, domainUid).getComplete(dirEntry.entryUid);
			List<ContainerWithDataSource> shardedContainers = allShardedContainers(dirEntry, domainUid, mailbox,
					monitor);
			final Cache<String, Optional<String>> dsCache = DataSourceRouter.initContextCache(context);

			// Is the dirEntry on the wrong location ?
			// We can't xfer the mailbox easily, so consider that the
			// dirEntry should be where the mailbox is

			ops.add(dry -> {
				if (!Boolean.TRUE.equals(dry)) {
					Cache<Object, Object> rootCache = CacheRegistry.get().get("KnownRoots.validatedRoots");
					if (rootCache == null) {
						monitor.log("Unable to get rootsFakeCache, replication will probably fail...");
					} else {
						rootCache.invalidateAll();
					}
				}
			});

			if (mailbox != null && !dirEntry.dataLocation.equals(mailbox.value.dataLocation)) {
				ops.add(dry -> {
					monitor.log("{}mailbox is on {} but dirEntry on {}: moving dirEntry to mailbox location",
							Level.WARN, dry ? "(dry mode) " : "", humanDataLocation(mailbox.value.dataLocation),
							humanDataLocation(dirEntry.dataLocation));
					monitor.notify("{} mailbox is on {} but dirEntry on {}",
							humanDataLocation(mailbox.value.dataLocation), humanDataLocation(dirEntry.dataLocation));
					dirEntry.dataLocation = mailbox.value.dataLocation;
					if (Boolean.FALSE.equals(dry)) {
						sp.instance(IInCoreDirectory.class, domainUid).update(dirEntry.entryUid, dirEntry);
					}
				});
				// Update the dirEntryLocation outside the work queue so tests
				// will use the correct value
				dirEntryLocation = mailbox.value.dataLocation;
				needFlushCacheGlobal = true;
			}

			// Now, there are different cases to address, we need to proceed container by
			// container
			List<Container> containers = shardedContainers.stream().map(ContainerWithDataSource::getContainer)
					.distinct().collect(Collectors.toList());
			for (Container container : containers) {
				String containerLogId = String.format("<Container type=%s name=%s uid=%s>", container.type,
						container.name, container.uid);
				List<ContainerWithDataSource> containersWithDs = shardedContainers.stream()
						.filter(cwds -> container.equals(cwds.getContainer())).collect(Collectors.toList());
				// Firstly, check if there is a "correct" container
				Optional<ContainerWithDataSource> optCwdsCorrect = containersWithDs.stream()
						.filter(cwds -> dirEntryLocation.equals(cwds.getLocation())).findAny();

				if (optCwdsCorrect.isPresent()) {
					// Check if the dataSourceRouter is wrong. If so, fix it please
					ContainerWithDataSource cwdsCorrect = optCwdsCorrect.get();
					String routerLocation = cwdsCorrect.getRouterLocation();
					String containerLocation = cwdsCorrect.getLocation();
					if (!routerLocation.equals(containerLocation)) {
						needFlushCacheGlobal = true;
						ops.add(dry -> {
							monitor.log("{} router has the wrong location {} should be {}. Fixing the location",
									Level.WARN, containerLogId, routerLocation, containerLocation);
							monitor.notify("{} router has the wrong location {} should be {}. Fixing the location",
									Level.WARN, containerLogId, routerLocation, containerLocation);
							if (Boolean.FALSE.equals(dry)) {
								try {
									directoryContainerStore.createOrUpdateContainerLocation(container,
											containerLocation);
								} catch (SQLException e) {
									monitor.log("Unable to set {} location={}: {}", Level.WARN, containerLogId,
											containerLocation, e.getMessage());
								}
							}
						});
					}

					// Now we know the dataSourceRouter is correct, remove duplicates
					List<ContainerWithDataSource> toRemoveCwds = containersWithDs.stream()
							.filter(cwds -> !cwds.equals(cwdsCorrect)).collect(Collectors.toList());
					if (!toRemoveCwds.isEmpty()) {
						monitor.log("correct container {} found: need to remove {} containers on the wrong dataSource",
								Level.WARN, cwdsCorrect, toRemoveCwds.size());
						needFlushCacheGlobal = true;
						for (ContainerWithDataSource toRemove : toRemoveCwds) {
							ops.add(dry -> {
								monitor.log("{}{} will be removed", dry ? "(dry mode) " : "", toRemove);
								if (Boolean.FALSE.equals(dry)) {
									ContainerXfer.removeTargetContainers(context, toRemove.dataSource,
											Lists.newArrayList(toRemove.getContainer()));
									JdbcAbstractStore.doOrFail(() -> {
										directoryContainerStore.createOrUpdateContainerLocation(
												cwdsCorrect.getContainer(), dirEntryLocation);
										return null;
									});
								}
							});
						}
					}
				} else {
					monitor.log("{} is not on the correct dataSource: xfer required", Level.WARN, containerLogId);
					monitor.log("{} is not on the correct dataSource: xfer required", containerLogId);
					// we don't know what container to xfer, so,
					// choose wisely, for while the true Container will bring you life, the false
					// Container will take it from you.
					Optional<ContainerWithDataSource> cwdsToXfer = containersWithDs.stream().findFirst();
					needFlushCacheGlobal = true;
					cwdsToXfer.ifPresent(cwds -> {
						ops.add(dry -> {
							// Tell the dataSourceRouter where the container really is
							dsCache.put(container.uid, Optional.ofNullable(cwds.getLocation()));
							if (!isXferable(container)) {
								monitor.log("cannot xfer {} (container is not xferable: removing)", Level.WARN, cwds);
								monitor.notify("cannot xfer {} (container is not xferable: removing)", cwds);
								if (Boolean.FALSE.equals(dry)) {
									ContainerXfer.removeTargetContainers(context, cwds.dataSource,
											Lists.newArrayList(cwds.getContainer()));
								}
								return;
							}

							monitor.log("{}{} will be moved to {}", Level.WARN, dry ? "(dry mode) " : "", cwds,
									humanDataLocation(dirEntryLocation));
							if (Boolean.FALSE.equals(dry)) {
								ContainerXfer containerXfer = new ContainerXfer(//
										cwds.getDataSource(), // origin
										context.getMailboxDataSource(dirEntryLocation), // target
										context, dirEntry);
								try {
									IDataShardSupport xferService = cwds.getDataShardService(dirEntryLocation);
									if (xferService != null) {
										containerXfer.xferContainer(xferService, cwds.getContainer());
										// Yeah this is VERY uggly, but I really don't know how to fix those unknown
										// caches everywhere

									} else {
										monitor.log("unable to xfer container {}: xferService not available",
												Level.WARN, cwds);
										monitor.notify("unable to xfer container {}: xferService not available", cwds);
									}
								} catch (SQLException e) {
									monitor.log("{} xferContainer failed: {}", Level.WARN, cwds, e.getMessage());
								}
								containerXfer.executeCleanups(LoggerFactory.getLogger(getClass()));
							}
						});
					});
				}
			}

			ops.add(dry -> {
				DataSourceRouter.removeContextCaches(context);
				if (!Boolean.TRUE.equals(dry)) {
					/*
					 * This cache can be problematic if the subtree already existed previously on
					 * this server
					 */
					Optional.ofNullable(CacheRegistry.get().get("KnownRoots.validatedRoots"))
							.ifPresent(Cache::invalidateAll);
				}
			});
			if (needFlushCacheGlobal) {
				ops.add(dry -> {
					if (Boolean.FALSE.equals(dry)) {
						CacheRegistry.get().invalidateAll();
					}
				});
			}

			return ops;
		}

		@Override
		public void check(String domainUid, DirEntry dirEntry, RepairTaskMonitor monitor) {
			getRepairActions(domainUid, dirEntry, monitor).forEach(op -> op.accept(true));
			monitor.end();
		}

		@Override
		public void repair(String domainUid, DirEntry dirEntry, RepairTaskMonitor monitor) {
			getRepairActions(domainUid, dirEntry, monitor).forEach(op -> op.accept(false));
			monitor.end();
		}

		private List<ContainerWithDataSource> allShardedContainers(DirEntry dirEntry, String domainUid,
				ItemValue<Mailbox> mailbox, RepairTaskMonitor monitor) {
			List<ContainerWithDataSource> containers = new ArrayList<>();
			Set<String> shardedContainerTypes = Sharding.containerTypes();
			for (DataSource ds : context.getAllMailboxDataSource()) {
				ContainerStore containerStore = new ContainerStore(context, ds, context.getSecurityContext());
				try {
					containers.addAll(containerStore.findByTypeAndOwner(null, dirEntry.entryUid).stream()
							.filter(c -> shardedContainerTypes.contains(c.type))
							.map(c -> new ContainerWithDataSource(context, ds, c, domainUid, dirEntry, mailbox))
							.collect(Collectors.toList()));
				} catch (SQLException e) {
					monitor.log("Unable to retrieve containers by owner " + dirEntry.entryUid + ": " + e.getMessage(),
							Level.WARN);
					monitor.notify(
							"Unable to retrieve containers by owner " + dirEntry.entryUid + ": " + e.getMessage());
				}
			}
			return containers;
		}

		public String humanDataLocation(String location) {
			if (location == null) {
				return "null";
			}
			Optional<String> ip = Topology.get().nodes().stream().filter(n -> location.equals(n.uid))
					.map(n -> n.value.ip).findFirst();
			return location + ip.map(x -> " (" + x + ")").orElse("");
		}

		public boolean shouldSkip(DirEntry entry) {
			return entry.system || entry.hidden;
		}
	}

	@Override
	public Set<MaintenanceOperation> availableOperations(Kind kind) {
		if (supportedKind(kind)) {
			return ImmutableSet.of(containerOp);
		} else {
			return Collections.emptySet();
		}
	}

	@Override
	public Set<InternalMaintenanceOperation> ops(Kind kind) {
		if (supportedKind(kind)) {
			return ImmutableSet.of(new ContainerMaintenance(context));
		} else {
			return Collections.emptySet();
		}
	}

	private boolean supportedKind(Kind kind) {
		return kind == Kind.USER || kind == Kind.RESOURCE || kind == Kind.MAILSHARE || kind == Kind.GROUP;
	}
}
