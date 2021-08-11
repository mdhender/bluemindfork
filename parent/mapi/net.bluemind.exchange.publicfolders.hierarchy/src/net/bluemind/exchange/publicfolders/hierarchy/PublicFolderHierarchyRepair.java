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
package net.bluemind.exchange.publicfolders.hierarchy;

import java.util.Collections;
import java.util.LinkedList;
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
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.MaintenanceOperation;
import net.bluemind.directory.service.IDirEntryRepairSupport;
import net.bluemind.exchange.publicfolders.common.PublicFolders;

public class PublicFolderHierarchyRepair implements IDirEntryRepairSupport {

	public static class Factory implements IDirEntryRepairSupport.Factory {
		@Override
		public IDirEntryRepairSupport create(BmContext context) {
			return new PublicFolderHierarchyRepair(context);
		}
	}

	private static final Logger logger = LoggerFactory.getLogger(PublicFolderHierarchyRepair.class);
	public static final MaintenanceOperation pfFlatHierOp = MaintenanceOperation
			.create(IFlatHierarchyUids.REPAIR_PF_OP_ID, "Check public folders hierarchy");

	private static class PFRootMaintenance extends InternalMaintenanceOperation {
		private final BmContext context;

		public PFRootMaintenance(BmContext ctx) {
			super(pfFlatHierOp.identifier, null, null, 1);
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
			// domain dirEntry is system=true
			if (entry.system && entry.kind != Kind.DOMAIN) {
				return;
			}

			logger.info("Repair PF flat hier for {} as {}", entry, context);
			String domainPublicFoldersUid = PublicFolders.mailboxGuid(domainUid);
			IInternalContainersFlatHierarchyMgmt mgmtApi = context.provider()
					.instance(IInternalContainersFlatHierarchyMgmt.class, domainUid, domainPublicFoldersUid);
			mgmtApi.init();
			// bm-cli runs on the root DirEntry before processing anything else
			if (entry.kind == Kind.DOMAIN) {
				monitor.log("RESET public folders hierarchy " + domainPublicFoldersUid);
				mgmtApi.delete();
				// we re-init to ensure we have a clean hierarchy on domains without mailshares
				mgmtApi.init();
				return;
			}

			IInternalContainersFlatHierarchy hierApi = context.provider()
					.instance(IInternalContainersFlatHierarchy.class, domainUid, domainPublicFoldersUid);
			IContainers contApi = context.provider().instance(IContainers.class);
			List<BaseContainerDescriptor> ownedContainers = contApi
					.allLight(ContainerQuery.ownerAndType(entry.entryUid, null)).stream()
					// as we did not clear the directory db, all databases have a copy of the
					// containers...
					.filter(distinctByKey(c -> c.uid)) //
					.collect(Collectors.toList());

			List<BaseContainerDescriptor> selfOwned = contApi
					.allLight(ContainerQuery.ownerAndType(domainPublicFoldersUid, null)).stream()
					// as we did not clear the directory db, all databases have a copy of the
					// containers...
					.filter(distinctByKey(c -> c.uid)) //
					.collect(Collectors.toList());
			ownedContainers = new LinkedList<BaseContainerDescriptor>(ownedContainers);
			ownedContainers.addAll(selfOwned);

			List<ItemValue<ContainerHierarchyNode>> knownNodes = hierApi.list();
			monitor.begin(ownedContainers.size(), "Repairing PF hierarchy " + domainPublicFoldersUid + " with "
					+ ownedContainers.size() + " containers (" + knownNodes.size() + " known nodes)");
			Set<String> nodeContUids = knownNodes.stream().map(n -> n.value.containerUid).collect(Collectors.toSet());
			for (BaseContainerDescriptor c : ownedContainers) {
				if (IFlatHierarchyUids.TYPE.equals(c.type)) {
					// skip
				} else if (nodeContUids.contains(c.uid)) {
					hierApi.update(ContainerHierarchyNode.uidFor(c.uid, c.type, domainUid),
							ContainerHierarchyNode.of(c));
				} else {
					hierApi.create(ContainerHierarchyNode.uidFor(c.uid, c.type, domainUid),
							ContainerHierarchyNode.of(c));
				}
				// we don't remove from here as we repair only one owner at a time
				monitor.progress(1, c.uid + " (" + c.type + ") repaired.");
			}

		}
	}

	private final BmContext context;

	public PublicFolderHierarchyRepair(BmContext context) {
		this.context = context;
	}

	@Override
	public Set<MaintenanceOperation> availableOperations(Kind kind) {
		if (kind == Kind.CALENDAR || kind == Kind.ADDRESSBOOK || kind == Kind.DOMAIN) {
			return ImmutableSet.of(pfFlatHierOp);
		} else {
			return Collections.emptySet();
		}
	}

	@Override
	public Set<InternalMaintenanceOperation> ops(Kind kind) {
		if (kind == Kind.CALENDAR || kind == Kind.ADDRESSBOOK || kind == Kind.DOMAIN) {
			return ImmutableSet.of(new PFRootMaintenance(context));
		} else {
			return Collections.emptySet();
		}
	}

}
