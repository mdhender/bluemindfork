/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2019
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
package net.bluemind.backend.mail.replica.service.internal.repair;

import java.util.Collections;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.config.Token;
import net.bluemind.core.api.report.DiagnosticReport;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.MaintenanceOperation;
import net.bluemind.directory.service.IDirEntryRepairSupport;
import net.bluemind.directory.service.IDirEntryRepairSupport.InternalMaintenanceOperation;
import net.bluemind.imap.Annotation;
import net.bluemind.imap.IMAPException;
import net.bluemind.imap.ListInfo;
import net.bluemind.imap.ListResult;
import net.bluemind.imap.StoreClient;
import net.bluemind.network.topology.Topology;
import net.bluemind.server.api.Server;

public class DefaultPartitionRepair extends InternalMaintenanceOperation {

	private static final Logger logger = LoggerFactory.getLogger(DefaultPartitionRepair.class);

	private static final String ID = "default.partition";
	private static final MaintenanceOperation op = MaintenanceOperation.create(ID,
			"Moves mailboxes in default partition the right place");

	public static class RepairFactory implements IDirEntryRepairSupport.Factory {
		@Override
		public IDirEntryRepairSupport create(BmContext context) {
			return new IDirEntryRepairSupport() {

				@Override
				public Set<MaintenanceOperation> availableOperations(Kind kind) {
					if (kind == Kind.DOMAIN) {
						return Sets.newHashSet(op);
					} else {
						return Collections.emptySet();
					}
				}

				@Override
				public Set<InternalMaintenanceOperation> ops(Kind kind) {
					if (kind == Kind.DOMAIN) {
						return Sets.newHashSet(new DefaultPartitionRepair(context));
					} else {
						return Collections.emptySet();
					}
				}

			};
		}
	}

	private final BmContext context;

	public DefaultPartitionRepair(BmContext ctx) {
		super(ID, null, null, 1);
		this.context = ctx;
	}

	@Override
	public void check(String domainUid, DirEntry entry, DiagnosticReport report, IServerTaskMonitor monitor) {
		run(false, domainUid, entry, report, monitor);
	}

	@Override
	public void repair(String domainUid, DirEntry entry, DiagnosticReport report, IServerTaskMonitor monitor) {
		run(true, domainUid, entry, report, monitor);
	}

	private void run(boolean repair, String domainUid, DirEntry entry, DiagnosticReport report,
			IServerTaskMonitor monitor) {
		monitor.log("Repairing " + entry + " as " + context.getSecurityContext().getSubject());
		Topology.get().nodes().stream().filter(srv -> srv.value.tags.contains("mail/imap")).forEach(backend -> {
			try (StoreClient sc = new StoreClient(backend.value.address(), 1143, "admin0", Token.admin0())) {
				boolean login = sc.login();
				if (login) {
					repairBackend(repair, domainUid, report, monitor, backend, sc);
				}

			}

		});

	}

	private void repairBackend(boolean repair, String domainUid, DiagnosticReport report, IServerTaskMonitor monitor,
			ItemValue<Server> backend, StoreClient sc) {
		CyrusPartition right = CyrusPartition.forServerAndDomain(backend, domainUid);
		ListResult allMailboxes = sc.listAllDomain(domainUid);
		monitor.begin(allMailboxes.size(), "Working on " + allMailboxes.size() + " mailbox(es)");
		for (ListInfo li : allMailboxes) {
			Annotation annots = sc.getAnnotation(li.getName(), "/vendor/cmu/cyrus-imapd/partition")
					.get("/vendor/cmu/cyrus-imapd/partition");
			if (annots == null) {
				monitor.log("Skip mailbox without annotation " + li.getName());
				continue;
			}
			String part = annots.valueShared;
			if ("default".equals(part)) {
				if (repair) {
					repairFolder(report, monitor, sc, right, li);
				} else {
					monitor.log(li.getName() + " should be repaired.");
					report.warn(ID, li.getName() + " should be repaired.");
				}

			}
			monitor.progress(1, li.getName() + " processed.");
		}
	}

	private void repairFolder(DiagnosticReport report, IServerTaskMonitor monitor, StoreClient sc, CyrusPartition right,
			ListInfo li) {
		try {
			monitor.log("Repairing " + li.getName() + "...");
			boolean result = sc.renameMailbox(li.getName(), li.getName(), right.name);
			monitor.log(li.getName() + " repaired => " + result);
			if (result) {
				report.ok(ID, li.getName() + " repaired.");
			}
		} catch (IMAPException ie) {
			monitor.log("Failed to repair " + li.getName() + ": " + ie.getMessage());
			logger.error(ie.getMessage(), ie);
		}
	}

}
