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
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.core.container.subscriptions.repair;

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.api.report.DiagnosticReport;
import net.bluemind.core.container.api.ContainerSubscription;
import net.bluemind.core.container.api.ContainerSubscriptionModel;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.api.IOwnerSubscriptionUids;
import net.bluemind.core.container.api.internal.IInternalOwnerSubscriptions;
import net.bluemind.core.container.api.internal.IInternalOwnerSubscriptionsMgmt;
import net.bluemind.core.container.model.BaseContainerDescriptor;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.DataSourceRouter;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.MaintenanceOperation;
import net.bluemind.directory.service.IDirEntryRepairSupport;
import net.bluemind.user.persistence.OneUserSubscriptionStore;

public class ShardedSubscriptionRepair implements IDirEntryRepairSupport {
	public static class Factory implements IDirEntryRepairSupport.Factory {
		@Override
		public IDirEntryRepairSupport create(BmContext context) {
			return new ShardedSubscriptionRepair(context);
		}
	}

	private static final Logger logger = LoggerFactory.getLogger(ShardedSubscriptionRepair.class);
	public static final MaintenanceOperation ownerSubs = MaintenanceOperation
			.create(IOwnerSubscriptionUids.REPAIR_OP_ID, "Sharded Subscriptions");

	private static class OwnerSubsMaintenance extends InternalMaintenanceOperation {

		private final BmContext context;

		public OwnerSubsMaintenance(BmContext ctx) {
			super(ownerSubs.identifier, null, null, 1);
			this.context = ctx;
		}

		@Override
		public void check(String domainUid, DirEntry entry, DiagnosticReport report, IServerTaskMonitor monitor) {
			logger.info("Check subs for {} as {}", entry, context);
		}

		@Override
		public void repair(String domainUid, DirEntry entry, DiagnosticReport report, IServerTaskMonitor monitor) {
			logger.info("Repair subs for {} as {}", entry, context);
			if (entry.system) {
				return;
			}
			IInternalOwnerSubscriptionsMgmt mgmtApi = context.provider().instance(IInternalOwnerSubscriptionsMgmt.class,
					domainUid, entry.entryUid);
			mgmtApi.init();
			IInternalOwnerSubscriptions subsApi = context.provider().instance(IInternalOwnerSubscriptions.class,
					domainUid, entry.entryUid);

			DataSource domLoc = DataSourceRouter.get(context, domainUid);
			ContainerStore domContStore = new ContainerStore(context, domLoc, context.getSecurityContext());
			Container domainContainer;
			try {
				domainContainer = domContStore.get(domainUid);
				OneUserSubscriptionStore store = new OneUserSubscriptionStore(context.getSecurityContext(), domLoc,
						domainContainer, entry.entryUid);
				List<ContainerSubscription> allSubs = store.listSubscriptions(null);

				Set<String> allKnownUids = subsApi.list().stream().map(iv -> iv.uid).collect(Collectors.toSet());
				Set<String> refreshed = new HashSet<>();
				Map<String, ContainerSubscription> byUid = allSubs.stream()
						.collect(Collectors.toMap(cs -> cs.containerUid, cs -> cs));
				IContainers descriptorsApi = context.su().provider().instance(IContainers.class);
				List<BaseContainerDescriptor> descriptorsFromShards = descriptorsApi
						.getContainersLight(allSubs.stream().map(cs -> cs.containerUid).collect(Collectors.toList()));
				for (BaseContainerDescriptor cd : descriptorsFromShards) {
					ContainerSubscriptionModel model = ContainerSubscriptionModel.create(cd,
							byUid.get(cd.uid).offlineSync);
					String subUid = IOwnerSubscriptionUids.subscriptionUid(cd.uid, entry.entryUid);
					if (allKnownUids.contains(subUid)) {
						subsApi.update(subUid, model);
						refreshed.add(subUid);
					} else {
						subsApi.create(subUid, model);
					}
				}
				Set<String> toClean = new HashSet<>(allKnownUids);
				toClean.removeAll(refreshed);
				for (String sub : toClean) {
					subsApi.delete(sub);
				}

			} catch (SQLException e) {
				throw ServerFault.sqlFault(e);
			}

		}

	}

	private final BmContext context;

	public ShardedSubscriptionRepair(BmContext context) {
		this.context = context;
	}

	@Override
	public Set<MaintenanceOperation> availableOperations(Kind kind) {
		if (kind != Kind.USER) {
			return Collections.emptySet();
		}
		return ImmutableSet.of(ownerSubs);
	}

	@Override
	public Set<InternalMaintenanceOperation> ops(Kind kind) {
		if (kind != Kind.USER) {
			return Collections.emptySet();
		}

		return ImmutableSet.of(new OwnerSubsMaintenance(context));
	}

}
