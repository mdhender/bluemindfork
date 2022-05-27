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

import org.slf4j.event.Level;

import com.google.common.collect.Sets;

import net.bluemind.config.Token;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.MaintenanceOperation;
import net.bluemind.directory.service.IDirEntryRepairSupport;
import net.bluemind.directory.service.IDirEntryRepairSupport.InternalMaintenanceOperation;
import net.bluemind.directory.service.RepairTaskMonitor;
import net.bluemind.imap.Annotation;
import net.bluemind.imap.IMAPException;
import net.bluemind.imap.ListInfo;
import net.bluemind.imap.ListResult;
import net.bluemind.imap.StoreClient;
import net.bluemind.network.topology.Topology;

public class DefaultPartitionRepair extends InternalMaintenanceOperation {

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
	public void check(String domainUid, DirEntry entry, RepairTaskMonitor monitor) {
		run(false, domainUid, entry, monitor);
		monitor.end();
	}

	@Override
	public void repair(String domainUid, DirEntry entry, RepairTaskMonitor monitor) {
		run(true, domainUid, entry, monitor);
		monitor.end();
	}

	private void run(boolean repair, String domainUid, DirEntry entry, RepairTaskMonitor monitor) {
		monitor.log("Repairing " + entry + " as " + context.getSecurityContext().getSubject());
		Topology.get().nodes().stream().filter(srv -> srv.value.tags.contains("mail/imap")).forEach(backend -> {
			try (StoreClient sc = new StoreClient(backend.value.address(), 1143, "admin0", Token.admin0())) {
				boolean login = sc.login();
				if (login) {
					repairBackend(repair, domainUid, monitor, sc);
				}

			}

		});

	}

	private void repairBackend(boolean repair, String domainUid, RepairTaskMonitor monitor, StoreClient sc) {
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
				monitor.notify("Folder {} is on default partition", li.getName());
				if (repair) {
					repairFolder(monitor, sc, li);
				} else {
					monitor.log(li.getName() + " should be repaired.");
				}

			}
			monitor.progress(1, li.getName() + " processed.");
		}
	}

	private void repairFolder(IServerTaskMonitor monitor, StoreClient sc, ListInfo li) {
		try {
			monitor.log("Repairing " + li.getName() + "...");
			boolean result = sc.rename(li.getName(), li.getName());
			monitor.log(li.getName() + " repaired => " + result, Level.WARN);
		} catch (IMAPException ie) {
			monitor.log("Failed to repair " + li.getName() + ": " + ie.getMessage());
		}
	}

}
