/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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
package net.bluemind.backend.cyrus.annotationdb;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.collect.ImmutableSet;

import net.bluemind.backend.cyrus.annotationdb.ConversationSync.CyrusConversationDbInitException;
import net.bluemind.core.api.report.DiagnosticReport;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.MaintenanceOperation;
import net.bluemind.directory.service.IDirEntryRepairSupport;

public class ConversationRepair implements IDirEntryRepairSupport {

	private static final String REPAIR_OP_ID = "conversations";
	private final BmContext context;

	public static final MaintenanceOperation conversationOp = MaintenanceOperation
			.create(ConversationRepair.REPAIR_OP_ID, "Resync user conversations");

	public static class Factory implements IDirEntryRepairSupport.Factory {
		@Override
		public IDirEntryRepairSupport create(BmContext context) {
			return new ConversationRepair(context);
		}
	}

	public ConversationRepair(BmContext context) {
		this.context = context;
	}

	private static class ConversationMaintenance extends InternalMaintenanceOperation {
		private final BmContext context;

		public ConversationMaintenance(BmContext ctx) {
			super(conversationOp.identifier, null, null, 1);
			this.context = ctx;
		}

		@Override
		public void check(String domainUid, DirEntry entry, DiagnosticReport report, IServerTaskMonitor monitor) {
			AtomicInteger updatedConversations = new AtomicInteger(0);

			try {
				new ConversationSync(this.context, "ConversationRepair",
						(uid, service, conversation) -> updatedConversations.incrementAndGet(),
						(service, conversation) -> updatedConversations.incrementAndGet()).execute(domainUid,
								entry.entryUid, monitor, report);
				String msg = "Resync conversations of " + entry.entryUid + "@" + domainUid + " would touch "
						+ updatedConversations.get() + " conversations";
				report.ok(entry.entryUid, msg);
				monitor.end(true, msg, "");
			} catch (CyrusConversationDbInitException e) {
				String msg = "[DRY] Cannot resync conversations of " + entry.entryUid + "@" + domainUid + ": "
						+ e.getMessage();
				report.ko(entry.entryUid, msg);
				monitor.end(false, msg, "");
			}
		}

		@Override
		public void repair(String domainUid, DirEntry entry, DiagnosticReport report, IServerTaskMonitor monitor) {
			try {
				new ConversationSync(this.context, "ConversationRepair").execute(domainUid, entry.entryUid, monitor,
						report);
				String msg = "Resync conversations of " + entry.entryUid + "@" + domainUid;
				report.ok(entry.entryUid, msg);
				monitor.end(true, msg, "");
			} catch (CyrusConversationDbInitException e) {
				String msg = "Cannot resync conversations of " + entry.entryUid + "@" + domainUid + ": "
						+ e.getMessage();
				report.ko(entry.entryUid, msg);
				monitor.end(false, msg, "");
			}
		}

	}

	@Override
	public Set<MaintenanceOperation> availableOperations(Kind kind) {
		if (supportedKind(kind)) {
			return ImmutableSet.of(conversationOp);
		} else {
			return Collections.emptySet();
		}
	}

	@Override
	public Set<InternalMaintenanceOperation> ops(Kind kind) {
		if (supportedKind(kind)) {
			return ImmutableSet.of(new ConversationMaintenance(context));
		} else {
			return Collections.emptySet();
		}
	}

	private boolean supportedKind(Kind kind) {
		return kind == Kind.USER;
	}

}
