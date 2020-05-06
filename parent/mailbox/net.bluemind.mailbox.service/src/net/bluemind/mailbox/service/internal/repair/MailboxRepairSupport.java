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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.api.report.DiagnosticReport;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.MaintenanceOperation;
import net.bluemind.directory.service.IDirEntryRepairSupport;
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
			return ImmutableSet.of();
		}

		ResourceBundle resourceBundle = ResourceBundle.getBundle("OSGI-INF/l10n/MailboxRepairSupport",
				new Locale(context.getSecurityContext().getLang()));

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
			return ImmutableSet.of();
		}

		return new HashSet<>(Arrays.asList(new MailboxAclsContainerMaintenanceOperation(context),
				new MailboxFilesystemMaintenanceOperation(context), new MailboxAclsMaintenanceOperation(context),
				new MailboxAutoSubscriptionsMaintenanceOperation(context),
				new MailboxExistsMaintenanceOperation(context), new MailboxFiltersMaintenanceOperation(context),
				new MailboxImapHierarchyMaintenanceOperation(context), new MailboxQuotaMaintenanceOperation(context),
				new MailboxIndexExistsMaintenanceOperation(context),
				new MailboxPostfixMapsMaintenanceOperation(context),
				new MailboxHsmMigrationMaintenanceOperation(context),
				new MailboxDefaultFoldersMaintenanceOperation(context)));
	}

	public abstract static class MailboxMaintenanceOperation extends InternalMaintenanceOperation {
		private static final Logger logger = LoggerFactory.getLogger(MailboxMaintenanceOperation.class);

		public enum DiagnosticReportCheckId {
			mailboxExists(true), mailboxIndexExists(true), mailboxAclsContainer(true), mailboxAcls(true),
			mailboxHsm(false), mailboxFilesystem(true), mailboxImapHierarchy(true), mailboxQuota(true),
			mailboxFilters(true), mailboxPostfixMaps(true), mailboxSubscription(true), mailboxDefaultFolders(true);

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
		public void check(String domainUid, DirEntry entry, DiagnosticReport report, IServerTaskMonitor monitor) {
			monitor.begin(2, String.format("Check entry %s, kind %s", entry.entryUid, entry.kind.name()));

			if (!isEntrySupported(domainUid, entry, report, monitor.subWork(1))) {
				monitor.end(true, "", "");
				return;
			}

			checkMailbox(domainUid, report, monitor.subWork(1));

			monitor.end(true, null, null);
		}

		@Override
		public void repair(String domainUid, DirEntry entry, DiagnosticReport report, IServerTaskMonitor monitor) {
			monitor.begin(2, String.format("Repair entry %s, kind %s", entry.entryUid, entry.kind.name()));

			if (!isEntrySupported(domainUid, entry, report, monitor.subWork(1))) {
				monitor.end(true, null, null);
				return;
			}

			repairMailbox(domainUid, report, monitor.subWork(1));

			monitor.end(true, null, null);
		}

		protected boolean isEntrySupported(String domainUid, DirEntry entry, DiagnosticReport report,
				IServerTaskMonitor monitor) {
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
			if (mailbox == null || mailbox.value == null) {
				logger.error("Mailbox {} not found in database", entry.entryUid);
				monitor.log(String.format("Mailbox %s not found in database", entry.entryUid));
				monitor.end(false, null, null);

				report.ko(identifier, String.format("Mailbox %s not found in database", entry.entryUid));
				return false;
			}

			if (mailbox.value.routing != Mailbox.Routing.internal) {
				monitor.log(String.format("Mailbox %s not managed, nothing to do", mailboxToString(domainUid)));
				monitor.end(true, null, null);

				report.ok(identifier,
						String.format("Mailbox %s not managed, nothing to do", mailboxToString(domainUid)));
				return false;
			}

			monitor.end(true, null, null);
			return true;
		}

		protected String mailboxToString(String domainUid) {
			return String.format("%s@%s (uid: %s)", (mailbox.value == null ? mailbox.uid : mailbox.value.name),
					domainUid, mailbox.uid);
		}

		protected abstract void checkMailbox(String domainUid, DiagnosticReport report, IServerTaskMonitor monitor);

		protected abstract void repairMailbox(String domainUid, DiagnosticReport report, IServerTaskMonitor monitor);
	}
}
