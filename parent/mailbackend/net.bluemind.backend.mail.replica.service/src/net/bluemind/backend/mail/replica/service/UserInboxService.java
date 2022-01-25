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
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.backend.mail.replica.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.backend.mail.api.IMailboxItems;
import net.bluemind.backend.mail.api.IUserInbox;
import net.bluemind.core.container.model.ItemFlag;
import net.bluemind.core.container.model.ItemFlagFilter;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.rest.BmContext;
import net.bluemind.mailbox.api.IMailboxAclUids;

public final class UserInboxService implements IUserInbox {

	private static final Logger logger = LoggerFactory.getLogger(UserInboxService.class);
	private final IMailboxItems itemsApi;
	private final String domainUid;
	private final String userUid;
	private final RBACManager rbac;

	public UserInboxService(BmContext context, String domainUid, String userUid, String inboxUid) {
		this.rbac = RBACManager.forContext(context).forContainer(IMailboxAclUids.uidForMailbox(userUid));
		this.itemsApi = context.provider().instance(IMailboxItems.class, inboxUid);
		this.domainUid = domainUid;
		this.userUid = userUid;
		logger.info("Inbox {} service for {}@{}", inboxUid, this.userUid, this.domainUid);
	}

	@Override
	public Integer unseen() {
		rbac.check(Verb.Read.name());
		return (int) itemsApi.count(ItemFlagFilter.create().mustNot(ItemFlag.Deleted, ItemFlag.Seen)).total;
	}

}
