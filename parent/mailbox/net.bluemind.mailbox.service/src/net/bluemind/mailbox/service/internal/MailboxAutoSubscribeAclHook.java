/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
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
package net.bluemind.mailbox.service.internal;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.IContainerManagement;
import net.bluemind.core.container.hooks.IAclHook;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.rest.BmContext;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.group.api.IGroup;
import net.bluemind.group.api.Member;
import net.bluemind.mailbox.api.IMailboxAclUids;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.IUserSubscription;

public class MailboxAutoSubscribeAclHook implements IAclHook {

	private static Logger logger = LoggerFactory.getLogger(MailboxAutoSubscribeAclHook.class);

	@Override
	public void onAclChanged(BmContext context, ContainerDescriptor container, List<AccessControlEntry> previous,
			List<AccessControlEntry> current) {

		if (!IMailboxAclUids.TYPE.equals(container.type)) {
			return;
		}

		try {
			handleChanges(context, container, current);
		} catch (Exception e) {
			logger.error("error during auto subscribe to mailbox", e);
		}
	}

	private void handleChanges(BmContext context, ContainerDescriptor container, List<AccessControlEntry> current)
			throws ServerFault {

		if (!IMailboxAclUids.TYPE.equals(container.type)) {
			return;
		}

		logger.debug("acl for mailbox container has changed");

		IContainerManagement cmgmt = context.su().provider().instance(IContainerManagement.class, container.uid);

		List<AccessControlEntry> acl = cmgmt.getAccessControlList();
		Set<String> users = asUsers(context, container.domainUid, acl);

		List<String> subs = cmgmt.subscribers();
		IUserSubscription userSubService = context.su().provider().instance(IUserSubscription.class,
				container.domainUid);

		for (String sub : subs) {
			if (!users.contains(sub)) {
				logger.debug("auto unsubscribe {} to {}", sub, container.uid);
				userSubService.unsubscribe(sub, Arrays.asList(container.uid));
			}
		}

	}

	private Set<String> asUsers(BmContext context, String domainUid, List<AccessControlEntry> acl) throws ServerFault {

		IDirectory dir = context.provider().instance(IDirectory.class, domainUid);
		IGroup groups = context.su().provider().instance(IGroup.class, domainUid);
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
		if (entries.stream().anyMatch((d) -> {
			return d.kind == Kind.DOMAIN;
		})) {
			// public sharing
			ret.addAll(context.provider().instance(IUser.class, domainUid).allUids());
		} else {

			for (DirEntry entry : entries) {
				if (entry.kind == Kind.USER) {
					ret.add(entry.entryUid);
				} else if (entry.kind == Kind.GROUP) {
					for (Member member : groups.getExpandedUserMembers(entry.entryUid)) {
						ret.add(member.uid);
					}
				}
			}
		}
		return ret;
	}

}
