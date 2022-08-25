/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.directory.service.internal;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.rest.ServerSideServiceProvider.IServerSideServiceFactory;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.core.task.service.ITasksManager;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirEntryMaintenance;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.directory.api.MaintenanceOperation;
import net.bluemind.directory.api.RepairConfig;
import net.bluemind.directory.service.IDirEntryRepairSupport.InternalMaintenanceOperation;
import net.bluemind.directory.service.IInternalDirEntryMaintenance;
import net.bluemind.directory.service.RepairTaskMonitor;

public class DirEntryMaintenance implements IDirEntryMaintenance, IInternalDirEntryMaintenance {
	private static final Logger logger = LoggerFactory.getLogger(DirEntryMaintenance.class);

	private BmContext context;
	private String domainUid;
	private DirEntry entry;
	private DirEntryRepairSupports supports;

	public static class Factory implements IServerSideServiceFactory<IDirEntryMaintenance> {

		@Override
		public Class<IDirEntryMaintenance> factoryClass() {
			return IDirEntryMaintenance.class;
		}

		@Override
		public IDirEntryMaintenance instance(BmContext context, String... params) throws ServerFault {
			if (params == null || params.length < 2) {
				throw new ServerFault("wrong number of instance parameters");
			}
			String domainUid = params[0];
			String entryUid = params[1];

			DirEntry entry = context.su().provider().instance(IDirectory.class, domainUid).findByEntryUid(entryUid);

			if (entry == null) {
				throw new ServerFault("entry " + entryUid + "@" + domainUid + " not found", ErrorCode.NOT_FOUND);
			}

			// make maintenance task executable for domain admins
			if (context.getSecurityContext().isDomainAdmin(domainUid)) {
				context = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).getContext();
			}

			return new DirEntryMaintenance(context, domainUid, entry);
		}

	}

	public static class InternalFactory implements IServerSideServiceFactory<IInternalDirEntryMaintenance> {

		@Override
		public Class<IInternalDirEntryMaintenance> factoryClass() {
			return IInternalDirEntryMaintenance.class;
		}

		@Override
		public IInternalDirEntryMaintenance instance(BmContext context, String... params) throws ServerFault {
			if (params == null || params.length < 2) {
				throw new ServerFault("wrong number of instance parameters");
			}
			String domainUid = params[0];
			String entryUid = params[1];

			DirEntry entry = context.su().provider().instance(IDirectory.class, domainUid).findByEntryUid(entryUid);

			if (entry == null) {
				throw new ServerFault("entry " + entryUid + "@" + domainUid + " not found", ErrorCode.NOT_FOUND);
			}

			return new DirEntryMaintenance(context, domainUid, entry);
		}

	}

	public DirEntryMaintenance(BmContext context, String domainUid, DirEntry entry) {
		this.context = context;
		this.domainUid = domainUid;
		this.entry = entry;
		this.supports = new DirEntryRepairSupports(context);
	}

	@Override
	public List<MaintenanceOperation> getAvailableOperations() {
		Set<MaintenanceOperation> availableOperations = supports.availableOperations(entry.kind);
		return supports
				.ops(null, entry.kind).stream().sequential().map(op -> availableOperations.stream()
						.filter(o -> o.identifier.equals(op.identifier)).findAny().orElse(null))
				.collect(Collectors.toList());
	}

	@Override
	public TaskRef repair(RepairConfig config) {
		return context.provider().instance(ITasksManager.class).run("repair-" + entry.entryUid + "@" + domainUid, m -> {
			RepairTaskMonitor monitor = new RepairTaskMonitor(m, config);
			try {
				repair(config, monitor);
			} catch (Exception e) {
				monitor.log("Unknown error: " + e.getMessage());
				monitor.end(false, "", "");

				logger.error("Unknown error: " + e.getMessage(), e);
				return;
			}

			monitor.end(true, "", "");
		});
	}

	@Override
	public void repair(RepairConfig config, IServerTaskMonitor monitor) {

		RepairTaskMonitor taskMon = monitor instanceof RepairTaskMonitor ? (RepairTaskMonitor) monitor
				: new RepairTaskMonitor(monitor, config);

		List<InternalMaintenanceOperation> ops = supports.ops(config.opIdentifiers, entry.kind);
		Integer full = ops.stream().map(op -> op.cost).reduce(0, (u, i) -> u + i);
		monitor.begin(full, String.format("Repair %s@%s in %d steps", entry.entryUid, domainUid, ops.size()));

		AtomicInteger ai = new AtomicInteger(0);
		ops.forEach(op -> {
			monitor.log(
					String.format("[%d/%d] Starting operation %s", ai.incrementAndGet(), ops.size(), op.identifier));
			if (config.dry) {
				op.check(domainUid, entry, (RepairTaskMonitor) taskMon.subWork("[" + op.identifier + "]:", op.cost));
			} else {
				op.repair(domainUid, entry, (RepairTaskMonitor) taskMon.subWork("[" + op.identifier + "]:", op.cost));
			}
		});
	}

}
