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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.api.report.DiagnosticReport;
import net.bluemind.core.container.api.ContainerSubscription;
import net.bluemind.core.container.api.IContainerManagement;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.group.api.IGroup;
import net.bluemind.mailbox.api.IMailboxAclUids;
import net.bluemind.mailbox.service.internal.repair.MailboxRepairSupport.MailboxMaintenanceOperation;
import net.bluemind.user.api.IUserSubscription;

public class MailboxAutoSubscriptionsMaintenanceOperation extends MailboxMaintenanceOperation {
	private static final Logger logger = LoggerFactory.getLogger(MailboxAutoSubscriptionsMaintenanceOperation.class);

	private static final String MAINTENANCE_OPERATION_ID = DiagnosticReportCheckId.mailboxSubscription.name();

	public MailboxAutoSubscriptionsMaintenanceOperation(BmContext context) {
		super(context, MAINTENANCE_OPERATION_ID);
	}

	@Override
	protected void checkMailbox(String domainUid, DiagnosticReport report, IServerTaskMonitor monitor) {
		checkAndRepair(false, domainUid, report, monitor);
	}

	@Override
	protected void repairMailbox(String domainUid, DiagnosticReport report, IServerTaskMonitor monitor) {
		checkAndRepair(true, domainUid, report, monitor);
	}

	private void checkAndRepair(boolean repair, String domainUid, DiagnosticReport report, IServerTaskMonitor monitor) {
		monitor.begin(2, String.format("Check mailbox auto-subscriptions for mailbox %s, uid: %s",
				mailboxToString(domainUid), mailbox.uid));

		String containerUid = IMailboxAclUids.uidForMailbox(mailbox.uid);
		IContainerManagement cmgmt = context.provider().instance(IContainerManagement.class, containerUid);

		List<AccessControlEntry> acl = cmgmt.getAccessControlList();
		Set<String> users = asUsers(domainUid, acl);

		List<String> subs = cmgmt.subscribers();

		IUserSubscription userSubService = context.su().provider().instance(IUserSubscription.class, domainUid);

		boolean ok = true;

		monitor.progress(1, String.format("Checking mailbox %s users subscription", mailboxToString(domainUid)));
		for (String userUid : users) {
			if (!subs.contains(userUid)) {
				logger.error("Mailbox {}: subscription of {} not present", mailboxToString(domainUid), userUid);

				if (!repair) {
					ok = false;

					report.ko(MAINTENANCE_OPERATION_ID, String.format("Mailbox %s: subscription of %s not present",
							mailboxToString(domainUid), userUid));
					monitor.log(String.format("subscription of %s not present", userUid));
				} else {
					userSubService.subscribe(userUid, Arrays.asList(ContainerSubscription.create(containerUid, false)));

					report.ok(MAINTENANCE_OPERATION_ID,
							String.format("Mailbox %s: subscription of %s added", mailboxToString(domainUid), userUid));
					monitor.log(String.format("subscription of %s added", userUid));
				}
			}
		}

		for (String sub : subs) {
			if (!users.contains(sub)) {
				if (!repair) {
					ok = false;

					report.ko(MAINTENANCE_OPERATION_ID, String.format("Mailbox %s: subscription of %s must be removed",
							mailboxToString(domainUid), containerUid));
					monitor.log(String.format("subscription of %s must be removed", containerUid));
				} else {
					userSubService.unsubscribe(sub, Arrays.asList(containerUid));

					report.ko(MAINTENANCE_OPERATION_ID, String.format("Mailbox %s: subscription of %s removed",
							mailboxToString(domainUid), containerUid));
					monitor.log(String.format("subscription of %s removed", containerUid));
				}
			}
		}

		monitor.end(ok, null, null);
	}

	private Set<String> asUsers(String domainUid, List<AccessControlEntry> acl) throws ServerFault {
		IDirectory dir = context.provider().instance(IDirectory.class, domainUid);
		IGroup groups = context.provider().instance(IGroup.class, domainUid);
		Set<DirEntry> entries = new HashSet<>();
		for (AccessControlEntry ace : acl) {
			DirEntry entry = dir.findByEntryUid(ace.subject);
			if (entry != null) {
				entries.add(entry);
			} else {
				logger.warn("did not found entry for {} in domain {}", ace.subject, domainUid);
			}
		}

		Set<String> ret = new HashSet<>(entries.size());
		for (DirEntry entry : entries) {
			if (entry.kind == Kind.USER) {
				ret.add(entry.entryUid);
			} else if (entry.kind == Kind.GROUP) {
				ret.addAll(groups.getExpandedUserMembers(entry.entryUid).stream().map(m -> m.uid)
						.collect(Collectors.toList()));
			}
		}

		return ret;
	}
}
