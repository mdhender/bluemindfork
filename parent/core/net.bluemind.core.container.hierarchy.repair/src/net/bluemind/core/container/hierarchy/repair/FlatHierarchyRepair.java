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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.core.container.hierarchy.repair;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;

import net.bluemind.core.api.report.DiagnosticReport;
import net.bluemind.core.container.api.ContainerHierarchyNode;
import net.bluemind.core.container.api.ContainerQuery;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.api.IFlatHierarchyUids;
import net.bluemind.core.container.api.internal.IInternalContainersFlatHierarchy;
import net.bluemind.core.container.api.internal.IInternalContainersFlatHierarchyMgmt;
import net.bluemind.core.container.model.BaseContainerDescriptor;
import net.bluemind.core.container.model.ItemFlag;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.persistance.DataSourceRouter;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.MaintenanceOperation;
import net.bluemind.directory.service.IDirEntryRepairSupport;

public class FlatHierarchyRepair implements IDirEntryRepairSupport {

	public static class Factory implements IDirEntryRepairSupport.Factory {
		@Override
		public IDirEntryRepairSupport create(BmContext context) {
			return new FlatHierarchyRepair(context);
		}
	}

	private static final Logger logger = LoggerFactory.getLogger(FlatHierarchyRepair.class);
	public static final MaintenanceOperation flatHierOp = MaintenanceOperation.create(IFlatHierarchyUids.REPAIR_OP_ID,
			"Check the hierarchy of owned containers");

	private static class FlagHierMaintenance extends InternalMaintenanceOperation {
		private final BmContext context;

		public FlagHierMaintenance(BmContext ctx) {
			super(flatHierOp.identifier, null, null, 1);
			this.context = ctx;
		}

		@Override
		public void check(String domainUid, DirEntry entry, DiagnosticReport report, IServerTaskMonitor monitor) {
			if (entry.system) {
				return;
			}
			logger.info("Check flat hier for {} as {}", entry, context);

		}

		private static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
			Set<Object> seen = ConcurrentHashMap.newKeySet();
			return t -> seen.add(keyExtractor.apply(t));
		}

		@Override
		public void repair(String domainUid, DirEntry entry, DiagnosticReport report, IServerTaskMonitor monitor) {
			if (entry.system) {
				return;
			}
			logger.info("Repair flat hier for {} as {}", entry, context);
			IInternalContainersFlatHierarchyMgmt mgmtApi = context.provider()
					.instance(IInternalContainersFlatHierarchyMgmt.class, domainUid, entry.entryUid);
			mgmtApi.init();
			IInternalContainersFlatHierarchy hierApi = context.provider()
					.instance(IInternalContainersFlatHierarchy.class, domainUid, entry.entryUid);
			IContainers contApi = context.provider().instance(IContainers.class);
			List<BaseContainerDescriptor> ownedContainers = contApi
					.allLight(ContainerQuery.ownerAndType(entry.entryUid, null)).stream()
					// as we did not clear the directory db, all databases have
					// a copy of the
					// containers...
					.filter(distinctByKey(c -> c.uid)) //
					.filter(c -> entry.dataLocation == null
							|| entry.dataLocation.equals(DataSourceRouter.location(context, c.uid)))
					.collect(Collectors.toList());

			IInternalContainersFlatHierarchy adminHierApi = ServerSideServiceProvider
					.getProvider(SecurityContext.SYSTEM)
					.instance(IInternalContainersFlatHierarchy.class, domainUid, entry.entryUid);
			List<ItemValue<ContainerHierarchyNode>> knownNodes = adminHierApi.list();

			monitor.begin(ownedContainers.size(), "Repairing hierarchy with " + ownedContainers.size() + " containers ("
					+ knownNodes.size() + " known nodes)");
			Set<String> nodeContUids = knownNodes.stream().map(n -> n.value.containerUid).collect(Collectors.toSet());
			Set<String> toRemoveNodes = new HashSet<>(nodeContUids);
			for (BaseContainerDescriptor c : ownedContainers) {
				String op = "skip";
				if (IFlatHierarchyUids.TYPE.equals(c.type)) {
					// skip
				} else if (nodeContUids.contains(c.uid)) {
					ContainerHierarchyNode value = ContainerHierarchyNode.of(c);
					ItemValue<ContainerHierarchyNode> current = hierApi
							.getComplete(ContainerHierarchyNode.uidFor(c.uid, c.type, domainUid));
					value.deleted = current.flags.contains(ItemFlag.Deleted);
					hierApi.update(ContainerHierarchyNode.uidFor(c.uid, c.type, domainUid), value);
					op = "update";
				} else {
					hierApi.create(ContainerHierarchyNode.uidFor(c.uid, c.type, domainUid),
							ContainerHierarchyNode.of(c));
					op = "create";
				}
				toRemoveNodes.remove(c.uid);
				monitor.progress(1, c.uid + " (" + c.type + ") repaired (action: " + op + ").");
			}
			knownNodes.stream().filter(n -> toRemoveNodes.contains(n.value.containerUid)).forEach(n -> {
				monitor.log("Removing " + n.uid + " from hierarchy.");
				hierApi.delete(n.uid);
			});

		}
	}

	private final BmContext context;

	public FlatHierarchyRepair(BmContext context) {
		this.context = context;
	}

	@Override
	public Set<MaintenanceOperation> availableOperations(Kind kind) {
		if (kind == Kind.USER || kind == Kind.RESOURCE || kind == Kind.MAILSHARE || kind == Kind.GROUP) {
			return ImmutableSet.of(flatHierOp);
		} else {
			return Collections.emptySet();
		}
	}

	@Override
	public Set<InternalMaintenanceOperation> ops(Kind kind) {
		if (kind == Kind.USER || kind == Kind.RESOURCE || kind == Kind.MAILSHARE || kind == Kind.GROUP) {
			return ImmutableSet.of(new FlagHierMaintenance(context));
		} else {
			return Collections.emptySet();
		}
	}

}
