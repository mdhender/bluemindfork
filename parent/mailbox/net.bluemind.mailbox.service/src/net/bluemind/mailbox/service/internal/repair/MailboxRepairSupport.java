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
package net.bluemind.mailbox.service.internal.repair;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.event.Level;

import com.google.common.collect.Sets;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.rest.BmContext;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.MaintenanceOperation;
import net.bluemind.directory.service.IDirEntryRepairSupport;
import net.bluemind.directory.service.RepairTaskMonitor;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.service.internal.MailboxStoreService;

public class MailboxRepairSupport implements IDirEntryRepairSupport {
	public static class Factory implements IDirEntryRepairSupport.Factory {
		@Override
		public IDirEntryRepairSupport create(BmContext context) {
			return new MailboxRepairSupport(context);
		}
	}

	private List<Kind> supportedKinds = Arrays.asList(Kind.GROUP, Kind.MAILSHARE, Kind.RESOURCE, Kind.USER);
	private BmContext context;

	public MailboxRepairSupport(BmContext context) {
		this.context = context;
	}

	@Override
	public Set<MaintenanceOperation> availableOperations(Kind kind) {
		if (!supportedKinds.contains(kind)) {
			return Set.of();
		}

		ResourceBundle resourceBundle = ResourceBundle.getBundle("OSGI-INF/l10n/MailboxRepairSupport",
				Locale.of(context.getSecurityContext().getLang()));

		return Stream.of(MailboxMaintenanceOperation.DiagnosticReportCheckId.values()).map(id -> {
			MaintenanceOperation op = new MaintenanceOperation();
			op.identifier = id.name();
			String key = id.name() + ".description";
			String msg = resourceBundle.containsKey(key) ? resourceBundle.getString(key) : key;
			op.description = msg;
			return op;
		}).collect(Collectors.toSet());
	}

	@Override
	public Set<InternalMaintenanceOperation> ops(Kind kind) {
		if (!supportedKinds.contains(kind)) {
			return Set.of();
		}

		return Sets.newHashSet(new MailboxAclsContainerMaintenanceOperation(context),
				new MailboxExistsMaintenanceOperation(context), new MailboxIndexExistsMaintenanceOperation(context),
				new MailboxPostfixMapsMaintenanceOperation(context));
	}

	public abstract static class MailboxMaintenanceOperation extends InternalMaintenanceOperation {

		public enum DiagnosticReportCheckId {
			mailboxExists(true), mailboxIndexExists(true), mailboxAclsContainer(true), mailboxPostfixMaps(true),
			mailboxSubscription(true), mailboxDefaultFolders(true);

			private DiagnosticReportCheckId(boolean sortable) {
				this.sortable = sortable;
			}

			private final boolean sortable;
		}

		protected BmContext context;
		protected ItemValue<Mailbox> mailbox;

		public MailboxMaintenanceOperation(BmContext context, String identifier) {
			super(identifier, null, getBeforeOperation(identifier), 1);

			this.context = context;
		}

		public MailboxMaintenanceOperation(BmContext context, String identifier, String beforeOp, String afterOp) {
			super(identifier, beforeOp, afterOp, 1);

			this.context = context;
		}

		private static String getBeforeOperation(String identifier) {
			DiagnosticReportCheckId currentOp = DiagnosticReportCheckId.valueOf(identifier);
			if (!currentOp.sortable) {
				return null;
			}
			String previousOp = null;
			for (DiagnosticReportCheckId id : DiagnosticReportCheckId.values()) {
				if (id == currentOp) {
					break;
				}
				if (id.sortable) {
					previousOp = id.name();
				}
			}

			return previousOp;
		}

		@Override
		public void check(String domainUid, DirEntry entry, RepairTaskMonitor monitor) {
			monitor.begin(2, String.format("Check entry %s, kind %s", entry.entryUid, entry.kind.name()));

			if (!isEntrySupported(domainUid, entry, new RepairTaskMonitor(monitor.subWork(1), monitor.config))) {
				monitor.end();
				return;
			}

			checkMailbox(domainUid, new RepairTaskMonitor(monitor.subWork(1), monitor.config));

			monitor.end();
		}

		@Override
		public void repair(String domainUid, DirEntry entry, RepairTaskMonitor monitor) {
			monitor.begin(2, String.format("Repair entry %s, kind %s", entry.entryUid, entry.kind.name()));

			if (!isEntrySupported(domainUid, entry, new RepairTaskMonitor(monitor.subWork(1), monitor.config))) {
				monitor.end();
				return;
			}

			repairMailbox(domainUid, new RepairTaskMonitor(monitor.subWork(1), monitor.config));

			monitor.end();
		}

		protected boolean isEntrySupported(String domainUid, DirEntry entry, RepairTaskMonitor monitor) {
			monitor.begin(1, String.format("Check entry %s, kind %s exists and is supported", entry.entryUid,
					entry.kind.name()));

			ContainerStore containerStore = new ContainerStore(context, context.getDataSource(),
					context.getSecurityContext());
			Container container = null;

			try {
				container = containerStore.get(domainUid);
			} catch (SQLException e) {
				monitor.end(false, null, null);
				throw ServerFault.sqlFault(e);
			}

			if (container == null) {
				monitor.end(false, null, null);
				throw new ServerFault(String.format("Container %s not found", domainUid));
			}

			MailboxStoreService storeService = new MailboxStoreService(context.getDataSource(),
					context.getSecurityContext(), container);

			monitor.progress(1, String.format("Lookup mailbox %s", entry.entryUid));

			mailbox = storeService.get(entry.entryUid, null);
			boolean mailboxExists = mailbox != null && mailbox.value != null;
			if (!mailboxExists) {
				monitor.log(String.format("Mailbox %s not found in database", entry.entryUid), Level.WARN);
				monitor.notify("Mailbox {} not found in database", entry.entryUid);
			}

			monitor.end();
			return mailboxExists;
		}

		protected String mailboxToString(String domainUid) {
			return String.format("%s@%s (uid: %s)", (mailbox.value == null ? mailbox.uid : mailbox.value.name),
					domainUid, mailbox.uid);
		}

		protected abstract void checkMailbox(String domainUid, RepairTaskMonitor monitor);

		protected abstract void repairMailbox(String domainUid, RepairTaskMonitor monitor);
	}
}
