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
package net.bluemind.mailbox.identity.service.repair;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;

import net.bluemind.core.api.report.DiagnosticReport;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.MaintenanceOperation;
import net.bluemind.directory.service.IDirEntryRepairSupport;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.identity.api.IdentityDescription;
import net.bluemind.user.api.IInternalUserMailIdentities;
import net.bluemind.user.api.IUserMailIdentities;

public class DefaultIdentityRepair implements IDirEntryRepairSupport {
	public static final MaintenanceOperation identityRepair = MaintenanceOperation.create(IUserMailIdentities.REPAIR_OP,
			"Create missing default identity for domain users");
	private static final Logger logger = LoggerFactory.getLogger(DefaultIdentityRepair.class);

	public DefaultIdentityRepair(BmContext context) {
		logger.debug("Repair default identity with ctx {}", context);
	}

	public static class RepairFactory implements IDirEntryRepairSupport.Factory {
		@Override
		public IDirEntryRepairSupport create(BmContext context) {
			return new DefaultIdentityRepair(context);
		}
	}

	@Override
	public Set<MaintenanceOperation> availableOperations(Kind kind) {
		if (kind == Kind.USER) {
			return ImmutableSet.of(identityRepair);
		} else {
			return Collections.emptySet();
		}
	}

	@Override
	public Set<InternalMaintenanceOperation> ops(Kind kind) {
		if (kind == Kind.USER) {
			return ImmutableSet.of(new DefaultIdentityRepairImpl());
		} else {
			return Collections.emptySet();
		}
	}

	private static class DefaultIdentityRepairImpl extends InternalMaintenanceOperation {

		public DefaultIdentityRepairImpl() {
			super(identityRepair.identifier, null, null, 1);
		}

		@Override
		public void check(String domainUid, DirEntry entry, DiagnosticReport report, IServerTaskMonitor monitor) {
			verifyUserDefaultIdentity(domainUid, entry, report, monitor, () -> {
				monitor.log("Repairing " + domainUid + " default user identity");
				logger.info("Repairing {} default user identity", domainUid);
			});
		}

		@Override
		public void repair(String domainUid, DirEntry entry, DiagnosticReport report, IServerTaskMonitor monitor) {
			verifyUserDefaultIdentity(domainUid, entry, report, monitor, () -> {
				monitor.log("Verifying " + domainUid + " default user identity");
				logger.info("Verifying {} default user identity", domainUid);

				createUserDefaultIdentity(domainUid, entry, monitor);
			});
		}

		private void verifyUserDefaultIdentity(String domainUid, DirEntry entry, DiagnosticReport report,
				IServerTaskMonitor monitor, Runnable maintenance) {
			List<IdentityDescription> identities = getIdentities(domainUid, entry.entryUid);
			if (identities.isEmpty() || identities.stream().noneMatch(i -> i.isDefault.booleanValue())) {
				monitor.log("Default identity missing for " + entry.displayName);
				logger.info("Default identity missing for {}", entry.displayName);
				maintenance.run();
			}
		}

		private void createUserDefaultIdentity(String domainUid, DirEntry entry, IServerTaskMonitor monitor) {
			IInternalUserMailIdentities userMailIdentities = ServerSideServiceProvider
					.getProvider(SecurityContext.SYSTEM)
					.instance(IInternalUserMailIdentities.class, domainUid, entry.entryUid);

			List<IdentityDescription> identities = getIdentities(domainUid, entry.entryUid);
			if (identities.isEmpty()) {
				ItemValue<Mailbox> mailboxItem = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
						.instance(IMailboxes.class, domainUid).getComplete(entry.entryUid);

				userMailIdentities.createDefaultIdentity(mailboxItem, entry);
				monitor.log("Default identity created for " + entry.displayName);
				logger.info("Default identity created for {}", entry.displayName);
			} else {
				userMailIdentities.setDefault(identities.get(0).id);
				monitor.log("Default identity updated for " + entry.displayName);
				logger.info("Default identity updated for {}", entry.displayName);
			}
		}

		private List<IdentityDescription> getIdentities(String domainUid, String mboxUid) {
			return ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
					.instance(IInternalUserMailIdentities.class, domainUid, mboxUid).getIdentities();
		}
	}
}
